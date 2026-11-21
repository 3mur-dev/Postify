package com.omar.postify.service.storage;

import com.omar.postify.dto.StorageMediaResult;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.PostMedia;
import com.omar.postify.entities.enums.MediaType;
import com.omar.postify.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
@ConditionalOnProperty(
        name = "app.storage.provider",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalFileStorageService implements FileStorageService {

    private final UploadPolicy uploadPolicy;
    private final RasterImageUpload rasterImageUpload;
    private final VideoUploadValidator videoUploadValidator;

    @Value("${app.storage.base-path}")
    private String basePath;

    @Override
    public StorageMediaResult storeMedia(MultipartFile media) {
        validateMedia(media);
        uploadPolicy.validateFileSize(media, false);

        String originalName = StringUtils.cleanPath(
                Objects.requireNonNull(media.getOriginalFilename())
        );

        if (originalName.contains("..")) {
            throw new BadRequestException("Invalid file path");
        }

        String contentType = media.getContentType();

        boolean isImage =
                contentType != null && contentType.startsWith("image/");

        boolean isVideo =
                contentType != null && contentType.startsWith("video/");

        Path uploadDir;
        String publicPath;
        MediaType mediaType;

        if (isImage) {
            uploadDir = getPostImageUploadDir();
            publicPath = "/images/posts/";
            mediaType = MediaType.IMAGE;

        } else if (isVideo) {
            uploadDir = getPostVideoUploadDir();
            publicPath = "/videos/posts/";
            mediaType = MediaType.VIDEO;

        } else {
            throw new BadRequestException("Unsupported media type");
        }

        try {
            Files.createDirectories(uploadDir);
            String extension = "";

            String fileName;
            Long width = null;
            Long height = null;
            Long durationMs = null;

            if (isImage) {
                Path tempFile = Files.createTempFile(uploadDir, "postify-upload-", ".tmp");
                try {
                    RasterImageUpload.ImageInfo imageInfo = rasterImageUpload.normalize(media, tempFile);
                    fileName = UUID.randomUUID() + "." + imageInfo.extension();
                    Path target = uploadDir.resolve(fileName).normalize();
                    if (!target.startsWith(uploadDir)) {
                        throw new BadRequestException("Invalid file path");
                    }
                    Files.move(tempFile, target);
                    width = (long) imageInfo.width();
                    height = (long) imageInfo.height();
                } finally {
                    Files.deleteIfExists(tempFile);
                }
            } else {
                Path tempFile = Files.createTempFile(uploadDir, "postify-upload-", ".tmp");
                try {
                    media.transferTo(tempFile.toFile());
                    extension = "." + videoUploadValidator.validate(media, tempFile);
                    fileName = UUID.randomUUID() + extension;
                    Path target = uploadDir.resolve(fileName).normalize();
                    if (!target.startsWith(uploadDir)) {
                        throw new BadRequestException("Invalid file path");
                    }
                    Files.move(tempFile, target);
                } finally {
                    try {
                        Files.deleteIfExists(tempFile);
                    } catch (IOException cleanupException) {
                        log.error("Failed to remove video upload temporary file {}", tempFile, cleanupException);
                    }
                }
            }

            return new StorageMediaResult(
                    publicPath + fileName,
                    mediaType,
                    width,
                    height,
                    durationMs
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to store media",
                    e
            );
        }
    }

    @Override
    public Resource loadPostMedia(PostMedia media) {
        String url = media.getUrl();
        Path uploadDirectory;
        String prefix;
        if (media.getType() == MediaType.IMAGE) {
            uploadDirectory = getPostImageUploadDir();
            prefix = "/images/posts/";
        } else if (media.getType() == MediaType.VIDEO) {
            uploadDirectory = getPostVideoUploadDir();
            prefix = "/videos/posts/";
        } else {
            throw new BadRequestException("Unsupported media type");
        }

        if (url == null || !url.startsWith(prefix)) {
            throw new BadRequestException("Media not found");
        }

        Path path = uploadDirectory.resolve(url.substring(prefix.length())).normalize();
        if (!path.startsWith(uploadDirectory) || !Files.isRegularFile(path)) {
            throw new BadRequestException("Media not found");
        }
        return new FileSystemResource(path);
    }

    @Override
    public String storeAvatar(MultipartFile avatar) {
        validateAvatar(avatar);
        uploadPolicy.validateFileSize(avatar, true);

        try {
            Files.createDirectories(getAvatarUploadDir());

            Path tempFile = Files.createTempFile(getAvatarUploadDir(), "postify-upload-", ".tmp");
            try {
                RasterImageUpload.ImageInfo imageInfo =
                        rasterImageUpload.normalize(avatar, tempFile);
                String fileName = UUID.randomUUID() + "." + imageInfo.extension();
                Path target = getAvatarUploadDir().resolve(fileName).normalize();
                if (!target.startsWith(getAvatarUploadDir())) {
                    throw new BadRequestException("Invalid file path");
                }
                Files.move(tempFile, target);
                return "/images/avatars/" + fileName;
            } finally {
                Files.deleteIfExists(tempFile);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to store avatar", e);
        }
    }

    @Override
    public void deletePostMediaIfExists(Post post) {

        if (post.getMedia() == null || post.getMedia().isEmpty()) {
            return;
        }

        for (PostMedia media : post.getMedia()) {

            String mediaUrl = media.getUrl();

            String mediaDirectory;
            Path uploadDirectory;
            if (mediaUrl != null && mediaUrl.startsWith("/images/posts/")) {
                mediaDirectory = "/images/posts/";
                uploadDirectory = getPostImageUploadDir();
            } else if (mediaUrl != null && mediaUrl.startsWith("/videos/posts/")) {
                mediaDirectory = "/videos/posts/";
                uploadDirectory = getPostVideoUploadDir();
            } else {
                continue;
            }

            String filename =
                    mediaUrl.substring(mediaDirectory.length());

            Path target = uploadDirectory
                    .resolve(filename)
                    .normalize();

            if (!target.startsWith(uploadDirectory)) {
                continue;
            }

            try {
                Files.deleteIfExists(target);
            } catch (IOException e) {
                log.warn("Failed to delete post media: {}", target, e);
            }
        }
    }

    @Override
    public void deleteAvatarIfExists(String avatarUrl) {
        if (avatarUrl == null ||
                !avatarUrl.startsWith("/images/avatars/")) {
            return;
        }

        String filename =
                avatarUrl.substring("/images/avatars/".length());

        Path target = getAvatarUploadDir()
                .resolve(filename)
                .normalize();

        if (!target.startsWith(getAvatarUploadDir())) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Failed to delete avatar: {}", target, e);
        }
    }

    @Override
    public String getAnalysisThumbnailUrl(PostMedia media) {

        if (media.getType() == MediaType.IMAGE) {
            return media.getUrl();
        }

        if (media.getType() != MediaType.VIDEO) {
            throw new IllegalArgumentException(
                    "Unsupported media type: " + media.getType()
            );
        }

        try {
            String filename = media.getUrl()
                    .substring("/videos/posts/".length());

            Path videoPath = getPostVideoUploadDir()
                    .resolve(filename)
                    .normalize();

            if (!videoPath.startsWith(getPostVideoUploadDir())) {
                throw new IllegalArgumentException("Invalid video path");
            }

            Path thumbnailDir = Paths.get(
                    basePath,
                    "images",
                    "video-thumbnails"
            ).toAbsolutePath().normalize();

            Files.createDirectories(thumbnailDir);

            String thumbnailName =
                    UUID.randomUUID() + ".jpg";

            Path thumbnailPath = thumbnailDir
                    .resolve(thumbnailName)
                    .normalize();

            Process process = new ProcessBuilder(
                    "ffmpeg",
                    "-y",
                    "-ss", "00:00:01",
                    "-i", videoPath.toString(),
                    "-frames:v", "1",
                    "-vf", "scale=640:-1",
                    thumbnailPath.toString()
            )
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();

            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                Files.deleteIfExists(thumbnailPath);
                throw new RuntimeException("FFmpeg thumbnail generation timed out");
            }
            int exitCode = process.exitValue();

            if (exitCode != 0) {
                throw new RuntimeException(
                        "FFmpeg failed to generate thumbnail"
                );
            }

            return "/images/video-thumbnails/" + thumbnailName;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Failed to generate video thumbnail",
                    e
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate video thumbnail", e);
        }
    }

    private void validateAvatar(MultipartFile avatar) {

        if (avatar == null || avatar.isEmpty()) {
            throw new BadRequestException("Avatar is required");
        }

        if (!List.of("image/png", "image/jpeg", "image/webp")
                .contains(avatar.getContentType())) {
            throw new BadRequestException(
                    "Only PNG, JPEG, and WebP images are allowed."
            );
        }
    }

    private void validateMedia(MultipartFile media) {

        if (media == null || media.isEmpty()) {
            throw new BadRequestException("Media is required");
        }

        String contentType = media.getContentType();

        if (contentType == null) {
            throw new BadRequestException("Media type is missing");
        }

        List<String> allowedTypes = List.of(
                // Images
                "image/jpeg",
                "image/png",
                "image/webp",

                // Videos
                "video/mp4",
                "video/webm",
                "video/quicktime"
        );

        if (!allowedTypes.contains(contentType)) {
            throw new BadRequestException(
                    "Unsupported media type: " + contentType
            );
        }
    }


    private Path getAvatarUploadDir() {
        return Paths.get(basePath, "avatars")
                .toAbsolutePath()
                .normalize();
    }
    private Path getPostImageUploadDir() {
        return Paths.get(basePath, "images", "posts")
                .toAbsolutePath()
                .normalize();
    }

    private Path getPostVideoUploadDir() {
        return Paths.get(basePath, "videos", "posts")
                .toAbsolutePath()
                .normalize();
    }
}