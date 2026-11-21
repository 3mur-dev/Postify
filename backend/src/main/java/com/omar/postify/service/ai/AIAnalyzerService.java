package com.omar.postify.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omar.postify.dto.TopicCandidate;
import com.omar.postify.entities.PostMedia;
import com.omar.postify.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;


@Service
@RequiredArgsConstructor
@Slf4j

public class AIAnalyzerService implements AIAnalyzer {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;

    @Override
    public List<TopicCandidate> analyze(String content, List<PostMedia> media) {


        boolean hasMedia = media != null && !media.isEmpty();

        String model = hasMedia
                ? "qwen_qwen3.5-4b-vision"
                : "qwen_qwen3.5-4b-text";

        int maxTokens = hasMedia ? 3584 : 10000;

        String systemPrompt =
                """
                        You are Postify's topic classifier.
                        Analyze the social media post and return ONLY valid JSON.
                        
                        You MUST return at least 1 topic and at most 5 topics.
                        
                        Rules:
                        - Never return an empty topics array.
                        - Every post must have at least one topic.
                        - Use topics directly supported by the post.
                        - Prefer specific meaningful topics over generic ones.
                        - Do not duplicate or combine redundant topics.
                        - Topic titles should be 1-3 words.
                        - Confidence must be between 0.0 and 1.0.
                        - Do not explain your answer.
                        
                        Output:
                        {
                          "topics": [
                            {
                              "title": "Authentication",
                              "confidence": 0.95
                            }
                          ]
                        }
                        """;

        List<Map<String, Object>> userContent = new ArrayList<>();

        userContent.add(Map.of(
                "type", "text",
                "text", """
                        Analyze this post.
                        
                        Content:
                        %s
                        """.formatted(
                        content != null ? content : "(none)"
                )
        ));

        if (hasMedia) {
            for (PostMedia postMedia : media) {

                String imageUrl =
                        fileStorageService.getAnalysisThumbnailUrl(postMedia);

                userContent.add(Map.of(
                        "type", "image_url",
                        "image_url", Map.of(
                                "url", imageUrl
                        )
                ));
            }
        }

        String response = restClient.method(HttpMethod.POST)
                .uri("http://localhost:8081/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", model,
                        "messages", List.of(
                                Map.of(
                                        "role", "system",
                                        "content", systemPrompt
                                ),
                                Map.of(
                                        "role", "user",
                                        "content", userContent
                                )
                        )
                ))
                .retrieve()
                .body(String.class);


        log.info("AI topic analysis completed; model={}, mediaPresent={}", model, hasMedia);

        try {
            String cleanedJson = cleanJsonResponse(response);

            JsonNode root = objectMapper.readTree(cleanedJson);

            String contentJson = root
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

            JsonNode contentRoot = objectMapper.readTree(contentJson);

            JsonNode topics = contentRoot.path("topics");

            List<TopicCandidate> candidates =
                    StreamSupport.stream(topics.spliterator(), false)
                            .map(topic -> new TopicCandidate(
                                    topic.path("title").asText(),
                                    topic.path("confidence").asDouble()
                            ))
                            .toList();

            if (candidates.isEmpty()) {
                throw new IllegalStateException(
                        "AI returned no topics."
                );
            }

            return candidates;

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse AI response", e);
        }
    }

    private String cleanJsonResponse(String content) {
        String cleaned = content.trim();

        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "");
            cleaned = cleaned.replaceFirst("\\s*```$", "");
        }

        return cleaned.trim();
    }
}
