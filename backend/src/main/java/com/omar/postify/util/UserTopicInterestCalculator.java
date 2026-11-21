package com.omar.postify.util;

import com.omar.postify.entities.TopicFeature;
import org.springframework.stereotype.Component;

@Component
public class UserTopicInterestCalculator {

    public double calculate(TopicFeature feature) {

        if (feature == null || feature.getEventCount() == 0) {
            return 0.0;
        }

        double score = 0.0;

        score += feature.getViewCount() * 0.05;
        score += feature.getViewStartCount() * 0.10;
        score += feature.getViewEndCount() * 0.15;

        score += feature.getLikeCount() * 1.00;
        score += feature.getSaveCount() * 1.25;
        score += feature.getCommentCount() * 1.25;
        score += feature.getMediaCompleteCount() * 0.75;
        score += feature.getLinkClickCount() * 0.50;

        score -= feature.getMediaSkipCount() * 0.15;

        if (feature.getCompletionRatioSamples() > 0) {
            double averageCompletion =
                    feature.getTotalCompletionRatio()
                            / feature.getCompletionRatioSamples();

            score += averageCompletion * 0.50;
        }

        if (feature.getViewCount() > 0) {
            double averageDwellMs =
                    (double) feature.getTotalViewDurationMs()
                            / feature.getViewCount();

            score += Math.min(averageDwellMs / 10_000.0, 1.0) * 0.50;
        }

        return normalize(score);
    }

    private double normalize(double score) {
        return score / (1.0 + Math.abs(score));
    }
}