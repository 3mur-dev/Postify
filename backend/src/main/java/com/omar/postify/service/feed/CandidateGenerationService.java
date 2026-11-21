package com.omar.postify.service.feed;

import com.omar.postify.dto.CandidateGenerationContext;
import com.omar.postify.dto.FeedCandidate;
import com.omar.postify.dto.FeedExclusionContext;
import com.omar.postify.entities.enums.CandidateSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateGenerationService {

    private static final double INTEREST_RATIO = 0.60;
    private static final double FOLLOWING_RATIO = 0.25;
    private static final Map<CandidateSource, Integer> SOURCE_PRIORITY =
            Map.of(
                    CandidateSource.FOLLOWING, 3,
                    CandidateSource.INTEREST, 2,
                    CandidateSource.EXPLORATION, 1
            );

    private final InterestCandidateGenerator interestCandidateGenerator;
    private final FollowingCandidateGenerator followingCandidateGenerator;
    private final ExplorationCandidateGenerator explorationCandidateGenerator;

    public List<FeedCandidate> generate(
            CandidateGenerationContext context
    ) {

        int target = context.candidateLimit();

        List<FeedCandidate> candidates = new ArrayList<>();
        Set<Long> generatedPostIds = new HashSet<>();

        int interestCapacity =
                (int) Math.round(target * INTEREST_RATIO);

        int followingCapacity =
                (int) Math.round(target * FOLLOWING_RATIO);

        int explorationCapacity =
                target - interestCapacity - followingCapacity;

        boolean interestExhausted = false;
        boolean followingExhausted = false;
        boolean explorationExhausted = false;

        // -------------------------
        // Initial allocation
        // -------------------------

        List<FeedCandidate> interestCandidates =
                interestCandidateGenerator.generate(
                        context,
                        interestCapacity
                );


        addUnique(
                candidates,
                generatedPostIds,
                interestCandidates
        );


        if (interestCandidates.size() < interestCapacity) {
            interestExhausted = true;
        }

        List<FeedCandidate> followingCandidates =
                followingCandidateGenerator.generate(
                        contextWithGeneratedPosts(context, generatedPostIds),
                        followingCapacity
                );

        addUnique(
                candidates,
                generatedPostIds,
                followingCandidates
        );

        if (followingCandidates.size() < followingCapacity) {
            followingExhausted = true;
        }

        List<FeedCandidate> explorationCandidates =
                explorationCandidateGenerator.generate(
                        contextWithGeneratedPosts(context, generatedPostIds),
                        explorationCapacity
                );

        addUnique(
                candidates,
                generatedPostIds,
                explorationCandidates
        );

        if (explorationCandidates.size() < explorationCapacity) {
            explorationExhausted = true;
        }

        // -------------------------
        // Refill unused capacity
        // -------------------------

        while (candidates.size() < target) {

            int remaining = target - candidates.size();

            if (!interestExhausted) {

                List<FeedCandidate> moreInterest =
                        interestCandidateGenerator.generate(
                                contextWithGeneratedPosts(
                                        context,
                                        generatedPostIds
                                ),
                                remaining
                        );

                addUnique(
                        candidates,
                        generatedPostIds,
                        moreInterest
                );

                if (moreInterest.size() < remaining) {
                    interestExhausted = true;
                }

                if (candidates.size() >= target) {
                    break;
                }
            }

            remaining = target - candidates.size();

            if (!followingExhausted && remaining > 0) {

                List<FeedCandidate> moreFollowing =
                        followingCandidateGenerator.generate(
                                contextWithGeneratedPosts(
                                        context,
                                        generatedPostIds
                                ),
                                remaining
                        );

                addUnique(
                        candidates,
                        generatedPostIds,
                        moreFollowing
                );

                if (moreFollowing.size() < remaining) {
                    followingExhausted = true;
                }

                if (candidates.size() >= target) {
                    break;
                }
            }

            remaining = target - candidates.size();

            if (!explorationExhausted && remaining > 0) {

                List<FeedCandidate> moreExploration =
                        explorationCandidateGenerator.generate(
                                contextWithGeneratedPosts(
                                        context,
                                        generatedPostIds
                                ),
                                remaining
                        );

                addUnique(
                        candidates,
                        generatedPostIds,
                        moreExploration
                );

                if (moreExploration.size() < remaining) {
                    explorationExhausted = true;
                }
            }

            // All sources are exhausted.
            if (interestExhausted
                    && followingExhausted
                    && explorationExhausted) {
                break;
            }
        }

        log.info("===== CANDIDATE GENERATION =====");
        log.info("User: {}", context.identity().isAuthenticated());
        log.info("Target: {}", target);
        log.info("Final candidates: {}", candidates.size());

        Map<CandidateSource, Long> sourceCounts =
                candidates.stream()
                        .collect(Collectors.groupingBy(
                                FeedCandidate::source,
                                Collectors.counting()
                        ));

        log.info("Final source distribution:");
        log.info("INTEREST: {}", sourceCounts.getOrDefault(
                CandidateSource.INTEREST, 0L
        ));
        log.info("FOLLOWING: {}", sourceCounts.getOrDefault(
                CandidateSource.FOLLOWING, 0L
        ));
        log.info("EXPLORATION: {}", sourceCounts.getOrDefault(
                CandidateSource.EXPLORATION, 0L
        ));
        log.info(
                "Candidate IDs: {}",
                candidates.stream()
                        .map(FeedCandidate::postId)
                        .toList()
        );

        log.info("================================");

        log.info("===== Personalization =====");

        log.info(
                "Interest candidates: {}",
                candidates.stream()
                        .filter(c -> c.source() == CandidateSource.INTEREST)
                        .map(c -> "post=" + c.postId()
                                + ", relevance=" + c.relevanceScore())
                        .toList()
        );

        log.info(
                "Following candidates: {}",
                candidates.stream()
                        .filter(c -> c.source() == CandidateSource.FOLLOWING)
                        .map(c -> "post=" + c.postId()
                                + ", relevance=" + c.relevanceScore())
                        .toList()
        );

        log.info(
                "Exploration candidates: {}",
                candidates.stream()
                        .filter(c -> c.source() == CandidateSource.EXPLORATION)
                        .map(c -> "post=" + c.postId()
                                + ", relevance=" + c.relevanceScore())
                        .toList()
        );

        return deduplicate(candidates);
    }

    private void addUnique(
            List<FeedCandidate> candidates,
            Set<Long> generatedPostIds,
            List<FeedCandidate> newCandidates
    ) {

        for (FeedCandidate candidate : newCandidates) {

            if (generatedPostIds.add(candidate.postId())) {
                candidates.add(candidate);
            }
        }
    }

    private CandidateGenerationContext contextWithGeneratedPosts(
            CandidateGenerationContext context,
            Set<Long> generatedPostIds
    ) {

        Set<Long> seenPostIds =
                new HashSet<>(context.exclusions().seenPostIds());

        seenPostIds.addAll(generatedPostIds);

        FeedExclusionContext exclusions =
                new FeedExclusionContext(
                        seenPostIds,
                        context.exclusions().userPostIds()
                );

        return new CandidateGenerationContext(
                context.identity(),
                context.candidateLimit(),
                context.followingUserIds(),
                exclusions,
                context.sessionStartedAt()
        );
    }

    private List<FeedCandidate> deduplicate(
            List<FeedCandidate> candidates
    ) {
        Map<Long, FeedCandidate> unique = new LinkedHashMap<>();

        for (FeedCandidate candidate : candidates) {

            FeedCandidate existing =
                    unique.get(candidate.postId());

            if (existing == null) {
                unique.put(candidate.postId(), candidate);
                continue;
            }

            if (isBetter(candidate, existing)) {
                unique.put(candidate.postId(), candidate);
            }
        }

        return new ArrayList<>(unique.values());
    }

    private boolean isBetter(
            FeedCandidate candidate,
            FeedCandidate existing
    ) {
        int scoreComparison =
                Double.compare(
                        candidate.relevanceScore(),
                        existing.relevanceScore()
                );

        if (scoreComparison != 0) {
            return scoreComparison > 0;
        }

        return SOURCE_PRIORITY.getOrDefault(
                candidate.source(), 0
        ) > SOURCE_PRIORITY.getOrDefault(
                existing.source(), 0
        );
    }
}