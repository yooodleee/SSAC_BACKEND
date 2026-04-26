package com.ssac.ssacbackend.domain.news;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 뉴스 조회 이벤트 엔티티.
 *
 * <p>뉴스가 조회될 때마다 한 건씩 기록된다.
 * {@link News#viewCount}는 최근 7일(viewed_at >= NOW() - 7days) 기준으로
 * 이 테이블에서 집계하여 갱신한다.
 */
@Entity
@Table(
    name = "news_views",
    indexes = {
        @Index(name = "idx_news_views_news_id_viewed_at",
            columnList = "news_id, viewed_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = false)
    private News news;

    @Column(name = "viewed_at", nullable = false, updatable = false)
    private LocalDateTime viewedAt;

    @Builder
    public NewsView(News news) {
        this.news = news;
    }

    @PrePersist
    private void prePersist() {
        this.viewedAt = LocalDateTime.now();
    }
}
