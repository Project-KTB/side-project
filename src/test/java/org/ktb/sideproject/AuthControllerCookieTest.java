package org.ktb.sideproject;

import org.junit.jupiter.api.Test;
import org.ktb.sideproject.controller.AuthController;
import org.ktb.sideproject.dto.auth.LoginResult;
import org.ktb.sideproject.dto.auth.req.LoginRequest;
import org.ktb.sideproject.dto.auth.res.LoginResponse;
import org.ktb.sideproject.dto.user.res.UserInfo;
import org.ktb.sideproject.error.CustomException;
import org.ktb.sideproject.error.ErrorCode;
import org.ktb.sideproject.error.GlobalExceptionHandler;
import org.ktb.sideproject.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerCookieTest {

    @Test
    void loginSetsSecureHttpOnlySameSiteRefreshCookie() throws Exception {
        AuthService authService = mock(AuthService.class);
        when(authService.login(any(LoginRequest.class))).thenReturn(new LoginResult(
                new LoginResponse(
                        "access-token",
                        new UserInfo(1L, "user@example.com", "nickname", null)
                ),
                "refresh-token"
        ));

        AuthController controller = new AuthController(authService);
        ReflectionTestUtils.setField(controller, "refreshTokenExpSeconds", 1209600L);
        ReflectionTestUtils.setField(controller, "refreshCookieSecure", true);
        ReflectionTestUtils.setField(controller, "refreshCookieSameSite", "Lax");
        ReflectionTestUtils.setField(controller, "refreshCookieDomain", "");
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();

        mockMvc.perform(post("/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"Password1!\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        containsString("refreshToken=refresh-token"),
                        containsString("Max-Age=1209600"),
                        containsString("Path=/"),
                        containsString("Secure"),
                        containsString("HttpOnly"),
                        containsString("SameSite=Lax")
                )));
    }

    @Test
    void reissueFailureClearsRefreshCookie() throws Exception {
        AuthService authService = mock(AuthService.class);
        when(authService.reissueToken("stale-refresh-token"))
                .thenThrow(new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        AuthController controller = new AuthController(authService);
        ReflectionTestUtils.setField(controller, "refreshTokenExpSeconds", 1209600L);
        ReflectionTestUtils.setField(controller, "refreshCookieSecure", true);
        ReflectionTestUtils.setField(controller, "refreshCookieSameSite", "Lax");
        ReflectionTestUtils.setField(controller, "refreshCookieDomain", "");
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();

        mockMvc.perform(post("/auth/reissue")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "stale-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        containsString("refreshToken="),
                        containsString("Max-Age=0"),
                        containsString("Path=/"),
                        containsString("Secure"),
                        containsString("HttpOnly"),
                        containsString("SameSite=Lax")
                )));
    }

}
