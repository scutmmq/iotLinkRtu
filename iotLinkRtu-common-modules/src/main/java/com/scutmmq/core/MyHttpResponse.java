package com.scutmmq.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

/**
 * HTTP 响应封装类
 * 提供便捷的响应写出方法，屏蔽 Netty 底层细节
 * 类比 Spring 中的 HttpServletResponse
 */
public class MyHttpResponse {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ChannelHandlerContext ctx;

    public MyHttpResponse(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    /**
     * 写出 JSON 响应（200 OK）
     * 自动将对象序列化为 JSON 字符串
     */
    public void json(Object data) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(data);
            writeResponse(HttpResponseStatus.OK, json);
        } catch (Exception e) {
            writeResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    "{\"code\":500,\"message\":\"响应序列化失败\"}");
        }
    }

    /**
     * 写出指定状态码的 JSON 响应
     */
    public void json(HttpResponseStatus status, Object data) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(data);
            writeResponse(status, json);
        } catch (Exception e) {
            writeResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    "{\"code\":500,\"message\":\"响应序列化失败\"}");
        }
    }

    /**
     * 写出纯文本响应
     */
    public void text(String text) {
        writeResponse(HttpResponseStatus.OK, text);
    }

    /**
     * 底层响应写出：构建 Netty HTTP 响应并发送
     */
    private void writeResponse(HttpResponseStatus status, String body) {
        byte[] contentBytes = body.getBytes(CharsetUtil.UTF_8);

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.wrappedBuffer(contentBytes)
        );

        response.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8")
                .setInt(HttpHeaderNames.CONTENT_LENGTH, contentBytes.length)
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*"); // 允许跨域

        // 发送后关闭连接（HTTP/1.1 短连接模式）
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
