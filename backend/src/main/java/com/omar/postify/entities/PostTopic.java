package com.omar.postify.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name= "post_topic",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_topic",
                        columnNames = {"post_id", "topic_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn( name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn( name = "topic_id", nullable = false)
    private Topic topic;

    @Column(nullable = false)
    private Double confidence;

}
