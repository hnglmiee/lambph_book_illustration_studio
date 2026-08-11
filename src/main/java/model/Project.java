package model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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

    @Column(name = "title", nullable = false, length = Integer.MAX_VALUE)
    private String title;

    @Column(name = "book_text_file_path", nullable = false, length = Integer.MAX_VALUE)
    private String bookTextFilePath;

    @ColumnDefault("'CREATED'")
    @Column(name = "status", columnDefinition = "project_status not null")
    private Object status;

    @ColumnDefault("'IDLE'")
    @Column(name = "step_state", columnDefinition = "step_state not null")
    private Object stepState;

    @Column(name = "step_started_at")
    private OffsetDateTime stepStartedAt;

    @Column(name = "step_failure_reason", length = Integer.MAX_VALUE)
    private String stepFailureReason;

    @Column(name = "style", length = Integer.MAX_VALUE)
    private String style;

    @Column(name = "book_file_uri", length = Integer.MAX_VALUE)
    private String bookFileUri;

    @Column(name = "last_text_interaction_id", length = Integer.MAX_VALUE)
    private String lastTextInteractionId;

    @Column(name = "last_image_interaction_id", length = Integer.MAX_VALUE)
    private String lastImageInteractionId;

    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ColumnDefault("0")
    @Column(name = "version", nullable = false)
    private Long version;


}