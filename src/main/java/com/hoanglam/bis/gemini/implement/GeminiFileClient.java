package com.hoanglam.bis.gemini.implement;

import com.hoanglam.bis.gemini.dto.GeminiFile;

public interface GeminiFileClient {
    GeminiFile uploadTextFile(byte[] content, String displayName);
}