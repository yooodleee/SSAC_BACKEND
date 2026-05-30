package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ssac.ssacbackend.domain.content.ContentProgress;
import com.ssac.ssacbackend.dto.response.UserSegmentResponse;
import com.ssac.ssacbackend.repository.ContentProgressRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserSegmentServiceTest {

    @Mock
    private ContentProgressRepository contentProgressRepository;

    @InjectMocks
    private UserSegmentService userSegmentService;

    @Test
    @DisplayName("getSegment - 완료 콘텐츠가 ADVANCED_THRESHOLD 이상이면 advanced를 반환한다")
    void getSegment_advanced() {
        given(contentProgressRepository.countByUserEmailAndProgressRateGreaterThanEqual(
            "user@test.com", ContentProgress.COMPLETION_THRESHOLD))
            .willReturn((long) ContentProgress.ADVANCED_THRESHOLD);

        UserSegmentResponse result = userSegmentService.getSegment("user@test.com");

        assertThat(result.segment()).isEqualTo("advanced");
    }

    @Test
    @DisplayName("getSegment - 완료 콘텐츠가 ADVANCED_THRESHOLD 미만이면 beginner를 반환한다")
    void getSegment_beginner() {
        given(contentProgressRepository.countByUserEmailAndProgressRateGreaterThanEqual(
            "user@test.com", ContentProgress.COMPLETION_THRESHOLD))
            .willReturn((long) ContentProgress.ADVANCED_THRESHOLD - 1);

        UserSegmentResponse result = userSegmentService.getSegment("user@test.com");

        assertThat(result.segment()).isEqualTo("beginner");
    }

    @Test
    @DisplayName("getSegment - 데이터가 없는 신규 사용자는 beginner를 반환한다")
    void getSegment_신규사용자_beginner() {
        given(contentProgressRepository.countByUserEmailAndProgressRateGreaterThanEqual(
            "new@test.com", ContentProgress.COMPLETION_THRESHOLD))
            .willReturn(0L);

        UserSegmentResponse result = userSegmentService.getSegment("new@test.com");

        assertThat(result.segment()).isEqualTo("beginner");
    }
}
