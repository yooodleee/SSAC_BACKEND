package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.dto.response.HomeResponse.CategoryDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.RecommendedContentDto;
import com.ssac.ssacbackend.repository.ContentProgressRepository;
import com.ssac.ssacbackend.repository.ContentRepository;
import com.ssac.ssacbackend.repository.UserInterestRepository;
import com.ssac.ssacbackend.service.HomeContentAssembler.ContentSections;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HomeContentAssemblerTest {

    @Mock
    private UserInterestRepository userInterestRepository;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private ContentProgressRepository contentProgressRepository;

    @InjectMocks
    private HomeContentAssembler assembler;

    @BeforeEach
    void setUp() {
        given(contentRepository.countPublishedGroupByCategory()).willReturn(List.of());
        given(contentProgressRepository.countCompletedByUserEmailGroupByCategory(anyString()))
            .willReturn(List.of());
        given(contentProgressRepository.findContinueLearning(anyString(), any()))
            .willReturn(List.of());
    }

    // ── 추천 콘텐츠 ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("추천 콘텐츠 빌드")
    class RecommendedContents {

        @Test
        @DisplayName("완료된 콘텐츠는 추천 목록에서 제외된다")
        void 완료된_콘텐츠_추천_제외() {
            given(userInterestRepository.findDomainIdsByUserId(1L)).willReturn(List.of());
            given(contentProgressRepository.findCompletedContentIdsByUserEmail("test@test.com"))
                .willReturn(List.of(100L));

            Content completedContent = mockContent(100L, "완료된 콘텐츠", "investment", UserLevel.SEED);
            Content newContent = mockContent(200L, "새 콘텐츠", "investment", UserLevel.SEED);
            given(contentRepository.findByDifficultyPublished(eq(UserLevel.SEED), any()))
                .willReturn(List.of(completedContent, newContent));
            given(contentRepository.findAllPublishedOrderByLastEdited(any()))
                .willReturn(List.of(completedContent, newContent));

            ContentSections result = assembler.build(1L, "test@test.com", UserLevel.SEED, Set.of());

            List<String> ids = result.recommended().stream()
                .map(RecommendedContentDto::id).toList();
            assertThat(ids).doesNotContain("100");
            assertThat(ids).contains("200");
        }

        @Test
        @DisplayName("추천 콘텐츠 소진 시 상위 레벨 미리보기 응답")
        void 추천_소진_시_상위_레벨_미리보기() {
            given(userInterestRepository.findDomainIdsByUserId(2L)).willReturn(List.of());
            given(contentProgressRepository.findCompletedContentIdsByUserEmail("test2@test.com"))
                .willReturn(List.of());

            given(contentRepository.findByDifficultyPublished(eq(UserLevel.SEED), any()))
                .willReturn(List.of());
            given(contentRepository.findAllPublishedOrderByLastEdited(any())).willReturn(List.of());

            Content previewContent = mockContent(500L, "SPROUT 미리보기", "investment", UserLevel.SPROUT);
            given(contentRepository.findByDifficultyPublished(eq(UserLevel.SPROUT), any()))
                .willReturn(List.of(previewContent));

            ContentSections result = assembler.build(2L, "test2@test.com", UserLevel.SEED, Set.of());

            assertThat(result.recommended()).anyMatch(RecommendedContentDto::isPreview);
        }

        @Test
        @DisplayName("관심 도메인 변경 후 추천 콘텐츠가 해당 도메인 콘텐츠를 우선 포함한다")
        void 관심_도메인_변경_후_추천_재구성() {
            given(userInterestRepository.findDomainIdsByUserId(3L)).willReturn(List.of("resume"));
            given(contentProgressRepository.findCompletedContentIdsByUserEmail("test3@test.com"))
                .willReturn(List.of());

            Content resumeContent = mockContent(800L, "이력서 작성", "resume", UserLevel.SEED);
            given(contentRepository.findByCategoriesInAndDifficultyPublished(
                eq(List.of("resume")), eq(UserLevel.SEED), any()))
                .willReturn(List.of(resumeContent));
            given(contentRepository.findByDifficultyPublished(eq(UserLevel.SEED), any()))
                .willReturn(List.of());
            given(contentRepository.findAllPublishedOrderByLastEdited(any())).willReturn(List.of());

            ContentSections result = assembler.build(3L, "test3@test.com", UserLevel.SEED, Set.of());

            assertThat(result.recommended()).anyMatch(c -> c.id().equals("800"));
        }

        @Test
        @DisplayName("레벨 변경 후 새 레벨 기반 콘텐츠가 추천에 포함된다")
        void 레벨_변경_후_새_레벨_기반_추천() {
            given(userInterestRepository.findDomainIdsByUserId(4L)).willReturn(List.of());
            given(contentProgressRepository.findCompletedContentIdsByUserEmail("test4@test.com"))
                .willReturn(List.of());

            Content treeContent = mockContent(700L, "TREE 콘텐츠", "investment", UserLevel.TREE);
            given(contentRepository.findByDifficultyPublished(eq(UserLevel.TREE), any()))
                .willReturn(List.of(treeContent));
            given(contentRepository.findAllPublishedOrderByLastEdited(any())).willReturn(List.of());

            ContentSections result = assembler.build(4L, "test4@test.com", UserLevel.TREE, Set.of());

            assertThat(result.recommended()).anyMatch(c -> c.id().equals("700"));
        }

        @Test
        @DisplayName("최근 추천 이력에 포함된 콘텐츠는 추천에서 제외된다")
        void 최근_추천_이력_제외() {
            given(userInterestRepository.findDomainIdsByUserId(5L)).willReturn(List.of());
            given(contentProgressRepository.findCompletedContentIdsByUserEmail("test5@test.com"))
                .willReturn(List.of());

            Content oldContent = mockContent(10L, "이전 추천", "investment", UserLevel.SEED);
            Content newContent = mockContent(20L, "새 추천", "investment", UserLevel.SEED);
            given(contentRepository.findByDifficultyPublished(eq(UserLevel.SEED), any()))
                .willReturn(List.of(oldContent, newContent));
            given(contentRepository.findAllPublishedOrderByLastEdited(any()))
                .willReturn(List.of(oldContent, newContent));

            ContentSections result = assembler.build(5L, "test5@test.com", UserLevel.SEED, Set.of(10L));

            List<String> ids = result.recommended().stream()
                .map(RecommendedContentDto::id).toList();
            assertThat(ids).doesNotContain("10");
            assertThat(ids).contains("20");
        }
    }

    // ── 카테고리 섹션 ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("카테고리 섹션 - GROUP BY 쿼리 최적화")
    class CategoryGroupBy {

        @Test
        @DisplayName("카테고리별 게시 수와 완료 수를 GROUP BY 쿼리 2회로 집계한다")
        void 카테고리_집계_groupby_쿼리_사용() {
            given(userInterestRepository.findDomainIdsByUserId(10L)).willReturn(List.of());
            given(contentProgressRepository.findCompletedContentIdsByUserEmail("cat@test.com"))
                .willReturn(List.of());
            given(contentRepository.findByDifficultyPublished(any(), any())).willReturn(List.of());
            given(contentRepository.findAllPublishedOrderByLastEdited(any())).willReturn(List.of());

            List<Object[]> publishedRows = new ArrayList<>();
            publishedRows.add(new Object[]{"realestate", 3L});
            publishedRows.add(new Object[]{"tax", 2L});
            List<Object[]> completedRows = new ArrayList<>();
            completedRows.add(new Object[]{"tax", 1L});
            given(contentRepository.countPublishedGroupByCategory()).willReturn(publishedRows);
            given(contentProgressRepository.countCompletedByUserEmailGroupByCategory("cat@test.com"))
                .willReturn(completedRows);

            ContentSections result = assembler.build(10L, "cat@test.com", UserLevel.SEED, Set.of());

            CategoryDto realestate = result.categories().stream()
                .filter(c -> "realestate".equals(c.id())).findFirst().orElseThrow();
            CategoryDto tax = result.categories().stream()
                .filter(c -> "tax".equals(c.id())).findFirst().orElseThrow();

            assertThat(realestate.totalCount()).isEqualTo(3L);
            assertThat(realestate.completedCount()).isEqualTo(0L);
            assertThat(tax.totalCount()).isEqualTo(2L);
            assertThat(tax.completedCount()).isEqualTo(1L);

            // 카테고리별 단건 쿼리(N+1)가 호출되지 않았음을 검증
            then(contentRepository).should(never()).countByPublishedAndCategory(anyString());
            then(contentProgressRepository).should(never())
                .countCompletedByUserEmailAndCategory(anyString(), anyString());
        }
    }

    // ── 오늘의 카드 ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("오늘의 카드 빌드")
    class TodayCard {

        @Test
        @DisplayName("후보 콘텐츠가 없으면 오늘의 카드는 null이다")
        void 후보_없으면_오늘의_카드_null() {
            given(userInterestRepository.findDomainIdsByUserId(20L)).willReturn(List.of());
            given(contentProgressRepository.findCompletedContentIdsByUserEmail("card@test.com"))
                .willReturn(List.of());
            given(contentRepository.findByDifficultyPublished(any(), any())).willReturn(List.of());
            given(contentRepository.findAllPublishedOrderByLastEdited(any())).willReturn(List.of());

            ContentSections result = assembler.build(20L, "card@test.com", UserLevel.SEED, Set.of());

            assertThat(result.todayCard()).isNull();
        }

        @Test
        @DisplayName("후보가 있으면 오늘의 카드가 반환된다")
        void 후보_있으면_오늘의_카드_반환() {
            given(userInterestRepository.findDomainIdsByUserId(21L)).willReturn(List.of());
            given(contentProgressRepository.findCompletedContentIdsByUserEmail("card2@test.com"))
                .willReturn(List.of());

            Content content = mockContent(300L, "카드 콘텐츠", "investment", UserLevel.SEED);
            given(contentRepository.findByDifficultyPublished(eq(UserLevel.SEED), any()))
                .willReturn(List.of(content));
            given(contentRepository.findAllPublishedOrderByLastEdited(any())).willReturn(List.of());

            ContentSections result = assembler.build(21L, "card2@test.com", UserLevel.SEED, Set.of());

            assertThat(result.todayCard()).isNotNull();
            assertThat(result.todayCard().id()).isEqualTo("300");
        }
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private Content mockContent(Long id, String title, String category, UserLevel difficulty) {
        Content content = mock(Content.class);
        given(content.getId()).willReturn(id);
        given(content.getTitle()).willReturn(title);
        given(content.getFirstCategory()).willReturn(category);
        given(content.getDifficulty()).willReturn(difficulty);
        return content;
    }
}
