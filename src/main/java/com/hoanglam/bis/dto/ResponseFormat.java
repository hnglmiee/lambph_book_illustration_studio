package com.hoanglam.bis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseFormat {
    private String type;

    @JsonProperty("mime_type")
    private String mimeType;

    private Object schema;
}