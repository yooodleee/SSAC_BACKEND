package com.ssac.ssacbackend.domain.content;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 콘텐츠 카테고리 정의.
 *
 * <p>홈 화면 categories 섹션과 추천 도메인에 사용된다.
 */
@Getter
@RequiredArgsConstructor
public enum ContentCategory {

    REALESTATE("realestate", "부동산/자취", "🏠"),
    TAX("tax", "세금/연말정산", "📋"),
    FINANCE("finance", "재테크/신용", "💰"),
    SCHOLARSHIP("scholarship", "학자금/장학금", "🎓");

    private final String id;
    private final String name;
    private final String emoji;

    public static Optional<ContentCategory> findById(String id) {
        return Arrays.stream(values()).filter(c -> c.id.equals(id)).findFirst();
    }

    public static List<ContentCategory> all() {
        return List.of(values());
    }
}
