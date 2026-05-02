package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.domain.news.News;

/**
 * 뉴스 조회 수 기록 저장소 추상화 인터페이스.
 *
 * <p>현재 구현체: {@link MysqlViewCountStore} — 조회 시 즉시 DB INSERT
 * <br>전환 예정: RedisViewCountStore — INCR + 주기적 flush 패턴으로 DB 부하 감소
 *
 * <p><b>Redis 전환 시 동작 패턴 (예정)</b>
 * <ol>
 *   <li>조회 발생 시 Redis INCR으로 뉴스별 카운터 증가</li>
 *   <li>스케줄러가 5분 간격으로 Redis 카운터를 DB에 일괄 flush</li>
 *   <li>flush 완료 후 Redis 카운터 초기화</li>
 * </ol>
 *
 * <p><b>flush 중 장애 대응 전략 (예정)</b>
 * <ul>
 *   <li>flush 전 Redis 카운터 백업 키 생성 (backup:viewcount:{newsId})</li>
 *   <li>DB 반영 성공 후 백업 키 삭제</li>
 *   <li>서버 재시작 시 백업 키 존재하면 DB 재반영 후 초기화</li>
 * </ul>
 *
 * <p>상세 내용: docs/decisions/006-store-abstraction.md
 */
public interface ViewCountStore {

    /**
     * 뉴스 조회 이벤트를 기록한다.
     *
     * @param news 조회된 뉴스 엔티티
     */
    void record(News news);
}
