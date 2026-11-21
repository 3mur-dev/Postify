package com.omar.postify.util;

import com.omar.postify.dto.admin.AdminPostFilterRequest;
import com.omar.postify.entities.Post;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;


public class PostSpecification {

    public static Specification<Post> build(AdminPostFilterRequest r) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (r.search() != null && !r.search().isBlank()) {
                predicates.add(
                        cb.like(cb.lower(root.get("content")),
                                "%" + r.search().toLowerCase() + "%")
                );
            }

            if (r.status() != null) {
                predicates.add(cb.equal(root.get("status"), r.status()));
            }

            if (r.userId() != null) {
                predicates.add(cb.equal(root.get("user").get("id"), r.userId()));
            }

            if (r.reported() != null && r.reported()) {
                predicates.add(cb.greaterThan(root.get("reportCount"), 0));
            }

            if (r.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), r.from()));
            }

            if (r.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), r.to()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}