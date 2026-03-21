package com.henashi.inventorycrm.exception;

import lombok.Getter;

/**
 * 基础业务异常
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String code;      // 业务错误码
    private final Object data;      // 额外数据

    public ResourceNotFoundException(String message) {
        super(message);
        this.code = "BUSINESS_ERROR";
        this.data = null;
    }

    public ResourceNotFoundException(String code, String message) {
        super(message);
        this.code = code;
        this.data = null;
    }

    public ResourceNotFoundException(String code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.code = "BUSINESS_ERROR";
        this.data = null;
    }
}