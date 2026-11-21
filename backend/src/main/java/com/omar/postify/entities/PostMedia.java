package com.omar.postify.entities;

import com.omar.postify.entities.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;



@Entity
@Table(
    name = "post_media",
    indexes = {
        @Index(name = "idx_post_media_post", columnList = "post_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaType type;

    @Column(nullable = false)
    private int position;

    private Long width;

    private Long height;

    private Long durationMs;
}