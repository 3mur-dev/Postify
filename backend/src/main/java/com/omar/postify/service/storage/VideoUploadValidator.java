package com.omar.postify.service.storage;

import com.omar.postify.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Component
public class VideoUploadValidator {

    private static final byte[] WEBM_SIGNATURE = {
            0x1A, 0x45, (byte) 0xDF, (byte) 0xA3
    };

    @Value("${postify.upload.max-video-duration-seconds:120}")
    private long maxDurationSeconds;

    @Value("${postify.upload.ffprobe-executable:ffprobe}")
    private String ffprobeExecutable;

    @Value("${postify.upload.ffprobe-timeout-seconds:10}")
    private long probeTimeoutSeconds;

    public String validate(MultipartFile upload, Path storedFile) {
        String extension = validateHeader(upload);
        validateDuration(storedFile);
        return extension;
    }

    static String validateHeader(MultipartFile upload) {
        String contentType = upload.getContentType();
        String extension = switch (contentType == null ? "" : contentType) {
            case "video/mp4" -> "mp4";
            case "video/webm" -> "webm";
            case "video/quicktime" -> "mov";
            default -> throw new BadRequestException("Unsupported video type.");
        };

        try (InputStream input = upload.getInputStream()) {
            byte[] header = input.readNBytes(12);
            boolean isValid = switch (extension) {
                case "webm" -> startsWith(header, WEBM_SIGNATURE);
                case "mp4", "mov" -> header.length >= 8
                        && header[4] == 'f'
                        && header[5] == 't'
                        && header[6] == 'y'
                        && header[7] == 'p';
                default -> false;
            };

            if (!isValid) {
                throw new BadRequestException(
                        "Uploaded video content does not match its declared type."
                );
            }

            return extension;
        } catch (IOException e) {
            throw new BadRequestException("Unable to read uploaded video.");
        }
    }

    private void validateDuration(Path file) {
        Path outputFile = null;
        Process process = null;
        try {
            outputFile = Files.createTempFile("postify-ffprobe-", ".txt");
            process = new ProcessBuilder(
                    ffprobeExecutable,
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    file.toAbsolutePath().toString()
            )
                    .redirectErrorStream(true)
                    .redirectOutput(outputFile.toFile())
                    .start();

            if (!process.waitFor(probeTimeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new BadRequestException("Unable to validate video duration.");
            }
            if (process.exitValue() != 0) {
                throw new BadRequestException("Uploaded video is invalid or unsupported.");
            }

            String durationText = Files.readString(outputFile).trim();
            double duration = Double.parseDouble(durationText);
            if (!Double.isFinite(duration) || duration <= 0 || duration > maxDurationSeconds) {
                throw new BadRequestException(
                        "Video duration exceeds the configured maximum of "
                                + maxDurationSeconds + " seconds."
                );
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Video probing failed; configure an available ffprobe executable.",
                    e
            );
        } catch (InterruptedException e) {
            if (process != null) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Video probing was interrupted.", e);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Uploaded video duration could not be determined.");
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            if (outputFile != null) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to remove ffprobe temporary output.", e);
                }
            }
        }
    }

    private static boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }

        for (int i = 0; i < prefix.length; i++) {
            if (value[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
