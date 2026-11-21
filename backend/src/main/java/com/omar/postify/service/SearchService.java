package com.omar.postify.service;

import com.omar.postify.dto.*;
import com.omar.postify.entities.User;
import com.omar.postify.mapper.PostMapper;
import com.omar.postify.mapper.PostMediaMapper;
import com.omar.postify.mapper.UserMapper;
import com.omar.postify.repository.*;
import com.omar.postify.service.ai.LocalAIEmbeddingService;
import com.omar.postify.service.ai.QdrantVectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final QdrantVectorStore qdrantVectorStore;
    private final LocalAIEmbeddingService localAIEmbeddingService;
    private final PostMapper postMapper;
    private final PostMediaRepository postMediaRepository;
    private final PostMediaMapper postMediaMapper;
    private final CurrentUserService currentUserService;
    private final PostFeedEnrichmentService postFeedEnrichmentService;

    @Transactional(readOnly = true)
    public SearchResponse search(String query) {

        if (query == null || query.isBlank()) {
            return new SearchResponse(
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        String normalizedQuery = query.trim();

        // -------------------------
        // USERS
        // -------------------------

        List<UserSearchDto> users =
                userRepository
                        .findByUsernameContainingIgnoreCase(normalizedQuery)
                        .stream()
                        .map(userMapper::toUserSearchDto)
                        .toList();

        // -------------------------
        // POSTS
        // -------------------------

        List<PostFeedBaseDto> keywordRows =
                postRepository.searchPostsByContent(normalizedQuery);

        List<PostFeedBaseDto> semanticRows;
        try {
            float[] queryEmbedding =
                    localAIEmbeddingService.generateEmbedding(
                            normalizedQuery
                    );

            List<SimilarPost> similarPosts =
                    qdrantVectorStore.searchSimilarPosts(
                                    queryEmbedding,
                                    10
                            ).stream()
                            .filter(result -> result.score() >= 0.60)
                            .toList();

            List<Long> semanticPostIds =
                    similarPosts.stream()
                            .map(SimilarPost::postId)
                            .toList();

            semanticRows =
                    semanticPostIds.isEmpty()
                            ? Collections.emptyList()
                            : postRepository.findFeedPostsByIds(semanticPostIds);
        } catch (Exception e) {
            log.warn("Semantic search unavailable; falling back to keyword-only results", e);
            semanticRows = Collections.emptyList();
        }

        List<PostFeedBaseDto> orderedRows = getOrderedRows(keywordRows, semanticRows);

        List<Long> orderedPostIds =
                orderedRows.stream()
                        .map(PostFeedBaseDto::id)
                        .toList();

        // -------------------------
        // ENRICH POSTS
        // -------------------------

        Long currentUserId =
                currentUserService.getCurrentUser()
                        .map(User::getId)
                        .orElse(null);

        Map<Long, List<PostMediaDto>> mediaByPost =
                postMediaRepository.findMediaByPostIds(orderedPostIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                PostMediaProjection::postId,
                                LinkedHashMap::new,
                                Collectors.mapping(
                                        postMediaMapper::toDto,
                                        Collectors.toList()
                                )
                        ));

        Map<Long, Long> likeCounts =
                postFeedEnrichmentService.loadLikeCounts(orderedPostIds);

        Map<Long, Long> commentCounts =
                postFeedEnrichmentService.loadCommentCounts(orderedPostIds);

        Set<Long> likedPostIds =
                postFeedEnrichmentService.loadLikedPostIds(
                        currentUserId,
                        orderedPostIds
                );

        Set<Long> savedPostIds =
                postFeedEnrichmentService.loadSavedPostIds(
                        currentUserId,
                        orderedPostIds
                );

        List<PostFeedDto> posts =
                orderedRows.stream()
                        .map(row ->
                                postMapper.toPostFeedDto(
                                        row,
                                        mediaByPost.getOrDefault(
                                                row.id(),
                                                List.of()
                                        ),
                                        likeCounts.getOrDefault(
                                                row.id(),
                                                0L
                                        ),
                                        commentCounts.getOrDefault(
                                                row.id(),
                                                0L
                                        ),
                                        likedPostIds.contains(
                                                row.id()
                                        ),
                                        savedPostIds.contains(
                                                row.id()
                                        )
                                )
                        )
                        .toList();

        return new SearchResponse(users, posts);
    }

    private static List<PostFeedBaseDto> getOrderedRows(List<PostFeedBaseDto> keywordRows, List<PostFeedBaseDto> semanticRows) {
        Map<Long, PostFeedBaseDto> mergedRows =
                new LinkedHashMap<>();

        for (PostFeedBaseDto row : keywordRows) {
            mergedRows.put(row.id(), row);

            if (mergedRows.size() >= 10) {
                break;
            }
        }

        for (PostFeedBaseDto row : semanticRows) {
            if (mergedRows.size() >= 10) {
                break;
            }

            mergedRows.putIfAbsent(row.id(), row);
        }

        return new ArrayList<>(mergedRows.values());
    }
}
