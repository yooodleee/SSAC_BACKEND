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

    private ContentProgress buildContentProgress(Long id, String lastPosition, int progressRate) {
        ContentProgress cp = ContentProgress.builder()
            .title("테스트 콘텐츠")
            .lastPosition(lastPosition)
            .progressRate(progressRate)
            .build();
        ReflectionTestUtils.setField(cp, "id", id);
        return cp;
    }
}
