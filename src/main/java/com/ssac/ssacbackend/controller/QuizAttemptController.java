package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.request.AttemptSortType;
import com.ssac.ssacbackend.dto.request.QuizSubmitRequest;
import com.ssac.ssacbackend.dto.request.StatPeriod;
import com.ssac.ssacbackend.dto.response.QuizAttemptDetailResponse;
import com.ssac.ssacbackend.dto.response.QuizAttemptSummaryResponse;
import com.ssac.ssacbackend.dto.response.UserStatsResponse;
import com.ssac.ssacbackend.service.QuizAttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 퀴즈 응시 기록 및 통계 엔드포인트.
 *
 * <p>모든 요청은 유효한 JWT Bearer 토큰이 있어야 한다.
 * 사용자는 본인의 기록만 조회할 수 있다.
 */
@Slf4j
@Tag(name = "QuizAttempt", description = "퀴즈 응시 기록 및 통계 API")
@RestController
@RequestMapping("/api/v1/quiz-attempts")
@RequiredArgsConstructor
public class QuizAttemptController {

    private final QuizAttemptService quizAttemptService;

    @Operation(
        summary = "퀴즈 제출",
        description = """
            [호출 화면] 퀴즈 풀기 완료 후
            [권한 조건] 로그인 필수 (JWT Bearer 토큰)
            [특이 동작] 서버에서 정답 검증 및 점수 계산. 결과를 즉시 반환한다.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "퀴즈 제출 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "잘못된 문항 ID 또는 유효성 검사 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "퀴즈를 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<QuizAttemptSummaryResponse>> submitQuiz(
        Authentication authentication,
        @RequestBody @Valid QuizSubmitRequest request) {
        log.debug("퀴즈 제출 요청: principal={}, quizId={}", authentication.getName(), request.quizId());

        boolean isGuest = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_GUEST"));

        QuizAttemptSummaryResponse result = isGuest
            ? quizAttemptService.submitQuizAsGuest(authentication.getName(), request)
            : quizAttemptService.submitQuiz(authentication.getName(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @Operation(
        summary = "퀴즈 응시 기록 목록 조회",
        description = """
            [호출 화면] 프로필 > 퀴즈 기록
            [권한 조건] 로그인 필수 (JWT Bearer 토큰)
            [특이 동작] 페이지네이션 및 정렬(LATEST/SCORE)을 지원한다.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "기록 목록 조회 성공")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<QuizAttemptSummaryResponse>>> getHistory(
        Authentication authentication,
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "페이지 크기", example = "10")
        @RequestParam(defaultValue = "10") int size,
        @Parameter(description = "정렬 기준 (LATEST: 최신 순, SCORE: 점수 순)")
        @RequestParam(defaultValue = "LATEST") AttemptSortType sort) {
        log.debug("퀴즈 기록 조회: email={}, page={}, size={}, sort={}",
            authentication.getName(), page, size, sort);
        Page<QuizAttemptSummaryResponse> result =
            quizAttemptService.getHistory(authentication.getName(), page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(
        summary = "퀴즈 응시 상세 결과 조회",
        description = """
            [호출 화면] 퀴즈 기록 > 상세 보기
            [권한 조건] 로그인 필수 (JWT Bearer 토큰)
            [특이 동작] 문항별 선택 답안, 정답, 정답 여부를 모두 반환한다. 본인 기록만 조회 가능.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "상세 결과 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "응시 기록을 찾을 수 없음")
    })
    @GetMapping("/{attemptId}")
    public ResponseEntity<ApiResponse<QuizAttemptDetailResponse>> getDetail(
        Authentication authentication,
        @Parameter(description = "응시 기록 ID", example = "1")
        @PathVariable Long attemptId) {
        log.debug("퀴즈 상세 조회: email={}, attemptId={}", authentication.getName(), attemptId);
        QuizAttemptDetailResponse result =
            quizAttemptService.getDetail(authentication.getName(), attemptId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(
        summary = "사용자 퀴즈 통계 조회",
        description = """
            [호출 화면] 프로필 > 학습 통계
            [권한 조건] 로그인 필수 (JWT Bearer 토큰)
            [특이 동작] 누적 통계와 기간별(일간/주간/월간) 통계를 함께 반환한다.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "통계 조회 성공")
    })
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getStats(
        Authentication authentication,
        @Parameter(description = "집계 기간 (DAILY: 최근 7일, WEEKLY: 최근 4주, MONTHLY: 최근 12개월)")
        @RequestParam(defaultValue = "DAILY") StatPeriod period) {
        log.debug("퀴즈 통계 조회: email={}, period={}", authentication.getName(), period);
        UserStatsResponse result =
            quizAttemptService.getStats(authentication.getName(), period);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
