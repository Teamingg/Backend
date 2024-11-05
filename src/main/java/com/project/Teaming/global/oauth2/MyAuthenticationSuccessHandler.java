package com.project.Teaming.global.oauth2;

import com.project.Teaming.domain.user.entity.User;
import com.project.Teaming.domain.user.service.UserService;
import com.project.Teaming.global.jwt.GeneratedToken;
import com.project.Teaming.global.jwt.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class MyAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // OAuth2User로 캐스팅하여 인증된 사용자 정보를 가져온다.
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        // 사용자 이메일을 가져온다.
        String email = oAuth2User.getAttribute("email");
        log.info("email : " + email);
        // 서비스 제공 플랫폼(GOOGLE, KAKAO, NAVER)이 어디인지 가져온다.
        String provider = oAuth2User.getAttribute("provider");
        log.info("provider : " + provider);

        // CustomOAuth2UserService에서 셋팅한 로그인한 회원 존재 여부를 가져온다.
        boolean isExist = oAuth2User.getAttribute("exist");
        // OAuth2User로 부터 Role을 얻어온다.
        String role = oAuth2User.getAuthorities().stream().
                findFirst() // 첫번째 Role을 찾아온다.
                .orElseThrow(IllegalAccessError::new) // 존재하지 않을 시 예외를 던진다.
                .getAuthority(); // Role을 가져온다.

        log.info("role : " + role);

        // 회원이 존재할경우
        if (isExist) {

            User user = userService.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));;

            // 회원이 존재하면 jwt token 발행을 시작한다.
            GeneratedToken token = jwtUtil.generateToken(email, role);
            log.debug("MyAuthenticationSuccessHandler 권한 처리 후 role : " + role);
            log.info("accessToken={}", token.getAccessToken());

            // accessToken 을 HttpOnly 쿠키에 저장
            ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", token.getAccessToken())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None")   // 교차 출처 요청 허용
                    .path("/")
                    .maxAge(1800)  // 30분 유효
                    .build();
            response.addHeader("Set-Cookie", accessTokenCookie.toString());
            log.info("Set-Cookie Header: " + accessTokenCookie.toString());

            // 추가 정보 기입 여부에 따라 리다이렉트 경로 설정
            String targetUrl;
            if (user.getName() != null) {
                targetUrl = "http://localhost:3000/loginSuccess";
                log.info("추가 정보 기입 완료. 홈 화면으로 리다이렉트");
            } else {
                targetUrl = "http://localhost:3000/auth";
                log.info("추가 정보 미기입. 추가 정보 기입 페이지로 리다이렉트");
            }
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } else {
            // 로그인한 회원 필수 정보 저장
            userService.saveUser(email, provider, role);

            // jwt token 발행
            GeneratedToken token = jwtUtil.generateToken(email, role);
            log.info("accessToken={}", token.getAccessToken());

            // accessToken 을 HttpOnly 쿠키에 저장
            ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", token.getAccessToken())
                    .httpOnly(true)
                    .domain("myspringserver.shop")
                    .secure(true)
                    .sameSite("None")   // 교차 출처 요청 허용
                    .path("/")
                    .maxAge(1800)  // 30분 유효
                    .build();
            response.addHeader("Set-Cookie", accessTokenCookie.toString());
            log.info("Set-Cookie Header: " + accessTokenCookie.toString());

            // 회원가입 페이지로 리다이렉트
            String targetUrl = "http://localhost:3000/auth";
            log.info("첫 로그인 성공. 추가 정보 기입 페이지로 리다이렉트");
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
    }
}
