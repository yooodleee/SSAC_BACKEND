package com.ssac.ssacbackend.domain.content;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 콘텐츠 카테고리 정의.
 *
 * <p>Notion Multi-select 태그 값과 1:1 매핑된다.
 * notionTag는 Notion DB 태그 값이자 API 응답값으로 사용된다.
 */
@Getter
@RequiredArgsConstructor
public enum ContentCategory {

    REALESTATE ("realestate",  "부동산/자취",   "🏠"),
    TAX        ("tax",         "세금/연말정산", "📋"),
    WORK       ("work",        "근로/급여",     "💼"),
    INVESTMENT ("investment",  "재테크/신용",   "💰"),
    SCHOLARSHIP("scholarship", "학자금/장학금", "🎓"),
    SERIES     ("series",      "시리즈",        "📚"),
    WELFARE    ("welfare",     "사회보험/복지", "🛡️"),
    BUDGET     ("budget",      "소비/예산관리", "💳");

    private final String notionTag;
    private final String label;
    private final String emoji;

    /**
     * Notion 태그 값으로 ContentCategory를 찾는다.
     *
     * @throws IllegalArgumentException 유효하지 않은 태그인 경우
     */
    public static ContentCategory fromNotionTag(String tag) {
        return Arrays.stream(values())
            .filter(c -> c.notionTag.equals(tag))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("알 수 없는 Notion 태그: " + tag));
    }

    public static Optional<ContentCategory> findById(String id) {
        return Arrays.stream(values()).filter(c -> c.notionTag.equals(id)).findFirst();
    }

    public static Optional<ContentCategory> findByName(String name) {
        return Arrays.stream(values()).filter(c -> c.label.equals(name)).findFirst();
    }

    public static List<ContentCategory> all() {
        return List.of(values());
    }

    /** @deprecated getNotionTag() 사용 권장 */
    @Deprecated
    public String getId() {
        return notionTag;
    }

    /** @deprecated getLabel() 사용 권장 */
    @Deprecated
    public String getName() {
        return label;
    }
}
