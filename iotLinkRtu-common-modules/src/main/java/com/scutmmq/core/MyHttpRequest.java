package com.scutmmq.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP 请求封装类
 * 对 Netty FullHttpRequest 的封装，提供便捷的参数获取方法
 * 类比 Spring 中的 HttpServletRequest
 */
public class MyHttpRequest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final FullHttpRequest nettyRequest;
    private final Map<String, String> pathParams; // 路径参数，如 /user/{id} 中的 id
    private Map<String, Object> bodyMap;           // 懒加载，避免重复解析

    public MyHttpRequest(FullHttpRequest nettyRequest, Map<String, String> pathParams) {
        this.nettyRequest = nettyRequest;
        this.pathParams   = pathParams;
    }

    /**
     * 获取请求体 JSON（POST / PUT 常用）
     * 返回 Map，可通过 key 取值
     */
    public Map<String, Object> bodyJson() {
        if (bodyMap == null) {
            String bodyStr = nettyRequest.content().toString(CharsetUtil.UTF_8);
            if (bodyStr == null || bodyStr.trim().isEmpty()) {
                bodyMap = new HashMap<>();
            } else {
                try {
                    bodyMap = OBJECT_MAPPER.readValue(bodyStr, new TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    bodyMap = new HashMap<>();
                }
            }
        }
        return bodyMap;
    }

    /**
     * 从请求体 JSON 中获取字符串字段
     */
    public String bodyString(String key) {
        Object val = bodyJson().get(key);
        return val != null ? val.toString() : null;
    }

    /**
     * 获取 URL 查询参数（GET 常用，如 ?name=xxx）
     */
    public String queryParam(String name) {
        QueryStringDecoder decoder = new QueryStringDecoder(nettyRequest.uri());
        List<String> values = decoder.parameters().get(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    /**
     * 获取路径参数（如 /user/{id} 中的 id）
     */
    public String pathParam(String name) {
        return pathParams.getOrDefault(name, null);
    }

    /**
     * 获取请求头
     */
    public String header(String name) {
        return nettyRequest.headers().get(name);
    }

    /**
     * 获取请求方法（GET / POST / PUT / DELETE）
     */
    public String method() {
        return nettyRequest.method().name();
    }

    /**
     * 获取请求路径（不含查询参数）
     */
    public String path() {
        return new QueryStringDecoder(nettyRequest.uri()).path();
    }

    /**
     * 获取完整 URI（含查询参数）
     */
    public String uri() {
        return nettyRequest.uri();
    }
}
