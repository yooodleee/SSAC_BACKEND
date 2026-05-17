package com.ssac.ssacbackend.dto.response;

import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.domain.user.UserType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 토큰 재발급(reissue) 응답 DTO.
 *
 * <p>Access Token은 응답 바디에 포함되며, Refresh Token은 Set-Cookie HttpOnly 쿠키로만 전달된다.
 * FE가 재접속 시 화면 분기(온보딩 미완료 → 온보딩, 완료 → 홈)에 필요한 사용자 컨텍스트를 포함한다.
 */
public record ReissueResponse(

    @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    String accessToken,

    @Schema(description = "토큰 타입", example = "Bearer")
    String tokenType,

    @Schema(description = "사용자 ID", example = "1")
    Long userId,

    @Schema(description = "닉네임", example = "닉네임123")
    String nickname,

    @Schema(description = "사용자 유형")
    UserType userType,

    @Schema(description = "사용자 레벨")
    UserLevel level,

    @Schema(description = "온보딩 완료 여부", example = "true")
    boolean onboardingCompleted

) {
    public static ReissueResponse of(String accessToken, User user) {
        return new ReissueResponse(
            accessToken,
            "Bearer",
            user.getId(),
            user.getNickname(),
            user.getUserType(),
            user.getLevel(),
            user.isOnboardingCompleted()
        );
    }
}
