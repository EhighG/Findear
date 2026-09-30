package com.findear.main.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findear.main.common.response.FailResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 서블릿 필터 단계(컨트롤러 어드바이스가 닿지 않는 곳)에서 공통 실패 형식 {"status", "message"}를 쓴다. */
final class SecurityErrorWriter {

    private SecurityErrorWriter() {
    }

    static void write(HttpServletResponse response, ObjectMapper objectMapper, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), new FailResponse(status.value(), message));
    }
}
