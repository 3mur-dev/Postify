package com.omar.postify.service.ai;

import com.omar.postify.dto.TopicCandidate;
import com.omar.postify.entities.PostMedia;

import java.util.List;

public interface AIAnalyzer {

    List<TopicCandidate> analyze(
            String content,
            List<PostMedia> media
    );
}