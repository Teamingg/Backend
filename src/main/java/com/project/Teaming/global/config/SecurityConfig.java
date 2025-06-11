package com.project.Teaming.global.config;

import com.project.Teaming.global.jwt.filter.JwtAuthFilter;
import com.project.Teaming.global.jwt.filter.JwtExceptionFilter;
import com.project.Teaming.global.oauth2.CustomOAuth2UserService;
import com.project.Teaming.global.oauth2.MyAuthenticationFailureHandler;
import com.project.Teaming.global.oauth2.MyAuthenticationSuccessHandler;
import com.project.Teaming.global.properties.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final MyAuthenticationSuccessHandler oAuth2LoginSuccessHandler;
    private final MyAuthenticationFailureHandler oAuth2LoginFailureHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtAuthFilter jwtAuthFilter;
    private final JwtExceptionFilter jwtExceptionFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CorsFilter corsFilter;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic(httpBasic -> httpBasic.disable()); // HTTP 기본 인증 비활성화

        http
                .csrf(csrf -> csrf.disable()); // CSRF 보호 기능 비활성화

        http
                .formLogin(AbstractHttpConfigurer::disable);

        http
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)); // 세션관리 정책을 STATELESS로 설정

        http
                .exceptionHandling(exception -> exception.authenticationEntryPoint(customAuthenticationEntryPoint));

        http
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/token/**").permitAll() // 토큰 발급 경로 허용
                        .requestMatchers("/", "/css/**", "/images/**", "/js/**", "/favicon.ico", "/h2-console/**", "/swagger-ui/**", "/v3/api-docs/**", "/login",
                                "/project/teams/{teamId}/status", "/project/posts",
                                "/mentoring/posts","/mentoring/teams/{teamId}","/mentoring/posts/{postId}",
                                "/users/{userId}/project","/users/{userId}/mentoring/teams","/health").permitAll() // 특정 경로 허용
                        .requestMatchers(HttpMethod.GET, "/project/teams/{teamId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/project/posts/{postId}").permitAll()
                        .anyRequest().authenticated() // 그 외 모든 요청 인증 필요
                );

        http
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)) // 사용자 서비스 설정
                        .failureHandler(oAuth2LoginFailureHandler) // 로그인 실패 핸들러
                        .successHandler(oAuth2LoginSuccessHandler) // 로그인 성공 핸들러
                );

        // CORS 필터 추가
        http
                .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class);

        // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
        return http
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtExceptionFilter, JwtAuthFilter.class)
                .build();
    }
}
