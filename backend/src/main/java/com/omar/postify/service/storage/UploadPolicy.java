package com.omar.postify.service.storage;

import com.omar.postify.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class UploadPolicy {

    @Value("${postify.upload.max-file-bytes:104857600}")
    private long maxFileBytes;

    @Value("${postify.upload.max-avatar-bytes:5242880}")
    private long maxAvatarBytes;

    @Value("${postify.upload.max-image-pixels:40000000}")
    private long maxImagePixels;

    public void validateFileSize(MultipartFile upload, boolean avatar) {
        long max = avatar ? maxAvatarBytes : maxFileBytes;
        if (upload.getSize() > max) {
            throw new BadRequestException(
                    avatar
                            ? "Avatar exceeds the maximum size of 5 MB."
                            : "Media exceeds the configured maximum upload size."
            );
        }
    }

    public long maxImagePixels() {
        return maxImagePixels;
    }
}
