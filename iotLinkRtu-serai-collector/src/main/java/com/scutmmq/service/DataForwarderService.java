package com.scutmmq.service;

import cn.hutool.core.util.HexUtil;
import com.scutmmq.client.GatewayClient;
import com.scutmmq.manager.SerialPortManager;
import com.scutmmq.manager.impl.JSerialCommManager;
import com.scutmmq.utils.Crc16Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 数据转发服务
 * 负责定时轮询串口设备，验证数据，并转发给网关
 */
public class DataForwarderService {
    private static final Logger logger = LoggerFactory.getLogger(DataForwarderService.class);

    private final SerialPortManager serialPortManager;
    private final GatewayClient gatewayClient;
    private final ScheduledExecutorService scheduler;

    // 配置参数
    private final String serialPortName;
    private final int baudRate;
    private static final String GATEWAY_HOST = "127.0.0.1";
    private static final int GATEWAY_PORT = 502;
    private static final long POLL_INTERVAL_MS = 1000; // 轮询间隔：1 秒

    // ModBus查询指令: 地址01, 功能码03, 起始0000, 长度0002
    // 01 03 00 00 00 02
    private static final byte[] QUERY_CMD_BASE = new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x02};

    public DataForwarderService() {
        this("COM3", 9600); // 默认 COM3 端口，9600 波特率
    }

    public DataForwarderService(String portName, int baudRate) {
        this.serialPortName = portName;
        this.baudRate = baudRate;
        this.serialPortManager = new JSerialCommManager();
        this.gatewayClient = new GatewayClient(GATEWAY_HOST, GATEWAY_PORT);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        logger.info("启动数据转发服务...");
        logger.info("目标串口: {}, 波特率: {}", serialPortName, baudRate);

        // 1. 打开串口
        if (!serialPortManager.open(serialPortName, baudRate)) {
            logger.error("串口 {} 打开失败，服务无法正常工作", serialPortName);
            // 这里可以选择退出或者重试策略
        }

        // 2. 启动网关客户端
        gatewayClient.start();

        // 3. 启动定时轮询
        scheduler.scheduleAtFixedRate(this::pollDevice, 1000, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void pollDevice() {
        try {
            if (!serialPortManager.isOpen()) {
                // ...existing code...
                if (!serialPortManager.open(serialPortName, baudRate)) {
                    return;
                }
            }

            // 构建带有CRC的查询指令
            byte[] cmd = Crc16Util.appendCrc16(QUERY_CMD_BASE);
            logger.debug("发送查询指令: {}", HexUtil.encodeHexStr(cmd));

            // 发送指令
            serialPortManager.write(cmd);

            // 读取响应
            // ModBus RTU 响应通常很快，但这可能是在进行人工调试
            // 增加超时时间到 5000ms，给予用户足够的时间点击"发送"
            byte[] response = serialPortManager.read(5000);

            if (response.length == 0) {
                logger.warn("未收到设备响应 (超时或无数据)");
                return;
            }

            logger.debug("收到响应数据: {}", HexUtil.encodeHexStr(response));

            // 验证 CRC
            if (Crc16Util.checkCrc16(response)) {
                logger.info("数据校验通过，转发至网关");
                gatewayClient.send(response);
            } else {
                logger.error("数据校验失败: {}", HexUtil.encodeHexStr(response));
            }

        } catch (Exception e) {
            logger.error("轮询设备异常", e);
        }
    }

    public void stop() {
        scheduler.shutdown();
        gatewayClient.stop();
        serialPortManager.close();
        logger.info("服务已停止");
    }
}

