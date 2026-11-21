package com.omar.postify.service.feed;

import com.omar.postify.dto.*;
import com.omar.postify.entities.enums.CandidateSource;
import com.omar.postify.exception.BadRequestException;
import com.omar.postify.entities.enums.IdentityType;
import com.omar.postify.entities.enums.InteractionEventType;
import com.omar.postify.mapper.PostMapper;
import com.omar.postify.mapper.PostMediaMapper;
import com.omar.postify.repository.*;
import com.omar.postify.service.PostFeedEnrichmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    static final int MAX_PAGE_SIZE = 10;
    private static final Map<CandidateSource, Integer> FALLBACK_SOURCE_PRIORITY = Map.of(
            CandidateSource.FOLLOWING, 3,
            CandidateSource.INTEREST, 2,
            CandidateSource.EXPLORATION, 1
    );

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final PostMediaRepository postMediaRepository;
    private final InteractionEventRepository interactionEventRepository;
    private final PostMediaMapper postMediaMapper;
    private final CandidateGenerationService candidateGenerationService;
    private final FollowRepository followRepository;
    private final RankingFeatureBuilder rankingFeatureBuilder;
    private final RankingModelClient rankingModelClient;
    private final PostFeedEnrichmentService postFeedEnrichmentService;

    @Transactional(readOnly = true)
    public Page<PostFeedDto> getFeed(
            RequestIdentity identity,
            int page,
            int size
    ) {
        validatePagination(page, size);
        Pageable pageable = PageRequest.of(page, size);

        boolean authenticated = identity.isAuthenticated();
        Long currentUserId =
                authenticated ? identity.id() : null;

        int candidateLimit = candidateLimitForSize(size);

        Instant sessionStartedAt = Instant.now();

        Set<Long> followingUserIds =
                authenticated
                        ? followRepository.findFollowingIdsByFollowerId(currentUserId)
                        : Set.of();


        Set<Long> seenPostIds = findSeenPostIds(identity);

        Set<Long> userPostIds =
                authenticated
                        ? postRepository.findIdsByUserId(currentUserId)
                        : Set.of();

        FeedExclusionContext exclusions =
                new FeedExclusionContext(seenPostIds, userPostIds);

        CandidateGenerationContext context =
                new CandidateGenerationContext(
                        identity,
                        candidateLimit,
                        followingUserIds,
                        exclusions,
                        sessionStartedAt
                );

        List<FeedCandidate> candidates =
                candidateGenerationService.generate(context);

        if (candidates.isEmpty()) {
            return new PageImpl<>(
                    List.of(),
                    pageable,
                    0
            );
        }

        List<RankingFeatures> features =
                rankingFeatureBuilder.buildFeatures(
                        context,
                        candidates
                );


        List<RankingScore> scores;
        try {
            scores = rankingModelClient.rank(features, sessionStartedAt);
        } catch (RestClientException | IllegalStateException e) {
            log.warn("ML ranking unavailable or invalid; using deterministic feed fallback", e);
            scores = fallbackScores(candidates, features);
        }

        log.info("Candidates before ranking: {}", candidates.size());
        log.info("Features before ranking: {}", features.size());
        log.info("Scores returned from ML: {}", scores.size());

        Map<Long, FeedCandidate> candidatesByPostId =
                candidates.stream()
                        .collect(Collectors.toMap(
                                FeedCandidate::postId,
                                Function.identity()
                        ));

        List<Long> rankedPostIds =
                scores.stream()
                        .sorted(
                                Comparator.comparingDouble(
                                        RankingScore::score
                                ).reversed()
                        )
                        .map(RankingScore::postId)
                        .filter(candidatesByPostId::containsKey)
                        .toList();

        log.info("Ranked post IDs after filtering: {}", rankedPostIds.size());

        int fromIndex = page * size;

        if (fromIndex >= rankedPostIds.size()) {
            return new PageImpl<>(
                    List.of(),
                    pageable,
                    rankedPostIds.size()
            );
        }

        int toIndex =
                Math.min(
                        fromIndex + size,
                        rankedPostIds.size()
                );

        List<Long> pagePostIds =
                rankedPostIds.subList(
                        fromIndex,
                        toIndex
                );

        List<PostFeedBaseDto> rows =
                postRepository.findFeedPostsByIds(pagePostIds);

        Map<Long, PostFeedBaseDto> rowsById =
                rows.stream()
                        .collect(Collectors.toMap(
                                PostFeedBaseDto::id,
                                Function.identity()
                        ));

        List<PostFeedBaseDto> orderedRows =
                pagePostIds.stream()
                        .map(rowsById::get)
                        .filter(Objects::nonNull)
                        .toList();

        if (orderedRows.isEmpty()) {
            return new PageImpl<>(
                    List.of(),
                    pageable,
                    rankedPostIds.size()
            );
        }

        List<Long> postIds =
                orderedRows.stream()
                        .map(PostFeedBaseDto::id)
                        .toList();

        Map<Long, List<PostMediaDto>> mediaByPost =
                postMediaRepository.findMediaByPostIds(postIds)
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
                postFeedEnrichmentService.loadLikeCounts(postIds);

        Map<Long, Long> commentCounts =
                postFeedEnrichmentService.loadCommentCounts(postIds);

        Set<Long> likedPostIds =
                postFeedEnrichmentService.loadLikedPostIds(currentUserId, postIds);

        Set<Long> savedPostIds =
                postFeedEnrichmentService.loadSavedPostIds(currentUserId, postIds);

        List<PostFeedDto> feed =
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

        return new PageImpl<>(
                feed,
                pageable,
                rankedPostIds.size()
        );
    }

    private Set<Long> findSeenPostIds(RequestIdentity identity) {

        Long userId = identity.type() == IdentityType.AUTHENTICATED
                ? identity.id()
                : null;

        Long anonymousUserId = identity.type() == IdentityType.ANONYMOUS
                ? identity.id()
                : null;

        return interactionEventRepository.findSeenPostIds(
                userId,
                anonymousUserId,
                InteractionEventType.IMPRESSION
        );
    }

    public static void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException(
                    "Feed page must be non-negative and size must be between 1 and "
                            + MAX_PAGE_SIZE + "."
            );
        }

    }

    static int candidateLimitForSize(int size) {
        return Math.min(Math.max(size * 20, 100), 200);
    }

    static List<RankingScore> fallbackScores(
            List<FeedCandidate> candidates,
            List<RankingFeatures> features
    ) {
        Map<Long, RankingFeatures> featuresByPostId = features.stream()
                .collect(Collectors.toMap(RankingFeatures::postId, Function.identity()));
        List<FeedCandidate> ordered = candidates.stream()
                .sorted(
                        Comparator.<FeedCandidate>comparingInt(
                                        candidate -> FALLBACK_SOURCE_PRIORITY.getOrDefault(
                                                candidate.source(),
                                                0
                                        )
                                )
                                .reversed()
                                .thenComparing(
                                        Comparator.comparingDouble(FeedCandidate::relevanceScore)
                                                .reversed()
                                )
                                .thenComparing(
                                        candidate -> {
                                            RankingFeatures feature =
                                                    featuresByPostId.get(candidate.postId());
                                            return feature == null ? null : feature.postCreatedAt();
                                        },
                                        Comparator.nullsLast(Comparator.reverseOrder())
                                )
                                .thenComparing(FeedCandidate::postId)
                )
                .toList();
        List<RankingScore> scores = new ArrayList<>(ordered.size());
        for (int i = 0; i < ordered.size(); i++) {
            scores.add(new RankingScore(ordered.get(i).postId(), ordered.size() - i, i + 1));
        }
        return scores;
    }
}
