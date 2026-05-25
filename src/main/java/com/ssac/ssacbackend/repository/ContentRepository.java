package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.content.ContentDifficulty;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 콘텐츠 데이터 접근 인터페이스.
 */
public interface ContentRepository extends JpaRepository<Content, Long> {

    Optional<Content> findByNotionPageId(String notionPageId);

    // ── 게시된 콘텐츠 조회 ──────────────────────────────────────────────────────

    @Query("SELECT DISTINCT c FROM Content c LEFT JOIN FETCH c.categories LEFT JOIN FETCH c.domains"
        + " WHERE c.isPublished = true ORDER BY c.notionLastEditedAt DESC")
    List<Content> findAllPublished();

    @Query("SELECT DISTINCT c FROM Content c LEFT JOIN FETCH c.categories LEFT JOIN FETCH c.domains"
        + " JOIN c.categories cat"
        + " WHERE c.isPublished = true AND cat = :category ORDER BY c.notionLastEditedAt DESC")
    List<Content> findAllPublishedByCategory(@Param("category") String category);

    @Query("SELECT DISTINCT c FROM Content c LEFT JOIN FETCH c.categories LEFT JOIN FETCH c.domains"
        + " WHERE c.isPublished = true AND c.difficulty = :difficulty"
        + " ORDER BY c.notionLastEditedAt DESC")
    List<Content> findAllPublishedByDifficulty(@Param("difficulty") ContentDifficulty difficulty);

    @Query("SELECT DISTINCT c FROM Content c LEFT JOIN FETCH c.categories LEFT JOIN FETCH c.domains"
        + " JOIN c.categories cat"
        + " WHERE c.isPublished = true AND cat = :category AND c.difficulty = :difficulty"
        + " ORDER BY c.notionLastEditedAt DESC")
    List<Content> findAllPublishedByCategoryAndDifficulty(
        @Param("category") String category,
        @Param("difficulty") ContentDifficulty difficulty);

    @Query("SELECT DISTINCT c FROM Content c LEFT JOIN FETCH c.categories LEFT JOIN FETCH c.domains"
        + " JOIN c.domains dom"
        + " WHERE c.isPublished = true AND dom = :domain ORDER BY c.notionLastEditedAt DESC")
    List<Content> findAllPublishedByDomain(@Param("domain") String domain);

    // ── HomeService 호환 쿼리 ───────────────────────────────────────────────────

    @Query("SELECT DISTINCT c FROM Content c"
        + " JOIN c.categories cat"
        + " WHERE cat IN :categories AND c.difficulty = :difficulty AND c.isPublished = true"
        + " ORDER BY c.notionLastEditedAt DESC")
    List<Content> findByCategoriesInAndDifficultyPublished(
        @Param("categories") List<String> categories,
        @Param("difficulty") ContentDifficulty difficulty);

    @Query("SELECT c FROM Content c WHERE c.isPublished = true AND c.difficulty = :difficulty"
        + " ORDER BY c.notionLastEditedAt DESC")
    List<Content> findByDifficultyPublished(@Param("difficulty") ContentDifficulty difficulty);

    @Query("SELECT c FROM Content c WHERE c.isPublished = true ORDER BY c.notionLastEditedAt DESC")
    List<Content> findAllPublishedOrderByLastEdited();

    @Query("SELECT COUNT(DISTINCT c) FROM Content c JOIN c.categories cat"
        + " WHERE cat = :category AND c.isPublished = true")
    long countByPublishedAndCategory(@Param("category") String category);

    long countByIsPublished(boolean isPublished);

    long countByDifficulty(ContentDifficulty difficulty);

    // ── 관리자 모니터링 ─────────────────────────────────────────────────────────

    Page<Content> findAllByOrderByNotionLastEditedAtDesc(Pageable pageable);

    // ── 검색 ───────────────────────────────────────────────────────────────────

    @Query("SELECT c FROM Content c WHERE c.isPublished = true AND c.title LIKE %:query%"
        + " ORDER BY c.title ASC, c.notionLastEditedAt DESC")
    List<Content> findByIsPublishedTrueAndTitleContaining(@Param("query") String query);

    @Query("SELECT c FROM Content c WHERE c.isPublished = true AND c.title LIKE %:query%"
        + " ORDER BY c.title ASC, c.notionLastEditedAt DESC")
    Page<Content> findByIsPublishedTrueAndTitleContainingPaged(
        @Param("query") String query,
        Pageable pageable);
}
