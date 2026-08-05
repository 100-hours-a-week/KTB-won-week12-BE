package com.example.board.service;

import com.example.board.configuration.s3.S3Properties;
import com.example.board.domain.board.BoardImageKeys;
import com.example.board.dto.uploadDTO.request.BoardImageUploadFileRequest;
import com.example.board.dto.uploadDTO.request.BoardImageUploadUrlRequest;
import com.example.board.dto.uploadDTO.response.BoardImageUploadUrlResponse;
import com.example.board.dto.uploadDTO.response.BoardImageUploadUrlsResponse;
import com.example.board.exception.BadRequestException;
import com.example.board.exception.BusinessException;
import com.example.board.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoardImageStorageService {

    static final long MAX_ORIGINAL_SIZE = 5L * 1024 * 1024;
    static final long MAX_THUMBNAIL_SIZE = 1024L * 1024;
    static final int MAX_IMAGE_COUNT = 5;
    static final String THUMBNAIL_CONTENT_TYPE = "image/webp";

    private static final Set<String> ORIGINAL_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    public BoardImageUploadUrlsResponse createUploadUrls(
            Long userId,
            BoardImageUploadUrlRequest request
    ) {
        if (request.images() == null
                || request.images().isEmpty()
                || request.images().size() > MAX_IMAGE_COUNT) {
            throw new BadRequestException(ErrorCode.BOARD_IMAGE_COUNT_LIMIT);
        }

        Instant expiresAt = Instant.now().plus(properties.uploadUrlExpiration());
        List<BoardImageUploadUrlResponse> responses = new ArrayList<>(request.images().size());

        for (BoardImageUploadFileRequest image : request.images()) {
            String originalContentType = normalizeContentType(image.originalContentType());
            validateUploadMetadata(image, originalContentType);

            String imageGroupId = UUID.randomUUID().toString();
            String baseKey = "boards/" + userId + "/" + imageGroupId + "/";
            String originalObjectKey = baseKey + "original." + extensionFor(originalContentType);
            String thumbnailObjectKey = baseKey + "thumbnail.webp";

            responses.add(new BoardImageUploadUrlResponse(
                    originalObjectKey,
                    createPutUrl(originalObjectKey, originalContentType),
                    originalContentType,
                    thumbnailObjectKey,
                    createPutUrl(thumbnailObjectKey, THUMBNAIL_CONTENT_TYPE),
                    THUMBNAIL_CONTENT_TYPE,
                    expiresAt
            ));
        }

        return new BoardImageUploadUrlsResponse(List.copyOf(responses));
    }

    /**
     * 클라이언트가 전달한 Key 문자열만 신뢰하지 않고 경로 소유권과 실제 S3 객체 메타데이터를 확인한다.
     */
    public void validateOwnedImages(Long userId, List<BoardImageKeys> images) {
        if (images.size() > MAX_IMAGE_COUNT) {
            throw new BadRequestException(ErrorCode.BOARD_IMAGE_COUNT_LIMIT);
        }

        Set<String> imageGroupIds = new HashSet<>();
        for (BoardImageKeys image : images) {
            String groupId = validateKeyPair(userId, image);
            if (!imageGroupIds.add(groupId)) {
                throw new BadRequestException(ErrorCode.IMAGE_OBJECT_KEY_INVALID);
            }

            validateStoredOriginal(headObject(image.originalObjectKey()));
            validateStoredThumbnail(headObject(image.thumbnailObjectKey()));
        }
    }

    public String createDownloadUrl(String objectKey) {
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

    private void validateUploadMetadata(BoardImageUploadFileRequest image, String originalContentType) {
        if (!ORIGINAL_CONTENT_TYPES.contains(originalContentType)) {
            throw new BadRequestException(ErrorCode.IMAGE_CONTENT_TYPE_INVALID);
        }
        if (image.originalSize() <= 0 || image.originalSize() > MAX_ORIGINAL_SIZE) {
            throw new BadRequestException(ErrorCode.IMAGE_ORIGINAL_SIZE_INVALID);
        }
        if (!THUMBNAIL_CONTENT_TYPE.equals(normalizeContentType(image.thumbnailContentType()))) {
            throw new BadRequestException(ErrorCode.IMAGE_THUMBNAIL_TYPE_INVALID);
        }
        if (image.thumbnailSize() <= 0 || image.thumbnailSize() > MAX_THUMBNAIL_SIZE) {
            throw new BadRequestException(ErrorCode.IMAGE_THUMBNAIL_SIZE_INVALID);
        }
    }

    private String validateKeyPair(Long userId, BoardImageKeys image) {
        String[] originalParts = splitKey(image.originalObjectKey());
        String[] thumbnailParts = splitKey(image.thumbnailObjectKey());

        boolean validOwner = originalParts[1].equals(String.valueOf(userId))
                && thumbnailParts[1].equals(String.valueOf(userId));
        boolean validPair = originalParts[2].equals(thumbnailParts[2]);
        boolean validNames = originalParts[3].matches("original\\.(jpg|png|webp)")
                && thumbnailParts[3].equals("thumbnail.webp");

        try {
            UUID.fromString(originalParts[2]);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(ErrorCode.IMAGE_OBJECT_KEY_INVALID);
        }

        if (!validOwner || !validPair || !validNames) {
            throw new BadRequestException(ErrorCode.IMAGE_OBJECT_KEY_INVALID);
        }
        return originalParts[2];
    }

    private String[] splitKey(String objectKey) {
        if (objectKey == null || objectKey.length() > 512) {
            throw new BadRequestException(ErrorCode.IMAGE_OBJECT_KEY_INVALID);
        }
        String[] parts = objectKey.split("/", -1);
        if (parts.length != 4 || !parts[0].equals("boards")) {
            throw new BadRequestException(ErrorCode.IMAGE_OBJECT_KEY_INVALID);
        }
        return parts;
    }

    private void validateStoredOriginal(HeadObjectResponse response) {
        if (!ORIGINAL_CONTENT_TYPES.contains(normalizeContentType(response.contentType()))) {
            throw new BadRequestException(ErrorCode.IMAGE_CONTENT_TYPE_INVALID);
        }
        if (response.contentLength() == null
                || response.contentLength() <= 0
                || response.contentLength() > MAX_ORIGINAL_SIZE) {
            throw new BadRequestException(ErrorCode.IMAGE_ORIGINAL_SIZE_INVALID);
        }
    }

    private void validateStoredThumbnail(HeadObjectResponse response) {
        if (!THUMBNAIL_CONTENT_TYPE.equals(normalizeContentType(response.contentType()))) {
            throw new BadRequestException(ErrorCode.IMAGE_THUMBNAIL_TYPE_INVALID);
        }
        if (response.contentLength() == null
                || response.contentLength() <= 0
                || response.contentLength() > MAX_THUMBNAIL_SIZE) {
            throw new BadRequestException(ErrorCode.IMAGE_THUMBNAIL_SIZE_INVALID);
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
