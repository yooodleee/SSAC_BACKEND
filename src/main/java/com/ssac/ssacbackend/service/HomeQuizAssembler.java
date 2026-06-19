package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.domain.quiz.Quiz;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.dto.response.HomeResponse.TodayQuizDto;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import com.ssac.ssacbackend.repository.QuizRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 홈 화면 오늘의 퀴즈 섹션 조립기.
 *
 * <p>오답 우선 정렬 → 결정론적 선택 순서로 오늘의 퀴즈를 빌드한다.
 * {@link HomeService}에서 위임받아 실행된다.
 */
@Service
@RequiredArgsConstructor
public class HomeQuizAssembler {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    /**
     * 오늘의 퀴즈를 빌드한다.
     *
     * <p>미완료 퀴즈 중 오답 기록이 있는 퀴즈를 우선 선택하고, 결정론적 인덱스로 하나를 반환한다.
     * 미완료 퀴즈가 없으면 null을 반환한다.
     */
    public TodayQuizDto build(Long userId, UserLevel level, String email) {
        List<Quiz> candidates = quizRepository.findUncompletedByDifficultyAndUserEmail(level, email);
        if (candidates.isEmpty()) {
            return null;
        }

        Set<Long> incorrectIds = new HashSet<>(
            quizAttemptRepository.findIncorrectQuizIdsByUserEmail(email));

        List<Quiz> sorted = new ArrayList<>();
        candidates.stream().filter(q -> incorrectIds.contains(q.getId())).forEach(sorted::add);
        candidates.stream().filter(q -> !incorrectIds.contains(q.getId())).forEach(sorted::add);

        int index = deterministicIndex(userId, sorted.size());
        Quiz picked = sorted.get(index);

        return new TodayQuizDto(
            String.valueOf(picked.getId()),
            picked.getTitle(),
            picked.getCategory(),
            picked.getDifficulty() != null ? picked.getDifficulty().name() : null
        );
    }

    private static int deterministicIndex(Long userId, int size) {
        long seed = (userId != null ? userId : 0L) + LocalDate.now().toEpochDay();
        return (int) Math.abs(seed % size);
    }
}
