package com.ssac.ssacbackend.dto.response;

/**
 * 메뉴별 클릭 집계 및 CTR 응답 DTO.
 *
 * <p>집계 기간: 최근 7일.
 * CTR = 메뉴 클릭 수 / 전체 고유 사용자 수 * 100.
 */
public record MenuClickStatResponse(
    String menuId,
    String menuName,
    long clickCount,
    double ctr
) {
}
