package com.ssac.ssacbackend.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.common.exception.ServiceUnavailableException;
import com.ssac.ssacbackend.common.exception.UnauthorizedException;
import io.sentry.EventProcessor;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions.BeforeSendCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class SentryConfigTest {

    private final SentryConfig sentryConfig = new SentryConfig();
    private final BeforeSendCallback beforeSendCallback = sentryConfig.beforeSendCallback();
    private final EventProcessor mdcEventProcessor = sentryConfig.mdcEventProcessor();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    // ── BeforeSendCallback ────────────────────────────────────────────────────

    @Nested
    @DisplayName("BeforeSendCallback — 4xx 필터링")
    class BeforeSendCallbackTest {

        @Test
        @DisplayName("NotFoundException(404) 발생 시 null 반환 — Sentry 미전송")
        void notFoundException_반환_null() {
            SentryEvent event = eventWith(new NotFoundException(ErrorCode.USER_NOT_FOUND));

            SentryEvent result = beforeSendCallback.execute(event, new Hint());

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("BadRequestException(400) 발생 시 null 반환 — Sentry 미전송")
        void badRequestException_반환_null() {
            SentryEvent event = eventWith(new BadRequestException(ErrorCode.INVALID_INPUT));

            SentryEvent result = beforeSendCallback.execute(event, new Hint());

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("UnauthorizedException(401) 발생 시 null 반환 — Sentry 미전송")
        void unauthorizedException_반환_null() {
            SentryEvent event = eventWith(new UnauthorizedException(ErrorCode.UNAUTHORIZED));

            SentryEvent result = beforeSendCallback.execute(event, new Hint());

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("ServiceUnavailableException(503) 발생 시 event 반환 — Sentry 전송")
        void serviceUnavailableException_반환_event() {
            SentryEvent event = eventWith(
                new ServiceUnavailableException(ErrorCode.REDIS_UNAVAILABLE));

            SentryEvent result = beforeSendCallback.execute(event, new Hint());

            assertThat(result).isSameAs(event);
        }

        @Test
        @DisplayName("NullPointerException 발생 시 event 반환 — Sentry 전송")
        void nullPointerException_반환_event() {
            SentryEvent event = eventWith(new NullPointerException("예상치 못한 예외"));

            SentryEvent result = beforeSendCallback.execute(event, new Hint());

            assertThat(result).isSameAs(event);
        }

        @Test
        @DisplayName("throwable 없는 이벤트는 event 그대로 반환")
        void throwable없는_이벤트_반환_event() {
            SentryEvent event = new SentryEvent();

            SentryEvent result = beforeSendCallback.execute(event, new Hint());

            assertThat(result).isSameAs(event);
        }
    }

    // ── EventProcessor (MDC 태그) ────────────────────────────────────────────

    @Nested
    @DisplayName("EventProcessor — MDC → Sentry 태그 연동")
    class MdcEventProcessorTest {

        @BeforeEach
        void setUpMdc() {
            MDC.put("traceId", "trace-abc123");
            MDC.put("userId", "42");
            MDC.put("method", "GET");
            MDC.put("path", "/api/v1/contents/99");
        }

        @Test
        @DisplayName("MDC 값이 Sentry 이벤트 태그로 포함된다")
        void mdc_태그_포함() {
            SentryEvent event = new SentryEvent();

            SentryEvent result = mdcEventProcessor.process(event, new Hint());

            assertThat(result.getTag("trace_id")).isEqualTo("trace-abc123");
            assertThat(result.getTag("user_id")).isEqualTo("42");
            assertThat(result.getTag("http_method")).isEqualTo("GET");
            assertThat(result.getTag("request_path")).isEqualTo("/api/v1/contents/99");
        }

        @Test
        @DisplayName("MDC가 비어 있으면 event 그대로 반환")
        void mdc_비어있으면_event_그대로() {
            MDC.clear();
            SentryEvent event = new SentryEvent();

            SentryEvent result = mdcEventProcessor.process(event, new Hint());

            assertThat(result).isSameAs(event);
            assertThat(result.getTags()).isNullOrEmpty();
        }

        @Test
        @DisplayName("email / nickname 등 개인정보는 Sentry 태그에 미포함")
        void 개인정보_태그_미포함() {
            MDC.put("email", "user@example.com");
            MDC.put("nickname", "홍길동");
            SentryEvent event = new SentryEvent();

            SentryEvent result = mdcEventProcessor.process(event, new Hint());

            assertThat(result.getTag("email")).isNull();
            assertThat(result.getTag("nickname")).isNull();
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private SentryEvent eventWith(Throwable throwable) {
        SentryEvent event = new SentryEvent(throwable);
        return event;
    }
}
