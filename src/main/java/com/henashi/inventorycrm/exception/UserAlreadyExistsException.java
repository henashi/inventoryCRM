package com.henashi.inventorycrm.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UserAlreadyExistsException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public UserAlreadyExistsException(String message) {
        super(message);
        this.code = "USER_EXISTS";
        this.status = HttpStatus.CONFLICT;
    }
}
