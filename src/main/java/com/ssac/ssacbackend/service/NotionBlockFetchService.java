package com.ssac.ssacbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ssac.ssacbackend.common.util.CacheKeys;
import com.ssac.ssacbackend.component.NotionImageMigrator;
import com.ssac.ssacbackend.config.NotionProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.NotionClient;
import notion.api.v1.model.blocks.Block;
import notion.api.v1.model.blocks.Blocks;
import notion.api.v1.model.blocks.UnsupportedBlock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Notion 블록 조회, Redis 캐싱, Cloudinary 이미지 마이그레이션을 담당하는 서비스.
 *
 * <p>Notion API 블록을 Gson으로 직렬화하여 Notion API 원본 형식(snake_case)을 그대로 반환한다.
 * SDK가 인식하지 못하는 블록 타입(heading_4 등)은 Notion API에 직접 HTTP 요청하여 원본 타입을 복원한다.
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
    private static final String NOTION_API_BASE = "https://api.notion.com/v1";
    private static final String NOTION_VERSION = "2022-06-28";

    private final NotionClient notionClient;
    private final NotionProperties notionProperties;
    private final NotionImageMigrator notionImageMigrator;
    private final StringRedisTemplate stringRedisTemplate;
    private final HttpClient httpClient;

    /**
     * Notion 페이지의 블록 목록을 반환한다.
     *
     * <p>Redis 캐시 히트 시 캐시를 반환하고, 미스 시 Notion API를 호출한다.
     * Image 타입 블록의 URL은 Cloudinary로 이전하여 만료 없는 영구 URL로 제공한다.
     * has_children이 true인 블록은 자식 블록을 함께 조회하여 재귀적으로 포함한다.
     *
     * @param notionPageId Notion 페이지 ID
     * @return 블록 목록 (Notion API 원본 형식)
     */
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
            List<Map<String, Object>> result = new ArrayList<>();
            String startCursor = null;
            do {
                Blocks blocks = notionClient.retrieveBlockChildren(notionPageId, startCursor, 100);
                result.addAll(processBlockList(blocks.getResults()));
                startCursor = Boolean.TRUE.equals(blocks.getHasMore()) ? blocks.getNextCursor() : null;
            } while (startCursor != null);
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
     * <p>has_children이 true인 블록은 재귀적으로 자식을 조회하여 포함한다.
     * Image 블록 URL은 Cloudinary로 이전한다.
     */
    List<Map<String, Object>> fetchChildBlocks(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> result = new ArrayList<>();
            String startCursor = null;
            do {
                Blocks children = notionClient.retrieveBlockChildren(blockId, startCursor, 100);
                result.addAll(processBlockList(children.getResults()));
                startCursor = Boolean.TRUE.equals(children.getHasMore()) ? children.getNextCursor() : null;
            } while (startCursor != null);
            return result;
        } catch (Exception e) {
            log.warn("자식 블록 조회 실패: blockId={}", blockId, e);
            return List.of();
        }
    }

    /**
     * Block 목록을 Map 목록으로 직렬화한다.
     *
     * <p>null 요소를 제거하고, has_children이 true인 블록은 자식을 재귀적으로 포함한다.
     * numbered_list_item 블록은 연속 순서대로 number 필드(1부터)를 주입한다.
     * Notion API는 번호 매기기 목록의 순번을 제공하지 않으므로 백엔드에서 직접 부여한다.
     */
    private List<Map<String, Object>> processBlockList(List<? extends Block> blocks) {
        List<Map<String, Object>> result = new ArrayList<>();
        int numberedListCounter = 0;
        for (Block block : blocks) {
            if (block == null) {
                continue;
            }
            Map<String, Object> map = serializeBlock(block);
            if (map == null) {
                continue;
            }
            if ("numbered_list_item".equals(map.get("type"))) {
                numberedListCounter++;
                injectNumber(map, numberedListCounter);
            } else {
                numberedListCounter = 0;
            }
            result.add(map);
        }
        return result;
    }

    /**
     * numbered_list_item 블록의 numbered_list_item 프로퍼티에 number 필드를 주입한다.
     */
    @SuppressWarnings("unchecked")
    private void injectNumber(Map<String, Object> map, int number) {
        Object element = map.get("numbered_list_item");
        if (element instanceof Map) {
            ((Map<String, Object>) element).put("number", number);
        }
    }

    /**
     * 단일 Block을 Map으로 직렬화한다.
     *
     * <p>SDK가 인식하지 못한 UnsupportedBlock은 Notion API에 직접 HTTP 요청하여 원본 타입과 콘텐츠를 복원한다.
     * GSON으로 직렬화한 뒤 ObjectMapper로 역직렬화한다.
     * BlockType enum은 Notion API 값(snake_case)으로 교체한다.
     * has_children이 true이면 자식 블록을 재귀적으로 포함한다.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> serializeBlock(Block block) {
        try {
            if (block instanceof UnsupportedBlock) {
                Map<String, Object> raw = fetchRawBlock(block.getId());
                if (raw != null) {
                    migrateImageUrl(raw);
                    if (Boolean.TRUE.equals(raw.get("has_children"))) {
                        String childId = (String) raw.get("id");
                        raw.put("children", fetchChildBlocks(childId));
                    }
                    return raw;
                }
            }
            String json = GSON.toJson(block);
            Map<String, Object> map = OBJECT_MAPPER.readValue(
                json, new TypeReference<Map<String, Object>>() {});
            if (map == null) {
                return null;
            }
            // GSON은 BlockType enum을 name()(PascalCase)으로 직렬화한다.
            // Notion API 원본 형식(snake_case)으로 교체한다.
            if (block.getType() != null) {
                map.put("type", block.getType().getValue());
            }
            migrateImageUrl(map);
            if (Boolean.TRUE.equals(map.get("has_children"))) {
                String childId = (String) map.get("id");
                map.put("children", fetchChildBlocks(childId));
            }
            return map;
        } catch (Exception e) {
            log.warn("블록 직렬화 실패: blockId={}", block.getId(), e);
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("id", block.getId());
            fallback.put("type", block.getType() != null
                ? block.getType().getValue() : "unknown");
            return fallback;
        }
    }

    /**
     * Notion API에 직접 HTTP 요청하여 단일 블록의 원본 JSON을 반환한다.
     *
     * <p>SDK가 인식하지 못하는 블록 타입(heading_4 등)의 실제 타입과 콘텐츠를 복원하는 데 사용된다.
     */
    Map<String, Object> fetchRawBlock(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return null;
        }
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(NOTION_API_BASE + "/blocks/" + blockId))
            .header("Authorization", "Bearer " + notionProperties.getApiKey())
            .header("Notion-Version", NOTION_VERSION)
            .GET()
            .build();
        try {
            HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return OBJECT_MAPPER.readValue(
                    response.body(), new TypeReference<Map<String, Object>>() {});
            }
            log.warn("Unsupported 블록 원본 조회 실패: blockId={}, status={}",
                blockId, response.statusCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Unsupported 블록 원본 조회 인터럽트: blockId={}", blockId, e);
        } catch (Exception e) {
            log.warn("Unsupported 블록 원본 조회 실패: blockId={}", blockId, e);
        }
        return null;
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
