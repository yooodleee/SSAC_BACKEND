package com.ssac.ssacbackend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 도메인별 에러 코드 정의.
 *
 * <p>각 코드는 "{도메인}-{번호}" 형식이며 클라이언트가 파싱할 수 있는 안정적인 식별자다.
 * 메시지는 사용자에게 노출되므로 한국어로 작성한다.
 *
 * <p>코드 추가/수정 후 contract/error-contract.yml 도 함께 갱신해야 한다.
 * 버전 규칙: 추가 → Patch, 수정 → Minor, 삭제/변경 → Major
 *
 * <p>코드 추가 기준: docs/conventions.md#예외-처리
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── 공통 (COMMON) ──────────────────────────────────────────────────────────
    INVALID_INPUT("COMMON-001", 400, "잘못된 입력값입니다."),
    PAGE_SIZE_EXCEEDED("COMMON-002", 400, "페이지 크기가 최대값을 초과했습니다."),
    INVALID_SORT_PARAMETER("COMMON-003", 400, "허용되지 않는 정렬 기준입니다."),

    // ── 서버 (SERVER) ──────────────────────────────────────────────────────────
    INTERNAL_SERVER_ERROR("SERVER-001", 500, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),

    // ── 인증 (AUTH) ────────────────────────────────────────────────────────────
    UNAUTHORIZED("AUTH-001", 401, "인증이 필요합니다."),
    TOKEN_EXPIRED("AUTH-002", 401, "토큰이 만료되었습니다."),
    TOKEN_INVALID("AUTH-003", 401, "유효하지 않은 토큰입니다."),
    ACCESS_DENIED("AUTH-004", 403, "접근 권한이 없습니다."),
    TOKEN_MISSING("AUTH-005", 401, "Refresh Token이 없습니다."),
    OAUTH_STATE_INVALID("AUTH-006", 400, "유효하지 않은 state 파라미터입니다."),
    OAUTH_STATE_EXPIRED("AUTH-007", 400, "만료된 state 파라미터입니다. 다시 로그인해 주세요."),
    OAUTH_AUTH_FAILED("AUTH-008", 400, "OAuth 인증에 실패했습니다."),
    OAUTH_PROFILE_FAILED("AUTH-009", 400, "OAuth 프로필 정보를 가져오는 데 실패했습니다."),

    // ── 사용자 (USER) ──────────────────────────────────────────────────────────
    USER_NOT_FOUND("USER-001", 404, "존재하지 않는 사용자입니다."),
    NICKNAME_DUPLICATED("USER-002", 409, "이미 사용 중인 닉네임입니다."),
    NICKNAME_INVALID("USER-003", 400, "사용할 수 없는 닉네임입니다."),
    ROLE_ASSIGNMENT_INVALID("USER-004", 400, "해당 역할은 직접 부여할 수 없습니다."),

    // ── 뉴스 (NEWS) ────────────────────────────────────────────────────────────
    NEWS_NOT_FOUND("NEWS-001", 404, "존재하지 않는 뉴스입니다."),

    // ── 알림 (NOTI) ────────────────────────────────────────────────────────────
    NOTIFICATION_NOT_FOUND("NOTI-001", 404, "존재하지 않는 알림입니다."),

    // ── 퀴즈 (QUIZ) ────────────────────────────────────────────────────────────
    QUIZ_NOT_FOUND("QUIZ-001", 404, "존재하지 않는 퀴즈입니다."),
    QUIZ_ATTEMPT_NOT_FOUND("QUIZ-002", 404, "존재하지 않는 응시 기록입니다."),
    QUIZ_QUESTION_MISMATCH("QUIZ-003", 400, "퀴즈에 속하지 않는 문항입니다."),

    // ── 콘텐츠 (CONTENT) ──────────────────────────────────────────────────────
    CONTENT_NOT_FOUND("CONTENT-001", 404, "진행 중인 콘텐츠를 찾을 수 없습니다."),

    // ── 이벤트 (EVENT) ────────────────────────────────────────────────────────
    INVALID_EVENT_DATA("EVENT-001", 400, "userId 또는 guestId 중 하나는 반드시 필요합니다."),

    // ── Guest (GUEST) ──────────────────────────────────────────────────────────
    GUEST_NOT_ALLOWED("GUEST-001", 403, "로그인이 필요한 기능입니다."),

    // ── 회원 가입 약관 (TERMS) ────────────────────────────────────────────────
    TERMS_REQUIRED("TERMS-001", 400, "필수 약관에 동의해주세요."),
    REGISTRATION_SESSION_EXPIRED("TERMS-002", 401, "회원가입 세션이 만료되었습니다. 다시 시도해주세요.");

    private final String code;
    private final int status;
    private final String message;
}
