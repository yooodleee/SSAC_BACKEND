package com.ssac.ssacbackend.domain.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 콘텐츠 조회 이력 엔티티.
 *
 * <p>사용자가 콘텐츠를 열람할 때마다 이력이 저장된다.
 * 완료 처리 시 {@link #markCompleted()} 를 호출한다.
 */
@Entity
@Table(name = "content_view_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentViewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    /**
     * 영속화 직전 viewedAt 기본값을 설정한다.
     */
    @PrePersist
    private void prePersist() {
        if (this.viewedAt == null) {
            this.viewedAt = LocalDateTime.now();
        }
    }

    /**
     * 콘텐츠 조회 이력을 생성한다.
     *
     * @param userId    사용자 ID
     * @param contentId 콘텐츠 ID
     * @return 생성된 이력 엔티티
     */
    public static ContentViewHistory of(Long userId, Long contentId) {
        ContentViewHistory h = new ContentViewHistory();
        h.userId = userId;
        h.contentId = contentId;
        h.isCompleted = false;
        h.viewedAt = LocalDateTime.now();
        return h;
    }

    /**
     * 콘텐츠 완료 상태로 마킹한다.
     */
    public void markCompleted() {
        this.isCompleted = true;
    }
}
