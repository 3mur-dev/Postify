package com.omar.postify.controller;

import com.omar.postify.entities.PostMedia;
import com.omar.postify.entities.enums.PostStatus;
import com.omar.postify.exception.ResourceNotFoundException;
import com.omar.postify.repository.PostMediaRepository;
import com.omar.postify.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PostMediaController {

    private final PostMediaRepository postMediaRepository;
    private final FileStorageService fileStorageService;

    @GetMapping("/api/post-media/{mediaId}")
    public ResponseEntity<Resource> getPostMedia(@PathVariable Long mediaId) {
        PostMedia media = postMediaRepository.findByIdAndPostStatus(
                        mediaId,
                        PostStatus.ACTIVE
                )
                .orElseThrow(() -> new ResourceNotFoundException("Media not found"));
        Resource resource = fileStorageService.loadPostMedia(media);
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElseGet(() -> media.getType()
                        == com.omar.postify.entities.enums.MediaType.VIDEO
                        ? MediaTypeFactory.getMediaType("file.mp4")
                        .orElse(MediaType.APPLICATION_OCTET_STREAM)
                        : MediaTypeFactory.getMediaType("file.jpg")
                        .orElse(MediaType.APPLICATION_OCTET_STREAM));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }
}
