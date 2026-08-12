package com.hoanglam.bis.controller;

import com.hoanglam.bis.dto.CreateInteractionRequest;
import com.hoanglam.bis.dto.GeminiInteraction;
import com.hoanglam.bis.dto.InputContentPart;
import com.hoanglam.bis.gemini.dto.GeminiFile;
import com.hoanglam.bis.gemini.implement.GeminiFileClient;
import com.hoanglam.bis.gemini.implement.GeminiInteractionClient;
import com.hoanglam.bis.gemini.implement.GeminiInteractionClientImpl;
import com.hoanglam.bis.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final GeminiFileClient geminiFileClient;
    private final GeminiInteractionClientImpl geminiInteractionClient;

    @PostMapping("/gemini-upload-test")
    public ApiResponse<GeminiFile> testUpload() {
        byte[] content = "Once upon a time, in a small burrow by the river...".getBytes(StandardCharsets.UTF_8);
        GeminiFile file = geminiFileClient.uploadTextFile(content, "test-book");
        return ApiResponse.success(file);
    }

    @PostMapping("/gemini-interaction-test")
    public ApiResponse<GeminiInteraction> testInteraction() {
        CreateInteractionRequest request = CreateInteractionRequest.builder()
                .model("gemini-3.6-flash")
                .input("Say hello in one sentence.")
                .build();
        GeminiInteraction result = geminiInteractionClient.createInteraction(request);
        return ApiResponse.success(result);
    }

    @PostMapping("/gemini-wire-test")
    public ApiResponse<Object> testWire() {
        // Bước 1: upload sách
        byte[] content = "The old mill stood by the river, creaking in the wind. Ratty loved the water more than anything...".getBytes(StandardCharsets.UTF_8);
        GeminiFile file = geminiFileClient.uploadTextFile(content, "test-book");

        // Bước 2: hỏi model dựa trên file vừa upload
        List<InputContentPart> input = List.of(
                InputContentPart.document(file.getUri(), "text/plain"),
                InputContentPart.text("Summarize this text in one sentence.")
        );

        CreateInteractionRequest request = CreateInteractionRequest.builder()
                .model("gemini-3.6-flash")
                .input(input)
                .build();

        GeminiInteraction result = geminiInteractionClient.createInteraction(request);

        return ApiResponse.success(Map.of(
                "uploadedFile", file,
                "interaction", result
        ));
    }
}