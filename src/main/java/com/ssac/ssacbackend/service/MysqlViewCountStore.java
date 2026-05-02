package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.domain.news.News;
import com.ssac.ssacbackend.domain.news.NewsView;
import com.ssac.ssacbackend.repository.NewsViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * MySQL JPA 기반 뉴스 조회 수 저장소.
 *
 * <p>ViewCountStore 인터페이스의 현재 운영 구현체.
 * 조회 시마다 news_views 테이블에 즉시 INSERT한다.
 *
 * <p>트래픽이 높아질 경우 DB INSERT 부하가 커질 수 있다.
 * Redis 전환 시 RedisViewCountStore를 추가하고 이 Bean을 비활성화한다.
 * 전환 절차: docs/decisions/006-store-abstraction.md 참고
 */
@Component
@RequiredArgsConstructor
public class MysqlViewCountStore implements ViewCountStore {

    private final NewsViewRepository newsViewRepository;

    /**
     * {@inheritDoc}
     *
     * <p>news_views 테이블에 즉시 INSERT한다.
     */
    @Override
    public void record(News news) {
        newsViewRepository.save(NewsView.builder().news(news).build());
    }
}
