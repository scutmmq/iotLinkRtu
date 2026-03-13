package com.scutmmq;

import com.scutmmq.exception.ErrorCode;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * 500 Internal Server Error - 内部服务错误
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class ServerException extends ApiException {

    public ServerException(int errorCode, String message) {
        super(HttpResponseStatus.INTERNAL_SERVER_ERROR, errorCode, message);
    }

    /** 快捷构造，使用默认错误码 50001 */
    public ServerException(String message) {
        this(50001, message);
    }
    
    /** 使用 ErrorCode 枚举构造 */
    public ServerException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    /** 使用 ErrorCode 枚举和自定义消息构造 */
    public ServerException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
