package org.project.floodalert.floodcore.service.impl;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodcore.config.FirebaseStorageProperties;
import org.project.floodalert.floodcore.dto.response.FileUploadResponse;
import org.project.floodalert.floodcore.exception.CoreErrorCode;
import org.project.floodalert.floodcore.service.FirebaseStorageService;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseStorageServiceImpl implements FirebaseStorageService {

    private final Storage storage;
    private final FirebaseStorageProperties properties;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final String FLOOD_REPORTS_PATH = "flood_reports";

    @Override
    public FileUploadResponse generateUploadUrl(UUID userId, String extension) {
        validateExtension(extension);
        
        String filePath = buildFilePath(userId, extension);
        log.info("Generating upload URL for userId={}, filePath={}", userId, filePath);

        try {
            BlobId blobId = BlobId.of(properties.getBucketName(), filePath);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(getContentType(extension))
                    .build();

            URL signedUrl = storage.signUrl(
                    blobInfo,
                    properties.getSignedUrlDurationMinutes(),
                    TimeUnit.MINUTES,
                    Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                    Storage.SignUrlOption.withV4Signature()
            );

            String publicUrl = buildPublicUrl(filePath);
            
            log.info("Successfully generated upload URL for userId={}", userId);
            
            return new FileUploadResponse(
                    signedUrl.toString(),
                    publicUrl,
                    filePath,
                    properties.getSignedUrlDurationMinutes()
            );
            
        } catch (Exception e) {
            log.error("Failed to generate upload URL for userId={}", userId, e);
            throw new AppException(CoreErrorCode.FIREBASE_STORAGE_ERROR);
        }
    }

    private void validateExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            throw new AppException(CoreErrorCode.INVALID_FILE_EXTENSION);
        }
        
        String normalizedExt = extension.toLowerCase().trim();
        if (!ALLOWED_EXTENSIONS.contains(normalizedExt)) {
            throw new AppException(CoreErrorCode.INVALID_FILE_EXTENSION);
        }
    }

    private String buildFilePath(UUID userId, String extension) {
        long timestamp = Instant.now().toEpochMilli();
        String normalizedExt = extension.toLowerCase().trim();
        return String.format("%s/%s/%d.%s", FLOOD_REPORTS_PATH, userId, timestamp, normalizedExt);
    }

    private String getContentType(String extension) {
        return switch (extension.toLowerCase().trim()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private String buildPublicUrl(String filePath) {
        // Firebase Storage public URL format
        return String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                properties.getBucketName(),
                filePath.replace("/", "%2F")
        );
    }
}
