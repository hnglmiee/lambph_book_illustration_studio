package com.hoanglam.bis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ChapterResponse {
    private UUID id;
    private Integer position;
    private String name;
    private String prompt;
    private Boolean illustrationReady;
    private String illustrationUrl;
}