package com.hoanglam.bis.gemini.implement;

import com.hoanglam.bis.dto.CreateInteractionRequest;
import com.hoanglam.bis.dto.GeminiInteraction;

public interface GeminiInteractionClient {
    GeminiInteraction createInteraction(CreateInteractionRequest request);
    GeminiInteraction getInteraction(String interactionId);
}