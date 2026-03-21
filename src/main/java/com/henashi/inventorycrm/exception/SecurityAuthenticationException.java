package com.henashi.inventorycrm.exception;

import org.springframework.security.core.AuthenticationException;

public class SecurityAuthenticationException extends AuthenticationException {
    public SecurityAuthenticationException(String msg) {
        super(msg);
    }
}
