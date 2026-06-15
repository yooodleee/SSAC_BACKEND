package com.ssac.ssacbackend.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChosungUtilsTest {

    // ── extract ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("한글 문자열의 초성을 추출한다")
    void 한글_초성_추출() {
        assertThat(ChosungUtils.extract("안녕하세요")).isEqualTo("ㅇㄴㅎㅅㅇ");
    }

    @Test
    @DisplayName("비한글 문자는 그대로 유지된다")
    void 비한글_문자_유지() {
        assertThat(ChosungUtils.extract("hello")).isEqualTo("hello");
    }

    @Test
    @DisplayName("한글과 비한글이 혼재된 문자열을 처리한다")
    void 한글_비한글_혼재() {
        assertThat(ChosungUtils.extract("AI학습")).isEqualTo("AIㅎㅅ");
    }

    @Test
    @DisplayName("빈 문자열을 입력하면 빈 문자열을 반환한다")
    void 빈_문자열_반환() {
        assertThat(ChosungUtils.extract("")).isEqualTo("");
    }

    // ── isAllChosung ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("모두 한글 자음(초성)이면 true를 반환한다")
    void 모두_초성이면_true() {
        assertThat(ChosungUtils.isAllChosung("ㄱㄴㄷ")).isTrue();
    }

    @Test
    @DisplayName("한글 초성이 아닌 문자가 포함되면 false를 반환한다")
    void 초성_외_문자_포함_false() {
        assertThat(ChosungUtils.isAllChosung("ㄱab")).isFalse();
    }

    @Test
    @DisplayName("완성형 한글이 포함되면 false를 반환한다")
    void 완성형_한글_포함_false() {
        assertThat(ChosungUtils.isAllChosung("가나다")).isFalse();
    }

    @Test
    @DisplayName("null 입력 시 false를 반환한다")
    void null_입력_false() {
        assertThat(ChosungUtils.isAllChosung(null)).isFalse();
    }

    @Test
    @DisplayName("빈 문자열 입력 시 false를 반환한다")
    void 빈_문자열_false() {
        assertThat(ChosungUtils.isAllChosung("")).isFalse();
    }

    @Test
    @DisplayName("공백 문자열 입력 시 false를 반환한다")
    void 공백_문자열_false() {
        assertThat(ChosungUtils.isAllChosung("   ")).isFalse();
    }
}
