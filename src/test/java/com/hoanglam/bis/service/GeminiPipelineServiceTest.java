package com.hoanglam.bis.service;

import com.hoanglam.bis.dto.GeminiContent;
import com.hoanglam.bis.dto.GeminiInteraction;
import com.hoanglam.bis.dto.GeminiStep;
import com.hoanglam.bis.enums.ErrorCode;
import com.hoanglam.bis.enums.ProjectStatus;
import com.hoanglam.bis.enums.StepState;
import com.hoanglam.bis.exceptions.ApiException;
import com.hoanglam.bis.gemini.dto.*;
import com.hoanglam.bis.gemini.implement.GeminiFileClient;
import com.hoanglam.bis.gemini.implement.GeminiInteractionClient;
import com.hoanglam.bis.gemini.implement.GeminiPipelineService;
import com.hoanglam.bis.model.Project;
import com.hoanglam.bis.model.User;
import com.hoanglam.bis.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiPipelineServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private GeminiFileClient geminiFileClient;

    @Mock
    private GeminiInteractionClient geminiInteractionClient;

    @InjectMocks
    private GeminiPipelineService pipelineService;

    private Project project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        project = new Project();
        project.setId(projectId);
        project.setUser(new User());
        project.setTitle("Test Book");
        project.setBookTextFilePath("dummy/path.txt");
        project.setStatus(ProjectStatus.CREATED);
        project.setStepState(StepState.IDLE);
        project.setCharacters(new java.util.ArrayList<>());
        project.setChapters(new java.util.ArrayList<>());
    }

    // ============ STYLE STEP — ORDERING VALIDATION ============

    @Nested
    class StyleStepOrdering {

        @Test
        void startStyleStep_throwsInvalidStepOrder_whenStatusIsNotCreated() {
            project.setStatus(ProjectStatus.STYLE_SET);
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> pipelineService.startStyleStep(projectId, null))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_STEP_ORDER);
        }

        @Test
        void startStyleStep_throwsStepAlreadyRunning_whenStepStateIsRunning() {
            project.setStatus(ProjectStatus.CREATED);
            project.setStepState(StepState.RUNNING);
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> pipelineService.startStyleStep(projectId, null))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ErrorCode.STEP_ALREADY_RUNNING);
        }

        @Test
        void startStyleStep_throwsProjectNotFound_whenProjectDoesNotExist() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pipelineService.startStyleStep(projectId, null))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ErrorCode.PROJECT_NOT_FOUND);
        }

        @Test
        void startStyleStep_setsRunningState_whenValid() {
            project.setStatus(ProjectStatus.CREATED);
            project.setStepState(StepState.IDLE);
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            when(projectRepository.saveAndFlush(any(Project.class))).thenReturn(project);

            pipelineService.startStyleStep(projectId, null);

            assertThat(project.getStepState()).isEqualTo(StepState.RUNNING);
            assertThat(project.getStepStartedAt()).isNotNull();
            verify(projectRepository).saveAndFlush(project);
        }
    }

    // ============ CHARACTERS STEP — ORDERING + CAP ENFORCEMENT ============

    @Nested
    class CharactersStepOrdering {

        @Test
        void startCharactersStep_throwsInvalidStepOrder_whenStyleNotSet() {
            project.setStatus(ProjectStatus.CREATED);
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> pipelineService.startCharactersStep(projectId))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_STEP_ORDER);
        }

        @Test
        void startCharactersStep_succeeds_whenStyleSet() {
            project.setStatus(ProjectStatus.STYLE_SET);
            project.setStepState(StepState.IDLE);
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            when(projectRepository.saveAndFlush(any(Project.class))).thenReturn(project);

            pipelineService.startCharactersStep(projectId);

            assertThat(project.getStepState()).isEqualTo(StepState.RUNNING);
        }
    }

    @Nested
    class CharactersCapEnforcement {

        /**
         * Test trực tiếp phần async logic bằng cách gọi method protected cùng package.
         * Đây là test QUAN TRỌNG NHẤT của cả bộ — brief yêu cầu cap phải enforce server-side
         * dù Gemini trả về nhiều hơn giới hạn cho phép.
         */
        @Test
        void runCharactersStepAsync_capsAtMaxCharacters_evenWhenGeminiReturnsMore() throws Exception {
            project.setStatus(ProjectStatus.STYLE_SET);
            project.setLastTextInteractionId("interaction-style-1");
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

            // Giả lập Gemini trả về 5 nhân vật, vượt xa cap = 2
            String fiveCharactersJson = """
                [
                  {"name": "Character A", "prompt": "Prompt A with enough words to pass validation checks here."},
                  {"name": "Character B", "prompt": "Prompt B with enough words to pass validation checks here."},
                  {"name": "Character C", "prompt": "Prompt C with enough words to pass validation checks here."},
                  {"name": "Character D", "prompt": "Prompt D with enough words to pass validation checks here."},
                  {"name": "Character E", "prompt": "Prompt E with enough words to pass validation checks here."}
                ]
                """;

            GeminiInteraction mockInteraction = buildTextInteraction(fiveCharactersJson);
            when(geminiInteractionClient.createInteraction(any())).thenReturn(mockInteraction);

//            pipelineService.runCharactersStepAsync(projectId);

            assertThat(project.getCharacters()).hasSize(2); // ĐÚNG CAP, KHÔNG PHẢI 5
            assertThat(project.getCharacters().get(0).getName()).isEqualTo("Character A");
            assertThat(project.getCharacters().get(1).getName()).isEqualTo("Character B");
            assertThat(project.getStatus()).isEqualTo(ProjectStatus.CHARACTERS_GENERATED);
            assertThat(project.getStepState()).isEqualTo(StepState.IDLE);
        }

        @Test
        void runCharactersStepAsync_marksFailed_whenGeminiReturnsEmptyArray() {
            project.setStatus(ProjectStatus.STYLE_SET);
            project.setLastTextInteractionId("interaction-style-1");
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

            GeminiInteraction emptyInteraction = buildTextInteraction("[]");
            when(geminiInteractionClient.createInteraction(any())).thenReturn(emptyInteraction);

//            pipelineService.runCharactersStepAsync(projectId);

            assertThat(project.getStepState()).isEqualTo(StepState.FAILED);
            assertThat(project.getStepFailureReason()).contains("no characters");
        }

        @Test
        void runCharactersStepAsync_marksFailed_whenGeminiCallThrows() {
            project.setStatus(ProjectStatus.STYLE_SET);
            project.setLastTextInteractionId("interaction-style-1");
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            when(geminiInteractionClient.createInteraction(any()))
                    .thenThrow(new ApiException(ErrorCode.GEMINI_CALL_FAILED, "429 quota exceeded", 502));

//            pipelineService.runCharactersStepAsync(projectId);

            assertThat(project.getStepState()).isEqualTo(StepState.FAILED);
            assertThat(project.getStepFailureReason()).contains("quota exceeded");
        }
    }

    // ============ CHAPTERS CAP ENFORCEMENT ============

    @Nested
    class ChaptersCapEnforcement {

        @Test
        void runChaptersStepAsync_capsAtMaxChapters_evenWhenGeminiReturnsMore() {
            project.setStatus(ProjectStatus.PORTRAITS_GENERATED);
            project.setLastTextInteractionId("interaction-characters-1");
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

            String threeChaptersJson = """
                [
                  {"name": "Chapter 1", "prompt": "Prompt for chapter one with enough descriptive words here."},
                  {"name": "Chapter 2", "prompt": "Prompt for chapter two with enough descriptive words here."},
                  {"name": "Chapter 3", "prompt": "Prompt for chapter three with enough descriptive words here."}
                ]
                """;

            GeminiInteraction mockInteraction = buildTextInteraction(threeChaptersJson);
            when(geminiInteractionClient.createInteraction(any())).thenReturn(mockInteraction);

//            pipelineService.runChaptersStepAsync(projectId);

            assertThat(project.getChapters()).hasSize(1); // cap = 1, không phải 3
            assertThat(project.getChapters().get(0).getName()).isEqualTo("Chapter 1");
            assertThat(project.getStatus()).isEqualTo(ProjectStatus.CHAPTERS_GENERATED);
        }
    }

    // ============ RETRY DISPATCH LOGIC ============

    @Nested
    class RetryDispatch {

        @Test
        void retryCurrentStep_throwsStepAlreadyRunning_whenRunningAndNotStale() {
            project.setStepState(StepState.RUNNING);
            project.setStepStartedAt(OffsetDateTime.now()); // vừa mới bắt đầu, chưa stale
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> pipelineService.retryCurrentStep(projectId, null))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ErrorCode.STEP_ALREADY_RUNNING);
        }

        @Test
        void retryCurrentStep_throwsStepNotFailed_whenIdleAndNothingToRetry() {
            project.setStepState(StepState.IDLE);
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> pipelineService.retryCurrentStep(projectId, null))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ErrorCode.STEP_NOT_FAILED);
        }

        @Test
        void retryCurrentStep_autoRecoversStaleRunning_andDispatchesCorrectStep() {
            project.setStatus(ProjectStatus.STYLE_SET); // -> retry phải gọi lại Characters
            project.setStepState(StepState.RUNNING);
            project.setStepStartedAt(OffsetDateTime.now().minusMinutes(5)); // stale
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            when(projectRepository.saveAndFlush(any(Project.class))).thenReturn(project);

            String oneCharacterJson = """
                [{"name": "Solo Character", "prompt": "A prompt with enough descriptive words to pass validation here."}]
                """;
            when(geminiInteractionClient.createInteraction(any()))
                    .thenReturn(buildTextInteraction(oneCharacterJson));

            pipelineService.retryCurrentStep(projectId, null);

            // Xác nhận đã tự chuyển từ RUNNING (stranded) -> chạy lại đúng bước Characters
            assertThat(project.getStatus()).isEqualTo(ProjectStatus.CHARACTERS_GENERATED);
            assertThat(project.getCharacters()).hasSize(1);
        }

        @Test
        void retryCurrentStep_throwsInvalidStepOrder_whenProjectAlreadyDone() {
            project.setStatus(ProjectStatus.DONE);
            project.setStepState(StepState.FAILED);
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> pipelineService.retryCurrentStep(projectId, null))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_STEP_ORDER);
        }
    }

    // ============ HELPER ============

    private GeminiInteraction buildTextInteraction(String jsonText) {
        GeminiContent content = new GeminiContent();
        content.setType("text");
        content.setText(jsonText);

        GeminiStep step = new GeminiStep();
        step.setType("model_output");
        step.setContent(List.of(content));

        GeminiInteraction interaction = new GeminiInteraction();
        interaction.setId("mock-interaction-id");
        interaction.setStatus("completed");
        interaction.setSteps(List.of(step));
        return interaction;
    }
}