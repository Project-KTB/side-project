package org.ktb.sideproject.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.dto.auth.LoginResult;
import org.ktb.sideproject.dto.auth.ReissueResult;
import org.ktb.sideproject.dto.auth.req.LoginRequest;
import org.ktb.sideproject.dto.auth.res.LoginResponse;
import org.ktb.sideproject.dto.auth.res.ReissueResponse;
import org.ktb.sideproject.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        LoginResult loginResult =  authService.login(loginRequest);
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
        ReissueResult reissueResult = authService.reissueToken(refreshToken);
        setRefreshCookie(reissueResult.refreshToken(), response);

        return ResponseEntity.ok(new ReissueResponse(reissueResult.accessToken()));
    }



    private void setRefreshCookie(String refreshToken, HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 14);
        response.addCookie(cookie);
    }

    private void deleteRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
