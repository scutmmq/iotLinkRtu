package com.scutmmq.restful;

import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 路由处理器
 * 负责接收 HTTP 请求、路由匹配、并将业务逻辑提交到业务线程池异步执行
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
@Slf4j
@ChannelHandler.Sharable
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
    /**
     * 处理 HTTP 请求的核心方法
     * 1. 解析请求路径
     * 2. 路由匹配找到对应的 Controller
     * 3. 创建 Controller实例并执行 handle() 方法
     * 4. 将业务逻辑提交到业务线程池，避免阻塞 Netty I/O 线程
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws Exception {

        log.debug("收到 HTTP 请求: {}", httpRequest.uri());

        // 解析请求路径（去掉查询参数部分）
        String path = new QueryStringDecoder(httpRequest.uri()).path();
        
        // 路由查找：根据路径找到对应的 Controller
        RestFulExpress.RouteMatch match = ROUTER.match(path);
        if (match == null) {
            // 未找到路由，返回 404
            MyHttpResponse response = new MyHttpResponse(ctx);
            response.json(
                HttpResponseStatus.NOT_FOUND,
                Map.of("code", 40400, "message", "Route not found: " + path)
            );
            return;
        }
        
        // 封装请求和响应对象
        MyHttpRequest request = new MyHttpRequest(httpRequest, match.pathParams());
        MyHttpResponse response = new MyHttpResponse(ctx);
        
        // ★ 关键：将业务逻辑提交到业务线程池异步执行，不阻塞 Netty I/O 线程
        // 引用计数 +1，防止在异步执行前被释放
        httpRequest.retain();
        
        BUSINESS_EXECUTOR.execute(() -> {
            try {
                // 创建 Controller实例
                BaseController controller = match.controllerClass().newInstance();
                // 执行 Controller 的 handle() 方法
                // handle() 方法会根据 request.method() 自动分发到 get()/post()/put()/delete()
                controller.handle(request, response);
            } catch (Exception e) {
                // 未预期异常处理：返回 500 错误
                response.json(
                    HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    Map.of("code", 50000, "message", "Internal error: " + e.getMessage())
                );
            } finally {
                // 引用计数 -1，释放内存
                httpRequest.release();
            }
        });
    }
}
