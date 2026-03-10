package com.scutmmq;

import com.scutmmq.config.Config;
import com.scutmmq.server.RtuServer;
import lombok.extern.slf4j.Slf4j;

/**
 * RTU 网关服务器启动类
 *
 * <p>负责启动 Netty 服务，监听指定端口，处理二进制帧协议通讯</p>
 *
 * @author mo.mingqin@xlink
 * @since 2026-03-08
 */
@Slf4j
public class Application {

    /**
     * 程序入口方法
     *
     * <p>创建并启动 RTU 网关服务器实例，监听配置的端口</p>
     *
     * @param args 命令行参数（未使用）
     * @throws InterruptedException 当服务器启动被中断时抛出
     */
    public static void main(String[] args) throws InterruptedException {
        log.info("========================================");
        log.info("  RTU Gateway Server 正在启动...");
        log.info("  监听端口: {}", Config.rtuPort);
        log.info("========================================");

        // 启动 RTU 网关服务器
        RtuServer rtuServer = new RtuServer(Config.rtuPort);
        rtuServer.start();
    }
}
