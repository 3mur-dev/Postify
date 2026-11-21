package com.omar.postify.rateLimit;

import io.github.bucket4j.Bandwidth;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RateLimitPolicy {

    public Bandwidth loginLimit() {
        return Bandwidth.builder()
                .capacity(10)
                .refillGreedy(10, Duration.ofMinutes(1))
                .build();
    }

    public Bandwidth registerLimit() {
        return Bandwidth.builder()
                .capacity(5)
                .refillGreedy(5, Duration.ofHours(1))
                .build();
    }

    public Bandwidth createPostLimit() {
        return Bandwidth.builder()
                .capacity(20)
                .refillGreedy(20, Duration.ofMinutes(1))
                .build();
    }

    public Bandwidth createCommentLimit() {
        return Bandwidth.builder()
                .capacity(30)
                .refillGreedy(30, Duration.ofMinutes(1))
                .build();
    }

    public Bandwidth followUserLimit() {
        return Bandwidth.builder()
                .capacity(60)
                .refillGreedy(60, Duration.ofHours(1))
                .build();
    }
}
