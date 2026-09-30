package com.findear.main.security;

import com.findear.main.Alarm.controller.AlarmController;
import com.findear.main.Alarm.controller.LocalAlarmController;
import com.findear.main.member.command.controller.LocalMemberController;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import com.findear.main.support.IntegrationTest;
import com.findear.main.support.TestMembers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** prod 프로필: 개발용 기능(전화번호 로그인·테스트 가입·알림 테스트 발송·test-member-type 헤더)이 열리지 않는다 (D-26). */
@IntegrationTest
@ActiveProfiles("prod")
@Transactional
class ProdProfileSecurityIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ApplicationContext context;
    @Autowired TestMembers testMembers;

    private final List<Member> loggedIn = new ArrayList<>();

    @AfterEach
    void cleanRedis() {
        loggedIn.forEach(testMembers::logout);
    }

    private Member normalMember() {
        Member member = testMembers.newMember(Role.NORMAL);
        loggedIn.add(member);
        return member;
    }

    @Test
    @DisplayName("prod에는 개발용 컨트롤러 빈이 없고, 일반 알림 컨트롤러는 있다")
    void localControllersAbsent() {
        assertThat(context.getBeanNamesForType(LocalMemberController.class)).isEmpty();
        assertThat(context.getBeanNamesForType(LocalAlarmController.class)).isEmpty();
        assertThat(context.getBeanNamesForType(AlarmController.class)).hasSize(1);
    }

    @Test
    @DisplayName("토큰 없이 POST /members/login, POST /members -> 공개 경로가 아니므로 401 JSON")
    void devEndpointsAreNotPublic() throws Exception {
        mvc.perform(post("/members/login").contentType(MediaType.APPLICATION_JSON).content("{\"phoneNumber\":\"01011112222\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(post("/members").contentType(MediaType.APPLICATION_JSON).content("{\"phoneNumber\":\"01011112222\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("토큰이 있어도 개발용 핸들러가 없다: 로그인은 405(같은 경로의 GET 콜백만 있음), 가입·회원 검색·send-*는 404")
    void devHandlersAreGoneEvenWithToken() throws Exception {
        Member member = normalMember();
        String token = testMembers.loginToken(member);
        String phone = "{\"phoneNumber\":\"01011112222\"}";

        mvc.perform(post("/members/login").header("access-token", token).contentType(MediaType.APPLICATION_JSON).content(phone))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
        mvc.perform(post("/members").header("access-token", token).contentType(MediaType.APPLICATION_JSON).content(phone))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(get("/members").header("access-token", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(post("/alarm/send-fcm/" + member.getId()).header("access-token", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"t\",\"message\":\"m\",\"type\":\"message\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(post("/alarm/send-data/" + member.getId()).header("access-token", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("test-member-type 헤더는 prod에서 무시된다: 헤더만으로는 401, 정상 토큰은 200")
    void testMemberTypeHeaderIsIgnored() throws Exception {
        Member member = normalMember();

        mvc.perform(get("/members/" + member.getId()).header("test-member-type", "normal"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mvc.perform(get("/members/" + member.getId())
                        .header("test-member-type", "normal").header("access-token", testMembers.loginToken(member)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("prod에서도 소셜 로그인 콜백(GET /members/login)은 공개 경로다")
    void socialLoginCallbackStaysPublic() throws Exception {
        mvc.perform(get("/members/login").param("code", "abc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("prod에서도 SSE 구독은 토큰이 필요하다 (/alarm/** 공개 제거)")
    void subscribeNeedsToken() throws Exception {
        mvc.perform(get("/alarm/subscribe/1").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized());
    }
}
