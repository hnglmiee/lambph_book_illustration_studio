package com.hoanglam.bis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ProjectDetailResponse {
    private UUID id;
    private String title;
    private String bookText;
    private String status;
    private String stepState;
    private String stepFailureReason;
    private OffsetDateTime stepStartedAt;
    private String style;
    private List<CharacterResponse> characters;
    private List<ChapterResponse> chapters;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}