package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ssac.ssacbackend.repository.QuestionRepository;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import com.ssac.ssacbackend.repository.QuizRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import com.ssac.ssacbackend.service.QuizAttemptService.MyPageQuizStats;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * QuizAttemptService 마이페이지 위임 메서드 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private LevelUpService levelUpService;
    @Mock
    private HomeCacheEvictService homeCacheEvictService;

    @InjectMocks
    private QuizAttemptService quizAttemptService;

    @Nested
    @DisplayName("getMyPageQuizStats")
    class GetMyPageQuizStats {

        @Test
        @DisplayName("응시 이력이 없으면 0으로 초기화된 통계를 반환한다")
        void 응시이력_없으면_기본값() {
            given(quizAttemptRepository.aggregateOverallStats("test@test.com"))
                .willReturn(Collections.emptyList());

            MyPageQuizStats result = quizAttemptService.getMyPageQuizStats("test@test.com");

            assertThat(result.totalCompleted()).isZero();
            assertThat(result.correctRate()).isZero();
        }

        @Test
        @DisplayName("집계 결과에서 완료 수와 정답률을 계산하여 반환한다")
        void 집계결과_정상계산() {
            // row: [totalScore, totalAttempts, totalCorrect, totalQuestions]
            Object[] row = {100L, 5L, 20L, 25L};
            given(quizAttemptRepository.aggregateOverallStats("test@test.com"))
                .willReturn(Collections.singletonList(row));

            MyPageQuizStats result = quizAttemptService.getMyPageQuizStats("test@test.com");

            assertThat(result.totalCompleted()).isEqualTo(5L);
            // 20 / 25 * 100 = 80%
            assertThat(result.correctRate()).isEqualTo(80);
        }

        @Test
        @DisplayName("집계 첫 행이 null이면 0으로 초기화된 통계를 반환한다")
        void 집계첫행_null이면_기본값() {
            given(quizAttemptRepository.aggregateOverallStats("test@test.com"))
                .willReturn(Collections.singletonList(new Object[]{null, null, null, null}));

            MyPageQuizStats result = quizAttemptService.getMyPageQuizStats("test@test.com");

            assertThat(result.totalCompleted()).isZero();
            assertThat(result.correctRate()).isZero();
        }
    }

    @Nested
    @DisplayName("findActivityTimestamps")
    class FindActivityTimestamps {

        @Test
        @DisplayName("퀴즈 응시 활동 일시 목록을 반환한다")
        void 활동일시_목록반환() {
            LocalDateTime ts = LocalDateTime.of(2026, 6, 19, 10, 0);
            given(quizAttemptRepository.findActivityTimestampsByUserEmail("test@test.com"))
                .willReturn(List.of(ts));

            List<LocalDateTime> result = quizAttemptService.findActivityTimestamps("test@test.com");

            assertThat(result).containsExactly(ts);
        }

        @Test
        @DisplayName("응시 이력이 없으면 빈 목록을 반환한다")
        void 응시이력_없으면_빈목록() {
            given(quizAttemptRepository.findActivityTimestampsByUserEmail("test@test.com"))
                .willReturn(List.of());

            List<LocalDateTime> result = quizAttemptService.findActivityTimestamps("test@test.com");

            assertThat(result).isEmpty();
        }
    }
}
