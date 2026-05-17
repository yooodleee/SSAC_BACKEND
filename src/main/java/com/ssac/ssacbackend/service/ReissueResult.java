package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.TokenPair;

/**
 * 토큰 재발급 결과 — 새 토큰 쌍과 사용자 엔티티를 함께 담는다.
 */
public record ReissueResult(TokenPair tokens, User user) {}
