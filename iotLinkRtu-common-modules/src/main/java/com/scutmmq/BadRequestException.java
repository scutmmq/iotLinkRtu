package com.scutmmq;

import io.netty.handler.codec.http.HttpResponseStatus;

/** 请求参数错误（400） */
public class BadRequestException extends ApiException {

    public BadRequestException(int errorCode, String message) {
        super(HttpResponseStatus.BAD_REQUEST, errorCode, message);
    }

    /** 快捷构造，使用默认错误码 40001 */
    public BadRequestException(String message) {
        this(40001, message);
    }
}
