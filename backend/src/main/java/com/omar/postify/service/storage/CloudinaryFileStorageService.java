package com.omar.postify.service.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.omar.postify.dto.StorageMediaResult;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.PostMedia;
import com.omar.postify.entities.enums.MediaType;
import com.omar.postify.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryFileStorageService implements FileStorageService {

    private final Cloudinary cloudinary;
    private final UploadPolicy uploadPolicy;
    private final RasterImageUpload rasterImageUpload;
    private final VideoUploadValidator videoUploadValidator;

    @Value("${app.storage.base-path}")
    private String basePath;

    @Override
    public StorageMediaResult storeMedia(MultipartFile media) {
        validateMedia(media);
        uploadPolicy.validateFileSize(media, false);

        String contentType = media.getContentType();

        boolean isVideo =
                contentType != null && contentType.startsWith("video/");

        MediaType mediaType =
                isVideo ? MediaType.VIDEO : MediaType.IMAGE;

        String resourceType =
                isVideo ? "video" : "image";

        try {

            Map<?, ?> result;

            if (isVideo) {

                Path tempFile = Files.createTempFile(
                        "postify-upload-",
                        "." + VideoUploadValidator.validateHeader(media)
                );

                try {
                    media.transferTo(tempFile.toFile());
                    videoUploadValidator.validate(media, tempFile);

                    result = cloudinary.uploader().uploadLarge(
                            tempFile.toFile(),
                            ObjectUtils.asMap(
                                    "folder", "postify/posts",
                                    "resource_type", "video",
                                    "type", "authenticated",
                                    "chunk_size", 20 * 1024 * 1024
                            )
                    );

                } finally {
                    Files.deleteIfExists(tempFile);
                }

            } else {
                Path tempDir = Files.createTempDirectory("postify-upload-");
                Path normalizedFile = tempDir.resolve("image");
                Path uploadFile = null;
                try {
                    RasterImageUpload.ImageInfo imageInfo =
                            rasterImageUpload.normalize(media, normalizedFile);
                    String extension = imageInfo.extension();
                    uploadFile = tempDir.resolve("image." + extension);
                    Files.move(normalizedFile, uploadFile);
                    result = cloudinary.uploader().upload(
                            uploadFile.toFile(),
                            ObjectUtils.asMap(
                                    "folder", "postify/posts",
                                    "resource_type", "image",
                                    "type", "authenticated"
                            )
                    );
                } finally {
                    Files.deleteIfExists(normalizedFile);
                    if (uploadFile != null) {
                        Files.deleteIfExists(uploadFile);
                    }
                    Files.deleteIfExists(tempDir);
                }
            }

            String publicId = (String) result.get("public_id");
            String format = (String) result.get("format");
            String url = "cloudinary://" + resourceType + "/" + publicId + "." + format;

            Long width = result.get("width") != null
                    ? ((Number) result.get("width")).longValue()
                    : null;

            Long height = result.get("height") != null
                    ? ((Number) result.get("height")).longValue()
                    : null;

            Long durationMs = null;

            if (result.get("duration") != null) {
                double durationSeconds =
                        ((Number) result.get("duration")).doubleValue();

                durationMs = Math.round(durationSeconds * 1000);
            }

            return new StorageMediaResult(
                    url,
                    mediaType,
                    width,
                    height,
                    durationMs
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to upload media to Cloudinary",
                    e
            );
        }
    }

    @Override
    public Resource loadPostMedia(PostMedia media) {
        String url = media.getUrl();
        if (url == null || url.isBlank()) {
            throw new BadRequestException("Media not found");
        }
        if (isLegacyLocalMediaUrl(url)) {
            return loadLegacyLocalMedia(media, url);
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return loadLegacyRemoteMedia(media, url);
        }
        if (!url.startsWith("cloudinary://")) {
            log.warn(
                    "Unsupported stored media reference (id={}, type={}, reference={})",
                    media.getId(),
                    media.getType(),
                    url
            );
            throw new BadRequestException("Media not found");
        }

        String publicId = extractPublicId(url, media.getType());
        String extension = url.substring(url.lastIndexOf('.') + 1);
        String resourceType = media.getType() == MediaType.VIDEO ? "video" : "image";
        String signedUrl = cloudinary.url()
                .resourceType(resourceType)
                .type("authenticated")
                .secure(true)
                .signed(true)
                .format(extension)
                .generate(publicId);
        try {
            return new UrlResource(signedUrl);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Failed to build Cloudinary media URL", e);
        }
    }

    private Resource loadLegacyRemoteMedia(PostMedia media, String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if ("res.cloudinary.com".equalsIgnoreCase(host)
                    || "picsum.photos".equalsIgnoreCase(host)) {
                String secureUrl = new URI(
                        "https",
                        null,
                        host.toLowerCase(Locale.ROOT),
                        -1,
                        uri.getPath(),
                        uri.getQuery(),
                        uri.getFragment()
                ).toString();
                return new UrlResource(secureUrl);
            }
            if (("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host))
                    && uri.getPath() != null
                    && isLegacyLocalMediaUrl(uri.getPath())) {
                return loadLegacyLocalMedia(media, uri.getPath());
            }
            log.warn(
                    "Unsupported remote media location (id={}, host={}, path={})",
                    media.getId(),
                    host,
                    uri.getPath()
            );
            throw new BadRequestException("Media not found");
        } catch (URISyntaxException | MalformedURLException e) {
            throw new BadRequestException("Media not found");
        }
    }

    private boolean isLegacyLocalMediaUrl(String url) {
        return url.startsWith("/images/posts/")
                || url.startsWith("/videos/posts/")
                || url.startsWith("/uploads/posts/")
                || url.startsWith("/uploads/videos/posts/")
                || url.startsWith("uploads/posts/")
                || url.startsWith("uploads/videos/posts/")
                || url.startsWith("images/posts/")
                || url.startsWith("videos/posts/");
    }

    private Resource loadLegacyLocalMedia(PostMedia media, String url) {
        String prefix;
        List<String> directories;
        if (media.getType() == MediaType.IMAGE) {
            if (url.startsWith("/images/posts/") || url.startsWith("images/posts/")) {
                prefix = url.startsWith("/") ? "/images/posts/" : "images/posts/";
                directories = List.of("images/posts", "posts");
            } else {
                prefix = url.startsWith("/") ? "/uploads/posts/" : "uploads/posts/";
                directories = List.of("posts");
            }
        } else if (media.getType() == MediaType.VIDEO) {
            if (url.startsWith("/videos/posts/") || url.startsWith("videos/posts/")) {
                prefix = url.startsWith("/") ? "/videos/posts/" : "videos/posts/";
                directories = List.of("videos/posts");
            } else {
                prefix = url.startsWith("/") ? "/uploads/videos/posts/" : "uploads/videos/posts/";
                directories = List.of("videos/posts");
            }
        } else {
            log.warn(
                    "Unsupported media type for legacy media (id={}, type={}, reference={})",
                    media.getId(),
                    media.getType(),
                    url
            );
            throw new BadRequestException("Media not found");
        }

        if (!url.startsWith(prefix)) {
            throw new BadRequestException("Media not found");
        }

        for (String directory : directories) {
            Path root = Paths.get(basePath, directory).toAbsolutePath().normalize();
            Path file = root.resolve(url.substring(prefix.length())).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                continue;
            }
            try {
                if (file.toRealPath().startsWith(root.toRealPath())) {
                    return new FileSystemResource(file);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Unable to resolve legacy media path", e);
            }
        }
        log.warn(
                "Legacy media file not found (id={}, type={}, reference={})",
                media.getId(),
                media.getType(),
                url
        );
        throw new BadRequestException("Media not found");
    }

    @Override
    public void deletePostMediaIfExists(Post post) {

        if (post.getMedia() == null || post.getMedia().isEmpty()) {
            return;
        }

        for (PostMedia media : post.getMedia()) {

            if (media.getUrl() == null || media.getUrl().isBlank()) {
                continue;
            }

            try {
                String publicId = extractPublicId(
                        media.getUrl(),
                        media.getType()
                );

                if (publicId == null) {
                    log.warn(
                            "Could not extract Cloudinary public ID from: {}",
                            media.getUrl()
                    );
                    continue;
                }

                String resourceType =
                        media.getType() == MediaType.VIDEO
                                ? "video"
                                : "image";

                cloudinary.uploader().destroy(
                        publicId,
                        ObjectUtils.asMap(
                                "resource_type", resourceType,
                                "type", media.getUrl().startsWith("cloudinary://")
                                        ? "authenticated"
                                        : "upload"
                        )
                );

            } catch (Exception e) {
                log.warn(
                        "Failed to delete Cloudinary media: {}",
                        media.getUrl(),
                        e
                );
            }
        }
    }

    @Override
    public String storeAvatar(MultipartFile avatar) {
        validateAvatar(avatar);
        uploadPolicy.validateFileSize(avatar, true);

        try {
            Path tempDir = Files.createTempDirectory("postify-avatar-");
            Path normalizedFile = tempDir.resolve("avatar");
            Path uploadFile = null;
            try {
                RasterImageUpload.ImageInfo imageInfo =
                        rasterImageUpload.normalize(avatar, normalizedFile);
                String extension = imageInfo.extension();
                uploadFile = tempDir.resolve("avatar." + extension);
                Files.move(normalizedFile, uploadFile);
                Map<?, ?> result = cloudinary.uploader().upload(
                        uploadFile.toFile(),
                        ObjectUtils.asMap(
                                "folder", "postify/avatars",
                                "resource_type", "image"
                        )
                );
                return (String) result.get("secure_url");
            } finally {
                Files.deleteIfExists(normalizedFile);
                if (uploadFile != null) {
                    Files.deleteIfExists(uploadFile);
                }
                Files.deleteIfExists(tempDir);
            }

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to upload avatar to Cloudinary",
                    e
            );
        }
    }

    @Override
    public void deleteAvatarIfExists(String avatarUrl) {

        if (avatarUrl == null || avatarUrl.isBlank()) {
            return;
        }

        try {
            String publicId = extractPublicId(
                    avatarUrl,
                    MediaType.IMAGE
            );

            if (publicId == null) {
                return;
            }

            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(
                            "resource_type", "image",
                            "type", "upload"
                    )
            );

        } catch (Exception e) {
            log.warn(
                    "Failed to delete Cloudinary avatar: {}",
                    avatarUrl,
                    e
            );
        }
    }

    @Override
    public String getAnalysisThumbnailUrl(PostMedia media) {

        String url = media.getUrl();

        if (url.startsWith("/images/posts/")
                || url.startsWith("/videos/posts/")) {

            return generateLocalAnalysisThumbnail(media);
        }

        if (url.contains("res.cloudinary.com")) {

            return generateCloudinaryAnalysisThumbnail(media);
        }

        if (url.startsWith("cloudinary://")) {
            return generateCloudinaryAnalysisThumbnail(media);
        }

        if (url.startsWith("http://")
                || url.startsWith("https://")) {

            return url;
        }

        throw new IllegalArgumentException(
                "Unknown media storage URL: " + url
        );
    }

    private String extractPublicId(
            String mediaUrl,
            MediaType mediaType
    ) {

        if (mediaUrl != null && mediaUrl.startsWith("cloudinary://")) {
            String prefix = "cloudinary://" + (mediaType == MediaType.VIDEO ? "video/" : "image/");
            if (!mediaUrl.startsWith(prefix)) {
                return null;
            }
            String path = mediaUrl.substring(prefix.length());
            int extensionIndex = path.lastIndexOf('.');
            return extensionIndex < 0 ? path : path.substring(0, extensionIndex);
        }

        String resourceType =
                mediaType == MediaType.VIDEO
                        ? "video"
                        : "image";

        String marker = "/" + resourceType + "/upload/";

        int uploadIndex = mediaUrl.indexOf(marker);

        if (uploadIndex == -1) {
            return null;
        }

        String path = mediaUrl.substring(
                uploadIndex + marker.length()
        );

        if (path.startsWith("v")) {

            int slashIndex = path.indexOf('/');

            if (slashIndex != -1) {
                path = path.substring(slashIndex + 1);
            }
        }

        int extensionIndex = path.lastIndexOf('.');

        if (extensionIndex != -1) {
            path = path.substring(0, extensionIndex);
        }

        return path;
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
                "image/jpeg",
                "image/png",
                "image/webp",
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

    private void validateAvatar(MultipartFile avatar) {

        if (avatar == null || avatar.isEmpty()) {
            throw new BadRequestException("Avatar is required");
        }

        String contentType = avatar.getContentType();

        if (!List.of(
                "image/png",
                "image/jpeg",
                "image/webp"
        ).contains(contentType)) {
            throw new BadRequestException(
                    "Only PNG, JPEG, and WebP images are allowed."
            );
        }
    }
    private String generateCloudinaryAnalysisThumbnail(PostMedia media) {

        String publicId = extractPublicId(
                media.getUrl(),
                media.getType()
        );

        if (publicId == null) {
            throw new IllegalArgumentException(
                    "Could not extract Cloudinary public ID from: "
                            + media.getUrl()
            );
        }

        var urlBuilder = cloudinary.url()
                .resourceType(media.getType() == MediaType.VIDEO ? "video" : "image")
                .format("jpg");
        if (media.getUrl().startsWith("cloudinary://")) {
            urlBuilder.type("authenticated").signed(true);
        }
        return urlBuilder.generate(publicId);
    }

    private String generateLocalAnalysisThumbnail(PostMedia media) {

        if (media.getType() == MediaType.IMAGE) {
            return generateLocalImageDataUrl(media);

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

    private Path getPostVideoUploadDir() {
        return Paths.get(basePath, "videos", "posts")
                .toAbsolutePath()
                .normalize();
    }

    private String generateLocalImageDataUrl(PostMedia media) {

        String filename = media.getUrl()
                .substring("/images/posts/".length());

        Path imagePath = Paths.get(
                basePath,
                "images",
                "posts",
                filename
        ).toAbsolutePath().normalize();

        Path imageDir = Paths.get(
                basePath,
                "images",
                "posts"
        ).toAbsolutePath().normalize();

        if (!imagePath.startsWith(imageDir)) {
            throw new IllegalArgumentException("Invalid image path");
        }

        try {
            byte[] imageBytes = Files.readAllBytes(imagePath);

            String base64 = Base64.getEncoder()
                    .encodeToString(imageBytes);

            return "data:image/png;base64," + base64;

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read local image: " + imagePath,
                    e
            );
        }
    }
}