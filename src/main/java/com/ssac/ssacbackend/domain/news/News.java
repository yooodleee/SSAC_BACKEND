package com.ssac.ssacbackend.domain.news;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 뉴스 콘텐츠 엔티티.
 *
 * <p>viewCount는 최근 7일 이내 누적 조회 수를 기준으로 집계된다.
 * 실제 집계는 {@link NewsView}에 기록된 조회 이벤트를 배치 또는 스케줄러로 합산하여 갱신한다.
 *
 * <p>인덱스 전략:
 * - published_at: latest 정렬 (publishedAt DESC)
 * - view_count + published_at: popularity 정렬 (viewCount DESC, publishedAt DESC)
 */
@Entity
@Table(
    name = "news",
    indexes = {
        @Index(name = "idx_news_published_at", columnList = "published_at DESC"),
        @Index(name = "idx_news_view_count_published_at",
            columnList = "view_count DESC, published_at DESC")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 1000)
    private String summary;

    /**
     * 최근 7일 이내 누적 조회 수.
     * popularity 정렬의 기준 필드이며, 배치 집계로 주기적으로 갱신된다.
     */
    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Builder
    public News(String title, String summary, int viewCount, LocalDateTime publishedAt) {
        this.title = title;
        this.summary = summary;
        this.viewCount = viewCount;
        this.publishedAt = publishedAt != null ? publishedAt : LocalDateTime.now();
    }
}
