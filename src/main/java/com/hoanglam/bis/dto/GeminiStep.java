package com.hoanglam.bis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiStep {
    private String type; // "model_output", "user_input", ...
    private List<GeminiContent> content;
}