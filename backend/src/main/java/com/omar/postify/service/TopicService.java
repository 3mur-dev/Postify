package com.omar.postify.service;

import com.omar.postify.dto.TopicCandidate;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.PostTopic;
import com.omar.postify.entities.Topic;
import com.omar.postify.repository.PostTopicRepository;
import com.omar.postify.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final PostTopicRepository postTopicRepository;

    @Transactional
    public void assignTopics(Post post, List<TopicCandidate> candidates) {

        Set<Long> assignedTopicIds = postTopicRepository
                .findAllByPost(post)
                .stream()
                .map(pt -> pt.getTopic().getId())
                .collect(Collectors.toSet());

        candidates.stream()
                .filter(candidate -> candidate.confidence() >= 0.50)
                .sorted(
                        Comparator.comparingDouble(
                                TopicCandidate::confidence
                        ).reversed()
                )
                .limit(3)
                .forEach(candidate ->
                        assignTopic(post, candidate, assignedTopicIds)
                );
    }

    /**
     * Replace all existing topic assignments for a post with new ones.
     * Removes outdated assignments first, then assigns new topics.
     * Does not delete Topic records — only PostTopic assignments.
     */
    @Transactional
    public void replaceTopics(Post post, List<TopicCandidate> candidates) {
        // Remove all existing assignments for this post
        List<PostTopic> existing = postTopicRepository.findAllByPost(post);
        if (!existing.isEmpty()) {
            postTopicRepository.deleteAll(existing);
        }

        // Assign new topics with the same rules as assignTopics
        Set<Long> assignedTopicIds = Set.of();

        candidates.stream()
                .filter(candidate -> candidate.confidence() >= 0.50)
                .sorted(
                        Comparator.comparingDouble(
                                TopicCandidate::confidence
                        ).reversed()
                )
                .limit(3)
                .forEach(candidate ->
                        assignTopic(post, candidate, assignedTopicIds)
                );
    }


    private void assignTopic(Post post, TopicCandidate topicCandidate, Set<Long> assignedTopicIds) {

        String title = topicCandidate.title();
        String slug = toSlug(title);

        Topic topic = topicRepository
                .findBySlug(slug)
                .orElseGet(() -> topicRepository.save(
                        Topic.builder()
                                .title(title)
                                .slug(slug)
                                .parent(null)
                                .build()
                ));

        // Skip if this topic is already assigned to the post.
        if (assignedTopicIds.contains(topic.getId())) {
            return;
        }

        postTopicRepository.save(
                PostTopic.builder()
                        .post(post)
                        .topic(topic)
                        .confidence(topicCandidate.confidence())
                        .build()
        );
    }

    private String toSlug(String title) {
        return title
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }
}