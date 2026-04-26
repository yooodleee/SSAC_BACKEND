package com.ssac.ssacbackend.dto.response;

import com.ssac.ssacbackend.domain.content.ContentProgress;

/**
 * 이어보기 콘텐츠 정보 응답 DTO.
 */
public record ResumeContentResponse(
    String id,
    String title,
    String lastPosition,
    int progressRate
) {
    public static ResumeContentResponse from(ContentProgress cp) {
        return new ResumeContentResponse(
            cp.getId().toString(),
            cp.getTitle(),
            cp.getLastPosition(),
            cp.getProgressRate()
        );
    }
}
