package com.ssac.ssacbackend.common.response;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 페이지네이션 공통 응답 래퍼.
 *
 * <p>Spring Data {@link Page}의 내부 구현 세부사항(pageable, numberOfElements 등)을
 * API 응답에서 숨기고 FE에서 필요한 최소 메타데이터만 노출한다.
 *
 * <pre>
 * {
 *   "totalCount": 100,
 *   "totalPages": 5,
 *   "page": 1,
 *   "size": 20,
 *   "hasNext": true,
 *   "items": [...]
 * }
 * </pre>
 *
 * <p>page는 1-based (컨트롤러 수신 기준).
 * 신규 페이지네이션 엔드포인트는 반드시 이 클래스를 사용한다.
 */
public record PageResponse<T>(
    long totalCount,
    int totalPages,
    int page,
    int size,
    boolean hasNext,
    List<T> items
) {

    /**
     * Spring Data {@link Page}를 1-based 페이지 번호와 함께 변환한다.
     *
     * @param pageResult    서비스에서 반환된 Spring Data 페이지 결과
     * @param requestedPage 컨트롤러가 수신한 1-based 페이지 번호
     */
    public static <T> PageResponse<T> of(Page<T> pageResult, int requestedPage) {
        return new PageResponse<>(
            pageResult.getTotalElements(),
            pageResult.getTotalPages(),
            requestedPage,
            pageResult.getSize(),
            pageResult.hasNext(),
            pageResult.getContent()
        );
    }
}
