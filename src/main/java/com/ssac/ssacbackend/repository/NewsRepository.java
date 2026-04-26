package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.news.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 뉴스 데이터 접근 인터페이스.
 */
public interface NewsRepository extends JpaRepository<News, Long> {

    /**
     * 전체 뉴스를 페이지네이션으로 조회한다.
     * 정렬 조건은 Pageable의 Sort로 전달한다.
     */
    Page<News> findAll(Pageable pageable);
}
