package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.domain.content.ContentProgress;
import com.ssac.ssacbackend.dto.response.UserSegmentResponse;
import com.ssac.ssacbackend.repository.ContentProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 세그먼트 서비스.
 *
 * <p>세그먼트 분류 기준:
 * <ul>
 *   <li>완료 콘텐츠 수 {@value ContentProgress#ADVANCED_THRESHOLD}개 미만 → beginner</li>
 *   <li>완료 콘텐츠 수 {@value ContentProgress#ADVANCED_THRESHOLD}개 이상 → advanced</li>
 * </ul>
 * 데이터가 없는 신규 사용자는 기본값 beginner를 반환한다.
 */
@Service
@RequiredArgsConstructor
public class UserSegmentService {

    private final ContentProgressRepository contentProgressRepository;

    @Transactional(readOnly = true)
    public UserSegmentResponse getSegment(String email) {
        long completedCount = contentProgressRepository
            .countByUserEmailAndProgressRateGreaterThanEqual(
                email, ContentProgress.COMPLETION_THRESHOLD);
        if (completedCount >= ContentProgress.ADVANCED_THRESHOLD) {
            return UserSegmentResponse.advanced();
        }
        return UserSegmentResponse.beginner();
    }
}
