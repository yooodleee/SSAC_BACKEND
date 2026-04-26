package com.ssac.ssacbackend.dto.response;

import com.ssac.ssacbackend.domain.news.News;
import java.time.LocalDateTime;

/**
 * 뉴스 단건 응답 DTO.
 */
public record NewsItemResponse(
    String id,
    String title,
    String summary,
    int viewCount,
    LocalDateTime publishedAt
) {
    public static NewsItemResponse from(News news) {
        return new NewsItemResponse(
            news.getId().toString(),
            news.getTitle(),
            news.getSummary(),
            news.getViewCount(),
            news.getPublishedAt()
        );
    }
}
