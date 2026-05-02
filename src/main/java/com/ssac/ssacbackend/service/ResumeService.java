package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.content.ContentProgress;
import com.ssac.ssacbackend.dto.response.ResumeContentResponse;
import com.ssac.ssacbackend.dto.response.ResumeResponse;
import com.ssac.ssacbackend.repository.ContentProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이어보기 서비스.
 */
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ContentProgressRepository contentProgressRepository;

    /**
     * 사용자의 최근 미완료 콘텐츠 진행 상황을 반환한다.
     * 진행 중인 콘텐츠가 없으면 hasResume: false와 content: null을 반환한다.
     */
    @Transactional(readOnly = true)
    public ResumeResponse getResume(String email) {
        return contentProgressRepository
            .findLatestInProgressByUserEmail(email)
            .map(cp -> ResumeResponse.of(ResumeContentResponse.from(cp)))
            .orElse(ResumeResponse.empty());
    }

    /**
     * 콘텐츠의 lastPosition과 progressRate를 갱신한다.
     */
    @Transactional
    public ResumeContentResponse updateProgress(Long id, String email,
        String lastPosition, int progressRate) {
        ContentProgress cp = contentProgressRepository
            .findByIdAndUserEmail(id, email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.CONTENT_NOT_FOUND));
        cp.updateProgress(lastPosition, progressRate);
        return ResumeContentResponse.from(cp);
    }
}
