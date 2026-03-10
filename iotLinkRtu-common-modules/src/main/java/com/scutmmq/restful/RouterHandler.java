package com.scutmmq.restful;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RouterHandler  extends SimpleChannelInboundHandler<FullHttpRequest> {

    // 获取路由组件
    private static final RestFulExpress ROUTER = RestFulExpress.instance();

    /**
     * 业务线程池
     * I/O 线程（Netty Worker）只负责收发网络数据，
     * 业务逻辑（数据库查询、复杂计算等）在此线程池中异步执行，防止 I/O 线程被阻塞。
     */
    private static final ThreadPoolExecutor BUSINESS_EXECUTOR = new ThreadPoolExecutor(
            4,                              // 核心线程数
            16,                             // 最大线程数
            60, TimeUnit.SECONDS,           // 空闲线程存活时间
            new LinkedBlockingQueue<>(1000), // 任务队列容量
            r -> {
                Thread t = new Thread(r, "http-business-" + System.nanoTime());
                t.setDaemon(true);          // 设为守护线程，JVM 退出时自动结束
                return t;
            }
    );
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest httpRequest) throws Exception {
       // 解析请求路径（去掉查询参数部分）和请求方法
        String path   = new QueryStringDecoder(httpRequest.uri()).path();
        String method = httpRequest.method().name();

        // 路由匹配，一个路径&&一个请求方法 -> 一个路由组件

    }
}
