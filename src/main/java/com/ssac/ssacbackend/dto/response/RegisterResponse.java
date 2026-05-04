package com.ssac.ssacbackend.dto.response;

/**
 * 회원 가입 완료 응답 DTO.
 */
public record RegisterResponse(
    String accessToken,
    String refreshToken,
    UserInfo user,
    MergedInfo merged
) {
    public record UserInfo(
        Long id,
        String nickname,
        String provider,
        String segment
    ) {}

    public record MergedInfo(int quizCount) {}
}
