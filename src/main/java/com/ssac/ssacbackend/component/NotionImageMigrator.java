package com.ssac.ssacbackend.component;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Notion 이미지 URL을 Cloudinary로 이전하는 컴포넌트.
 *
 * <p>Notion 이미지 URL은 1시간 후 만료되므로 동기화 시점에 Cloudinary로 복사하여
 * 영구 URL로 교체한다. 이미 Cloudinary URL이면 건너뛴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotionImageMigrator {

    private static final String CLOUDINARY_HOST = "res.cloudinary.com";
    private static final String FOLDER = "content-thumbnails";

    private final Cloudinary cloudinary;

    /**
     * 이미지 URL을 Cloudinary URL로 교체한다.
     *
     * @param imageUrl 원본 URL (null 허용)
     * @return Cloudinary URL, 또는 마이그레이션 불필요/실패 시 원본 URL
     */
    public String migrateIfNeeded(String imageUrl) {
        if (imageUrl == null || imageUrl.contains(CLOUDINARY_HOST)) {
            return imageUrl;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                imageUrl,
                ObjectUtils.asMap("folder", FOLDER)
            );
            String secureUrl = (String) result.get("secure_url");
            log.debug("이미지 마이그레이션 완료: {} → {}", imageUrl, secureUrl);
            return secureUrl;
        } catch (Exception e) {
            log.warn("이미지 마이그레이션 실패, 원본 URL 유지: {}", imageUrl, e);
            return imageUrl;
        }
    }
}
