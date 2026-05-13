package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.dto.AuthCodeResult;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.dto.request.AuthCodeExchangeRequest;
import com.ssac.ssacbackend.dto.response.AuthTokenResponse;
import com.ssac.ssacbackend.service.AuthCodeService;
import com.ssac.ssacbackend.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 일회용 인가 코드(authCode)를 JWT 토큰으로 교환하는 엔드포인트.
 *
 * <p>OAuth 콜백 완료 후 FE로 리다이렉트할 때 JWT를 URL에 직접 노출하지 않기 위해
 * 단기(30초) 일회용 코드를 사용한다. FE는 이 엔드포인트를 호출해 실제 토큰을 교환한다.
 *
 * <pre>
 * [흐름]
 * 1. Provider → BE OAuth 콜백
 * 2. BE: authCode 발급 → FE redirect (/auth/callback?authCode=&lt;code&gt;&amp;provider=&lt;PROVIDER&gt;)
 * 3. FE: POST /api/v1/auth/token { authCode }
 * 4. BE: authCode 검증 → JWT 발급 (기존 회원) 또는 tempToken 반환 (신규 회원)
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "인가 코드 교환", description = "OAuth 완료 후 일회용 인가 코드를 JWT로 교환하는 API")
public class AuthTokenController {

    private final AuthCodeService authCodeService;
    private final TokenService tokenService;

    /**
     * 일회용 인가 코드를 소비하고 JWT 토큰 또는 신규 회원 임시 토큰을 반환한다.
     *
     * <p>authCode는 TTL 30초이며 단 한 번만 소비 가능하다.
     * 만료되거나 이미 소비된 코드는 400을 반환한다.
     *
     * @param request authCode를 포함한 요청 바디
     * @return 기존 회원: accessToken + refreshToken / 신규 회원: tempToken + provider
     */
    @PostMapping("/token")
    @Operation(
        summary = "인가 코드 → JWT 교환",
        description = "OAuth 콜백이 발급한 일회용 authCode를 JWT로 교환한다. "
            + "기존 회원이면 accessToken과 refreshToken을 반환한다. "
            + "신규 회원이면 회원 가입 플로우에 사용할 tempToken과 provider를 반환한다. "
            + "authCode는 30초 TTL이며 1회만 사용 가능하다."
    )
    public ResponseEntity<AuthTokenResponse> exchangeToken(
        @RequestBody @Valid AuthCodeExchangeRequest request
    ) {
        AuthCodeResult result = authCodeService.consume(request.authCode())
            .orElseThrow(() -> new BadRequestException(ErrorCode.AUTH_CODE_INVALID));

        if (result.newUser()) {
            log.info("인가 코드 교환(신규 회원): provider={}", result.provider());
            return ResponseEntity.ok(
                AuthTokenResponse.newUser(result.tempToken(), result.provider())
            );
        }

        TokenPair tokens = tokenService.issueTokensByUserId(result.userId());
        log.info("인가 코드 교환(기존 회원): userId={}", result.userId());
        return ResponseEntity.ok(
            AuthTokenResponse.existingUser(tokens.accessToken(), tokens.refreshToken())
        );
    }
}
