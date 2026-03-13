package com.scutmmq;

import com.scutmmq.exception.ErrorCode;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * 400 Bad Request - 请求参数错误
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class BadRequestException extends ApiException {

    public BadRequestException(int errorCode, String message) {
        super(HttpResponseStatus.BAD_REQUEST, errorCode, message);
    }

    /** 快捷构造，使用默认错误码 40001 */
    public BadRequestException(String message) {
        this(40001, message);
    }
    
    /** 使用 ErrorCode 枚举构造 */
    public BadRequestException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    /** 使用 ErrorCode 枚举和自定义消息构造 */
    public BadRequestException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
