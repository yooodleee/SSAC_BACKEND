package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.dto.response.ContentDetailResponse;
import com.ssac.ssacbackend.dto.response.ContentItemDto;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * NotionContentLoader 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class NotionContentLoaderTest {

    @Mock
    private NotionSyncService notionSyncService;
    @Mock
    private NotionBlockFetchService notionBlockFetchService;

    @InjectMocks
    private NotionContentLoader notionContentLoader;

    @Nested
    @DisplayName("getPublishedContentItems")
    class GetPublishedContentItems {

        @Test
        @DisplayName("NotionSyncService에 목록 조회를 위임한다")
        void 목록조회_위임() {
            ContentItemDto item = new ContentItemDto(
                "1", "제목", null, List.of("AI"), List.of(), "SEED", "왕초보", false, null);
            given(notionSyncService.getPublishedContentItems(List.of("AI"), null, null))
                .willReturn(List.of(item));

            List<ContentItemDto> result =
                notionContentLoader.getPublishedContentItems(List.of("AI"), null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo("1");
            verify(notionSyncService).getPublishedContentItems(List.of("AI"), null, null);
        }

        @Test
        @DisplayName("필터 없이 전체 목록을 조회한다")
        void 전체목록_조회() {
            given(notionSyncService.getPublishedContentItems(null, null, null))
                .willReturn(List.of());

            List<ContentItemDto> result =
                notionContentLoader.getPublishedContentItems(null, null, null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("buildContentDetail")
    class BuildContentDetail {

        @Test
        @DisplayName("NotionBlockFetchService에 블록 조회를 위임하고 ContentDetailResponse를 조립한다")
        void 상세조회_조립() {
            Content content = mock(Content.class);
            given(content.getId()).willReturn(10L);
            given(content.getNotionPageId()).willReturn("page-abc");
            given(content.getTitle()).willReturn("테스트 콘텐츠");
            given(content.getThumbnailUrl()).willReturn(null);
            given(content.getCategories()).willReturn(List.of("AI"));
            given(content.getDomains()).willReturn(new LinkedHashSet<>());
            given(content.getDifficulty()).willReturn(UserLevel.SEED);
            given(content.getNotionCreatedAt()).willReturn(null);
            given(content.getNotionLastEditedAt()).willReturn(null);

            List<Map<String, Object>> blocks = List.of(Map.of("type", "paragraph"));
            given(notionBlockFetchService.fetchBlocks("page-abc")).willReturn(blocks);

            ContentDetailResponse result = notionContentLoader.buildContentDetail(content);

            assertThat(result.id()).isEqualTo("10");
            assertThat(result.title()).isEqualTo("테스트 콘텐츠");
            assertThat(result.difficulty()).isEqualTo("SEED");
            assertThat(result.difficultyLabel()).isEqualTo("왕초보");
            assertThat(result.blocks()).hasSize(1);
            verify(notionBlockFetchService).fetchBlocks("page-abc");
        }

        @Test
        @DisplayName("difficulty가 null이면 difficultyLabel은 빈 문자열이다")
        void difficulty_null이면_label_empty() {
            Content content = mock(Content.class);
            given(content.getId()).willReturn(1L);
            given(content.getNotionPageId()).willReturn("page-xyz");
            given(content.getTitle()).willReturn("제목");
            given(content.getThumbnailUrl()).willReturn(null);
            given(content.getCategories()).willReturn(List.of());
            given(content.getDomains()).willReturn(new LinkedHashSet<>());
            given(content.getDifficulty()).willReturn(null);
            given(content.getNotionCreatedAt()).willReturn(null);
            given(content.getNotionLastEditedAt()).willReturn(null);
            given(notionBlockFetchService.fetchBlocks(any())).willReturn(List.of());

            ContentDetailResponse result = notionContentLoader.buildContentDetail(content);

            assertThat(result.difficulty()).isNull();
            assertThat(result.difficultyLabel()).isEqualTo("");
        }
    }
}
