package com.hoanglam.bis.controller;

import com.hoanglam.bis.dto.CreateProjectRequest;
import com.hoanglam.bis.dto.ProjectDetailResponse;
import com.hoanglam.bis.dto.ProjectSummaryResponse;
import com.hoanglam.bis.response.ApiResponse;
import com.hoanglam.bis.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProjectSummaryResponse>> createProject(
            @Valid @org.springdoc.core.annotations.ParameterObject CreateProjectRequest request,
            @RequestParam(value = "bookFile", required = false) MultipartFile bookFile
    ) {
        ProjectSummaryResponse created = projectService.createProject(request, bookFile);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project created successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectSummaryResponse>>> listProjects() {
        List<ProjectSummaryResponse> projects = projectService.listMyProjects();
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectDetailResponse>> getProject(@PathVariable UUID id) {
        ProjectDetailResponse detail = projectService.getProjectDetail(id);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }
}