package com.ssac.ssacbackend.dto.request;

/**
 * 뉴스 정렬 기준.
 *
 * <ul>
 *   <li>{@link #LATEST} - publishedAt 기준 내림차순</li>
 *   <li>{@link #POPULARITY} - 최근 7일 viewCount 기준 내림차순, 동점 시 publishedAt 내림차순</li>
 * </ul>
 *
 * <p>ArchUnit 규칙(Controller는 Domain에 접근 불가)에 따라 dto 패키지에 위치한다.
 */
public enum NewsSortType {
    LATEST, POPULARITY
}
