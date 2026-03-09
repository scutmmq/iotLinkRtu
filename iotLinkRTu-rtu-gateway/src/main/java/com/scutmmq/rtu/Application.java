package com.scutmmq.rtu;

import com.scutmmq.rtu.config.Config;
import com.scutmmq.rtu.server.RtuServer;

/**
 * ModBus-RTU 服务器启动类
 * 
 * <p>负责启动 Netty 服务，监听指定端口，处理 ModBus-RTU 协议通讯</p>
 * 
 * @author mo.mingqin@xlink
 * @since 2026-03-08
 */
public class Application {

    /**
     * 程序入口方法
     * 
     * <p>创建并启动 RTU 服务器实例，监听配置的端口</p>
     * 
     * @param args 命令行参数（未使用）
     * @throws InterruptedException 当服务器启动被中断时抛出
     */
    public static void main(String[] args) throws InterruptedException {

        // 启动 Netty 服务获取数据
        RtuServer rtuServer = new RtuServer(Config.rtuPort);
        rtuServer.start();

    }
}
