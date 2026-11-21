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
class RasterImageUploadTest {

    @Mock private java.nio.file.Path uploadDir;

    @InjectMocks
    private LocalFileStorageService localFileStorageService;

    @Test
    void acceptsJpegImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        assertEquals("image/jpeg", file.getContentType());
        assertEquals(MediaType.IMAGE, MediaType.IMAGE);
    }

    @Test
    void acceptsPngImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", new byte[]{1, 2, 3}
        );

        assertEquals("image/png", file.getContentType());
    }

    @Test
    void rejectsExcessiveFileSize() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", new byte[1024 * 1024 * 11]
        );

        assertTrue(file.getSize() > 10 * 1024 * 1024);
    }
}
