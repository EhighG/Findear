package com.findear.main.security;

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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** local 프로필: 개발용 기능이 살아 있고, 토큰이 필요한 경로는 토큰을 요구하며, SSE 구독은 본인만 된다. */
@IntegrationTest
@ActiveProfiles("local")
@Transactional
class LocalProfileSecurityIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ApplicationContext context;
    @Autowired TestMembers testMembers;
    @Autowired StringRedisTemplate redis;

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
    @DisplayName("local에는 개발용 컨트롤러가 등록된다")
    void localControllersExist() {
        assertThat(context.getBeanNamesForType(LocalMemberController.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(LocalAlarmController.class)).hasSize(1);
    }

    @Test
    @DisplayName("전화번호 로그인: 토큰을 주고 refresh token이 refresh:{memberId} 키로 저장된다 (TTL 1439분)")
    void phoneLoginStoresRefreshToken() throws Exception {
        Member member = normalMember();

        mvc.perform(post("/members/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + member.getPhoneNumber() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result.accessToken").isNotEmpty());

        String key = "refresh:" + member.getId();
        assertThat(redis.opsForValue().get(key)).isNotBlank();
        Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(86_300L, 86_340L);
    }

    @Test
    @DisplayName("테스트 가입: 토큰 없이 가입할 수 있다")
    void registerWithoutToken() throws Exception {
        mvc.perform(post("/members").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"01099990000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("토큰 없이 /alarm/alarm-list -> 401 공통 실패 형식")
    void alarmListWithoutToken() throws Exception {
        mvc.perform(get("/alarm/alarm-list"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(JwtAuthenticationEntryPoint.MESSAGE));
    }

    @Test
    @DisplayName("잘못된 토큰 -> 401")
    void invalidToken() throws Exception {
        mvc.perform(get("/alarm/alarm-list").header("access-token", "not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("SSE 구독: 본인은 text/event-stream 시작, 다른 회원 ID는 403 JSON (Accept가 event-stream뿐이어도 JSON)")
    void subscribeOnlyOwn() throws Exception {
        Member me = normalMember();
        Member other = normalMember();
        String token = testMembers.loginToken(me);

        MvcResult started = mvc.perform(get("/alarm/subscribe/" + me.getId())
                        .header("access-token", token).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();
        assertThat(started.getResponse().getContentType()).startsWith("text/event-stream");

        // Accept가 없어도(curl 등) 구독된다
        MvcResult noAccept = mvc.perform(get("/alarm/subscribe/" + me.getId()).header("access-token", token))
                .andExpect(request().asyncStarted())
                .andReturn();
        assertThat(noAccept.getResponse().getContentType()).startsWith("text/event-stream");

        mvc.perform(get("/alarm/subscribe/" + other.getId())
                        .header("access-token", token).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403));

        mvc.perform(get("/alarm/subscribe/" + other.getId()).header("access-token", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("test-member-type 헤더만으로 인증된다 (local 개발 기능 유지), 모르는 타입은 401")
    void testMemberTypeHeaderWorksInLocal() throws Exception {
        Member sample = normalMember();

        mvc.perform(get("/members/" + sample.getId()).header("test-member-type", "normal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.memberId").value(sample.getId()));

        mvc.perform(get("/members/" + sample.getId()).header("test-member-type", "nobody"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("알림 테스트 발송(/alarm/send-fcm)은 local에서 토큰이 있으면 라우팅된다, 없으면 401")
    void sendFcmNeedsTokenButExistsInLocal() throws Exception {
        Member me = normalMember();
        String body = "{\"title\":\"t\",\"message\":\"m\",\"type\":\"message\"}";

        mvc.perform(post("/alarm/send-fcm/" + me.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());

        int status = mvc.perform(post("/alarm/send-fcm/" + me.getId()).contentType(MediaType.APPLICATION_JSON)
                        .header("access-token", testMembers.loginToken(me)).content(body))
                .andReturn().getResponse().getStatus();
        assertThat(status).isNotIn(401, 403, 404, 405);
    }

    @Test
    @DisplayName("기관 등록(PATCH /members/{id}/role): 본인은 200(MANAGER로 변경), 다른 회원 ID는 403 JSON")
    void changeToManagerOnlyForSelf() throws Exception {
        Member me = normalMember();
        Member other = normalMember();
        String token = testMembers.loginToken(me);
        String body = "{\"name\":\"테스트 유실물센터\",\"address\":\"서울 용산구\",\"xpos\":\"126.97\",\"ypos\":\"37.55\"}";

        mvc.perform(patch("/members/" + other.getId() + "/role").header("access-token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403));

        mvc.perform(patch("/members/" + me.getId() + "/role").header("access-token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("회원 검색(GET /members)은 local에 있고 토큰이 필요하다. keyword가 없으면 전체, 있으면 전화번호로 거른다")
    void findMembersExistsInLocal() throws Exception {
        assertThat(context.getBeansOfType(LocalMemberController.class)).hasSize(1);
        Member me = normalMember();
        String token = testMembers.loginToken(me);

        mvc.perform(get("/members"))
                .andExpect(status().isUnauthorized());

        // keyword 없음: NPE 없이 전체 (방금 만든 회원 포함)
        mvc.perform(get("/members").header("access-token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[?(@.memberId == " + me.getId() + ")]").isNotEmpty());

        mvc.perform(get("/members").param("keyword", me.getPhoneNumber()).header("access-token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[?(@.memberId == " + me.getId() + ")]").isNotEmpty());

        mvc.perform(get("/members").param("keyword", "no-such-phone").header("access-token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isEmpty());
    }
}
