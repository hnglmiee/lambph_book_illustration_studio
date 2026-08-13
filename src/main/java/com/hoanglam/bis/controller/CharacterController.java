package com.hoanglam.bis.controller;

import com.hoanglam.bis.gemini.implement.GeminiPipelineService;
import com.hoanglam.bis.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@RestController
@RequestMapping("/api/projects/{projectId}/steps")
@RequiredArgsConstructor
@Slf4j
public class CharacterController {
    private final GeminiPipelineService pipelineService;

    @PostMapping("/characters/run")
    public ResponseEntity<ApiResponse<Void>> runCharactersStep(@PathVariable UUID projectId) {
        log.info(">>> ENTERED runCharactersStep controller for {}", id);
        pipelineService.startCharactersStep(projectId);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Characters generation started", null));
    }


}
