package com.scutmmq;

import io.netty.handler.codec.http.HttpResponseStatus;

/** 内部服务错误（500） */
public class ServerException extends ApiException {

    public ServerException(int errorCode, String message) {
        super(HttpResponseStatus.INTERNAL_SERVER_ERROR, errorCode, message);
    }

    /** 快捷构造，使用默认错误码 50001 */
    public ServerException(String message) {
        this(50001, message);
    }
}
