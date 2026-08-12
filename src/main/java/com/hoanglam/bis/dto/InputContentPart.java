package com.hoanglam.bis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InputContentPart {
    private String type;

    private String text;

    private String uri;

    @JsonProperty("mime_type")
    private String mimeType;

    public static InputContentPart text(String text) {
        return InputContentPart.builder().type("text").text(text).build();
    }

    public static InputContentPart document(String uri, String mimeType) {
        return InputContentPart.builder().type("document").uri(uri).mimeType(mimeType).build();
    }
}