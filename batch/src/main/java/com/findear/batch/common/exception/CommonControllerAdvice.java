package com.findear.batch.common.exception;

import com.findear.batch.common.response.FailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 컨트롤러 예외를 공통 실패 형식 {@code {"status", "message"}} JSON으로 바꾼다 (main의 CommonControllerAdvice와 같은 규칙, D-51).
 * 본문의 status는 항상 실제 HTTP 상태와 같다. 더 구체적인 두 advice가 먼저 처리한다 (HIGHEST_PRECEDENCE):
 * {@code Lost112ExceptionAdvice}(키 없음 503·Lost112 호출 실패 502), {@code BatchJobExceptionAdvice}(같은 잡 실행 중 409).
 *
 * <pre>
 * 예외                                                   HTTP  message
 * NotFoundException (없는 분실물 등)                     404   예외 메시지
 * BadRequestException (날짜 형식, page·size 범위 등)     400   예외 메시지
 * MatchServerException (match 연결 불가·시간 초과·오류)  502   고정 문구 (원인은 로그에만)
 * Spring MVC 표준 예외 (경로 없음 404, 메서드 405, 필수 파라미터 누락·타입 불일치·깨진 JSON 400, 415, 406 등)
 *                                                        각 상태  ProblemDetail.detail이 있으면 그것(예: "Failed to read request"),
 *                                                                 없으면 상태 설명(예: "Not Found")
 * 그 밖의 모든 예외                                      500   고정 문구 (예외 메시지 미노출), 로그에는 스택 기록
 * </pre>
 * 응답 Content-Type은 항상 application/json이다.
 */
@Slf4j
@RestControllerAdvice
public class CommonControllerAdvice extends ResponseEntityExceptionHandler {

    static final String MATCH_SERVER_FAILED = "매칭 서버 호출에 실패했습니다.";
    static final String INTERNAL_ERROR = "서버 내부 오류가 발생했습니다.";

    @ExceptionHandler(Exception.class)
    public ResponseEntity<FailResponse> handleAllException(Exception e) {
        log.error("처리되지 않은 예외", e);
        return fail(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<FailResponse> handleNotFound(NotFoundException e) {
        log.info(e.getMessage());
        return fail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<FailResponse> handleBadRequest(BadRequestException e) {
        log.info(e.getMessage());
        return fail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MatchServerException.class)
    public ResponseEntity<FailResponse> handleMatchServer(MatchServerException e) {
        log.warn("{} (원인: {})", e.getMessage(), e.getCause() == null ? "없음" : e.getCause().toString());
        return fail(HttpStatus.BAD_GATEWAY, MATCH_SERVER_FAILED);
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
