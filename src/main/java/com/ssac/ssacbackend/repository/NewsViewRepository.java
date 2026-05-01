package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.news.NewsView;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 뉴스 조회 이벤트 레포지토리.
 */
public interface NewsViewRepository extends JpaRepository<NewsView, Long> {
}
