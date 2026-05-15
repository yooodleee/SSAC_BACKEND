package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.user.UserLevel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
