package com.scutmmq.core;

import com.scutmmq.ApiException;
import com.scutmmq.BadRequestException;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Map;

/**
 * Controller 抽象基类
 * 子类按需重写对应 HTTP 方法，未重写的方法默认返回 405 Method Not Allowed
 * 类比 Spring 中的 @RestController
 */
public abstract class BaseController {

    /**
     * 执行请求的入口（框架内部调用，子类不需要重写）
     * 负责方法路由和统一异常处理
     */
    public final void handle(MyHttpRequest request, MyHttpResponse response) {
        try {
            switch (request.method()) {
                case "GET"    -> get(request, response);
                case "POST"   -> post(request, response);
                case "PUT"    -> put(request, response);
                case "DELETE" -> delete(request, response);
                default       -> response.json(
                        HttpResponseStatus.METHOD_NOT_ALLOWED,
                        buildErrorResponse(40500, "Method Not Allowed: " + request.method())
                );
            }
        } catch (ApiException e) {
            // 业务异常：转为对应 HTTP 错误响应
            response.json(e.getStatus(), buildErrorResponse(e.getErrorCode(), e.getErrorMsg()));
        } catch (Exception e) {
            // 未预期异常：统一返回 500
            response.json(
                    HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    buildErrorResponse(50000, "Internal Server Error: " + e.getMessage())
            );
        }
    }

    // ---- 子类按需重写以下方法 ----

    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        throw new BadRequestException(40500, "GET method not supported");
    }

    protected void post(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        throw new BadRequestException(40500, "POST method not supported");
    }

    protected void put(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        throw new BadRequestException(40500, "PUT method not supported");
    }

    protected void delete(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        throw new BadRequestException(40500, "DELETE method not supported");
    }

    // ---- 工具方法（子类可直接调用）----

    /** 必填对象校验：为 null 则抛 400 */
    protected void requireNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " 不能为空");
        }
    }

    /** 必填字符串校验：为空或空字符串则抛 400 */
    protected void requireNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(fieldName + " 不能为空");
        }
    }

    /**
     * 将 body JSON 中的 Object 安全转换为 Integer
     * 支持 Number 子类和字符串数字，null 或转换失败时返回 null
     */
    protected Integer toInteger(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return null; }
    }

    /**
     * 将 URL 查询参数字符串解析为 int
     * 解析失败时返回默认值
     */
    protected int parseIntParam(String val, int defaultVal) {
        if (val == null) return defaultVal;
        try { return Integer.parseInt(val.trim()); } catch (Exception e) { return defaultVal; }
    }

    /** 将 body 中的数字对象安全转换为 int，失败时返回默认值 */
    private int toInt(Object val, int defaultVal) {
        if (val == null) return defaultVal;
        try { return ((Number) val).intValue(); } catch (Exception e) { return defaultVal; }
    }

    /** 错误响应体格式 */
    record ErrorBody(int code, String message) {
        static ErrorBody of(int code, String msg) {
            return new ErrorBody(code, msg);
        }
    }
    
    /**
     * 构建统一的错误响应体
     * 
     * @param code 错误码
     * @param message 错误消息
     * @return 错误响应 Map
     */
    protected java.util.Map<String, Object> buildErrorResponse(int code, String message) {
        return java.util.Map.of(
            "code", code,
            "message", message
        );
    }
    
    /**
     * 构建成功的响应体（不含 data）
     * 
     * @return 成功响应 Map
     */
    protected java.util.Map<String, Object> buildSuccessResponse() {
        return java.util.Map.of(
            "code", 200,
            "message", "success"
        );
    }
    
    /**
     * 构建成功的响应体（含 data）
     * 
     * @param data 数据
     * @return 成功响应 Map
     */
    protected java.util.Map<String, Object> buildSuccessResponse(Object data) {
        return java.util.Map.of(
            "code", 200,
            "message", "success",
            "data", data
        );
    }
}

