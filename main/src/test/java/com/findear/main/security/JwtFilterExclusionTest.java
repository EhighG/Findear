package com.findear.main.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtFilter 인증 제외 목록 매칭 (PathPatternRequestMatcher 전환 회귀 확인).
 * 제외 목록 자체의 변경은 R-27 범위라 여기서는 현재 목록의 동작만 고정한다.
 */
class JwtFilterExclusionTest {

    private JwtFilter jwtFilter;

    @BeforeEach
    void setup() {
        jwtFilter = new JwtFilter(Mockito.mock(JwtAuthenticationProvider.class));
    }

    private boolean excluded(String method, String uri) {
        return jwtFilter.isExclusiveRequest(new MockHttpServletRequest(method, uri));
    }

    @DisplayName("제외 경로(guest로 통과)")
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
            "POST,/members/login",
            "GET,/members/emails/abc",
            "POST,/members/find-password",
            "GET,/members/duplicate",
            "GET,/actuator/health",
            "GET,/actuator/prometheus",
            "GET,/error",
            "GET,/assets/app.js",
            "GET,/acquisitions/lost112",
            "POST,/members/token/refresh",
            "GET,/favicon.ico",
            "GET,/members/after-login",
            "GET,/acquisitions/lost112/total-page",
            "GET,/acquisitions/returns/count",
            "GET,/location/search",
            "POST,/members",
            "OPTIONS,/anything",
            "OPTIONS,/losts/1",
            "GET,/acquisitions",
            "GET,/losts"
    })
    void excludedRequests(String method, String uri) {
        assertThat(excluded(method, uri)).isTrue();
    }

    @DisplayName("제외 아님(토큰 필요)")
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
            "DELETE,/members"
    })
    void protectedRequests(String method, String uri) {
        assertThat(excluded(method, uri)).isFalse();
    }

    @DisplayName("메서드 조건이 있는 항목은 다른 메서드에서는 제외되지 않는다")
    @Test
    void methodSpecificExclusions() {
        assertThat(excluded("POST", "/losts")).isFalse();
        assertThat(excluded("PUT", "/acquisitions")).isFalse();
        assertThat(excluded("GET", "/members")).isFalse();
    }

    @DisplayName("끝 슬래시: Ant 매처와 같이 /losts/ 는 /losts 와 다른 경로로 본다")
    @Test
    void trailingSlash() {
        assertThat(excluded("GET", "/losts/")).isFalse();
        assertThat(excluded("POST", "/members/login/")).isFalse();
    }
}
