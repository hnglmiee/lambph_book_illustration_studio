package com.hoanglam.bis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateInteractionRequest {
    private String model;
    private Object input;

    @JsonProperty("previous_interaction_id")
    private String previousInteractionId;

    @JsonProperty("response_format")
    private ResponseFormat responseFormat;

    private Boolean background;
}