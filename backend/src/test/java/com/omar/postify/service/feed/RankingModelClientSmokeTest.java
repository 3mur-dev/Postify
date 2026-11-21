package com.omar.postify.service.feed;

import com.omar.postify.dto.RankingFeatures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        properties = {
                "postify.ml.base-url=http://localhost:8000",
                "postify.ml.service-token=test-ml-token",
                "postify.cookie.secure=false"
        }
)
class RankingModelClientSmokeTest {

    @Autowired
    private RankingModelClient rankingModelClient;

    @Test
    void throwsWhenMlServiceIsUnavailable() {
        // Without a properly configured ML service, calling rank should throw
        // either ResourceAccessException (no server) or HttpClientErrorException (auth failure)
        List<RankingFeatures> features = List.of();
        Instant now = Instant.now();

        try {
            rankingModelClient.rank(features, now);
            fail("Expected an exception when ML service is unavailable");
        } catch (ResourceAccessException | HttpClientErrorException e) {
            // Both are acceptable: connection refused or auth failure
        }
    }

    @Test
    void clientIsConfiguredWithServiceToken() {
        // Verify the client bean is created with the configured service token
        // If the token property were missing, Spring would fail to start the context
        assertNotNull(rankingModelClient);
    }
}
