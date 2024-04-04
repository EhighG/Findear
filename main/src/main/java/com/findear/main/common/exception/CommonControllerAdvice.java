package com.findear.main.common.exception;

import com.findear.main.Alarm.common.exception.AlarmException;
import com.findear.main.common.response.FailResponse;
import com.findear.main.message.common.exception.MessageException;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.naming.AuthenticationException;

@Slf4j
@RestControllerAdvice
public class CommonControllerAdvice {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllException(Exception e) {
        e.printStackTrace();
        log.info("공용 exception 발생");

        return ResponseEntity.badRequest().body(new FailResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException e) {
        System.out.println("e.getMessage() = " + e.getMessage());
//        return ResponseEntity
//                .badRequest()
//                .body(new FailResponse(HttpStatus.UNAUTHORIZED.value(), e.getMessage()));
        return new ResponseEntity<>(new FailResponse(HttpStatus.UNAUTHORIZED.value(), e.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthorizationServiceException.class)
    public ResponseEntity<?> handleAuthorizationException(AuthorizationServiceException e) {
        e.printStackTrace();
        return new ResponseEntity<>(new FailResponse(HttpStatus.FORBIDDEN.value(), e.getMessage()), HttpStatus.FORBIDDEN);
    }


    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<?> handleExpiredJwtException(ExpiredJwtException e) {

        log.info("jwt 토큰 만료 exception 발생");

        return ResponseEntity.badRequest().body(new FailResponse(HttpStatus.UNAUTHORIZED.value(), e.getMessage()));
    }
    @ExceptionHandler(MessageException.class)
    public ResponseEntity<?> handleMessageException(Exception e) {

        log.info(e.getMessage());

        return ResponseEntity.badRequest().body(new FailResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
    }

    @ExceptionHandler(AlarmException.class)
    public ResponseEntity<?> handleAlarmException(Exception e) {

        log.info(e.getMessage());

        return ResponseEntity.badRequest().body(new FailResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
    }

}
