package com.hoanglam.bis.gemini.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiFile {
    private String name;       // "files/abc-123" — dùng để reference lại sau này
    private String displayName;
    private String mimeType;
    private String uri;        // dùng khi đưa vào input của Interactions API
    private String state;      // PROCESSING, ACTIVE, FAILED
}