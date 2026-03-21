package com.henashi.inventorycrm.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AuthenticationException extends RuntimeException {

    private final String code;

    private final HttpStatus status;

    public AuthenticationException(String message) {
        super(message);
        this.code = "AUTH_ERROR";
        this.status = HttpStatus.UNAUTHORIZED;
    }
}