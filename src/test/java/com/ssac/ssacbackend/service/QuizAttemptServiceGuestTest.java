package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.domain.quiz.Question;
import com.ssac.ssacbackend.domain.quiz.Quiz;
import com.ssac.ssacbackend.domain.quiz.QuizAttempt;
import com.ssac.ssacbackend.dto.request.QuizSubmitRequest;
import com.ssac.ssacbackend.dto.response.QuizAttemptSummaryResponse;
import com.ssac.ssacbackend.repository.QuestionRepository;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import com.ssac.ssacbackend.repository.QuizRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceGuestTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @InjectMocks
    private QuizAttemptService quizAttemptService;

    @Test
    @DisplayName("submitQuizAsGuest는 user 없이 guestId로 응시 기록을 저장한다")
    void submitQuizAsGuest_savesAttemptWithGuestIdAndNoUser() {
        String guestId = "test-guest-uuid";
        Quiz quiz = buildQuiz(1L, 20, 2);
        Question q1 = buildQuestion(1L, quiz, "A", 10);
        Question q2 = buildQuestion(2L, quiz, "B", 10);

        given(quizRepository.findById(1L)).willReturn(Optional.of(quiz));
        given(questionRepository.findByQuizIdOrderByQuestionOrder(1L)).willReturn(List.of(q1, q2));
        given(quizAttemptRepository.save(any(QuizAttempt.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        QuizSubmitRequest request = new QuizSubmitRequest(1L, List.of(
            new QuizSubmitRequest.AnswerItem(1L, "A"),
            new QuizSubmitRequest.AnswerItem(2L, "B")
        ));

        QuizAttemptSummaryResponse result = quizAttemptService.submitQuizAsGuest(guestId, request);

        ArgumentCaptor<QuizAttempt> captor = ArgumentCaptor.forClass(QuizAttempt.class);
        verify(quizAttemptRepository).save(captor.capture());
        QuizAttempt saved = captor.getValue();

        assertThat(saved.getGuestId()).isEqualTo(guestId);
        assertThat(saved.getUser()).isNull();
    }

    @Test
    @DisplayName("submitQuizAsGuest는 서버 채점 결과를 정확히 계산해 저장한다")
    void submitQuizAsGuest_calculatesScoreCorrectly() {
        String guestId = "test-guest-uuid";
        Quiz quiz = buildQuiz(1L, 20, 2);
        Question q1 = buildQuestion(1L, quiz, "A", 10);
        Question q2 = buildQuestion(2L, quiz, "B", 10);

        given(quizRepository.findById(1L)).willReturn(Optional.of(quiz));
        given(questionRepository.findByQuizIdOrderByQuestionOrder(1L)).willReturn(List.of(q1, q2));
        given(quizAttemptRepository.save(any(QuizAttempt.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // q1 정답(A), q2 오답(C)
        QuizSubmitRequest request = new QuizSubmitRequest(1L, List.of(
            new QuizSubmitRequest.AnswerItem(1L, "A"),
            new QuizSubmitRequest.AnswerItem(2L, "C")
        ));

        QuizAttemptSummaryResponse result = quizAttemptService.submitQuizAsGuest(guestId, request);

        assertThat(result.earnedScore()).isEqualTo(10);
        assertThat(result.correctCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("submitQuizAsGuest는 모든 문항이 오답이면 점수 0을 저장한다")
    void submitQuizAsGuest_allWrong_savesZeroScore() {
        String guestId = "test-guest-uuid";
        Quiz quiz = buildQuiz(1L, 20, 2);
        Question q1 = buildQuestion(1L, quiz, "A", 10);
        Question q2 = buildQuestion(2L, quiz, "B", 10);

        given(quizRepository.findById(1L)).willReturn(Optional.of(quiz));
        given(questionRepository.findByQuizIdOrderByQuestionOrder(1L)).willReturn(List.of(q1, q2));
        given(quizAttemptRepository.save(any(QuizAttempt.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        QuizSubmitRequest request = new QuizSubmitRequest(1L, List.of(
            new QuizSubmitRequest.AnswerItem(1L, "X"),
            new QuizSubmitRequest.AnswerItem(2L, "X")
        ));

        QuizAttemptSummaryResponse result = quizAttemptService.submitQuizAsGuest(guestId, request);

        assertThat(result.earnedScore()).isEqualTo(0);
        assertThat(result.correctCount()).isEqualTo(0);
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private Quiz buildQuiz(Long id, int maxScore, int totalQuestions) {
        Quiz quiz = mock(Quiz.class);
        given(quiz.getId()).willReturn(id);
        given(quiz.getMaxScore()).willReturn(maxScore);
        given(quiz.getTotalQuestions()).willReturn(totalQuestions);
        given(quiz.getTitle()).willReturn("테스트 퀴즈");
        return quiz;
    }

    private Question buildQuestion(Long id, Quiz quiz, String correctAnswer, int points) {
        Question q = mock(Question.class);
        given(q.getId()).willReturn(id);
        given(q.getCorrectAnswer()).willReturn(correctAnswer);
        // getPoints()는 정답인 경우에만 호출되므로 lenient 처리
        Mockito.lenient().when(q.getPoints()).thenReturn(points);
        return q;
    }
}
