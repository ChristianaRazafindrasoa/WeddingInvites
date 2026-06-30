package com.wedding.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.RateLimitException;
import com.stripe.exception.StripeException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(WeddingException.class)
    public ResponseEntity<Map<String, String>> handleWeddingException(WeddingException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(CardException.class)
    public ResponseEntity<Map<String, String>> handleCardException(CardException ex) {
        log.warn("Card declined: code={} message={}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRequestException(InvalidRequestException ex) {
        log.warn("Invalid Stripe request: param={} message={}", ex.getParam(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException ex) {
        log.error("Stripe authentication failed — check API key", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Payment service configuration error."));
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, String>> handleRateLimitException(RateLimitException ex) {
        log.warn("Stripe rate limit hit", ex);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "Too many requests. Please try again later."));
    }

    @ExceptionHandler(StripeException.class)
    public ResponseEntity<Map<String, String>> handleStripeException(StripeException ex) {
        log.error("Unexpected Stripe error: requestId={}", ex.getRequestId(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", ex.getMessage()));
    }
}
