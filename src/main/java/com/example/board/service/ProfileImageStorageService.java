package com.example.board.service;

import com.example.board.configuration.s3.S3Properties;
import com.example.board.dto.uploadDTO.request.ProfileImageUploadUrlRequest;
import com.example.board.dto.uploadDTO.response.ProfileImageUploadUrlResponse;
import com.example.board.exception.BadRequestException;
import com.example.board.exception.BusinessException;
import com.example.board.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileImageStorageService {

    static final long MAX_PROFILE_IMAGE_SIZE = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    public ProfileImageUploadUrlResponse createUploadUrl(
            Long userId,
            ProfileImageUploadUrlRequest request
    ) {
        String contentType = normalizeContentType(request.contentType());
        validateUploadMetadata(request, contentType);

        // 사용자별 디렉터리와 매 업로드마다 새로운 UUID를 사용해 이전 프로필 객체를 덮어쓰지 않는다.
        String objectKey = "profiles/" + userId + "/" + UUID.randomUUID()
                + "/original." + extensionFor(contentType);
        Instant expiresAt = Instant.now().plus(properties.uploadUrlExpiration());

        return new ProfileImageUploadUrlResponse(
                objectKey,
                createPutUrl(objectKey, contentType),
                contentType,
                expiresAt
        );
    }

    public void validateOwnedProfileImage(Long userId, String objectKey) {
        String[] keyParts = splitAndValidateObjectKey(userId, objectKey);
        HeadObjectResponse storedObject = headObject(objectKey);
        String storedContentType = normalizeContentType(storedObject.contentType());

        if (!ALLOWED_CONTENT_TYPES.contains(storedContentType)) {
            throw new BadRequestException(ErrorCode.IMAGE_CONTENT_TYPE_INVALID);
        }
        if (storedObject.contentLength() == null
                || storedObject.contentLength() <= 0
                || storedObject.contentLength() > MAX_PROFILE_IMAGE_SIZE) {
            throw new BadRequestException(ErrorCode.IMAGE_ORIGINAL_SIZE_INVALID);
        }

        // Key 확장자와 S3 Content-Type이 다르면 변조되거나 잘못 업로드된 객체로 판단한다.
        String expectedFileName = "original." + extensionFor(storedContentType);
        if (!keyParts[3].equals(expectedFileName)) {
            throw new BadRequestException(ErrorCode.IMAGE_OBJECT_KEY_INVALID);
        }
    }

    public String createDownloadUrl(String objectKey) {
        if (objectKey == null) {
            // 프로필을 등록하지 않은 사용자는 불필요한 Presigner 호출 없이 null을 그대로 응답한다.
            return null;
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    .build();
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                    GetObjectPresignRequest.builder()
                            .signatureDuration(properties.downloadUrlExpiration())
                            .getObjectRequest(getObjectRequest)
                            .build()
            );
            return presignedRequest.url().toExternalForm();
        } catch (SdkException exception) {
            throw new BusinessException(ErrorCode.IMAGE_STORAGE_UNAVAILABLE);
        }
    }

    private String createPutUrl(String objectKey, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    // Presigned URL 발급 시 서명한 MIME 타입을 브라우저 PUT 요청에서도 동일하게 사용해야 한다.
                    .contentType(contentType)
                    .build();
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(
                    PutObjectPresignRequest.builder()
                            .signatureDuration(properties.uploadUrlExpiration())
                            .putObjectRequest(putObjectRequest)
                            .build()
            );
            return presignedRequest.url().toExternalForm();
        } catch (SdkException exception) {
            // AWS SDK 내부 오류나 자격 증명 문제를 구현 세부 정보 없이 공통 저장소 오류로 변환한다.
            throw new BusinessException(ErrorCode.IMAGE_STORAGE_UNAVAILABLE);
        }
    }

    private HeadObjectResponse headObject(String objectKey) {
        try {
            return s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    .build());
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new BadRequestException(ErrorCode.IMAGE_OBJECT_NOT_FOUND);
            }
            throw new BusinessException(ErrorCode.IMAGE_STORAGE_UNAVAILABLE);
        } catch (SdkException exception) {
            throw new BusinessException(ErrorCode.IMAGE_STORAGE_UNAVAILABLE);
        }
    }

    private String[] splitAndValidateObjectKey(Long userId, String objectKey) {
        if (objectKey == null || objectKey.isBlank() || objectKey.length() > 512) {
            throw new BadRequestException(ErrorCode.IMAGE_OBJECT_KEY_INVALID);
        }

        String[] parts = objectKey.split("/", -1);
        boolean validStructure = parts.length == 4
                && parts[0].equals("profiles")
                && parts[1].equals(String.valueOf(userId))
                && parts[3].matches("original\\.(jpg|png|webp)");
        if (!validStructure) {
            throw new BadRequestException(ErrorCode.IMAGE_OBJECT_KEY_INVALID);
        }

        try {
            UUID.fromString(parts[2]);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(ErrorCode.IMAGE_OBJECT_KEY_INVALID);
        }
        return parts;
    }

    private void validateUploadMetadata(
            ProfileImageUploadUrlRequest request,
            String contentType
    ) {
        if (request.fileName() == null || request.fileName().isBlank()) {
            throw new BadRequestException(ErrorCode.IMAGE_FILE_NAME_REQUIRED);
        }
        if (request.fileName().length() > 255) {
            throw new BadRequestException(ErrorCode.IMAGE_FILE_NAME_LENGTH_LIMIT);
        }
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException(ErrorCode.IMAGE_CONTENT_TYPE_INVALID);
        }
        if (request.size() <= 0 || request.size() > MAX_PROFILE_IMAGE_SIZE) {
            throw new BadRequestException(ErrorCode.IMAGE_ORIGINAL_SIZE_INVALID);
        }
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new BadRequestException(ErrorCode.IMAGE_CONTENT_TYPE_INVALID);
        };
    }
}
