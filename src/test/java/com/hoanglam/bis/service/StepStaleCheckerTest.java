package com.hoanglam.bis.service;

import com.hoanglam.bis.config.StepStaleChecker;
import com.hoanglam.bis.enums.StepState;
import com.hoanglam.bis.model.Project;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class StepStaleCheckerTest {

    @Test
    void isStale_returnsFalse_whenStepStateIsIdle() {
        Project project = new Project();
        project.setStepState(StepState.IDLE);
        project.setStepStartedAt(OffsetDateTime.now().minusMinutes(10));

        assertThat(StepStaleChecker.isStale(project)).isFalse();
    }

    @Test
    void isStale_returnsFalse_whenStepStateIsFailed() {
        Project project = new Project();
        project.setStepState(StepState.FAILED);
        project.setStepStartedAt(OffsetDateTime.now().minusMinutes(10));

        assertThat(StepStaleChecker.isStale(project)).isFalse();
    }

    @Test
    void isStale_returnsFalse_whenRunningButRecentlyStarted() {
        Project project = new Project();
        project.setStepState(StepState.RUNNING);
        project.setStepStartedAt(OffsetDateTime.now().minusSeconds(10));

        assertThat(StepStaleChecker.isStale(project)).isFalse();
    }

    @Test
    void isStale_returnsTrue_whenRunningPastThreshold() {
        Project project = new Project();
        project.setStepState(StepState.RUNNING);
        project.setStepStartedAt(OffsetDateTime.now().minusMinutes(5));

        assertThat(StepStaleChecker.isStale(project)).isTrue();
    }

    @Test
    void isStale_returnsFalse_whenRunningButStepStartedAtIsNull() {
        Project project = new Project();
        project.setStepState(StepState.RUNNING);
        project.setStepStartedAt(null);

        assertThat(StepStaleChecker.isStale(project)).isFalse();
    }
}