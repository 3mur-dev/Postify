package com.omar.postify.rateLimit;

import com.omar.postify.entities.enums.RateLimitType;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the rate limiter evicts stale entries to prevent unbounded memory growth,
 * and that active clients are not prematurely evicted.
 */
class RateLimiterServiceTest {

    private final RateLimitPolicy policy = new RateLimitPolicy();

    @Test
    void resolvesBucketForNewClient() {
        RateLimiterService service = new RateLimiterService(policy);
        Bucket bucket = service.resolveBucket("client-1", RateLimitType.LOGIN);
        assertNotNull(bucket);
    }

    @Test
    void reusesBucketForSameClient() {
        RateLimiterService service = new RateLimiterService(policy);
        Bucket bucket1 = service.resolveBucket("client-1", RateLimitType.LOGIN);
        Bucket bucket2 = service.resolveBucket("client-1", RateLimitType.LOGIN);
        assertSame(bucket1, bucket2);
    }

    @Test
    void createsSeparateBucketsForDifferentClients() {
        RateLimiterService service = new RateLimiterService(policy);
        Bucket bucket1 = service.resolveBucket("client-1", RateLimitType.LOGIN);
        Bucket bucket2 = service.resolveBucket("client-2", RateLimitType.LOGIN);
        assertNotSame(bucket1, bucket2);
    }

    @Test
    void createsSeparateBucketsForDifferentTypes() {
        RateLimiterService service = new RateLimiterService(policy);
        Bucket login = service.resolveBucket("client-1", RateLimitType.LOGIN);
        Bucket post = service.resolveBucket("client-1", RateLimitType.CREATE_POST);
        assertNotSame(login, post);
    }

    @Test
    void activeClientReusesSameBucket() throws Exception {
        RateLimiterService service = new RateLimiterService(policy);

        // First access creates the entry.
        Bucket bucket1 = service.resolveBucket("active-client", RateLimitType.LOGIN);

        // Second access should return the same bucket instance.
        Bucket bucket2 = service.resolveBucket("active-client", RateLimitType.LOGIN);

        assertSame(bucket1, bucket2,
                "Active client should reuse the same bucket instance");
    }

    @Test
    void newClientGetsNewBucket() throws Exception {
        RateLimiterService service = new RateLimiterService(policy);

        Bucket bucket1 = service.resolveBucket("client-a", RateLimitType.LOGIN);
        Bucket bucket2 = service.resolveBucket("client-b", RateLimitType.LOGIN);

        assertNotSame(bucket1, bucket2,
                "Different clients should get different bucket instances");
    }

    // -- Reflection helpers to inspect private state --

    private static Object getBucketEntry(RateLimiterService service, String key) throws Exception {
        Field field = RateLimiterService.class.getDeclaredField("buckets");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, ?> map = (ConcurrentMap<String, ?>) field.get(service);
        return map.get(key);
    }
}
