package com.scutmmq.server;

import com.scutmmq.handler.BinaryFrameHandler;
import com.scutmmq.manager.RtuConnectionManager;
import com.scutmmq.mqtt.MqttClientManager;
import com.scutmmq.mqtt.MqttPublisher;
import com.scutmmq.protocol.BinaryFrameDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * RTU 网关服务器
 *
 * <p>基于 Netty 实现的 RTU 网关服务器</p>
 * <p>负责接收 serial-collector 的连接，处理二进制帧协议</p>
 *
 * @author mo.mingqin@xlink
 * @since 2026-03-08
 */
@Slf4j
public class RtuServer {

    /**
     * 服务器监听端口
     */
    private final int port;

    /**
     * Boss 事件循环组（负责接受客户端连接）
     */
    private NioEventLoopGroup bossGroup;

    /**
     * Worker 事件循环组（负责处理 I/O 读写）
     */
    private NioEventLoopGroup workerGroup;

    /**
     * 服务器 Channel
     */
    private Channel serverChannel;

    /**
     * RTU 连接管理器
     */
    private final RtuConnectionManager connectionManager;

    /**
     * MQTT 客户端管理器
     */
    private final MqttClientManager mqttClientManager;

    /**
     * MQTT 消息发布器
     */
    private final MqttPublisher mqttPublisher;

    /**
     * 带参数的构造函数
     *
     * @param port 服务器监听端口号
     */
    public RtuServer(int port) {
        this.port = port;
        this.connectionManager = new RtuConnectionManager();
        this.mqttClientManager = new MqttClientManager();
        this.mqttPublisher = new MqttPublisher(mqttClientManager);
    }

    /**
     * 启动 RTU 服务器
     *
     * <p>初始化 Boss 和 Worker 线程组，绑定端口，启动 Netty 服务</p>
     * <p>服务启动后会持续运行，直到被显式关闭或发生异常</p>
     *
     * @throws InterruptedException 当服务器启动或运行被中断时抛出
     */
    public void start() throws InterruptedException {
        // 启动 MQTT 客户端
        log.info("正在启动 MQTT 客户端...");
        mqttClientManager.start();

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(4);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline()
                                    // 日志处理器
                                    .addLast(new LoggingHandler(LogLevel.DEBUG))
                                    // 空闲检测（读超时60秒，写超时0，读写超时0）
                                    .addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS))
                                    // 二进制帧解码器
                                    .addLast(new BinaryFrameDecoder())
                                    // 业务逻辑处理器（注入 MQTT 发布器）
                                    .addLast(new BinaryFrameHandler(connectionManager, mqttPublisher));
                        }
                    });

            ChannelFuture channelFuture = bootstrap.bind(port).sync();
            serverChannel = channelFuture.channel();

            log.info("RTU Gateway Server 启动成功，监听端口: {}", port);

            // 阻塞等待服务关闭
            serverChannel.closeFuture().sync();
            log.info("RTU Gateway Server 已停止");

        } catch (InterruptedException e) {
            log.error("RTU Gateway Server 启动失败", e);
            throw e;
        } finally {
            shutdown();
        }
    }

    /**
     * 优雅地关闭 RTU Server，确保所有资源都被正确释放
     *
     * <p>依次关闭服务器 Channel、Worker 线程组和 Boss 线程组</p>
     */
    public void shutdown() {
        log.info("正在关闭 RTU Gateway Server...");

        if (serverChannel != null) {
            serverChannel.close();
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }

        // 关闭 MQTT 客户端
        if (mqttClientManager != null) {
            mqttClientManager.stop();
        }

        log.info("RTU Gateway Server 已完全关闭");
    }

    /**
     * 获取连接管理器
     *
     * @return RtuConnectionManager
     */
    public RtuConnectionManager getConnectionManager() {
        return connectionManager;
    }
}
