package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.dto.ErrorLogEntry;
import com.ssac.ssacbackend.service.ErrorLogService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AdminLogController")
class AdminLogControllerTest {

    private ErrorLogService errorLogService;
    private AdminLogController controller;

    @BeforeEach
    void setUp() {
        errorLogService = mock(ErrorLogService.class);
        controller = new AdminLogController(errorLogService);
    }

    // ── getErrorsByTraceId ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getErrorsByTraceId - traceId 기반 에러 로그 조회")
    class GetErrorsByTraceId {

        @Test
        @DisplayName("유효한 traceId로 에러 로그 조회 성공 시 로그 목록을 반환한다")
        void getErrorsByTraceId_성공() {
            String traceId = "trace-abc-123";
            List<ErrorLogEntry> mockEntries = List.of(
                new ErrorLogEntry("2024-01-01T10:00:00", "ERROR", "오류 발생", "StackTrace...")
            );
            given(errorLogService.findByTraceId(traceId)).willReturn(mockEntries);

            AdminLogController.TraceLogResponse result =
                controller.getErrorsByTraceId(traceId);

            assertThat(result.traceId()).isEqualTo(traceId);
            assertThat(result.logs()).hasSize(1);
            assertThat(result.logs().get(0).message()).isEqualTo("오류 발생");
        }

        @Test
        @DisplayName("로그가 없는 traceId 조회 시 빈 목록을 반환한다")
        void getErrorsByTraceId_로그없음() {
            String traceId = "trace-not-found";
            given(errorLogService.findByTraceId(traceId)).willReturn(List.of());

            AdminLogController.TraceLogResponse result =
                controller.getErrorsByTraceId(traceId);

            assertThat(result.logs()).isEmpty();
            verify(errorLogService).findByTraceId(traceId);
        }
    }
}
