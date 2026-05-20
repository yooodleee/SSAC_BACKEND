package com.ssac.ssacbackend.dto.response;

/**
 * 관리자 홈 화면 응답 DTO.
 */
public record AdminHomeResponse(
    AdminInfo admin,
    Stats stats
) {
    public record AdminInfo(
        String nickname,
        String role
    ) {
    }

    public record Stats(
        long totalUsers,
        long totalFeedbacks,
        long pendingFeedbacks
    ) {
    }
}
