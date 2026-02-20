package com.omar.postify.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The author of the post
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Post content
    @Column(nullable = false, length = 1000)
    private String content;

    //Image
    private String imageUrl;

    // Creation timestamp
    private LocalDateTime createdAt = LocalDateTime.now();

    // update timestamp
    private LocalDateTime updatedAt;
}

