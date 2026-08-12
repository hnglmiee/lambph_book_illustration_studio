package com.hoanglam.bis.gemini.implement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoanglam.bis.dto.*;
import com.hoanglam.bis.enums.ErrorCode;
import com.hoanglam.bis.enums.PipelineStep;
import com.hoanglam.bis.enums.ProjectStatus;
import com.hoanglam.bis.enums.StepState;
import com.hoanglam.bis.exceptions.ApiException;
import com.hoanglam.bis.gemini.dto.*;
import com.hoanglam.bis.gemini.dto.GeminiFile;
import com.hoanglam.bis.model.Project;
import com.hoanglam.bis.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiPipelineService {

    private final ProjectRepository projectRepository;
    private final GeminiFileClient geminiFileClient;
    private final GeminiInteractionClient geminiInteractionClient;
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final int MAX_CHARACTERS = 2;

    private static final String TEXT_MODEL = "gemini-3.6-flash";

    /**
     * Entry point gọi từ Controller — validate + lock + trả về ngay,
     * việc gọi Gemini thật chạy nền qua @Async.
     */
    @Transactional
    public void startStyleStep(UUID projectId, String userStyle) {
        Project project = loadForUpdate(projectId);

        if (project.getStatus() != ProjectStatus.CREATED) {
            throw new ApiException(ErrorCode.INVALID_STEP_ORDER,
                    "Style step can only run when project status is CREATED", 400);
        }
        if (project.getStepState() == StepState.RUNNING) {
            throw new ApiException(ErrorCode.STEP_ALREADY_RUNNING,
                    "Style step is already running", 409);
        }

        project.setStepState(StepState.RUNNING);
        project.setStepStartedAt(OffsetDateTime.now());
        project.setStepFailureReason(null);

        try {
            projectRepository.saveAndFlush(project);
        } catch (ObjectOptimisticLockingFailureException e) {
            // Race condition: request khác vừa đổi state trước ta -> báo đang chạy, không tự retry
            throw new ApiException(ErrorCode.STEP_ALREADY_RUNNING,
                    "Style step is already running (concurrent request detected)", 409);
        }

        runStyleStepAsync(projectId, userStyle);
    }

    @Async("geminiTaskExecutor")
    protected void runStyleStepAsync(UUID projectId, String userStyle) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new IllegalStateException("Project vanished mid-run: " + projectId));

            // 1. Upload sách lên Gemini nếu chưa có (chỉ làm 1 lần duy nhất cho cả project)
            if (project.getBookFileUri() == null) {
                uploadBookAndInitInteraction(project);
            }

            // 2. Gọi bước Style, nối tiếp interaction hiện tại
            String stylePrompt = buildStylePrompt(userStyle);
            CreateInteractionRequest request = CreateInteractionRequest.builder()
                    .model(TEXT_MODEL)
                    .input(stylePrompt)
                    .previousInteractionId(project.getLastTextInteractionId())
                    .build();

            GeminiInteraction interaction = geminiInteractionClient.createInteraction(request);
            String styleText = extractTextOutput(interaction);

            if (styleText == null || styleText.isBlank()) {
                throw new IllegalStateException("Gemini returned empty style text");
            }

            // 3. Lưu kết quả, đánh dấu bước hoàn tất
            project.setStyle(styleText);
            project.setLastTextInteractionId(interaction.getId());
            project.setStatus(ProjectStatus.STYLE_SET);
            project.setStepState(StepState.IDLE);
            project.setStepStartedAt(null);
            projectRepository.save(project);

            log.info("Style step completed for project {}", projectId);

        } catch (Exception e) {
            log.error("Style step failed for project {}", projectId, e);
            markStepFailed(projectId, e.getMessage());
        }
    }

    /**
     * Upload sách + tạo interaction gốc (book_interaction) — chỉ chạy đúng 1 lần cho toàn project,
     * mọi bước sau đều previous_interaction_id nối tiếp từ đây, không gửi lại full text sách.
     */
    private void uploadBookAndInitInteraction(Project project) {
        String bookText = readBookTextFromDisk(project.getBookTextFilePath());
        GeminiFile file = geminiFileClient.uploadTextFile(
                bookText.getBytes(StandardCharsets.UTF_8),
                "book-" + project.getId()
        );
        project.setBookFileUri(file.getName()); // lưu "files/abc-123" — định danh ổn định

        List<InputContentPart> input = List.of(
                InputContentPart.document(file.getUri(), "text/plain"),
                InputContentPart.text(
                        "This is the book we will work with for the rest of this conversation. " +
                                "Acknowledge you have read it, then wait for further instructions.")
        );

        CreateInteractionRequest request = CreateInteractionRequest.builder()
                .model(TEXT_MODEL)
                .input(input)
                .build();

        GeminiInteraction bookInteraction = geminiInteractionClient.createInteraction(request);
        project.setLastTextInteractionId(bookInteraction.getId());
        projectRepository.save(project);
    }

    private String buildStylePrompt(String userStyle) {
        if (userStyle == null || userStyle.isBlank()) {
            return "Can you define an art style that would fit the story but with a twist? " +
                    "Just give us the prompt for the art style that will be added to future prompts.";
        }
        return "The art style will be: \"" + userStyle + "\". Keep that in mind when generating " +
                "future prompts. Keep quiet for now, instructions will follow.";
    }

    private String extractTextOutput(GeminiInteraction interaction) {
        if (interaction.getSteps() == null) return null;
        return interaction.getSteps().stream()
                .filter(step -> "model_output".equals(step.getType()))
                .filter(step -> step.getContent() != null)
                .flatMap(step -> step.getContent().stream())
                .filter(content -> "text".equals(content.getType()))
                .map(GeminiContent::getText)
                .reduce("", (a, b) -> a + b);
    }

    private String readBookTextFromDisk(String filePath) {
        try {
            return Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read book text from disk", e);
        }
    }

    @Transactional
    protected void markStepFailed(UUID projectId, String reason) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) return;
        project.setStepState(StepState.FAILED);
        project.setStepFailureReason(reason);
        projectRepository.save(project);
    }

    private Project loadForUpdate(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND, "Project not found", 404));
    }



    @Transactional
    public void startCharactersStep(UUID projectId) {
        Project project = loadForUpdate(projectId);

        if (project.getStatus() != ProjectStatus.STYLE_SET) {
            throw new ApiException(ErrorCode.INVALID_STEP_ORDER,
                    "Characters step requires Style to be completed first", 400);
        }
        if (project.getStepState() == StepState.RUNNING) {
            throw new ApiException(ErrorCode.STEP_ALREADY_RUNNING,
                    "Characters step is already running", 409);
        }

        project.setStepState(StepState.RUNNING);
        project.setStepStartedAt(OffsetDateTime.now());
        project.setStepFailureReason(null);

        try {
            projectRepository.saveAndFlush(project);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ApiException(ErrorCode.STEP_ALREADY_RUNNING,
                    "Characters step is already running (concurrent request detected)", 409);
        }

        runCharactersStepAsync(projectId);
    }

    @Async("geminiTaskExecutor")
    protected void runCharactersStepAsync(UUID projectId) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new IllegalStateException("Project vanished mid-run: " + projectId));

            Map<String, Object> schema = Map.of(
                    "type", "array",
                    "items", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "name", Map.of("type", "string"),
                                    "prompt", Map.of("type", "string")
                            ),
                            "required", List.of("name", "prompt")
                    )
            );

            ResponseFormat responseFormat = ResponseFormat.builder()
                    .type("text")
                    .mimeType("application/json")
                    .schema(schema)
                    .build();

            String prompt = "Can you describe the main characters (only the adults) and prepare a prompt " +
                    "describing them with as much detail as possible (use descriptions from the book) so " +
                    "Nano Banana can generate images of them? List at most " + MAX_CHARACTERS +
                    " main characters. Each prompt should be at least 50 words.";

            CreateInteractionRequest request = CreateInteractionRequest.builder()
                    .model(TEXT_MODEL)
                    .input(prompt)
                    .previousInteractionId(project.getLastTextInteractionId())
                    .responseFormat(responseFormat)
                    .build();

            GeminiInteraction interaction = geminiInteractionClient.createInteraction(request);
            String jsonText = extractTextOutput(interaction);

            List<CharacterPromptDto> parsed = JSON_MAPPER.readValue(
                    jsonText, new TypeReference<List<CharacterPromptDto>>() {});

            // Enforce cap SERVER-SIDE — dù Gemini trả nhiều hơn, chỉ lấy đúng MAX_CHARACTERS đầu tiên
            List<CharacterPromptDto> capped = parsed.stream()
                    .limit(MAX_CHARACTERS)
                    .toList();

            if (capped.isEmpty()) {
                throw new IllegalStateException("Gemini returned no characters");
            }

            List<com.hoanglam.bis.model.Character> entities = new ArrayList<>();
            for (int i = 0; i < capped.size(); i++) {
                CharacterPromptDto dto = capped.get(i);
                com.hoanglam.bis.model.Character character = new com.hoanglam.bis.model.Character();
                character.setProject(project);
                character.setPosition(i);
                character.setName(dto.getName());
                character.setPrompt(dto.getPrompt());
                character.setPortraitReady(false);
                entities.add(character);
            }

            project.getCharacters().clear();
            project.getCharacters().addAll(entities);
            project.setLastTextInteractionId(interaction.getId());
            project.setStatus(ProjectStatus.CHARACTERS_GENERATED);
            project.setStepState(StepState.IDLE);
            project.setStepStartedAt(null);
            projectRepository.save(project);

            log.info("Characters step completed for project {} with {} characters", projectId, entities.size());

        } catch (Exception e) {
            log.error("Characters step failed for project {}", projectId, e);
            markStepFailed(projectId, e.getMessage());
        }
    }
}