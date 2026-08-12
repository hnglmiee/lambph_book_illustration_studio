package com.hoanglam.bis.gemini.implement;

import com.hoanglam.bis.dto.CreateInteractionRequest;
import com.hoanglam.bis.dto.GeminiInteraction;
import com.hoanglam.bis.enums.ErrorCode;
import com.hoanglam.bis.exceptions.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class GeminiInteractionClientImpl implements GeminiInteractionClient {

    private final RestClient geminiRestClient;

    @Override
    public GeminiInteraction createInteraction(CreateInteractionRequest request) {
        try {
            return geminiRestClient.post()
                    .uri("/v1beta/interactions")
                    .body(request)
                    .retrieve()
                    .body(GeminiInteraction.class);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.GEMINI_CALL_FAILED,
                    "Failed to create Gemini interaction: " + e.getMessage(), 502);
        }
    }

    @Override
    public GeminiInteraction getInteraction(String interactionId) {
        try {
            return geminiRestClient.get()
                    .uri("/v1beta/interactions/{id}", interactionId)
                    .retrieve()
                    .body(GeminiInteraction.class);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.GEMINI_CALL_FAILED,
                    "Failed to fetch Gemini interaction: " + e.getMessage(), 502);
        }
    }
}