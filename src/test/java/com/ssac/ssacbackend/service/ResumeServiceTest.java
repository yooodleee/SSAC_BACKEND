package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.content.ContentProgress;
import com.ssac.ssacbackend.dto.response.ResumeResponse;
import com.ssac.ssacbackend.repository.ContentProgressRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock
    private ContentProgressRepository contentProgressRepository;

    @InjectMocks
    private ResumeService resumeService;

    @Test
    @DisplayName("getResume - 진행 중인 콘텐츠가 있으면 hasResume: true를 반환한다")
    void getResume_진행중콘텐츠_있음() {
        ContentProgress cp = buildContentProgress(1L, "chapter-3", 60);
        given(contentProgressRepository.findLatestInProgressByUserEmail("user@test.com"))
            .willReturn(Optional.of(cp));

        ResumeResponse result = resumeService.getResume("user@test.com");

        assertThat(result.hasResume()).isTrue();
        assertThat(result.content()).isNotNull();
        assertThat(result.content().lastPosition()).isEqualTo("chapter-3");
        assertThat(result.content().progressRate()).isEqualTo(60);
    }

    @Test
    @DisplayName("getResume - 진행 중인 콘텐츠가 없으면 hasResume: false, content: null을 반환한다")
    void getResume_진행중콘텐츠_없음() {
        given(contentProgressRepository.findLatestInProgressByUserEmail("user@test.com"))
            .willReturn(Optional.empty());

        ResumeResponse result = resumeService.getResume("user@test.com");

        assertThat(result.hasResume()).isFalse();
        assertThat(result.content()).isNull();
    }

    @Test
    @DisplayName("updateProgress - 진행 기록을 갱신하고 응답을 반환한다")
    void updateProgress_정상() {
        ContentProgress cp = buildContentProgress(1L, "chapter-1", 20);
        given(contentProgressRepository.findByIdAndUserEmail(1L, "user@test.com"))
            .willReturn(Optional.of(cp));

        var result = resumeService.updateProgress(1L, "user@test.com", "chapter-5", 80);

        assertThat(result.lastPosition()).isEqualTo("chapter-5");
        assertThat(result.progressRate()).isEqualTo(80);
    }

    @Test
    @DisplayName("updateProgress - 존재하지 않는 진행 기록이면 NotFoundException을 던진다")
    void updateProgress_존재하지않음() {
        given(contentProgressRepository.findByIdAndUserEmail(99L, "user@test.com"))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> resumeService.updateProgress(99L, "user@test.com", "chapter-1", 10))
            .isInstanceOf(NotFoundException.class);
    }

    /**
     * [비정규화 동기화 정책]
     *
     * <p>ContentProgress.category는 ContentService.completeContent() 또는 startContent() 호출 시
     * Content.getFirstCategory() 값을 복사해 저장하는 비정규화 필드다.
     *
     * <p>updateProgress()는 학습 위치(lastPosition)와 진행률(progressRate)만 갱신하며
     * category는 절대 변경하지 않는다. 이 계약이 깨지면 홈 화면 이어보기 카테고리 필터가
     * 오동작한다.
     *
     * <p>Notion 콘텐츠의 카테고리가 변경된 경우, ContentProgress.category는 즉시 반영되지 않는다.
     * 갱신이 필요하다면 NotionSyncService 배치 이후 별도 마이그레이션으로 처리한다.
     */
    @Test
    @DisplayName("updateProgress - category는 updateProgress 호출로 변경되지 않는다 (비정규화 동기화 정책)")
    void updateProgress_category_불변() {
        ContentProgress cp = buildContentProgressWithCategory(1L, "chapter-1", 20, "realestate");
        given(contentProgressRepository.findByIdAndUserEmail(1L, "user@test.com"))
            .willReturn(Optional.of(cp));

        resumeService.updateProgress(1L, "user@test.com", "chapter-5", 80);

        assertThat(cp.getCategory())
            .as("category는 updateProgress()로 변경되지 않는다")
            .isEqualTo("realestate");
    }

    @Test
    @DisplayName("updateProgress - lastPosition과 progressRate만 갱신된다")
    void updateProgress_lastPosition_progressRate_만_변경() {
        ContentProgress cp = buildContentProgressWithCategory(1L, "chapter-1", 20, "tax");
        given(contentProgressRepository.findByIdAndUserEmail(1L, "user@test.com"))
            .willReturn(Optional.of(cp));

        resumeService.updateProgress(1L, "user@test.com", "chapter-9", 95);

        assertThat(cp.getLastPosition()).isEqualTo("chapter-9");
        assertThat(cp.getProgressRate()).isEqualTo(95);
        assertThat(cp.getCategory()).isEqualTo("tax");
    }

    private ContentProgress buildContentProgress(Long id, String lastPosition, int progressRate) {
        ContentProgress cp = ContentProgress.builder()
            .title("테스트 콘텐츠")
            .lastPosition(lastPosition)
            .progressRate(progressRate)
            .build();
        ReflectionTestUtils.setField(cp, "id", id);
        return cp;
    }

    private ContentProgress buildContentProgressWithCategory(
            Long id, String lastPosition, int progressRate, String category) {
        ContentProgress cp = ContentProgress.builder()
            .title("테스트 콘텐츠")
            .lastPosition(lastPosition)
            .progressRate(progressRate)
            .category(category)
            .build();
        ReflectionTestUtils.setField(cp, "id", id);
        return cp;
    }
}
