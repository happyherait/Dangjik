package io.github.happyherait.dangjik.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 업무 규칙 위반을 화면이 읽을 수 있는 메시지로 돌려준다. */
@RestControllerAdvice
public class ApiError {

    public record Body(String message) {
    }

    /** 요청은 올바르지만 지금 상태에서는 할 수 없는 작업 */
    public static class Conflict extends RuntimeException {
        public Conflict(String message) {
            super(message);
        }
    }

    @ExceptionHandler(Conflict.class)
    public ResponseEntity<Body> conflict(Conflict e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new Body(e.getMessage()));
    }
}
