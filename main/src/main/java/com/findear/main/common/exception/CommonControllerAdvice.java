package com.findear.main.common.exception;

import com.findear.main.Alarm.common.exception.AlarmException;
import com.findear.main.board.common.exception.BoardException;
import com.findear.main.common.response.FailResponse;
import com.findear.main.message.common.exception.MessageException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 컨트롤러 예외를 공통 실패 형식 {@code {"status", "message"}} JSON으로 바꾼다. 본문의 status는 항상 실제 HTTP 상태와 같다.
 * 외부 연동 예외(503·502)는 {@link ExternalServiceExceptionAdvice}가 먼저 처리한다.
 *
 * <pre>
 * 예외                                                  HTTP  message
 * Spring Security AuthenticationException               401   예외 메시지
 * ExpiredJwtException, JwtException                     401   고정 문구(토큰 내용 미포함)
 * AuthorizationServiceException, AccessDeniedException  403   예외 메시지 / 고정 문구
 * IllegalArgumentException, UsernameNotFoundException,
 *   MessageException, AlarmException, BoardException    400   예외 메시지
 * Spring MVC 표준 예외 (경로 없음 404, 메서드 405, 본문·파라미터 오류 400, 415, 406 등)
 *                                                       각 상태  상태 설명(ProblemDetail.detail)
 * 그 밖의 모든 예외                                     500   고정 문구(예외 메시지 미노출), 로그에는 스택 기록
 * </pre>
 * 응답 Content-Type은 항상 application/json이다 (SSE 구독처럼 Accept가 text/event-stream뿐인 요청도 JSON 오류를 받는다).
 */
@Slf4j
@RestControllerAdvice
public class CommonControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<FailResponse> handleAllException(Exception e) {
        log.error("처리되지 않은 예외", e);
        return fail(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<FailResponse> handleAuthenticationException(AuthenticationException e) {
        log.info("인증 실패: {}", e.getMessage());
        return fail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<FailResponse> handleExpiredJwtException(ExpiredJwtException e) {
        log.info("jwt 토큰 만료 exception 발생");
        return fail(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<FailResponse> handleJwtException(JwtException e) {
        log.info("jwt 토큰 오류: {}", e.getClass().getSimpleName());
        return fail(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
    }

    @ExceptionHandler(AuthorizationServiceException.class)
    public ResponseEntity<FailResponse> handleAuthorizationException(AuthorizationServiceException e) {
        log.info("권한 없음: {}", e.getMessage());
        return fail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<FailResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.info("접근 거부: {}", e.getMessage());
        return fail(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
    }

    @ExceptionHandler({IllegalArgumentException.class, UsernameNotFoundException.class,
            MessageException.class, AlarmException.class, BoardException.class})
    public ResponseEntity<FailResponse> handleBadRequest(Exception e) {
        log.info(e.getMessage());
        return fail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Spring MVC 표준 예외(404·405·400·415·406 등)도 같은 형식으로 내보낸다. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        String message = body instanceof ProblemDetail problemDetail && problemDetail.getDetail() != null
                ? problemDetail.getDetail()
                : (statusCode instanceof HttpStatus status ? status.getReasonPhrase() : "요청을 처리하지 못했습니다.");
        return ResponseEntity.status(statusCode)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new FailResponse(statusCode.value(), message));
    }

    private static ResponseEntity<FailResponse> fail(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new FailResponse(status.value(), message));
    }
}
