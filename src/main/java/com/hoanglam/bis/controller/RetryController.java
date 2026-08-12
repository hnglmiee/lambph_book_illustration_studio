package com.hoanglam.bis.controller;

import com.hoanglam.bis.dto.RunStyleStepRequest;
import com.hoanglam.bis.gemini.implement.GeminiPipelineService;
import com.hoanglam.bis.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/steps")
@RequiredArgsConstructor
public class RetryController {

    private final GeminiPipelineService pipelineService;

    @PostMapping("/retry")
    public ResponseEntity<ApiResponse<Void>> retryStep(
            @PathVariable UUID projectId,
            @RequestBody(required = false) RunStyleStepRequest request
    ) {
        String userStyle = request != null ? request.getUserStyle() : null;
        pipelineService.retryCurrentStep(projectId, userStyle);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Retry started for current step", null));
    }
}