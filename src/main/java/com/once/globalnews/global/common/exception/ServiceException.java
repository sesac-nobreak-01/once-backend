package com.once.globalnews.global.common.exception;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {
    private final String errCode;
    private final String errMessage;

    public ServiceException(String errorCode, String errMessage) {
        super(errMessage);
        this.errCode = errorCode;
        this.errMessage = errMessage;
    }

}
