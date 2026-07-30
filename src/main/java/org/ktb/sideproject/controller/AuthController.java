package org.ktb.sideproject.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ktb.sideproject.dto.auth.LoginResult;
import org.ktb.sideproject.dto.auth.ReissueResult;
import org.ktb.sideproject.dto.auth.req.LoginRequest;
import org.ktb.sideproject.dto.auth.res.LoginResponse;
import org.ktb.sideproject.dto.auth.res.ReissueResponse;
import org.ktb.sideproject.error.CustomException;
import org.ktb.sideproject.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final AuthService authService;

    @Value("${jwt.refresh-token-exp-seconds:1209600}")
    private long refreshTokenExpSeconds;

    @Value("${app.auth.refresh-cookie.secure:false}")
    private boolean refreshCookieSecure;

    @Value("${app.auth.refresh-cookie.same-site:Lax}")
    private String refreshCookieSameSite;

    @Value("${app.auth.refresh-cookie.domain:}")
    private String refreshCookieDomain;

    @PostMapping
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        LoginResult loginResult = authService.login(loginRequest);
        setRefreshCookie(loginResult.refreshToken(), response);

        return ResponseEntity.ok(loginResult.loginResponse());
    }

    @DeleteMapping
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Long userId,
            HttpServletResponse response,
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {
        authService.logout(refreshToken, userId);
        deleteRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reissue")
    public ResponseEntity<ReissueResponse> reissue(
            HttpServletResponse response,
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {
        log.info("[TOKEN_REISSUE] 토큰 재발급 요청 수신. hasRefreshToken={}", refreshToken != null);

        try {
            ReissueResult reissueResult = authService.reissueToken(refreshToken);
            setRefreshCookie(reissueResult.refreshToken(), response);

            log.info("[TOKEN_REISSUE] 토큰 재발급 응답 완료");

            return ResponseEntity.ok(new ReissueResponse(reissueResult.accessToken()));
        } catch (CustomException e) {
            deleteRefreshCookie(response);
            throw e;
        }
    }

    private void setRefreshCookie(String refreshToken, HttpServletResponse response) {
        addRefreshCookie(response, refreshToken, Duration.ofSeconds(refreshTokenExpSeconds));
    }

    private void deleteRefreshCookie(HttpServletResponse response) {
        addRefreshCookie(response, "", Duration.ZERO);
    }

    private void addRefreshCookie(HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .path("/")
                .maxAge(maxAge);

        if (StringUtils.hasText(refreshCookieSameSite)) {
            cookieBuilder.sameSite(refreshCookieSameSite.trim());
        }

        if (StringUtils.hasText(refreshCookieDomain)) {
            cookieBuilder.domain(refreshCookieDomain.trim());
        }

        response.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
    }
}
