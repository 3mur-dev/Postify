package com.omar.postify.util;

import com.omar.postify.entities.Post;
import com.omar.postify.entities.Report;
import com.omar.postify.entities.User;
import com.omar.postify.entities.enums.ReportReason;
import com.omar.postify.entities.enums.ReportStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;


public class ReportSpecification {

    public static Specification<Report> withFilters(
            String search,
            ReportStatus status,
            ReportReason reason
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(
                        cb.equal(root.get("status"), status)
                );
            }

            if (reason != null) {
                predicates.add(
                        cb.equal(root.get("reason"), reason)
                );
            }

            if (search != null && !search.isBlank()) {

                Join<Report, User> reporter =
                        root.join("reporter", JoinType.LEFT);

                Join<Report, Post> post =
                        root.join("post", JoinType.LEFT);

                String pattern =
                        "%" + search.toLowerCase() + "%";

                predicates.add(
                        cb.or(
                                cb.like(
                                        cb.lower(
                                                reporter.get("username")
                                        ),
                                        pattern
                                ),
                                cb.like(
                                        cb.lower(
                                                post.get("content")
                                        ),
                                        pattern
                                )
                        )
                );
            }

            return cb.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }
}