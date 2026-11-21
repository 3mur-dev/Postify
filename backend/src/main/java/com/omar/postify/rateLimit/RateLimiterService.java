package com.omar.postify.rateLimit;
import com.omar.postify.entities.enums.RateLimitType;
import com.omar.postify.exception.ResourceNotFoundException;
import io.github.bucket4j.Bucket;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
@Component
public class RateLimiterService {

    /**
     * Maps client keys (IP or user ID) to their rate-limit buckets.
     * Each entry tracks the last access time so stale entries can be evicted.
     */
    private final Map<String, ClientBucketEntry> buckets = new ConcurrentHashMap<>();
    private final RateLimitPolicy policy;

    /**
     * Evict buckets that have not been accessed for more than 5 minutes.
     * This prevents unbounded memory growth from unique IP addresses.
     */
    @Scheduled(fixedRate = 300_000)
    public void evictStaleBuckets() {
        Instant cutoff = Instant.now().minusSeconds(300);
        buckets.entrySet().removeIf(entry -> entry.getValue().lastAccessedAt.isBefore(cutoff));
    }

    public Bucket resolveBucket(String clientKey, RateLimitType type) {

        ClientBucketEntry entry = buckets
                .computeIfAbsent(clientKey, k -> new ClientBucketEntry());

        entry.lastAccessedAt = Instant.now();

        return entry.typeBuckets
                .computeIfAbsent(type, this::createBucket);
    }

    private Bucket createBucket(RateLimitType type) {

        return switch (type) {

            case LOGIN -> Bucket.builder()
                    .addLimit(policy.loginLimit())
                    .build();

            case REGISTER -> Bucket.builder()
                    .addLimit(policy.registerLimit())
                    .build();
            case CREATE_POST -> Bucket.builder()
                    .addLimit(policy.createPostLimit())
                    .build();
            case CREATE_COMMENT -> Bucket.builder()
                    .addLimit(policy.createCommentLimit())
                    .build();
            case FOLLOW_USER -> Bucket.builder()
                    .addLimit(policy.followUserLimit())
                    .build();

            default -> throw new ResourceNotFoundException("Rate limit type not found: " + type);
        };
    }

    /**
     * Holds the per-type buckets for a single client key and tracks last access time.
     */
    private static class ClientBucketEntry {
        Map<RateLimitType, Bucket> typeBuckets = new ConcurrentHashMap<>();
        Instant lastAccessedAt = Instant.now();
    }
}
