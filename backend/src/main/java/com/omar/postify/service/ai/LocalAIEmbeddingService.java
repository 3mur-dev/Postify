package com.omar.postify.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class LocalAIEmbeddingService implements EmbeddingService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Override
    public float[] generateEmbedding(String content) {

        if (content == null || content.isBlank()) {
            return new float[0];
        }

        String response = restClient.post()
                .uri("/v1/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", "qwen3-embedding-0.6b",
                        "input", content,
                        "stream", false
                ))
                .retrieve()
                .body(String.class);

        try {
            JsonNode embedding = objectMapper
                    .readTree(response)
                    .path("data")
                    .get(0)
                    .path("embedding");

            float[] vector = new float[embedding.size()];

            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = (float) embedding.get(i).asDouble();
            }

            return vector;

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to parse embedding response", e
            );
        }
    }
}