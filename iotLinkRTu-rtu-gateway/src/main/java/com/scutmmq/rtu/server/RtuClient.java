package com.scutmmq.rtu.server;

import com.scutmmq.rtu.codec.ModBusCodec;
import com.scutmmq.rtu.hanlder.ModBusClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ModBus-RTU 客户端 测试类
 * 
 * <p>用于测试 ModBus-RTU 协议的客户端连接，模拟从机返回温湿度等传感器数据</p>
 * <p>连接到本地 502 端口，发送模拟的响应数据</p>
 * 
 * @author mo.mingqin@xlink
 * @since 2026-03-08
 */

public class RtuClient {
    /**
     * 单线程执行服务池
     * <p>用于处理服务器输出任务，线程名为 "rtu-server-output-thread"</p>
     */
    private static ExecutorService executorService = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        // 服务向外输出信息
        thread.setName("rtu-server-output-thread");
        return thread;
    });
    
    /**
     * 程序入口方法（测试用）
     * 
     * <p>创建 Netty 客户端，连接到本地 502 端口，并发送模拟的温湿度响应数据</p>
     *
     * @param args 命令行参数（未使用）
     * @throws InterruptedException 当连接被中断时抛出
     */
    public static void main(String[] args) throws InterruptedException {
        ChannelFuture channelFuture = new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new LoggingHandler(LogLevel.DEBUG))
                                .addLast(new ModBusCodec())
                                .addLast(new ModBusClientHandler());
                    }
                })                .connect("127.0.0.1", 502)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        System.out.println("✅ 连接成功，开始发送数据...");
                        
                        // 构造模拟的温湿度响应数据（模拟从机返回）
                        byte[] data = new byte[]{
                                0x01, //地址码
                                0x03, //功能码
                                0x04, // 返回有效字节数
                                0x02, (byte) 0x92, // 湿度
                                (byte) 0xFF,(byte) 0x9B, // 温度
                                (byte) 0xDC,  // 校验码低位
                                (byte) 0xC3   // 校验码高位
                        };
                        ByteBuf byteBuf = future.channel().alloc().buffer();
                        byteBuf.writeBytes(data);
                        future.channel().writeAndFlush(byteBuf);
                    } else {
                        System.err.println("❌ 连接失败：" + future.cause().getMessage());
                        future.channel().close();
                    }
                });
        channelFuture.channel().closeFuture().sync();
    }
}
