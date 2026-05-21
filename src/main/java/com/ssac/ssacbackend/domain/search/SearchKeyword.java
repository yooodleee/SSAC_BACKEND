package com.ssac.ssacbackend.domain.search;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 검색 키워드 집계 엔티티.
 *
 * <p>사용자가 검색어를 제출할 때마다 searchCount가 증가한다.
 */
@Entity
@Table(name = "search_keywords")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String keyword;

    @Column(name = "search_count", nullable = false)
    private long searchCount;

    @Column(name = "last_searched_at", nullable = false)
    private LocalDateTime lastSearchedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static SearchKeyword create(String keyword) {
        SearchKeyword sk = new SearchKeyword();
        sk.keyword = keyword;
        sk.searchCount = 1;
        sk.lastSearchedAt = LocalDateTime.now();
        sk.createdAt = LocalDateTime.now();
        return sk;
    }

    public void incrementCount() {
        this.searchCount++;
        this.lastSearchedAt = LocalDateTime.now();
    }
}
