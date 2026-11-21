package com.omar.postify.service.ai;

import com.omar.postify.dto.SimilarPost;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Common;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class QdrantVectorStore implements VectorStore {
    private static final String qdrantCollectionName = "post_embeddings";

    private final QdrantClient qdrantClient;

    public void savePostEmbedding(Long postId, float[] embedding) {

        List<Float> vectorValues = new ArrayList<>(embedding.length);

        for (float value : embedding) {
            vectorValues.add(value);
        }

        Points.PointStruct point = Points.PointStruct.newBuilder()
                .setId(
                        Common.PointId.newBuilder()
                                .setNum(postId)
                                .build()
                )
                .setVectors(
                        Points.Vectors.newBuilder()
                                .setVector(
                                        Points.Vector.newBuilder()
                                                .addAllData(vectorValues)
                                                .build()
                                )
                                .build()
                )
                .build();

        try {
            qdrantClient.upsertAsync(
                    qdrantCollectionName,
                    Collections.singletonList(point)
            ).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Qdrant upsert interrupted", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Qdrant upsert failed", e);
        }
    }

    public void deletePostEmbedding(Long postId) {
        try {
            qdrantClient.deleteAsync(
                    qdrantCollectionName,
                    List.of(Common.PointId.newBuilder().setNum(postId).build())
            ).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Qdrant delete interrupted", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Qdrant delete failed", e);
        }
    }

    public List<SimilarPost> searchSimilarPosts(float[] embedding, int limit) {

        List<Float> vectorValues = new ArrayList<>(embedding.length);

        for (float value : embedding) {
            vectorValues.add(value);

        }

        List<Points.ScoredPoint> results;

        try {
            results = qdrantClient.searchAsync(
                    Points.SearchPoints.newBuilder()
                            .setCollectionName(qdrantCollectionName)
                            .addAllVector(vectorValues)
                            .setLimit(limit)
                            .build()
            ).get(5, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Qdrant search interrupted", e);

        } catch (TimeoutException e) {
            throw new RuntimeException("Qdrant search timed out", e);

        } catch (ExecutionException e) {
            throw new RuntimeException("Qdrant search failed", e);
        }

        results.forEach(result ->
                log.info(
                        "Qdrant search result: postId={}, score={}",
                        result.getId().getNum(),
                        result.getScore()
                )
        );

        return results.stream()
                .map(result -> new SimilarPost(
                        result.getId().getNum(),
                        result.getScore()
                ))
                .toList();
    }
}