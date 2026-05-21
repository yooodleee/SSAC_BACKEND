package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.user.UserLevel;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 콘텐츠 데이터 접근 인터페이스.
 */
public interface ContentRepository extends JpaRepository<Content, Long> {

    List<Content> findByCategoryInAndDifficultyOrderByViewCountDesc(
        List<String> categories, UserLevel difficulty);

    List<Content> findByDifficultyOrderByViewCountDesc(UserLevel difficulty);

    List<Content> findAllByOrderByViewCountDesc();

    List<Content> findByCategoryAndDifficultyOrderByViewCountDesc(String category, UserLevel difficulty);

    List<Content> findByCategoryOrderByViewCountDesc(String category);

    long countByCategory(String category);

    long countByDifficulty(UserLevel difficulty);

    @Query("SELECT c FROM Content c WHERE c.title LIKE %:query% ORDER BY c.title ASC, c.viewCount DESC")
    List<Content> findByTitleContainingOrdered(@Param("query") String query);

    @Query("SELECT c FROM Content c WHERE c.titleChosung LIKE %:chosung% ORDER BY c.title ASC, c.viewCount DESC")
    List<Content> findByChosungContainingOrdered(@Param("chosung") String chosung);

    @Query("SELECT c FROM Content c WHERE c.title LIKE %:query% ORDER BY c.title ASC, c.viewCount DESC")
    Page<Content> findByTitleContainingOrderedPaged(
        @Param("query") String query,
        Pageable pageable);
}
