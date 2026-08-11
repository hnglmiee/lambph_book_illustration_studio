package com.hoanglam.bis.model;

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
@Table(name = "characters", uniqueConstraints = {
    @UniqueConstraint(name = "uq_characters_project_position", columnNames = {"project_id", "position"})
})
public class Character {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "prompt", nullable = false)
    private String prompt;

    @ColumnDefault("false")
    @Column(name = "portrait_ready", nullable = false)
    private Boolean portraitReady = false;

    @Column(name = "portrait_path")
    private String portraitPath;

    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
