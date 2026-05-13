package com.ssac.ssacbackend.dto.response;

import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.domain.user.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 사용자 프로필 응답 DTO.
 */
public record ProfileResponse(

    @Schema(description = "사용자 ID", example = "1")
    Long id,

    @Schema(description = "이메일", example = "user@example.com")
    String email,

    @Schema(description = "닉네임", example = "닉네임123")
    String nickname,

    @Schema(description = "사용자 유형")
    UserType userType,

    @Schema(description = "사용자 레벨")
    UserLevel level,

    @Schema(description = "온보딩 완료 여부")
    boolean onboardingCompleted,

    @Schema(description = "가입일시")
    LocalDateTime createdAt

) {
    public static ProfileResponse from(User user) {
        boolean onboardingCompleted = user.getUserType() != null && user.getLevel() != null;
        return new ProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getUserType(),
            user.getLevel(),
            onboardingCompleted,
            user.getCreatedAt()
        );
    }
}
