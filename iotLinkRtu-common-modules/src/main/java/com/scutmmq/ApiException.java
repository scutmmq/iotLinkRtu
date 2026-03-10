package com.scutmmq;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * API 业务异常基类
 * 抛出后框架自动转为对应 HTTP 错误响应
 */
public class ApiException extends RuntimeException {

    private final HttpResponseStatus status;
    private final int errorCode;

    public ApiException(HttpResponseStatus status, int errorCode, String message) {
        super(message);
        this.status    = status;
        this.errorCode = errorCode;
    }

    public HttpResponseStatus getStatus()    { return status; }
    public int                getErrorCode() { return errorCode; }
}
