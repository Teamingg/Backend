package com.project.Teaming.global.config;

import com.project.Teaming.global.jwt.filter.JwtAuthFilter;
import com.project.Teaming.global.jwt.filter.JwtExceptionFilter;
import com.project.Teaming.global.oauth2.CustomOAuth2UserService;
import com.project.Teaming.global.oauth2.MyAuthenticationFailureHandler;
import com.project.Teaming.global.oauth2.MyAuthenticationSuccessHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final MyAuthenticationSuccessHandler oAuth2LoginSuccessHandler;
    private final MyAuthenticationFailureHandler oAuth2LoginFailureHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtAuthFilter jwtAuthFilter;
    private final JwtExceptionFilter jwtExceptionFilter;


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
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/token/**").permitAll() // 토큰 발급 경로 허용
                        .requestMatchers("/", "/css/**", "/images/**", "/js/**", "/favicon.ico", "/h2-console/**", "/user", "/swagger-ui/**", "/v3/api-docs/**",
                                "/project/team/{team_id}").permitAll() // 특정 경로 허용
                        .requestMatchers("/user/portfolio/save", "/user/portfolio", "/project/team", "/project/team/{team_id}/edit",
                                "project/team/{team_id}/delete").hasRole("USER")
                        .anyRequest().authenticated() // 그 외 모든 요청 인증 필요
                );

        http
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)) // 사용자 서비스 설정
                        .failureHandler(oAuth2LoginFailureHandler) // 로그인 실패 핸들러
                        .successHandler(oAuth2LoginSuccessHandler) // 로그인 성공 핸들러
                );

        // CORS 설정 추가
        http.cors(cors -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowCredentials(true); // 쿠키 전송 허용
            config.addAllowedOrigin("https://myspringserver.shop");
            config.addAllowedOrigin("https://localhost:3000");
            config.addAllowedOrigin("http://localhost:3000");
            config.addAllowedOrigin("http://localhost:8080"); // 도메인 모두 허용
            config.addAllowedHeader("*");
            config.addAllowedMethod("*");
            config.setExposedHeaders(List.of("Authorization", "accessToken")); // 클라이언트에서 접근할 수 있도록 노출할 헤더

            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", config);
            CorsFilter corsFilter = new CorsFilter(source);
            http.addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class);
        });

        // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
        return http
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtExceptionFilter, JwtAuthFilter.class)
                .build();
    }
}
