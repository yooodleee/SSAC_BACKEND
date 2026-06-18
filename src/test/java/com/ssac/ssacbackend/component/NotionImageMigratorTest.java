package com.ssac.ssacbackend.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * NotionImageMigrator 단위 테스트.
 *
 * <p>Cloudinary 업로더를 Mock으로 대체하여 각 분기를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class NotionImageMigratorTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private NotionImageMigrator migrator;

    @Nested
    @DisplayName("스킵 케이스 — Cloudinary 호출 없음")
    class SkipCases {

        @Test
        @DisplayName("null 입력 시 null 반환")
        void null_입력_시_null_반환() {
            assertThat(migrator.migrateIfNeeded(null)).isNull();
            verify(cloudinary, never()).uploader();
        }

        @Test
        @DisplayName("이미 Cloudinary URL이면 그대로 반환")
        void 이미_Cloudinary_URL이면_그대로_반환() {
            String cloudinaryUrl = "https://res.cloudinary.com/demo/image/upload/sample.jpg";
            assertThat(migrator.migrateIfNeeded(cloudinaryUrl)).isEqualTo(cloudinaryUrl);
            verify(cloudinary, never()).uploader();
        }
    }

    @Nested
    @DisplayName("Cloudinary 업로드")
    class UploadCases {

        @Test
        @DisplayName("업로드 성공 시 secure_url 반환")
        void 업로드_성공_시_secure_url_반환() throws Exception {
            String originalUrl = "https://prod-files-secure.s3.amazonaws.com/image.png";
            String secureUrl = "https://res.cloudinary.com/myapp/image/upload/content-thumbnails/image.png";
            given(cloudinary.uploader()).willReturn(uploader);
            given(uploader.upload(eq(originalUrl), any())).willReturn(Map.of("secure_url", secureUrl));

            assertThat(migrator.migrateIfNeeded(originalUrl)).isEqualTo(secureUrl);
        }

        @Test
        @DisplayName("업로드 실패 시 원본 URL 반환 (예외 삼킴)")
        void 업로드_실패_시_원본_URL_반환() throws Exception {
            String originalUrl = "https://unsplash.com/ko/사진/test-id";
            given(cloudinary.uploader()).willReturn(uploader);
            given(uploader.upload(eq(originalUrl), any()))
                .willThrow(new RuntimeException("401 Unauthorized"));

            assertThat(migrator.migrateIfNeeded(originalUrl)).isEqualTo(originalUrl);
        }
    }
}
