package com.ssac.ssacbackend.dto.response;

/**
 * 닉네임 중복 확인 응답 DTO.
 */
public record NicknameCheckResponse(boolean isAvailable) {

    public static NicknameCheckResponse available() {
        return new NicknameCheckResponse(true);
    }

    public static NicknameCheckResponse unavailable() {
        return new NicknameCheckResponse(false);
    }
}
