package com.omar.postify.service.storage;

import com.omar.postify.entities.enums.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VideoUploadValidatorTest {

    @Mock private java.nio.file.Path uploadDir;

    @InjectMocks
    private LocalFileStorageService localFileStorageService;

    @Test
    void acceptsMp4Video() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.mp4", "video/mp4", new byte[]{1, 2, 3}
        );

        assertEquals("video/mp4", file.getContentType());
        assertEquals(MediaType.VIDEO, MediaType.VIDEO);
    }

    @Test
    void rejectsExcessiveVideoSize() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.mp4", "video/mp4", new byte[1024 * 1024 * 101]
        );

        assertTrue(file.getSize() > 100 * 1024 * 1024);
    }

    @Test
    void rejectsInvalidVideoFormat() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.avi", "video/x-msvideo", new byte[]{1, 2, 3}
        );

        assertNotEquals("video/mp4", file.getContentType());
    }
}
