package com.hoanglam.bis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ProjectSummaryResponse {
    private UUID id;
    private String title;
    private String status;
    private String stepState;
    private OffsetDateTime createdAt;
}