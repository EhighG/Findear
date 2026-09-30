package com.findear.main.common.exception;

import com.findear.main.Alarm.common.exception.AlarmException;
import com.findear.main.message.common.exception.MessageException;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 공통 예외 응답: 본문 status는 항상 실제 HTTP 상태와 같고, 형식은 {"status", "message"} JSON이다. */
class CommonControllerAdviceTest {

    @RestController
    static class ThrowingController {
        @GetMapping("/boom/{kind}")
        public String boom(@PathVariable String kind) {
            switch (kind) {
                case "authn": throw new AuthenticationServiceException("잘못된 refreshToken");
                case "authz": throw new AuthorizationServiceException("권한이 없습니다.");
                case "expired": throw new ExpiredJwtException(null, null, "JWT expired at 2026-01-01T00:00:00Z");
                case "illegal": throw new IllegalArgumentException("해당 게시글이 없습니다.");
                case "username": throw new UsernameNotFoundException("회원정보가 존재하지 않습니다.");
                case "message": throw new MessageException("존재하는 메시지 방이 없습니다.");
                case "alarm": throw new AlarmException("해당 유저가 존재하지 않습니다.");
                default: throw new IllegalStateException("내부 사정: secret-detail");
            }
        }

        @PostMapping("/only-post")
        public String onlyPost() {
            return "ok";
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new CommonControllerAdvice(), new ExternalServiceExceptionAdvice())
                // jackson-dataformat-xml이 있어 Accept가 없으면 XML이 먼저 선택되므로 WebConfig와 같이 JSON을 기본으로 한다
                .setContentNegotiationManager(new ContentNegotiationManager(
                        new HeaderContentNegotiationStrategy(), new FixedContentNegotiationStrategy(MediaType.APPLICATION_JSON)))
                .build();
    }

    private void expect(String path, int status, String message) throws Exception {
        mvc.perform(get(path))
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    @DisplayName("401: Spring Security AuthenticationException")
    void authentication() throws Exception {
        expect("/boom/authn", 401, "잘못된 refreshToken");
    }

    @Test
    @DisplayName("401: 만료된 JWT (이전에는 HTTP 400에 본문 401)")
    void expiredJwt() throws Exception {
        expect("/boom/expired", 401, "토큰이 만료되었습니다.");
    }

    @Test
    @DisplayName("403: AuthorizationServiceException")
    void authorization() throws Exception {
        expect("/boom/authz", 403, "권한이 없습니다.");
    }

    @Test
    @DisplayName("400: 잘못된 요청 계열 예외는 메시지를 그대로")
    void badRequests() throws Exception {
        expect("/boom/illegal", 400, "해당 게시글이 없습니다.");
        expect("/boom/username", 400, "회원정보가 존재하지 않습니다.");
        expect("/boom/message", 400, "존재하는 메시지 방이 없습니다.");
        expect("/boom/alarm", 400, "해당 유저가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("500: 처리되지 않은 예외는 본문 status도 500, 내부 메시지는 노출하지 않는다")
    void unexpected() throws Exception {
        expect("/boom/other", 500, "서버 내부 오류가 발생했습니다.");
    }

    @Test
    @DisplayName("404·405: 경로가 없거나 메서드가 다르면 실제 상태와 같은 본문")
    void noHandlerAndWrongMethod() throws Exception {
        mvc.perform(get("/nothing/here"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(get("/only-post"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(405));
        mvc.perform(post("/only-post").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isOk());
    }
}
