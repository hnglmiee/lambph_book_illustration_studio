package com.hoanglam.bis.controller;

import com.hoanglam.bis.gemini.implement.GeminiPipelineService;
import com.hoanglam.bis.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/illustration")
@RequiredArgsConstructor
public class IllustrationController {
    private final GeminiPipelineService pipelineService;

    @PostMapping("/illustrations/run")
    public ResponseEntity<ApiResponse<Void>> runIllustrationsStep(@PathVariable UUID projectId) {
        pipelineService.startIllustrationsStep(projectId);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Illustrations generation started", null));
    }
}
