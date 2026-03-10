package com.scutmmq;

import io.netty.handler.codec.http.HttpResponseStatus;

/** 资源不存在（404） */
public class NotFoundException extends ApiException {

    public NotFoundException(int errorCode, String message) {
        super(HttpResponseStatus.NOT_FOUND, errorCode, message);
    }

    /** 快捷构造，使用默认错误码 40401 */
    public NotFoundException(String message) {
        this(40401, message);
    }
}
