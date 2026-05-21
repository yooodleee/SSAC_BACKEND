package com.ssac.ssacbackend.domain.content;

import com.ssac.ssacbackend.domain.user.UserLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 학습 콘텐츠 엔티티.
 *
 * <p>카테고리와 난이도 기반으로 사용자에게 추천된다.
 */
@Entity
@Table(name = "contents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content {

    private static final char[] CHOSUNG_LIST = {
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ',
        'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserLevel difficulty;

    @Column(name = "estimated_minutes")
    private int estimatedMinutes;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "title_chosung", length = 255)
    private String titleChosung;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Content(String title, String category, UserLevel difficulty, int estimatedMinutes) {
        this.title = title;
        this.category = category;
        this.difficulty = difficulty;
        this.estimatedMinutes = estimatedMinutes;
        this.viewCount = 0;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.titleChosung = extractChosung(this.title);
    }

    @PreUpdate
    private void preUpdate() {
        this.titleChosung = extractChosung(this.title);
    }

    private static String extractChosung(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) {
                result.append(CHOSUNG_LIST[(c - 0xAC00) / 588]);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
