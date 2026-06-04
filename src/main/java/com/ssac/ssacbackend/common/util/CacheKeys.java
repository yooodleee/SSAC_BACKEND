package com.ssac.ssacbackend.common.util;

/**
 * Redis 캐시 키 상수 모음.
 *
 * <p>키 형식: {@code domain:type:{id}}
 */
public final class CacheKeys {

    /** 콘텐츠 목록 캐시 키 접두사. */
    public static final String CONTENT_LIST_PREFIX = "contents:v4:";

    /** Notion 블록 캐시 키 접두사. */
    public static final String BLOCK_PREFIX = "content:blocks:";

    /** Notion 블록 캐시 TTL (초). */
    public static final long BLOCK_TTL_SECONDS = 86400L;

    private CacheKeys() {
    }
}
