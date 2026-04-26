package com.ssac.ssacbackend.dto.response;

/**
 * 사용자 세그먼트 응답 DTO.
 */
public record UserSegmentResponse(String segment) {

    public static UserSegmentResponse beginner() {
        return new UserSegmentResponse("beginner");
    }

    public static UserSegmentResponse advanced() {
        return new UserSegmentResponse("advanced");
    }
}
