package com.findear.main.security;

import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import com.findear.main.member.query.service.MemberQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** JwtFilter의 test-member-type 헤더 인증(K-12)은 local에서만 동작하고, 샘플 회원 조회는 기동 때 나가지 않는다. */
class JwtFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static MockHttpServletRequest requestWithHeader(String name, String value) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/members/1");
        request.addHeader(name, value);
        return request;
    }

    @Test
    @DisplayName("prod: test-member-type 헤더는 무시되고 토큰이 없으면 401 예외, 샘플 인증은 호출되지 않는다")
    void prodIgnoresTestHeader() {
        JwtAuthenticationProvider provider = mock(JwtAuthenticationProvider.class);
        JwtFilter filter = new JwtFilter(provider, new PublicPaths(false), false);

        assertThatThrownBy(() -> filter.doFilter(requestWithHeader("test-member-type", "normal"),
                new MockHttpServletResponse(), new MockFilterChain()))
                .isInstanceOf(AuthenticationServiceException.class);
        verify(provider, never()).getSampleAuthentication(anyString());
    }

    @Test
    @DisplayName("local: test-member-type 헤더로 샘플 회원 인증")
    void localUsesTestHeader() throws Exception {
        JwtAuthenticationProvider provider = mock(JwtAuthenticationProvider.class);
        Authentication sample = JwtAuthenticationToken.authenticated(1L, "sampleAccessToken", List.of());
        when(provider.getSampleAuthentication("normal")).thenReturn(sample);
        JwtFilter filter = new JwtFilter(provider, new PublicPaths(true), true);

        filter.doFilter(requestWithHeader("test-member-type", "normal"), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(sample);
    }

    @Test
    @DisplayName("access-token이 있으면 프로필과 관계없이 토큰 인증이 우선한다")
    void tokenTakesPrecedence() throws Exception {
        JwtAuthenticationProvider provider = mock(JwtAuthenticationProvider.class);
        Authentication authenticated = JwtAuthenticationToken.authenticated(5L, "t", List.of());
        when(provider.authenticateAccessToken("t")).thenReturn(authenticated);
        JwtFilter filter = new JwtFilter(provider, new PublicPaths(true), true);

        MockHttpServletRequest request = requestWithHeader("access-token", "t");
        request.addHeader("test-member-type", "normal");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authenticated);
        verify(provider, never()).getSampleAuthentication(anyString());
    }

    @Test
    @DisplayName("토큰 검증에서 IllegalArgumentException이 나오면 401 예외로 바꾼다")
    void illegalArgumentBecomesUnauthorized() {
        JwtAuthenticationProvider provider = mock(JwtAuthenticationProvider.class);
        when(provider.authenticateAccessToken(any())).thenThrow(new IllegalArgumentException("회원정보가 존재하지 않습니다."));
        JwtFilter filter = new JwtFilter(provider, new PublicPaths(false), false);

        assertThatThrownBy(() -> filter.doFilter(requestWithHeader("access-token", "t"),
                new MockHttpServletResponse(), new MockFilterChain()))
                .isInstanceOf(AuthenticationServiceException.class);
    }

    @Test
    @DisplayName("공개 경로는 토큰 없이 guest로 통과한다")
    void publicPathPassesAsGuest() throws Exception {
        JwtAuthenticationProvider provider = mock(JwtAuthenticationProvider.class);
        JwtFilter filter = new JwtFilter(provider, new PublicPaths(false), false);

        filter.doFilter(new MockHttpServletRequest("GET", "/losts"), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("guest");
        verifyNoInteractions(provider);
    }

    // ---- JwtAuthenticationProvider ----

    @Test
    @DisplayName("provider는 생성될 때 DB를 조회하지 않는다 (prod·local 모두)")
    void providerDoesNotQueryOnConstruction() {
        MemberQueryService memberQueryService = mock(MemberQueryService.class);

        new JwtAuthenticationProvider(memberQueryService, false);
        new JwtAuthenticationProvider(memberQueryService, true);

        verifyNoInteractions(memberQueryService);
    }

    @Test
    @DisplayName("prod provider는 샘플 인증 요청을 거부하고 DB를 조회하지 않는다")
    void prodProviderRejectsSample() {
        MemberQueryService memberQueryService = mock(MemberQueryService.class);
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(memberQueryService, false);

        assertThatThrownBy(() -> provider.getSampleAuthentication("normal"))
                .isInstanceOf(AuthenticationServiceException.class);
        verifyNoInteractions(memberQueryService);
    }

    @Test
    @DisplayName("local provider는 요청 때 역할별 첫 회원을 조회하고, 없는 타입은 401 예외")
    void localProviderLooksUpSample() {
        MemberQueryService memberQueryService = mock(MemberQueryService.class);
        Member normal = Member.builder().id(3L).naverUid("u").phoneNumber("010").role(Role.NORMAL).build();
        when(memberQueryService.findFirstMembersPerGroup()).thenReturn(List.of(normal));
        when(memberQueryService.internalFindById(3L)).thenReturn(normal);
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(memberQueryService, true);

        Authentication authentication = provider.getSampleAuthentication("Normal");
        assertThat(authentication.getPrincipal()).isEqualTo(3L);
        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_NORMAL");

        assertThatThrownBy(() -> provider.getSampleAuthentication("manager"))
                .isInstanceOf(AuthenticationServiceException.class);
    }
}
