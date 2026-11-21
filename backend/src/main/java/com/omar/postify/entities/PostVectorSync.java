package com.omar.postify.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "post_vector_sync")
@Getter
@Setter
@NoArgsConstructor
public class PostVectorSync {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private long revision;
}
