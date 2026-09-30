package com.findear.main.security;

import com.findear.main.common.profile.LocalProfile;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 토큰 없이 접근할 수 있는 경로 목록. 이 클래스가 유일한 정의이고, {@link SecurityConfig}(permitAll)와
 * {@link JwtFilter}(토큰 검증 제외)가 같은 매처를 쓴다. 경로를 바꿀 때는 여기만 고친다.
 * local 프로필에서만 공개되는 개발용 경로(전화번호 로그인, 테스트 가입)는 prod에서 목록에 들어가지 않는다 (D-26).
 */
@Component
public class PublicPaths {

    private final List<RequestMatcher> matchers = new ArrayList<>();

    @Autowired
    public PublicPaths(Environment environment) {
        this(LocalProfile.isActive(environment));
    }

    PublicPaths(boolean local) {
        PathPatternRequestMatcher.Builder path = PathPatternRequestMatcher.withDefaults();

        // 메서드 무관
        for (String pattern : List.of(
                "/members/duplicate", "/members/token/refresh", "/members/after-login",
                "/acquisitions/lost112", "/acquisitions/lost112/total-page", "/acquisitions/returns/count",
                "/location/**",
                "/actuator/**", "/error", "/favicon.ico")) {
            matchers.add(path.matcher(pattern));
        }

        // 메서드 지정
        matchers.add(path.matcher(HttpMethod.GET, "/members/login")); // 소셜 로그인 리다이렉트(인가코드 수신)
        matchers.add(path.matcher(HttpMethod.GET, "/acquisitions"));
        matchers.add(path.matcher(HttpMethod.GET, "/losts"));
        matchers.add(path.matcher(HttpMethod.OPTIONS, "/**"));

        if (local) {
            matchers.add(path.matcher(HttpMethod.POST, "/members/login")); // 전화번호 로그인 (K-06)
            matchers.add(path.matcher(HttpMethod.POST, "/members")); // 테스트 가입 (K-06)
        }
    }

    public boolean matches(HttpServletRequest request) {
        return matchers.stream().anyMatch(matcher -> matcher.matches(request));
    }

    public RequestMatcher asRequestMatcher() {
        return new OrRequestMatcher(List.copyOf(matchers));
    }
}
