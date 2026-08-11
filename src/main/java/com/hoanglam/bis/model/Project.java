package com.hoanglam.bis.model;

import enums.ProjectStatus;
import enums.StepState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "book_text_file_path", nullable = false)
    private String bookTextFilePath;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @ColumnDefault("'CREATED'")
    @Column(name = "status", nullable = false)
    private ProjectStatus status = ProjectStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @ColumnDefault("'IDLE'")
    @Column(name = "step_state", nullable = false)
    private StepState stepState = StepState.IDLE;

    @Column(name = "step_started_at")
    private OffsetDateTime stepStartedAt;

    @Column(name = "step_failure_reason")
    private String stepFailureReason;

    @Column(name = "style")
    private String style;

    @Column(name = "book_file_uri")
    private String bookFileUri;

    @Column(name = "last_text_interaction_id")
    private String lastTextInteractionId;

    @Column(name = "last_image_interaction_id")
    private String lastImageInteractionId;

    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @ColumnDefault("0")
    @Column(name = "version", nullable = false)
    private Long version;
}
