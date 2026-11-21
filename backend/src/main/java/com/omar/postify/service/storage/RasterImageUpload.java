package com.omar.postify.service.storage;

import com.omar.postify.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
final class RasterImageUpload {

    private final UploadPolicy uploadPolicy;

    private static final Map<String, String> FORMATS = Map.of(
            "jpeg", "jpg",
            "jpg", "jpg",
            "png", "png",
            "webp", "webp"
    );

    ImageInfo normalize(MultipartFile upload, Path target) throws IOException {
        try (InputStream input = upload.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {

            if (imageInput == null) {
                throw new BadRequestException("Uploaded file is not a supported raster image.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new BadRequestException("Uploaded file is not a supported raster image.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String extension = FORMATS.get(format);
                if (extension == null || !matchesDeclaredContentType(upload.getContentType(), extension)) {
                    throw new BadRequestException("Uploaded image content does not match a supported image type.");
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0
                        || (long) width * height > uploadPolicy.maxImagePixels()) {
                    throw new BadRequestException("Image dimensions exceed the supported limit.");
                }

                BufferedImage image = reader.read(0);
                if (image == null || !ImageIO.write(image, format, target.toFile())) {
                    throw new BadRequestException("Uploaded file is not a supported raster image.");
                }

                return new ImageInfo(extension, width, height);
            } finally {
                reader.dispose();
            }
        }
    }

    private static boolean matchesDeclaredContentType(String contentType, String extension) {
        return switch (extension) {
            case "jpg" -> "image/jpeg".equals(contentType);
            case "png" -> "image/png".equals(contentType);
            case "webp" -> "image/webp".equals(contentType);
            default -> false;
        };
    }

    record ImageInfo(String extension, int width, int height) {
    }
}
