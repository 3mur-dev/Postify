package com.omar.postify.service.feed;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.omar.postify.dto.RankingFeatures;
import com.omar.postify.dto.RankingRequest;
import com.omar.postify.dto.RankingResponse;
import com.omar.postify.dto.RankingScore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class RankingModelClient {

    private static final String ML_SERVICE_TOKEN_HEADER = "X-ML-Service-Token";

    private final RestClient restClient;
    private final String serviceToken;

    public RankingModelClient(
            @Value("${postify.ml.base-url}") String baseUrl,
            @Value("${postify.ml.service-token}") String serviceToken
    ) {
        this.serviceToken = serviceToken;

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        /*
         * Set read timeout so that a slow or unresponsive ML service fails fast
         * instead of blocking the feed thread indefinitely.
         */
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(ML_SERVICE_TOKEN_HEADER, serviceToken)
                .build();
    }

    public List<RankingScore> rank(
            List<RankingFeatures> features,
            Instant sessionStartedAt
    ) {

        RankingRequest request =
                new RankingRequest(
                        features,
                        sessionStartedAt
                );

        RankingResponse response =
                restClient.post()
                        .uri("/rank")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(RankingResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "ML service returned empty response"
            );
        }

        List<RankingScore> scores =
                response.rankedCandidates();

        log.info("ML ranking returned {} scores", scores.size());

        log.info(
                "ML ranked IDs: {}",
                scores.stream()
                        .map(RankingScore::postId)
                        .toList()
        );

        return scores;
    }
}