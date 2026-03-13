package com.scutmmq;

import com.scutmmq.exception.ErrorCode;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * 404 Not Found - 资源不存在
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class NotFoundException extends ApiException {

    public NotFoundException(int errorCode, String message) {
        super(HttpResponseStatus.NOT_FOUND, errorCode, message);
    }

    /** 快捷构造，使用默认错误码 40401 */
    public NotFoundException(String message) {
        this(40401, message);
    }
    
    /** 使用 ErrorCode 枚举构造 */
    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    /** 使用 ErrorCode 枚举和自定义消息构造 */
    public NotFoundException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
