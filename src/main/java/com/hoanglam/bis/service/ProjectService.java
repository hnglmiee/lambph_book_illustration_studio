package com.hoanglam.bis.service;

import com.hoanglam.bis.config.StepStaleChecker;
import com.hoanglam.bis.dto.ChapterResponse;
import com.hoanglam.bis.dto.CharacterResponse;
import com.hoanglam.bis.dto.CreateProjectRequest;
import com.hoanglam.bis.dto.ProjectDetailResponse;
import com.hoanglam.bis.dto.ProjectSummaryResponse;
import com.hoanglam.bis.enums.ErrorCode;
import com.hoanglam.bis.exceptions.ApiException;
import com.hoanglam.bis.model.Chapter;
import com.hoanglam.bis.model.Character;
import com.hoanglam.bis.model.Project;
import com.hoanglam.bis.enums.ProjectStatus;
import com.hoanglam.bis.enums.StepState;
import com.hoanglam.bis.model.User;
import com.hoanglam.bis.repository.ProjectRepository;
import com.hoanglam.bis.repository.UserRepository;
import com.hoanglam.bis.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    private static final String BOOKS_DIR = "./data/books";

    @Transactional
    public ProjectSummaryResponse createProject(CreateProjectRequest request, MultipartFile bookFile) {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, "User not found", 404));

        String bookContent = resolveBookContent(request, bookFile);
        if (bookContent == null || bookContent.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "Book text is required — paste text or upload a .txt file", 400);
        }

        Project project = new Project();
        project.setUser(user);
        project.setTitle(request.getTitle());
        project.setStatus(ProjectStatus.CREATED);
        project.setStepState(StepState.IDLE);
        project.setBookTextFilePath("PENDING"); // placeholder tạm, cột NOT NULL

        Project saved = projectRepository.save(project);

        String filePath = saveBookTextToDisk(saved.getId(), bookContent);
        saved.setBookTextFilePath(filePath);
        saved = projectRepository.save(saved);

        return toSummaryResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> listMyProjects() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return projectRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getProjectDetail(UUID projectId) {
        Project project = findOwnedProject(projectId);
        String bookText = readBookTextFromDisk(project.getBookTextFilePath());
        return toDetailResponse(project, bookText);
    }

    // --- helpers ---

    private Project findOwnedProject(UUID projectId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND, "Project not found", 404));

        if (!project.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.PROJECT_NOT_FOUND, "Project not found", 404);
        }
        return project;
    }

    private String resolveBookContent(CreateProjectRequest request, MultipartFile bookFile) {
        if (bookFile != null && !bookFile.isEmpty()) {
            try {
                return new String(bookFile.getBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Failed to read uploaded file", 400);
            }
        }
        return request.getBookText();
    }

    private String saveBookTextToDisk(UUID projectId, String content) {
        try {
            Path dir = Paths.get(BOOKS_DIR);
            Files.createDirectories(dir);
            Path filePath = dir.resolve(projectId + ".txt");
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            return filePath.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save book text to disk", e);
        }
    }

    private String readBookTextFromDisk(String filePath) {
        try {
            return Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read book text from disk", e);
        }
    }

    private ProjectSummaryResponse toSummaryResponse(Project p) {
        return ProjectSummaryResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .status(p.getStatus().name())
                .stepState(p.getStepState().name())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private ProjectDetailResponse toDetailResponse(Project p, String bookText) {
        List<CharacterResponse> characters = p.getCharacters() == null ? List.of() :
                p.getCharacters().stream().map(this::toCharacterResponse).collect(Collectors.toList());

        List<ChapterResponse> chapters = p.getChapters() == null ? List.of() :
                p.getChapters().stream().map(this::toChapterResponse).collect(Collectors.toList());

        return ProjectDetailResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .bookText(bookText)
                .status(p.getStatus().name())
                .stepState(p.getStepState().name())
                .stepFailureReason(p.getStepFailureReason())
                .stepStartedAt(p.getStepStartedAt())
                .style(p.getStyle())
                .characters(characters)
                .chapters(chapters)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .stale(StepStaleChecker.isStale(p))
                .build();
    }

    private CharacterResponse toCharacterResponse(Character c) {
        return CharacterResponse.builder()
                .id(c.getId())
                .position(c.getPosition())
                .name(c.getName())
                .prompt(c.getPrompt())
                .portraitReady(c.getPortraitReady())
                .portraitUrl(Boolean.TRUE.equals(c.getPortraitReady()) ? "/api/files/portrait/" + c.getId() : null)
                .build();
    }

    private ChapterResponse toChapterResponse(Chapter c) {
        return ChapterResponse.builder()
                .id(c.getId())
                .position(c.getPosition())
                .name(c.getName())
                .prompt(c.getPrompt())
                .illustrationReady(c.getIllustrationReady())
                .illustrationUrl(Boolean.TRUE.equals(c.getIllustrationReady()) ? "/api/files/illustration/" + c.getId() : null)
                .build();
    }
}