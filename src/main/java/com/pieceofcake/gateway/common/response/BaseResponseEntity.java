package com.pieceofcake.gateway.common.response;

import org.springframework.http.HttpStatus;

public record BaseResponseEntity<T>(HttpStatus httpStatus, boolean isSuccess, String message, int code, T result) {

    public BaseResponseEntity(String message, T result) {
        this(HttpStatus.OK, true, message, 200, result);
    }

    public BaseResponseEntity(String message) {
        this(HttpStatus.OK, true, message, 200, null);
    }
}
