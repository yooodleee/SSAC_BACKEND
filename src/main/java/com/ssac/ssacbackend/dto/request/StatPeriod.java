package com.ssac.ssacbackend.dto.request;

import java.time.LocalDateTime;

/**
 * 퀴즈 통계 집계 기간.
 *
 * <p>ArchUnit 규칙(Controller는 Domain에 접근 불가)에 따라 dto 패키지에 위치한다.
 */
public enum StatPeriod {

    /** 최근 7일 (일별 집계). */
    DAILY(7),

    /** 최근 4주 (주별 집계). */
    WEEKLY(28),

    /** 최근 12개월 (월별 집계). */
    MONTHLY(365);

    private final int lookbackDays;

    StatPeriod(int lookbackDays) {
        this.lookbackDays = lookbackDays;
    }

    /**
     * 이 기간의 시작 시각을 반환한다.
     */
    public LocalDateTime since() {
        return LocalDateTime.now().minusDays(lookbackDays);
    }
}
