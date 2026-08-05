package com.example.board.service;

import com.example.board.configuration.s3.S3Properties;
import com.example.board.domain.board.BoardImageKeys;
import com.example.board.dto.uploadDTO.request.BoardImageUploadFileRequest;
import com.example.board.dto.uploadDTO.request.BoardImageUploadUrlRequest;
import com.example.board.exception.BadRequestException;
import com.example.board.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardImageStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private BoardImageStorageService service;

    @BeforeEach
    void setUp() {
        service = new BoardImageStorageService(
                s3Client,
                s3Presigner,
                new S3Properties(
                        "test-bucket",
                        "ap-northeast-2",
                        Duration.ofMinutes(5),
                        Duration.ofHours(1)
                )
        );
    }

    @Test
    @DisplayName("원본과 썸네일용 Presigned PUT URL 및 사용자 소유 Object Key를 발급한다.")
    void createsUploadUrlsForOriginalAndThumbnail() throws Exception {
        PresignedPutObjectRequest originalRequest = org.mockito.Mockito.mock(PresignedPutObjectRequest.class);
        PresignedPutObjectRequest thumbnailRequest = org.mockito.Mockito.mock(PresignedPutObjectRequest.class);
        when(originalRequest.url()).thenReturn(URI.create("https://s3.example/original").toURL());
        when(thumbnailRequest.url()).thenReturn(URI.create("https://s3.example/thumbnail").toURL());
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenReturn(originalRequest, thumbnailRequest);

        var response = service.createUploadUrls(15L, new BoardImageUploadUrlRequest(List.of(
                new BoardImageUploadFileRequest(
                        "accident.png",
                        "image/png",
                        2L * 1024 * 1024,
                        "image/webp",
                        100L * 1024
                )
        )));

        assertThat(response.images()).hasSize(1);
        var image = response.images().getFirst();
        assertThat(image.originalObjectKey()).startsWith("boards/15/").endsWith("/original.png");
        assertThat(image.thumbnailObjectKey()).startsWith("boards/15/").endsWith("/thumbnail.webp");
        assertThat(image.originalUploadUrl()).isEqualTo("https://s3.example/original");
        assertThat(image.thumbnailUploadUrl()).isEqualTo("https://s3.example/thumbnail");
        assertThat(image.originalContentType()).isEqualTo("image/png");
        assertThat(image.thumbnailContentType()).isEqualTo("image/webp");
    }

    @Test
    @DisplayName("허용하지 않은 원본 MIME 타입은 Presigned URL 발급 전에 거부한다.")
    void rejectsUnsupportedOriginalContentType() {
        var request = new BoardImageUploadUrlRequest(List.of(
                new BoardImageUploadFileRequest(
                        "accident.gif",
                        "image/gif",
                        1024,
                        "image/webp",
                        512
                )
        ));

        assertThatThrownBy(() -> service.createUploadUrls(15L, request))
                .isInstanceOf(BadRequestException.class)
                .extracting(exception -> ((BadRequestException) exception).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_CONTENT_TYPE_INVALID);

        verify(s3Presigner, never()).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    @DisplayName("5MB를 초과한 원본 이미지는 Presigned URL 발급 전에 거부한다.")
    void rejectsOversizedOriginal() {
        var request = new BoardImageUploadUrlRequest(List.of(
                new BoardImageUploadFileRequest(
                        "accident.png",
                        "image/png",
                        BoardImageStorageService.MAX_ORIGINAL_SIZE + 1,
                        "image/webp",
                        512
                )
        ));

        assertThatThrownBy(() -> service.createUploadUrls(15L, request))
                .isInstanceOf(BadRequestException.class)
                .extracting(exception -> ((BadRequestException) exception).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_ORIGINAL_SIZE_INVALID);
    }

    @Test
    @DisplayName("Object Key 사용자와 요청 사용자가 다르면 S3 조회 전에 거부한다.")
    void rejectsAnotherUsersObjectKeys() {
        BoardImageKeys keys = imageKeys(99L);

        assertThatThrownBy(() -> service.validateOwnedImages(15L, List.of(keys)))
                .isInstanceOf(BadRequestException.class)
                .extracting(exception -> ((BadRequestException) exception).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_OBJECT_KEY_INVALID);

        verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
    }

    @Test
    @DisplayName("현재 사용자 이미지의 원본과 썸네일이 S3에 존재하고 메타데이터가 유효한지 확인한다.")
    void validatesStoredObjectMetadata() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(
                        HeadObjectResponse.builder()
                                .contentType("image/png")
                                .contentLength(2L * 1024 * 1024)
                                .build(),
                        HeadObjectResponse.builder()
                                .contentType("image/webp")
                                .contentLength(100L * 1024)
                                .build()
                );

        service.validateOwnedImages(15L, List.of(imageKeys(15L)));

        verify(s3Client, org.mockito.Mockito.times(2)).headObject(any(HeadObjectRequest.class));
    }

    @Test
    @DisplayName("업로드가 완료되지 않아 S3 객체가 없으면 게시글에 연결하지 않는다.")
    void rejectsMissingStoredObject() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).message("Not Found").build());

        assertThatThrownBy(() -> service.validateOwnedImages(15L, List.of(imageKeys(15L))))
                .isInstanceOf(BadRequestException.class)
                .extracting(exception -> ((BadRequestException) exception).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_OBJECT_NOT_FOUND);
    }

    private BoardImageKeys imageKeys(Long userId) {
        String baseKey = "boards/" + userId + "/11111111-1111-1111-1111-111111111111/";
        return new BoardImageKeys(
                baseKey + "original.png",
                baseKey + "thumbnail.webp"
        );
    }
}
