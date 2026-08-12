package com.hoanglam.bis.config;

import com.hoanglam.bis.enums.StepState;
import com.hoanglam.bis.model.Project;

import java.time.Duration;
import java.time.OffsetDateTime;

public class StepStaleChecker {

    private static final long STALE_THRESHOLD_SECONDS = 120;

    private StepStaleChecker() {}

    public static boolean isStale(Project project) {
        return project.getStepState() == StepState.RUNNING
                && project.getStepStartedAt() != null
                && Duration.between(project.getStepStartedAt(), OffsetDateTime.now()).getSeconds() > STALE_THRESHOLD_SECONDS;
    }
}