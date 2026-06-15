package com.ssac.ssacbackend.config;

import com.ssac.ssacbackend.common.exception.BusinessException;
import io.sentry.EventProcessor;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions.BeforeSendCallback;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sentry 에러 모니터링 설정.
 *
 * <p>4xx 비즈니스 예외를 Sentry 전송 대상에서 제외하고,
 * MDC 컨텍스트(traceId, userId, method, path)를 Sentry 이벤트 태그로 연동한다.
 *
 * <p>개인정보 보호: MDC에 저장된 값 중 식별자(traceId, userId)와 요청 정보(method, path)만
 * 태그로 포함한다. 이메일/이름/닉네임/토큰 값은 절대 포함하지 않는다.
 * application-prod.yml의 send-default-pii: false와 함께 이중으로 보호한다.
 */
@Configuration
public class SentryConfig {

    /**
     * 4xx 비즈니스 예외를 Sentry 전송에서 제외한다.
     *
     * <p>Sentry 미전송 대상 (4xx):
     * <ul>
     *   <li>NotFoundException (404) — 존재하지 않는 콘텐츠/사용자</li>
     *   <li>BadRequestException (400) — 잘못된 입력값/온보딩 미완료</li>
     *   <li>UnauthorizedException (401) — 토큰 누락/만료</li>
     *   <li>ForbiddenException (403) — 관리자 권한 없음</li>
     *   <li>ConflictException (409) — 닉네임 중복 등</li>
     * </ul>
     *
     * <p>Sentry 전송 대상 (5xx / 예상치 못한 예외):
     * <ul>
     *   <li>InternalServerErrorException (500)</li>
     *   <li>ServiceUnavailableException (503) — Redis/외부 API 장애</li>
     *   <li>NullPointerException 등 예상치 못한 런타임 예외</li>
     * </ul>
     */
    @Bean
    public BeforeSendCallback beforeSendCallback() {
        return (event, hint) -> {
            Throwable throwable = event.getThrowable();

            // throwable이 없는 이벤트는 그대로 전송
            if (throwable == null) {
                return event;
            }

            // 비즈니스 예외 (BusinessException 계층) 필터링
            if (throwable instanceof BusinessException ex) {
                // 4xx → Sentry 미전송
                if (ex.getStatus().is4xxClientError()) {
                    return null;
                }
            }

            // 5xx 및 예상치 못한 예외 → Sentry 전송
            return event;
        };
    }

    /**
     * MDC 컨텍스트를 Sentry 이벤트 태그로 연동한다.
     *
     * <p>포함 태그:
     * <ul>
     *   <li>trace_id — 요청 추적 식별자 (Railway 로그 교차 조회용)</li>
     *   <li>user_id  — 내부 숫자 ID (PII 아님)</li>
     *   <li>http_method — HTTP 메서드</li>
     *   <li>request_path — 요청 경로</li>
     * </ul>
     *
     * <p>미포함 항목 (개인정보 차단):
     * email, name, nickname, phoneNumber, accessToken, refreshToken, 요청 Body
     */
    @Bean
    public EventProcessor mdcEventProcessor() {
        return new EventProcessor() {
            @Override
            public SentryEvent process(SentryEvent event, Hint hint) {
                Map<String, String> mdcContext = MDC.getCopyOfContextMap();

                if (mdcContext == null || mdcContext.isEmpty()) {
                    return event;
                }

                if (mdcContext.containsKey("traceId")) {
                    event.setTag("trace_id", mdcContext.get("traceId"));
                }
                if (mdcContext.containsKey("userId")) {
                    event.setTag("user_id", mdcContext.get("userId"));
                }
                if (mdcContext.containsKey("method")) {
                    event.setTag("http_method", mdcContext.get("method"));
                }
                if (mdcContext.containsKey("path")) {
                    event.setTag("request_path", mdcContext.get("path"));
                }

                return event;
            }
        };
    }
}
