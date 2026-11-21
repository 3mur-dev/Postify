package com.omar.postify.entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(
        name = "user_topic_features",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_topic_feature",
                        columnNames = {"user_id", "topic_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_user_topic_features_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_user_topic_features_topic",
                        columnList = "topic_id"
                ),
                @Index(
                        name = "idx_user_topic_features_user_updated",
                        columnList = "user_id, updated_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTopicFeature implements MutableTopicFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_topic_feature_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "topic_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_topic_feature_topic")
    )
    private Topic topic;

    @Column(nullable = false)
    @Builder.Default
    private long impressionCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private long viewCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private long viewStartCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private long viewEndCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private long likeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private long saveCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private long commentCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private long totalViewDurationMs = 0;

    @Column(nullable = false)
    @Builder.Default
    private double totalCompletionRatio = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private long completionRatioSamples = 0;

    @Column(nullable = false)
    @Builder.Default
    private long mediaStartCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private long mediaProgressCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private long mediaCompleteCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private long mediaSkipCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private long linkClickCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private long eventCount = 0;

    @Column(nullable = false)
    private Instant lastInteractionAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {

        Instant now = Instant.now();

        if (lastInteractionAt == null) {
            lastInteractionAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
