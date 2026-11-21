package com.omar.postify.service.ai;

import com.omar.postify.dto.SimilarPost;

import java.util.List;

public interface VectorStore {

    void savePostEmbedding(Long postId, float[] embedding);

    List<SimilarPost> searchSimilarPosts(float[] embedding, int limit);
}