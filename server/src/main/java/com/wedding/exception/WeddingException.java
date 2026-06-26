package com.wedding.exception;

import org.springframework.http.HttpStatus;

public class WeddingException extends RuntimeException {
    private final HttpStatus status;

    public WeddingException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
