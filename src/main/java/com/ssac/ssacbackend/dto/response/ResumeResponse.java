package com.ssac.ssacbackend.dto.response;

/**
 * 이어보기 응답 DTO.
 */
public record ResumeResponse(
    boolean hasResume,
    ResumeContentResponse content
) {
    public static ResumeResponse empty() {
        return new ResumeResponse(false, null);
    }

    public static ResumeResponse of(ResumeContentResponse content) {
        return new ResumeResponse(true, content);
    }
}
