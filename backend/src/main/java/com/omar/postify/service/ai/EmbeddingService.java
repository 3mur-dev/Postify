package com.omar.postify.service.ai;

public interface EmbeddingService {

    float[] generateEmbedding(String content);
}