package com.ssac.ssacbackend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryRateLimitStoreTest {

    private InMemoryRateLimitStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryRateLimitStore();
    }

    // ── increment 기본 동작 ───────────────────────────────────────────────────

    @Test
    @DisplayName("첫 요청은 1을 반환한다")
    void 첫_요청_카운트_1() {
        assertThat(store.increment("1.2.3.4:/api/v1/auth", 60_000L)).isEqualTo(1L);
    }

    @Test
    @DisplayName("연속 요청 시 카운트가 누적된다")
    void 연속_요청_카운트_누적() {
        store.increment("1.2.3.4:/api/v1/auth", 60_000L);
        store.increment("1.2.3.4:/api/v1/auth", 60_000L);
        assertThat(store.increment("1.2.3.4:/api/v1/auth", 60_000L)).isEqualTo(3L);
    }

    @Test
    @DisplayName("윈도우가 만료되면 카운터가 1로 초기화된다")
    void 만료_후_카운트_초기화() throws InterruptedException {
        store.increment("1.2.3.4:/api/v1/auth", 1L); // windowMs=1ms
        Thread.sleep(5);
        assertThat(store.increment("1.2.3.4:/api/v1/auth", 1L)).isEqualTo(1L);
    }

    // ── evictExpired ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("만료된 엔트리는 evictExpired 호출 후 Map에서 제거된다")
    void 만료_엔트리_제거() throws InterruptedException {
        store.increment("1.2.3.4:/api/v1/auth", 1L); // windowMs=1ms → 즉시 만료
        Thread.sleep(5);

        store.evictExpired();

        // 제거된 뒤 다음 increment는 카운터 1로 재시작
        assertThat(store.increment("1.2.3.4:/api/v1/auth", 60_000L)).isEqualTo(1L);
    }

    @Test
    @DisplayName("아직 유효한 엔트리는 evictExpired 호출 후에도 유지된다")
    void 유효_엔트리_유지() {
        store.increment("1.2.3.4:/api/v1/auth", 60_000L);
        store.increment("1.2.3.4:/api/v1/auth", 60_000L);

        store.evictExpired();

        // 유효한 엔트리이므로 카운트 3으로 누적
        assertThat(store.increment("1.2.3.4:/api/v1/auth", 60_000L)).isEqualTo(3L);
    }

    // ── MAX_ENTRIES 상한 보호 ─────────────────────────────────────────────────

    @Test
    @DisplayName("엔트리 수가 MAX_ENTRIES를 초과하면 만료 엔트리를 즉시 퇴출한다")
    void MAX_ENTRIES_초과_시_만료_엔트리_퇴출() throws InterruptedException {
        // MAX_ENTRIES개 엔트리를 짧은 windowMs로 추가 (즉시 만료)
        for (int i = 0; i < InMemoryRateLimitStore.MAX_ENTRIES; i++) {
            store.increment("ip" + i + ":/api/v1/auth", 1L);
        }
        Thread.sleep(5); // 모든 엔트리 만료

        // MAX_ENTRIES 초과 시점 — 내부에서 evictExpired 호출됨
        long count = store.increment("new-ip:/api/v1/auth", 60_000L);

        assertThat(count).isEqualTo(1L);
    }
}
