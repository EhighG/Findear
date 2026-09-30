package com.findear.main.security;

import com.findear.main.member.common.domain.Role;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final String ACCESS_TOKEN = "access-token";
    private final String REFRESH_TOKEN = "refresh-token";
    private final String TESTHEADER = "test-member-type";
    private final JwtAuthenticationProvider authenticationProvider;

    private final Authentication authenticationForGuests;
    private List<RequestMatcher> exclusiveRequestMatchers;

    public JwtFilter(JwtAuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
        authenticationForGuests = JwtAuthenticationToken.authenticated("guest", "guestCredntial",
                null);
        setExclusiveRequestMatchers();
    }

    private void setExclusiveRequestMatchers() {
        exclusiveRequestMatchers = new ArrayList<>();
        PathPatternRequestMatcher.Builder pathMatcher = PathPatternRequestMatcher.withDefaults();
        // only pattern
        List<String> exclusiveUris = Arrays.asList("/members/login", "/members/emails/**", "/members/find-password", "/members/duplicate", "/actuator/**", "/error", "/assets/**"
        ,"/acquisitions/lost112", "/members/token/refresh", "/favicon.ico", "/members/after-login", "/acquisitions/lost112/total-page",
                "/acquisitions/returns/count", "/location/**");
        for (String uri : exclusiveUris) {
            exclusiveRequestMatchers.add(pathMatcher.matcher(uri));
        }
        // pattern, http method
        exclusiveRequestMatchers.add(pathMatcher.matcher(HttpMethod.POST, "/members"));
        exclusiveRequestMatchers.add(pathMatcher.matcher(HttpMethod.OPTIONS, "/**"));
        exclusiveRequestMatchers.add(pathMatcher.matcher(HttpMethod.GET, "/acquisitions"));
        exclusiveRequestMatchers.add(pathMatcher.matcher(HttpMethod.GET, "/losts"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        Authentication authentication;
        if (isExclusiveRequest(request)) {
            authentication = authenticationForGuests;
        } else {
            try {
                authentication = attemptAuthenticate(request);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                throw new AuthenticationServiceException("401 unauthorized");
            }
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    // 토큰 검증을 건너뛰고 guest로 통과시킬 요청인지 (테스트에서 직접 호출)
    boolean isExclusiveRequest(HttpServletRequest request) {
        return exclusiveRequestMatchers.stream().anyMatch(matcher -> matcher.matches(request));
    }

    private Authentication attemptAuthenticate(HttpServletRequest request) {
        String accessToken = request.getHeader(ACCESS_TOKEN);

        if (!StringUtils.isEmpty(accessToken)) {
            return authenticationProvider.authenticateAccessToken(accessToken);
        } else if (isTestRequest(request)){
            String memberType = request.getHeader(TESTHEADER); // Normal or Manager
            return authenticationProvider.getSampleAuthentication(memberType);
        } else {
            throw new IllegalArgumentException("AccessToken이 필요합니다.");
        }
    }

    private boolean isTestRequest(HttpServletRequest request) {
        return request.getHeader(TESTHEADER) != null;
    }
}
