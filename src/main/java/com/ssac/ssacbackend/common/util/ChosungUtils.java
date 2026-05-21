package com.ssac.ssacbackend.common.util;

/**
 * 한글 초성 추출 유틸리티.
 */
public final class ChosungUtils {

    private static final char[] CHOSUNG_LIST = {
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ',
        'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    private ChosungUtils() {}

    /**
     * 텍스트에서 한글 초성을 추출한다. 비한글 문자는 그대로 유지한다.
     */
    public static String extract(String text) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) {
                result.append(CHOSUNG_LIST[(c - 0xAC00) / 588]);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 입력값이 모두 한글 자음(초성)인지 확인한다.
     */
    public static boolean isAllChosung(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.chars().allMatch(c -> c >= 0x3131 && c <= 0x314E);
    }
}
