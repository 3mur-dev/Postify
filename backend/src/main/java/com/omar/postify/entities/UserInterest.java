package com.omar.postify.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "user_interests",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_interest_topic",
                        columnNames = {"user_id", "topic_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_user_interests_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_user_interests_topic",
                        columnList = "topic_id"
                ),
                @Index(
                        name = "idx_user_interests_user_score",
                        columnList = "user_id, score"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    /**
     * Current learned interest strength.
     *
     * Higher value = stronger interest.
     */
    @Column(nullable = false)
    private double score;

    /**
     * Number of interactions that contributed
     * to this learned interest.
     */
    @Column(nullable = false)
    private long interactionCount;

    /**
     * Last time this interest was recalculated.
     */
    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}