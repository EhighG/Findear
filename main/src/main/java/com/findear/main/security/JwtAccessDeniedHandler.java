package com.findear.main.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 인증은 됐지만 권한이 없는 요청: 403 + 공통 실패 형식 JSON. */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    static final String MESSAGE = "접근 권한이 없습니다.";

    private final ObjectMapper objectMapper;

    public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        SecurityErrorWriter.write(response, objectMapper, HttpStatus.FORBIDDEN, MESSAGE);
    }
}
