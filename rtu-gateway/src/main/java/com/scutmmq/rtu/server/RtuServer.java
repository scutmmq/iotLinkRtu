package com.scutmmq.rtu.server;

import com.scutmmq.rtu.codec.ModBusCodec;
import com.scutmmq.rtu.hanlder.ModBusClientHandler;
import com.scutmmq.rtu.protocol.ModBusMessage;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;


/**
 * ModBus-RTU 服务器
 * 
 * <p>基于 Netty 实现的 ModBus-RTU 协议服务器</p>
 * <p>负责监听指定端口，接收客户端连接，处理 ModBus 消息的编解码和业务逻辑</p>
 * 
 * @author mo.mingqin@xlink
 * @since 2026-03-08
 */
@Slf4j
public class RtuServer {

    /**
     * 服务器监听端口
     */
    private int port;
    
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
     * 默认构造函数
     */
    RtuServer(){}

    /**
     * 带参数的构造函数
     *
     * @param port 服务器监听端口号
     */
    public RtuServer(int port){
        this.port = port;
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
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(3);
        try {
            ChannelFuture channelFuture = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel channel) throws Exception {
                            channel.pipeline()
                                    // 添加日志处理器
                                    .addLast(new LoggingHandler(LogLevel.DEBUG))
                                    // ModBus-RTU 协议编解码器
                                    .addLast(new ModBusCodec())
                                    // 添加业务逻辑处理器
                                    .addLast(new ModBusClientHandler());
                            ModBusMessage modbusMessage = new ModBusMessage();
                            modbusMessage.setAddress(0x01);
                            modbusMessage.setFunctionCode(0x03);
                            modbusMessage.setData(new byte[]{0x01, 0x02, 0x03, 0x04});
                            // 测试发送ModBusMessage对象是否被encoded
                            channel.writeAndFlush(modbusMessage);
                        }
                    }).bind(port).sync();
            serverChannel = channelFuture.channel();

            log.debug("RTU Server started on {}", serverChannel.localAddress().toString());

            // 阻塞等待服务关闭
            serverChannel.closeFuture().sync();
            log.debug("RTU Server stopped");
        } catch (InterruptedException e) {
            log.error("RTU Server started error", e);
            throw new RuntimeException(e);
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
        log.info("Shutting down RTU Server...");

        if (serverChannel != null) {
            serverChannel.close();
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }

        log.info("RTU Server shutdown complete");
    }
}
