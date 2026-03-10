package com.scutmmq.restful;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class HttpServer {

    private final Integer port;

    private NioEventLoopGroup boss;

    private NioEventLoopGroup worker;

    private final static RouterHandler ROUTER_HANDLER = new RouterHandler();

    public HttpServer(Integer port) {
        this.port = port;
    }

    private void start(){
        boss = new NioEventLoopGroup(1);
        worker = new NioEventLoopGroup(3);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap
                    .group(boss, worker)
                    .channel(NioServerSocketChannel.class)         // 使用 NIO 非阻塞模式
                    .option(ChannelOption.SO_BACKLOG, 128)         // TCP 连接等待队列长度
                    .childOption(ChannelOption.SO_KEEPALIVE, true) // 保持长连接心跳
                    .handler(new LoggingHandler(LogLevel.INFO))    // 服务端连接日志
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    // 1. HTTP 编解码器：将 TCP 字节流解析为 HTTP 请求/响应对象
                                    .addLast(new HttpServerCodec())
                                    // 2. HTTP 聚合器：将分片的 HTTP 消息合并为完整的 FullHttpRequest
                                    //    最大请求体：10MB
                                    .addLast(new HttpObjectAggregator(10 * 1024 * 1024))
                                    // 3. 路由处理器（核心）：路由匹配 → 分发到业务线程池
                                    .addLast(ROUTER_HANDLER);
                        }
                    });
            ChannelFuture channelFuture = bootstrap.bind(port);

            // 阻塞主线程，直到服务器 Channel 关闭
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 优雅关闭，释放资源
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }

    }


    public Integer getPort() {
        return port;
    }
}
