package com.hoanglam.bis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiFile {
    private String name;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("mime_type")
    private String mimeType;

    private String uri;
    private String state;
}