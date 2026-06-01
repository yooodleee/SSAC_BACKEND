package com.ssac.ssacbackend.dto.response;

import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.domain.user.UserType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 토큰 재발급(reissue) 응답 DTO.
 *
 * <p>Access Token과 Refresh Token 모두 응답 바디에 포함된다.
 * Refresh Token은 Set-Cookie HttpOnly 쿠키로도 전달되지만,
 * Next.js BFF 환경에서 Set-Cookie 포워딩이 불안정하므로 body에도 포함하여
 * BFF가 cookies().set()으로 직접 설정할 수 있도록 한다.
 * FE가 재접속 시 화면 분기(온보딩 미완료 → 온보딩, 완료 → 홈)에 필요한 사용자 컨텍스트를 포함한다.
 */
public record ReissueResponse(

    @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    String accessToken,

    @Schema(description = "리프레시 토큰 (BFF cookies().set() 용도, HttpOnly 쿠키로도 병행 전달)")
    String refreshToken,

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
    public static ReissueResponse of(String accessToken, String refreshToken, User user) {
        return new ReissueResponse(
            accessToken,
            refreshToken,
            "Bearer",
            user.getId(),
            user.getNickname(),
            user.getUserType(),
            user.getLevel(),
            user.isOnboardingCompleted()
        );
    }
}
