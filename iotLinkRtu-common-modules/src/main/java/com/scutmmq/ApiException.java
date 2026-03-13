package com.scutmmq;

import com.scutmmq.exception.ErrorCode;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * API 业务异常基类
 * 抛出后框架自动转为对应 HTTP 错误响应
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class ApiException extends RuntimeException {

    private final HttpResponseStatus status;
    private final int errorCode;
    private final String errorMsg;

    public ApiException(HttpResponseStatus status, int errorCode, String message) {
        super(message);
        this.status    = status;
        this.errorCode = errorCode;
        this.errorMsg  = message;
    }
    
    /**
     * 使用 ErrorCode 枚举构造异常
     * 
     * @param errorCode 错误码枚举
     */
    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = mapToHttpStatus(errorCode.getCode());
        this.errorCode = errorCode.getCode();
        this.errorMsg = errorCode.getMessage();
    }
    
    /**
     * 使用 ErrorCode 枚举和自定义消息构造异常
     * 
     * @param errorCode 错误码枚举
     * @param customMessage 自定义消息
     */
    public ApiException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.status = mapToHttpStatus(errorCode.getCode());
        this.errorCode = errorCode.getCode();
        this.errorMsg = customMessage;
    }

    public HttpResponseStatus getStatus()    { return status; }
    public int                getErrorCode() { return errorCode; }
    public String             getErrorMsg()  { return errorMsg; }
    
    /**
     * 将业务错误码映射到 HTTP 状态码
     */
    private static HttpResponseStatus mapToHttpStatus(int code) {
        if (code >= 200 && code < 300) {
            return HttpResponseStatus.OK;
        } else if (code >= 400 && code < 500) {
            return HttpResponseStatus.BAD_REQUEST;
        } else if (code == 401) {
            return HttpResponseStatus.UNAUTHORIZED;
        } else if (code == 403) {
            return HttpResponseStatus.FORBIDDEN;
        } else if (code == 404) {
            return HttpResponseStatus.NOT_FOUND;
        } else if (code >= 500) {
            return HttpResponseStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpResponseStatus.BAD_REQUEST;
    }
}
