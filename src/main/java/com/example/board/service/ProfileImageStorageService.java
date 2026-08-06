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
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

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
