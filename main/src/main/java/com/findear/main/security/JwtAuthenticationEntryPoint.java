package com.findear.main.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 인증되지 않은 요청: 401 + 공통 실패 형식 JSON. */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    static final String MESSAGE = "인증이 필요합니다.";

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        SecurityErrorWriter.write(response, objectMapper, HttpStatus.UNAUTHORIZED, MESSAGE);
    }
}
