package com.ssac.ssacbackend.domain.content;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 학습 콘텐츠 엔티티.
 *
 * <p>Notion 데이터베이스와 동기화되며, NotionSyncService가 1시간마다 갱신한다.
 * 콘텐츠 편집은 Notion에서만 이루어지고 BE는 조회·제공만 담당한다.
 */
@Entity
@Table(name = "contents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String title;

    @Column(name = "notion_page_id", length = 100, unique = true)
    private String notionPageId;

    @Column(name = "notion_database_id", length = 100)
    private String notionDatabaseId;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "content_categories", joinColumns = @JoinColumn(name = "content_id"))
    @Column(name = "category", length = 50)
    private List<String> categories = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "content_domains", joinColumns = @JoinColumn(name = "content_id"))
    @Column(name = "domain", length = 50)
    private Set<String> domains = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ContentDifficulty difficulty;

    @Column(name = "is_published", nullable = false)
    private boolean isPublished = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "notion_created_at")
    private LocalDateTime notionCreatedAt;

    @Column(name = "notion_last_edited_at")
    private LocalDateTime notionLastEditedAt;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Notion 페이지 ID 기반으로 빈 콘텐츠를 생성한다.
     */
    public static Content fromNotion(String notionPageId, String notionDatabaseId) {
        Content content = new Content();
        content.notionPageId = notionPageId;
        content.notionDatabaseId = notionDatabaseId;
        return content;
    }

    /**
     * Notion 데이터로 콘텐츠 필드를 갱신한다.
     */
    public void syncFromNotion(String title, String thumbnailUrl, List<String> categories,
                                List<String> domains, ContentDifficulty difficulty,
                                boolean isPublished, LocalDateTime notionCreatedAt,
                                LocalDateTime notionLastEditedAt, LocalDateTime publishedAt) {
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.categories.clear();
        this.categories.addAll(categories);
        this.domains.clear();
        this.domains.addAll(domains);
        this.difficulty = difficulty;
        this.isPublished = isPublished;
        this.notionCreatedAt = notionCreatedAt;
        this.notionLastEditedAt = notionLastEditedAt;
        this.publishedAt = publishedAt;
        this.syncedAt = LocalDateTime.now();
    }

    /**
     * categories 컬렉션의 첫 번째 값을 반환한다. 이전 코드와의 호환성을 위해 제공한다.
     */
    public String getFirstCategory() {
        return categories.isEmpty() ? null : categories.get(0);
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
