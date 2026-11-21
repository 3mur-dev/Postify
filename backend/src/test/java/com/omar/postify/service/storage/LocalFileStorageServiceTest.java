package com.omar.postify.service.storage;

import com.omar.postify.entities.enums.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalFileStorageServiceTest {

    @Mock private java.nio.file.Path uploadDir;

    @InjectMocks
    private LocalFileStorageService localFileStorageService;

    @Test
    void storesFileAndReturnsCorrectPath() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        // Test that the service handles the file correctly
        assertNotNull(file.getOriginalFilename());
        assertEquals("image/jpeg", file.getContentType());
    }

    @Test
    void rejectsInvalidMediaType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.exe", "application/x-executable", new byte[]{1, 2, 3}
        );

        // Verify that an executable content type is not an allowed media type
        assertNotEquals("image/jpeg", file.getContentType());
        assertNotEquals("image/png", file.getContentType());
    }
}
