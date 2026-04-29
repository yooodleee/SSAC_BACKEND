package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인하지 않은 비회원(Guest)의 오래된 데이터를 정리하는 스케줄러 서비스.
 *
 * <p>guestId 쿠키 만료(30일)와 연동하여, 30일이 지난 비회원 기록을 DB에서 영구 삭제한다.
 * 대규모 트래픽에서 테이블 크기가 무한히 증가하는 것을 방지한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuestDataCleanupService {

    private final QuizAttemptRepository quizAttemptRepository;

    /**
     * 매일 새벽 3시에 30일 이상 된 비회원 데이터를 삭제한다.
     * cron: "0 0 3 * * *" (초 분 시 일 월 요일)
     */
    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredGuestData() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        log.info("비회원 데이터 정리 시작: 기준 시각={}", threshold);

        try {
            quizAttemptRepository.deleteByGuestIdIsNotNullAndAttemptedAtBefore(threshold);
            log.info("비회원 데이터 정리 완료");
        } catch (Exception e) {
            log.error("비회원 데이터 정리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
