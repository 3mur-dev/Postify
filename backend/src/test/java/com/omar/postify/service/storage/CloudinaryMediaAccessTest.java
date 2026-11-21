package com.omar.postify.service.storage;

import com.cloudinary.Cloudinary;
import com.omar.postify.entities.PostMedia;
import com.omar.postify.entities.enums.MediaType;
import com.omar.postify.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryMediaAccessTest {

    @Mock private Cloudinary cloudinary;
    @Mock private UploadPolicy uploadPolicy;
    @Mock private RasterImageUpload rasterImageUpload;
    @Mock private VideoUploadValidator videoUploadValidator;

    @InjectMocks
    private CloudinaryFileStorageService cloudinaryService;

    @Test
    void throwsWhenMediaUrlIsBlank() {
        PostMedia media = PostMedia.builder()
                .url("")
                .type(MediaType.IMAGE)
                .build();

        assertThrows(BadRequestException.class, () -> {
            cloudinaryService.loadPostMedia(media);
        });
    }

    @Test
    void throwsWhenMediaUrlIsNull() {
        PostMedia media = PostMedia.builder()
                .url(null)
                .type(MediaType.IMAGE)
                .build();

        assertThrows(BadRequestException.class, () -> {
            cloudinaryService.loadPostMedia(media);
        });
    }
}
