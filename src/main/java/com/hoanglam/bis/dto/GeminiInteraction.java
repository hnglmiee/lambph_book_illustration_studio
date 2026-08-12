package com.hoanglam.bis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiInteraction {
    private String id;
    private String model;
    private String status;
    private List<GeminiStep> steps;

    @JsonProperty("output_text")
    private String outputText;
}