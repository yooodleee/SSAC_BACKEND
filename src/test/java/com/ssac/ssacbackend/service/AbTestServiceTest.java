package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ssac.ssacbackend.config.AbTestProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbTestServiceTest {

    // ── A/B 테스트 비활성화 상태 ──────────────────────────────────────────────

    @Test
    @DisplayName("A/B 테스트 비활성화 시 adoptedGroup을 반환한다")
    void 테스트_비활성화_시_adoptedGroup_반환() {
        AbTestService service = new AbTestService(new AbTestProperties(false, "B", 0.5));

        assertThat(service.assignGroup("any-user")).isEqualTo("B");
    }

    @Test
    @DisplayName("A/B 테스트 비활성화 시 adoptedGroup이 소문자여도 대문자로 정규화된다")
    void 테스트_비활성화_시_adoptedGroup_대문자_정규화() {
        AbTestService service = new AbTestService(new AbTestProperties(false, "a", 0.5));

        assertThat(service.assignGroup("any-user")).isEqualTo("A");
    }

    @Test
    @DisplayName("A/B 테스트 비활성화 시 adoptedGroup이 null이면 기본값 A를 반환한다")
    void 테스트_비활성화_adoptedGroup_null이면_기본값_A() {
        AbTestService service = new AbTestService(new AbTestProperties(false, null, 0.5));

        assertThat(service.assignGroup("any-user")).isEqualTo("A");
    }

    @Test
    @DisplayName("A/B 테스트 비활성화 시 adoptedGroup이 빈 문자열이면 기본값 A를 반환한다")
    void 테스트_비활성화_adoptedGroup_빈문자열이면_기본값_A() {
        AbTestService service = new AbTestService(new AbTestProperties(false, "  ", 0.5));

        assertThat(service.assignGroup("any-user")).isEqualTo("A");
    }

    // ── A/B 테스트 활성화 상태 ────────────────────────────────────────────────

    @Test
    @DisplayName("A/B 테스트 활성화 시 동일한 식별자는 항상 동일한 그룹을 받는다")
    void 동일_식별자_항상_동일_그룹_할당() {
        AbTestService service = new AbTestService(new AbTestProperties(true, "A", 0.5));
        String identifier = "stable-user-id";

        String first = service.assignGroup(identifier);
        String second = service.assignGroup(identifier);
        String third = service.assignGroup(identifier);

        assertThat(first).isEqualTo(second).isEqualTo(third);
    }

    @Test
    @DisplayName("groupARatio=0.0이면 모든 사용자가 B 그룹에 할당된다")
    void groupARatio_0이면_모두_B그룹() {
        AbTestService service = new AbTestService(new AbTestProperties(true, "A", 0.0));

        // ratio < 0.0 이 아닌 모든 해시 → B
        assertThat(service.assignGroup("user-1")).isEqualTo("B");
        assertThat(service.assignGroup("user-2")).isEqualTo("B");
        assertThat(service.assignGroup("user-3")).isEqualTo("B");
    }

    @Test
    @DisplayName("groupARatio=1.0이면 모든 사용자가 A 그룹에 할당된다")
    void groupARatio_1이면_모두_A그룹() {
        AbTestService service = new AbTestService(new AbTestProperties(true, "B", 1.0));

        // ratio < 1.0 → 0.0 ~ 0.99 범위는 모두 A
        assertThat(service.assignGroup("user-1")).isEqualTo("A");
        assertThat(service.assignGroup("user-2")).isEqualTo("A");
        assertThat(service.assignGroup("user-3")).isEqualTo("A");
    }

    @Test
    @DisplayName("A/B 테스트 활성화 시 반환 값은 A 또는 B 중 하나이다")
    void 테스트_활성화_시_반환값은_A_또는_B() {
        AbTestService service = new AbTestService(new AbTestProperties(true, "A", 0.5));

        for (int i = 0; i < 50; i++) {
            String group = service.assignGroup("user-" + i);
            assertThat(group).isIn("A", "B");
        }
    }
}
