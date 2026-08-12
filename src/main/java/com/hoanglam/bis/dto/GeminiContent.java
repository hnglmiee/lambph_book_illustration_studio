package com.hoanglam.bis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiContent {
    private String type;
    private String text;
    private String data;

    @JsonProperty("mime_type")
    private String mimeType;

    private String uri;
}