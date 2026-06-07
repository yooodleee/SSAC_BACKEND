package com.ssac.ssacbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ssac.ssacbackend.common.util.CacheKeys;
import com.ssac.ssacbackend.component.NotionImageMigrator;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.NotionClient;
import notion.api.v1.model.blocks.Blocks;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Notion 블록 조회, Redis 캐싱, Cloudinary 이미지 마이그레이션을 담당하는 서비스.
 *
 * <p>Notion API 블록을 Gson으로 직렬화하여 Notion API 원본 형식(snake_case)을 그대로 반환한다.
 * 조회 결과는 Redis에 {@value CacheKeys#BLOCK_TTL_SECONDS}초 동안 캐싱된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotionBlockFetchService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Gson GSON = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();

    private final NotionClient notionClient;
    private final NotionImageMigrator notionImageMigrator;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Notion 페이지의 블록 목록을 반환한다.
     *
     * <p>Redis 캐시 히트 시 캐시를 반환하고, 미스 시 Notion API를 호출한다.
     * Image 타입 블록의 URL은 Cloudinary로 이전하여 만료 없는 영구 URL로 제공한다.
     * has_children이 true인 블록은 자식 블록을 함께 조회하여 포함한다.
     *
     * @param notionPageId Notion 페이지 ID
     * @return 블록 목록 (Notion API 원본 형식)
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchBlocks(String notionPageId) {
        if (notionPageId == null || notionPageId.isBlank()) {
            return List.of();
        }
        String cacheKey = CacheKeys.BLOCK_PREFIX + notionPageId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return OBJECT_MAPPER.readValue(cached,
                    new TypeReference<List<Map<String, Object>>>() {});
            } catch (JsonProcessingException e) {
                log.warn("블록 캐시 역직렬화 실패, Notion 재조회: notionPageId={}", notionPageId, e);
            }
        }
        try {
            Blocks blocks = notionClient.retrieveBlockChildren(notionPageId, null, 100);
            List<Map<String, Object>> result = blocks.getResults().stream()
                .filter(Objects::nonNull)
                .<Map<String, Object>>map(block -> {
                    try {
                        String json = GSON.toJson(block);
                        Map<String, Object> map = OBJECT_MAPPER.readValue(
                            json, new TypeReference<Map<String, Object>>() {});
                        if (map == null) {
                            return null;
                        }
                        migrateImageUrl(map);
                        if (Boolean.TRUE.equals(map.get("has_children"))) {
                            map.put("children", fetchChildBlocks((String) map.get("id")));
                        }
                        return map;
                    } catch (Exception e) {
                        log.warn("블록 직렬화 실패: blockId={}", block.getId(), e);
                        Map<String, Object> fallback = new HashMap<>();
                        fallback.put("id", block.getId());
                        fallback.put("type", block.getType() != null
                            ? block.getType().toString() : "unknown");
                        return fallback;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
            try {
                String json = OBJECT_MAPPER.writeValueAsString(result);
                stringRedisTemplate.opsForValue()
                    .set(cacheKey, json, Duration.ofSeconds(CacheKeys.BLOCK_TTL_SECONDS));
            } catch (JsonProcessingException e) {
                log.warn("블록 캐시 직렬화 실패: notionPageId={}", notionPageId, e);
            }
            return result;
        } catch (Exception e) {
            log.warn("Notion 블록 조회 실패: notionPageId={}", notionPageId, e);
            return List.of();
        }
    }

    /**
     * 자식 블록 목록을 조회하여 직렬화한다.
     *
     * <p>Image 블록 URL은 Cloudinary로 이전한다.
     */
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> fetchChildBlocks(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return List.of();
        }
        try {
            Blocks children = notionClient.retrieveBlockChildren(blockId, null, 100);
            return children.getResults().stream()
                .filter(Objects::nonNull)
                .<Map<String, Object>>map(block -> {
                    try {
                        String json = GSON.toJson(block);
                        Map<String, Object> map = OBJECT_MAPPER.readValue(
                            json, new TypeReference<Map<String, Object>>() {});
                        if (map == null) {
                            return null;
                        }
                        migrateImageUrl(map);
                        return map;
                    } catch (Exception e) {
                        log.warn("자식 블록 직렬화 실패: blockId={}", block.getId(), e);
                        Map<String, Object> fallback = new HashMap<>();
                        fallback.put("id", block.getId());
                        fallback.put("type", block.getType() != null
                            ? block.getType().toString() : "unknown");
                        return fallback;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
        } catch (Exception e) {
            log.warn("자식 블록 조회 실패: blockId={}", blockId, e);
            return List.of();
        }
    }

    /**
     * Image 타입 블록의 URL을 Cloudinary URL로 교체한다.
     *
     * <p>file 타입과 external 타입 모두 처리한다.
     */
    @SuppressWarnings("unchecked")
    void migrateImageUrl(Map<String, Object> block) {
        if (!"image".equals(block.get("type"))) {
            return;
        }
        Map<String, Object> imageMap = (Map<String, Object>) block.get("image");
        if (imageMap == null) {
            return;
        }
        String imageType = (String) imageMap.get("type");
        if ("file".equals(imageType)) {
            Map<String, Object> fileMap = (Map<String, Object>) imageMap.get("file");
            if (fileMap != null && fileMap.get("url") instanceof String url) {
                fileMap.put("url", notionImageMigrator.migrateIfNeeded(url));
                // Cloudinary URL은 만료되지 않으므로 Notion S3의 expiry_time 메타데이터를 제거한다
                fileMap.remove("expiry_time");
            }
        } else if ("external".equals(imageType)) {
            Map<String, Object> externalMap = (Map<String, Object>) imageMap.get("external");
            if (externalMap != null && externalMap.get("url") instanceof String url) {
                externalMap.put("url", notionImageMigrator.migrateIfNeeded(url));
            }
        }
    }
}
