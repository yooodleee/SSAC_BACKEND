package com.ssac.ssacbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * A/B 테스트 설정 프로퍼티.
 *
 * <p>ab-test.menu.enabled=true 이면 테스트 진행 중, false 이면 종료 상태.
 * 테스트 종료 시 ab-test.menu.adopted-group 에 채택된 그룹(A 또는 B)을 지정한다.
 * 그룹 A 할당 비율은 ab-test.menu.group-a-ratio 로 설정한다 (0.0 ~ 1.0, 기본 0.5).
 */
@ConfigurationProperties(prefix = "ab-test.menu")
public record AbTestProperties(
    boolean enabled,
    String adoptedGroup,
    double groupARatio
) {
    public AbTestProperties {
        if (groupARatio < 0.0 || groupARatio > 1.0) {
            throw new IllegalArgumentException("groupARatio must be between 0.0 and 1.0");
        }
    }
}
