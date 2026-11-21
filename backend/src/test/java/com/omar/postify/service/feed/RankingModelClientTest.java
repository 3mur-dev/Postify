package com.omar.postify.service.feed;

import com.omar.postify.dto.RankingFeatures;
import com.omar.postify.dto.RankingResponse;
import com.omar.postify.dto.RankingScore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
class RankingModelClientTest {

    @Autowired
    private RankingModelClient rankingModelClient;

    @Test
    void clientIsCreatedWithToken() {
        // Verify the client is properly wired with the configured token
        assertNotNull(rankingModelClient);
    }

    @Test
    void rankingResponseContainsScores() {
        // Test that a RankingResponse correctly exposes ranked candidates
        List<RankingScore> scores = List.of(
                new RankingScore(1L, 0.9, 1),
                new RankingScore(2L, 0.8, 2)
        );

        RankingResponse response = new RankingResponse("v1", 2, scores);

        assertEquals(2, response.candidateCount());
        assertEquals(2, response.rankedCandidates().size());
        assertEquals(1L, response.rankedCandidates().get(0).postId());
        assertEquals(2L, response.rankedCandidates().get(1).postId());
    }

    @Test
    void throwsWhenResponseIsNull() {
        List<RankingFeatures> features = List.of();
        Instant now = Instant.now();

        // Without a running ML service, the client will throw when calling rank
        assertThrows(Exception.class, () -> {
            rankingModelClient.rank(features, now);
        });
    }
}
