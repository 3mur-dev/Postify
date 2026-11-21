package com.omar.postify.service;

import com.omar.postify.dto.PostCommentCount;
import com.omar.postify.dto.PostLikeCount;
import com.omar.postify.repository.CommentRepository;
import com.omar.postify.repository.LikeRepository;
import com.omar.postify.repository.SavedPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostFeedEnrichmentService {

    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final SavedPostRepository savedPostRepository;

    public Map<Long, Long> loadLikeCounts(Collection<Long> postIds) {
        return likeRepository.countByPostIdInGrouped(postIds).stream()
                .collect(Collectors.toMap(
                        PostLikeCount::getPostId,
                        PostLikeCount::getCount,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    public Map<Long, Long> loadCommentCounts(Collection<Long> postIds) {
        return commentRepository.countByPostIdInGrouped(postIds).stream()
                .collect(Collectors.toMap(
                        PostCommentCount::getPostId,
                        PostCommentCount::getCount,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    public Set<Long> loadLikedPostIds(Long currentUserId, Collection<Long> postIds) {
        if (currentUserId == null || currentUserId < 0 || postIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(likeRepository.findLikedPostIds(currentUserId, postIds));
    }

    public Set<Long> loadSavedPostIds(Long currentUserId, Collection<Long> postIds) {
        if (currentUserId == null || currentUserId < 0 || postIds.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(savedPostRepository.findSavedPostIds(currentUserId, postIds));
    }
}
