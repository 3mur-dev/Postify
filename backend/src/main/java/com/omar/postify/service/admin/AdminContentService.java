package com.omar.postify.service.admin;

import com.omar.postify.dto.admin.AdminPostDto;
import com.omar.postify.dto.admin.AdminPostFilterRequest;
import com.omar.postify.dto.AdminContext;
import com.omar.postify.dto.enums.PostSortField;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.enums.PostStatus;
import com.omar.postify.exception.ResourceNotFoundException;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.service.PostVectorSyncService;
import com.omar.postify.util.PostSpecification;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class AdminContentService {

    private final PostRepository postRepository;
    private final PostVectorSyncService postVectorSyncService;

    public Page<AdminPostDto> getPosts(AdminPostFilterRequest request) {


        Page<Post> posts = postRepository.findAll(
                PostSpecification.build(request),
                PageRequest.of(
                        request.pageOrDefault(),
                        request.sizeOrDefault(),
                        Sort.by(request.directionOrDefault(), mapSort(request.sortFieldOrDefault()))
                )
        );

        return posts.map(this::mapToDto);
    }

    public AdminPostDto getPost(String publicId) {
        return postRepository.findByPublicIdWithUserAndMedia(publicId)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    @Transactional
    public AdminPostDto hidePost(String publicId, AdminContext adminContext) {

        return postRepository.findByPublicIdWithUserAndStatusIsActive(publicId)
                .map(
                        post -> {
                            post.setStatus(PostStatus.HIDDEN);
                            post.setModeratedBy(adminContext.adminId());
                            post.setModeratedAt(LocalDateTime.now());
                            post.setDeletedAt(null);
                            Post saved = postRepository.save(post);
                            postVectorSyncService.scheduleSync(saved.getId());
                            return mapToDto(saved);
                        }
                ).orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    @Transactional
    public AdminPostDto restorePost(String publicId, AdminContext adminContext) {

        Post post = postRepository.findByPublicIdWithUserAndMedia(publicId)
                .filter(existing -> existing.getStatus() == PostStatus.HIDDEN)
                .orElseThrow(() -> new ResourceNotFoundException("Hidden post not found"));
        post.setStatus(PostStatus.ACTIVE);
        post.setDeletedAt(null);
        post.setModeratedBy(adminContext.adminId());
        post.setModeratedAt(LocalDateTime.now());
        Post saved = postRepository.save(post);
        postVectorSyncService.scheduleSync(saved.getId());
        return mapToDto(saved);
    }

    @Transactional
    public AdminPostDto softDeletePost(String publicId, AdminContext adminContext) {

        Post post = postRepository.findByPublicIdWithUserAndMedia(publicId)
                .filter(existing -> existing.getStatus() != PostStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        LocalDateTime now = LocalDateTime.now();
        post.setStatus(PostStatus.DELETED);
        post.setDeletedAt(now);
        post.setModeratedBy(adminContext.adminId());
        post.setModeratedAt(now);
        Post saved = postRepository.save(post);
        postVectorSyncService.scheduleSync(saved.getId());
        return mapToDto(saved);
    }


    private String mapSort(PostSortField field) {
        return switch (field) {
            case CREATED_AT -> "createdAt";
            case UPDATED_AT -> "updatedAt";
            case LIKE_COUNT -> "likeCount";
            case COMMENT_COUNT -> "commentCount";
            case REPORT_COUNT -> "reportCount";
            case STATUS -> "status";
        };
    }

    private AdminPostDto mapToDto(Post post) {
        return new AdminPostDto(
                post.getPublicId(),
                post.getUser().getPublicId(),
                post.getUser().getUsername(),
                post.getContent(),
                post.getMedia(),
                post.getStatus(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getReportCount(),
                post.getLastReportReason(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getDeletedAt() != null

        );
    }
}
