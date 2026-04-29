package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.config.AbTestProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * A/B 테스트 그룹 할당 서비스.
 *
 * <p>동일 식별자(userId 또는 guestId)는 항상 동일한 그룹을 받는다.
 * 할당은 식별자의 해시값을 groupARatio 기준으로 나누어 결정한다.
 * 테스트 종료(enabled=false) 시 모든 사용자에게 adoptedGroup을 반환한다.
 */
@Service
@RequiredArgsConstructor
public class AbTestService {

    private final AbTestProperties abTestProperties;

    /**
     * 사용자에게 할당된 A/B 테스트 그룹을 반환한다.
     *
     * @param identifier userId 또는 guestId
     * @return "A" 또는 "B"
     */
    public String assignGroup(String identifier) {
        if (!abTestProperties.enabled()) {
            return normalizeGroup(abTestProperties.adoptedGroup());
        }
        int hash = Math.abs(identifier.hashCode());
        double ratio = (double) (hash % 100) / 100.0;
        return ratio < abTestProperties.groupARatio() ? "A" : "B";
    }

    private String normalizeGroup(String group) {
        if (group == null || group.isBlank()) {
            return "A";
        }
        return group.trim().toUpperCase();
    }
}
