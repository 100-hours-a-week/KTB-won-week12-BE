package com.example.board.service;

import com.example.board.configuration.s3.S3Properties;
import com.example.board.dto.uploadDTO.request.ProfileImageUploadUrlRequest;
import com.example.board.exception.BadRequestException;
import com.example.board.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileImageStorageServiceTest {

    @Mock
    private S3Presigner s3Presigner;

    private ProfileImageStorageService service;

    @BeforeEach
    void setUp() {
        service = new ProfileImageStorageService(
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
    @DisplayName("현재 사용자 전용 Object Key와 프로필 원본 Presigned PUT URL을 발급한다.")
    void createsProfileImageUploadUrl() throws Exception {
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(URI.create("https://s3.example/profile").toURL());
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        var response = service.createUploadUrl(
                15L,
                new ProfileImageUploadUrlRequest("profile.png", " Image/PNG ", 1024)
        );

        assertThat(response.objectKey())
                .startsWith("profiles/15/")
                .endsWith("/original.png");
        assertThat(response.uploadUrl()).isEqualTo("https://s3.example/profile");
        assertThat(response.contentType()).isEqualTo("image/png");

        // 실제 Presigned 요청에도 응답과 같은 버킷, Key, MIME 타입이 서명됐는지 확인한다.
        ArgumentCaptor<PutObjectPresignRequest> captor =
                ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(captor.capture());
        assertThat(captor.getValue().putObjectRequest().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().putObjectRequest().key()).isEqualTo(response.objectKey());
        assertThat(captor.getValue().putObjectRequest().contentType()).isEqualTo("image/png");
        assertThat(captor.getValue().signatureDuration()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("허용하지 않은 프로필 이미지 MIME 타입은 URL 발급 전에 거부한다.")
    void rejectsUnsupportedContentType() {
        assertThatThrownBy(() -> service.createUploadUrl(
                15L,
                new ProfileImageUploadUrlRequest("profile.gif", "image/gif", 1024)
        ))
                .isInstanceOf(BadRequestException.class)
                .extracting(exception -> ((BadRequestException) exception).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_CONTENT_TYPE_INVALID);

        verify(s3Presigner, never()).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    @DisplayName("5MB를 초과하거나 비어 있는 프로필 이미지는 URL 발급 전에 거부한다.")
    void rejectsInvalidProfileImageSize() {
        assertThatThrownBy(() -> service.createUploadUrl(
                15L,
                new ProfileImageUploadUrlRequest(
                        "profile.jpg",
                        "image/jpeg",
                        ProfileImageStorageService.MAX_PROFILE_IMAGE_SIZE + 1
                )
        ))
                .isInstanceOf(BadRequestException.class)
                .extracting(exception -> ((BadRequestException) exception).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_ORIGINAL_SIZE_INVALID);

        assertThatThrownBy(() -> service.createUploadUrl(
                15L,
                new ProfileImageUploadUrlRequest("profile.jpg", "image/jpeg", 0)
        ))
                .isInstanceOf(BadRequestException.class)
                .extracting(exception -> ((BadRequestException) exception).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_ORIGINAL_SIZE_INVALID);
    }

    @Test
    @DisplayName("프로필 이미지 파일 이름이 비어 있으면 URL 발급 전에 거부한다.")
    void rejectsBlankFileName() {
        assertThatThrownBy(() -> service.createUploadUrl(
                15L,
                new ProfileImageUploadUrlRequest(" ", "image/png", 1024)
        ))
                .isInstanceOf(BadRequestException.class)
                .extracting(exception -> ((BadRequestException) exception).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_FILE_NAME_REQUIRED);
    }
}
