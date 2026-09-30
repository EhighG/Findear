package com.findear.main.security;

import com.findear.main.common.profile.LocalProfile;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final String ACCESS_TOKEN = "access-token";
    private static final String TEST_HEADER = "test-member-type";

    private final JwtAuthenticationProvider authenticationProvider;
    private final PublicPaths publicPaths;
    // test-member-type 헤더 인증(K-12)은 local 프로필에서만 동작한다. prod에서는 헤더를 무시하고 일반 토큰 인증만 한다 (D-26)
    private final boolean testHeaderEnabled;

    private final Authentication authenticationForGuests;

    @Autowired
    public JwtFilter(JwtAuthenticationProvider authenticationProvider, PublicPaths publicPaths, Environment environment) {
        this(authenticationProvider, publicPaths, LocalProfile.isActive(environment));
    }

    JwtFilter(JwtAuthenticationProvider authenticationProvider, PublicPaths publicPaths, boolean testHeaderEnabled) {
        this.authenticationProvider = authenticationProvider;
        this.publicPaths = publicPaths;
        this.testHeaderEnabled = testHeaderEnabled;
        authenticationForGuests = JwtAuthenticationToken.authenticated("guest", "guestCredntial",
                null);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication;
        if (publicPaths.matches(request)) {
            authentication = authenticationForGuests;
        } else {
            try {
                authentication = attemptAuthenticate(request);
            } catch (IllegalArgumentException | JwtException e) {
                // 토큰 값이 로그에 남지 않도록 예외 종류만 기록한다
                log.debug("인증 실패: {}", e.getClass().getSimpleName());
                throw new AuthenticationServiceException("401 unauthorized");
            }
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private Authentication attemptAuthenticate(HttpServletRequest request) {
        String accessToken = request.getHeader(ACCESS_TOKEN);

        if (StringUtils.hasText(accessToken)) {
            return authenticationProvider.authenticateAccessToken(accessToken);
        } else if (testHeaderEnabled && request.getHeader(TEST_HEADER) != null) {
            String memberType = request.getHeader(TEST_HEADER); // normal or manager
            return authenticationProvider.getSampleAuthentication(memberType);
        } else {
            throw new IllegalArgumentException("AccessToken이 필요합니다.");
        }
    }
}
