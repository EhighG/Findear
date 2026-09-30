package com.findear.main.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 공개 경로 목록(PublicPaths) 고정. SecurityConfig(permitAll)와 JwtFilter(토큰 검증 제외)가 같은 목록을 쓰므로
 * 여기서 목록을 확인하면 둘 다 확인한 것이다. local·prod가 다른 항목은 각각 따로 확인한다.
 */
class JwtFilterExclusionTest {

    private final PublicPaths localPaths = new PublicPaths(true);
    private final PublicPaths prodPaths = new PublicPaths(false);

    private static HttpServletRequest request(String method, String uri) {
        return new MockHttpServletRequest(method, uri);
    }

    // ---- local·prod 공통 ----

    @DisplayName("공개 경로(guest로 통과) - local·prod 공통")
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
            "GET,/members/login",
            "POST,/members/duplicate",
            "GET,/actuator/health",
            "GET,/actuator/prometheus",
            "GET,/error",
            "GET,/acquisitions/lost112",
            "POST,/members/token/refresh",
            "GET,/favicon.ico",
            "GET,/members/after-login",
            "GET,/acquisitions/lost112/total-page",
            "GET,/acquisitions/returns/count",
            "GET,/location/search",
            "OPTIONS,/anything",
            "OPTIONS,/losts/1",
            "GET,/acquisitions",
            "GET,/losts"
    })
    void publicInBothProfiles(String method, String uri) {
        assertThat(localPaths.matches(request(method, uri))).isTrue();
        assertThat(prodPaths.matches(request(method, uri))).isTrue();
    }

    @DisplayName("토큰 필요 - local·prod 공통")
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
            "POST,/losts",
            "GET,/members/1",
            "GET,/members",
            "POST,/acquisitions",
            "GET,/acquisitions/1",
            "GET,/losts/1",
            "GET,/members/loginx",
            "GET,/scraps",
            "DELETE,/members",
            // 알림은 전부 토큰 필요 (/alarm/** 공개 제거)
            "GET,/alarm/subscribe/1",
            "GET,/alarm/alarm-list",
            "GET,/alarm/1",
            "POST,/alarm/send-data/1",
            "POST,/alarm/send-fcm/1",
            // 컨트롤러가 없어 목록에서 뺀 경로
            "GET,/members/emails/abc",
            "POST,/members/find-password",
            "GET,/assets/app.js"
    })
    void protectedInBothProfiles(String method, String uri) {
        assertThat(localPaths.matches(request(method, uri))).isFalse();
        assertThat(prodPaths.matches(request(method, uri))).isFalse();
    }

    // ---- local에서만 공개되는 개발용 경로 (K-06) ----

    @DisplayName("전화번호 로그인·테스트 가입은 local에서만 공개")
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
            "POST,/members/login",
            "POST,/members"
    })
    void localOnly(String method, String uri) {
        assertThat(localPaths.matches(request(method, uri))).isTrue();
        assertThat(prodPaths.matches(request(method, uri))).isFalse();
    }

    @DisplayName("메서드 조건이 있는 항목은 다른 메서드에서는 공개되지 않는다")
    @Test
    void methodSpecificExclusions() {
        for (PublicPaths paths : new PublicPaths[]{localPaths, prodPaths}) {
            assertThat(paths.matches(request("POST", "/losts"))).isFalse();
            assertThat(paths.matches(request("PUT", "/acquisitions"))).isFalse();
            assertThat(paths.matches(request("GET", "/members"))).isFalse();
            assertThat(paths.matches(request("PUT", "/members/login"))).isFalse();
        }
    }

    @DisplayName("끝 슬래시: Ant 매처와 같이 /losts/ 는 /losts 와 다른 경로로 본다")
    @Test
    void trailingSlash() {
        assertThat(localPaths.matches(request("GET", "/losts/"))).isFalse();
        assertThat(localPaths.matches(request("POST", "/members/login/"))).isFalse();
        assertThat(prodPaths.matches(request("GET", "/losts/"))).isFalse();
    }

    @DisplayName("SecurityConfig가 쓰는 RequestMatcher도 같은 목록이다")
    @Test
    void requestMatcherIsSameList() {
        var localMatcher = localPaths.asRequestMatcher();
        var prodMatcher = prodPaths.asRequestMatcher();
        assertThat(localMatcher.matches(request("POST", "/members/login"))).isTrue();
        assertThat(prodMatcher.matches(request("POST", "/members/login"))).isFalse();
        assertThat(prodMatcher.matches(request("GET", "/losts"))).isTrue();
        assertThat(prodMatcher.matches(request("GET", "/alarm/alarm-list"))).isFalse();
    }
}
