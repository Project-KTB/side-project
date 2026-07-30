package org.ktb.sideproject.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.auth.JwtAuthenticationFilter;
import org.ktb.sideproject.error.ErrorCode;
import org.ktb.sideproject.error.ErrorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${app.image-upload.enabled:true}")
    private boolean imageUploadEnabled;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeErrorResponse(response, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeErrorResponse(response, ErrorCode.ACCESS_DENIED))
                )
                .authorizeHttpRequests(auth -> {
                    auth
                            .requestMatchers(HttpMethod.POST, "/auth").permitAll()
                            .requestMatchers(HttpMethod.POST, "/users").permitAll()
                            .requestMatchers(HttpMethod.POST, "/auth/reissue").permitAll()
                            .requestMatchers(HttpMethod.GET, "/health").permitAll();

                    if (imageUploadEnabled) {
                        auth
                                .requestMatchers(HttpMethod.POST, "/images/posts").authenticated()
                                .requestMatchers(HttpMethod.POST, "/images/posts/presigned-url").authenticated()
                                .requestMatchers(HttpMethod.POST, "/images/profile").authenticated()
                                .requestMatchers(HttpMethod.POST, "/images/profile/presigned-url").authenticated()
                                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll();
                    } else {
                        auth
                                .requestMatchers(HttpMethod.POST, "/images/posts").denyAll()
                                .requestMatchers(HttpMethod.POST, "/images/posts/presigned-url").denyAll()
                                .requestMatchers(HttpMethod.POST, "/images/profile").denyAll()
                                .requestMatchers(HttpMethod.POST, "/images/profile/presigned-url").denyAll()
                                .requestMatchers(HttpMethod.GET, "/uploads/**").denyAll();
                    }

                    auth
                            .requestMatchers(HttpMethod.GET, "/users/email/check").permitAll()
                            .requestMatchers(HttpMethod.GET, "/users/nickname/check").permitAll()
                            .requestMatchers(HttpMethod.GET, "/posts").permitAll()
                            .requestMatchers(HttpMethod.GET, "/posts/{postId}").permitAll()
                            .requestMatchers(HttpMethod.GET, "/comments/{commentId}").permitAll()
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 어떤 출처의 요청을 허용할지
        configuration.setAllowedOrigins(allowedOrigins);

        // 프론트에서 사용할 허용할 메소드
        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PATCH",
                "PUT",
                "DELETE",
                "OPTIONS" // preflight용
        ));

        // 프론트가 요청에 실어 보낼 수 있는 헤더를 허용
        configuration.setAllowedHeaders(List.of(
                "Authorization",    // 토큰
                "Content-Type"      // JSON 전송용
        ));

        // 나중에 쿠키 할떄 설정해야함
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.from(errorCode));
    }
}
