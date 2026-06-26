package com.wedding.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.stripe.exception.StripeException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(WeddingException.class)
    public ResponseEntity<Map<String, String>> handleWeddingException (
            WeddingException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of("error", ex.getLocalizedMessage()));
    }

    @ExceptionHandler(StripeException.class)
    public ResponseEntity<Map<String, String>> handleStripeException (
            StripeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", ex.getLocalizedMessage()));
    }
}
