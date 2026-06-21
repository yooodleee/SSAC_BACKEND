package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.user.UserLevel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 콘텐츠 데이터 접근 인터페이스.
 */
public interface ContentRepository extends JpaRepository<Content, Long> {

    @Query("SELECT DISTINCT c FROM Content c LEFT JOIN FETCH c.categories LEFT JOIN FETCH c.domains"
        + " WHERE c.notionPageId = :notionPageId")
    Optional<Content> findByNotionPageId(@Param("notionPageId") String notionPageId);

    // ── 게시된 콘텐츠 조회 ──────────────────────────────────────────────────────

    @Query("SELECT DISTINCT c FROM Content c LEFT JOIN FETCH c.categories LEFT JOIN FETCH c.domains"
        + " WHERE c.isPublished = true ORDER BY c.notionLastEditedAt DESC")
    List<Content> findAllPublished();

    @Query("SELECT DISTINCT c FROM Content c LEFT JOIN FETCH c.categories LEFT JOIN FETCH c.domains"
        + " WHERE c.isPublished = true AND :category MEMBER OF c.categories"
        + " ORDER BY c.notionLastEditedAt DESC")
    List<Content> findAllPublishedByCategory(@Param("category") String category);

    @Query("SELECT DISTINCT c FROM Content c LEFT JOIN FETCH c.categories LEFT JOIN FETCH c.domains"
        + " WHERE c.isPublished = true AND c.difficulty = :difficulty"
        + " ORDER BY c.notionLastEditedAt DESC")
    List<Content> findAllPublishedByDifficulty(@Param("difficulty") UserLevel difficulty);

    @Query("SELECT DISTINCT c FROM Content c LEFT JOIN FETCH c.categories LEFT JOIN FETCH c.domains"
        + " WHERE c.isPublished = true AND :category MEMBER OF c.categories"
        + " AND c.difficulty = :difficulty ORDER BY c.notionLastEditedAt DESC")
    List<Content> findAllPublishedByCategoryAndDifficulty(
        @Param("category") String category,
        @Param("difficulty") UserLevel difficulty);

    @Query("SELECT DISTINCT c FROM Content c LEFT JOIN FETCH c.categories LEFT JOIN FETCH c.domains"
        + " JOIN c.domains dom"
        + " WHERE c.isPublished = true AND dom = :domain ORDER BY c.notionLastEditedAt DESC")
    List<Content> findAllPublishedByDomain(@Param("domain") String domain);

    @Query("SELECT DISTINCT c FROM Content c LEFT JOIN FETCH c.categories LEFT JOIN FETCH c.domains"
        + " JOIN c.categories cat"
        + " WHERE c.isPublished = true AND cat IN :categories ORDER BY c.notionLastEditedAt DESC")
    List<Content> findAllPublishedByCategoriesIn(@Param("categories") List<String> categories);

    // ── HomeService 호환 쿼리 ───────────────────────────────────────────────────

    /**
     * 관심 카테고리 + 난이도 필터로 게시된 콘텐츠를 조회한다.
     *
     * <p>Pageable로 DB 레벨 LIMIT를 적용하여 대량 콘텐츠 전체 로드를 방지한다.
     */
    @Query("SELECT DISTINCT c FROM Content c"
        + " JOIN c.categories cat"
        + " WHERE cat IN :categories AND c.difficulty = :difficulty AND c.isPublished = true"
        + " ORDER BY c.notionLastEditedAt DESC")
    List<Content> findByCategoriesInAndDifficultyPublished(
        @Param("categories") List<String> categories,
        @Param("difficulty") UserLevel difficulty,
        Pageable pageable);

    /**
     * 난이도 필터로 게시된 콘텐츠를 조회한다.
     *
     * <p>Pageable로 DB 레벨 LIMIT를 적용하여 대량 콘텐츠 전체 로드를 방지한다.
     */
    @Query("SELECT c FROM Content c WHERE c.isPublished = true AND c.difficulty = :difficulty"
        + " ORDER BY c.notionLastEditedAt DESC")
    List<Content> findByDifficultyPublished(
        @Param("difficulty") UserLevel difficulty,
        Pageable pageable);

    /**
     * 최근 수정 순으로 게시된 콘텐츠를 조회한다.
     *
     * <p>Pageable로 DB 레벨 LIMIT를 적용하여 대량 콘텐츠 전체 로드를 방지한다.
     */
    @Query("SELECT c FROM Content c WHERE c.isPublished = true ORDER BY c.notionLastEditedAt DESC")
    List<Content> findAllPublishedOrderByLastEdited(Pageable pageable);

    @Query("SELECT COUNT(DISTINCT c) FROM Content c JOIN c.categories cat"
        + " WHERE cat = :category AND c.isPublished = true")
    long countByPublishedAndCategory(@Param("category") String category);

    /**
     * 카테고리별 게시 콘텐츠 수를 한 번에 조회한다 (홈 categories 섹션 N+1 개선용).
     *
     * @return [category(String), count(Long)] 쌍의 목록
     */
    @Query("SELECT cat, COUNT(DISTINCT c) FROM Content c JOIN c.categories cat"
        + " WHERE c.isPublished = true GROUP BY cat")
    List<Object[]> countPublishedGroupByCategory();

    long countByIsPublished(boolean isPublished);

    long countByDifficulty(UserLevel difficulty);

    // ── 관리자 모니터링 ─────────────────────────────────────────────────────────

    Page<Content> findAllByOrderByNotionLastEditedAtDesc(Pageable pageable);

    // ── 검색 ───────────────────────────────────────────────────────────────────

    /**
     * 자동완성용 — COUNT 쿼리 없이 LIMIT만 적용한다 (Slice).
     */
    @Query("SELECT c FROM Content c WHERE c.isPublished = true AND c.title LIKE %:query%"
        + " ORDER BY c.title ASC, c.notionLastEditedAt DESC")
    Slice<Content> findSuggestionsByTitleContaining(
        @Param("query") String query,
        Pageable pageable);

    @Query("SELECT c FROM Content c WHERE c.isPublished = true AND c.title LIKE %:query%"
        + " ORDER BY c.title ASC, c.notionLastEditedAt DESC")
    Page<Content> findByIsPublishedTrueAndTitleContainingPaged(
        @Param("query") String query,
        Pageable pageable);
}
