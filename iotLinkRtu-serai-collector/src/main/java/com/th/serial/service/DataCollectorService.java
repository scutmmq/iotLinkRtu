package com.th.serial.service;

import com.th.serial.client.GatewayClient;
import com.th.serial.manager.SerialPortManager;
import com.th.serial.protocol.BinaryProtocol;
import com.th.serial.protocol.ModbusProtocol;
import com.th.serial.utils.MicroConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 数据采集服务
 * 负责定时从串口读取 Modbus 数据并发送给网关
 */
public class DataCollectorService {

    private static final Logger logger = LoggerFactory.getLogger(DataCollectorService.class);

    private final SerialPortManager serialPortManager;
    private final GatewayClient gatewayClient;
    private final ScheduledExecutorService scheduler;

    private int deviceAddress;
    private int collectInterval;
    private boolean running = false;

    public DataCollectorService(SerialPortManager serialPortManager, GatewayClient gatewayClient) {
        this.serialPortManager = serialPortManager;
        this.gatewayClient = gatewayClient;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * 启动数据采集服务
     */
    public void start() {
        if (running) {
            logger.warn("数据采集服务已经在运行");
            return;
        }

        // 读取配置
        deviceAddress = parseHexAddress(MicroConfig.readString("modbus.device.address", "0x01"));
        collectInterval = MicroConfig.readInt("collect.interval", 1);

        logger.info("数据采集服务启动: 设备地址=0x{}, 采集间隔={}秒",
            String.format("%02X", deviceAddress), collectInterval);

        running = true;

        // 启动定时采集任务
        scheduler.scheduleAtFixedRate(this::collectData, 0, collectInterval, TimeUnit.SECONDS);
    }

    /**
     * 停止数据采集服务
     */
    public void stop() {
        if (!running) {
            return;
        }

        logger.info("正在停止数据采集服务...");
        running = false;

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("数据采集服务已停止");
    }

    /**
     * 采集数据（核心逻辑）
     */
    private void collectData() {
        try {
            // 检查串口是否打开
            if (!serialPortManager.isOpen()) {
                logger.warn("串口未打开，跳过本次采集");
                return;
            }

            // 检查网关是否已认证
            if (!gatewayClient.isAuthenticated()) {
                logger.debug("网关未认证，跳过本次采集");
                return;
            }

            // 1. 构建 Modbus 读取命令
            byte[] modbusCommand = ModbusProtocol.buildReadTempHumidityCommand(deviceAddress);
            logger.debug("发送 Modbus 命令: {}", BinaryProtocol.bytesToHex(modbusCommand));

            // 2. 发送命令到串口
            int written = serialPortManager.write(modbusCommand);
            if (written != modbusCommand.length) {
                logger.error("Modbus 命令发送不完整: 期望={}, 实际={}", modbusCommand.length, written);
                return;
            }

            // 3. 等待并读取响应（超时500ms）
            byte[] modbusResponse = serialPortManager.read(500);

            if (modbusResponse == null || modbusResponse.length == 0) {
                logger.warn("未收到 Modbus 响应（超时）");
                return;
            }

            logger.debug("收到 Modbus 响应: {}", BinaryProtocol.bytesToHex(modbusResponse));

            // 4. 验证响应帧
            if (!ModbusProtocol.verifyCRC(modbusResponse)) {
                logger.error("Modbus 响应 CRC 校验失败");
                return;
            }

            // 5. 解析温湿度数据（可选，用于日志）
            ModbusProtocol.TempHumidityData data = ModbusProtocol.parseTempHumidityResponse(modbusResponse);
            if (data != null) {
                logger.info("采集成功: {}", data);
            } else {
                logger.warn("Modbus 响应解析失败");
            }

            // 6. 将 Modbus 原始响应发送给网关
            gatewayClient.sendModbusData(modbusResponse);

        } catch (Exception e) {
            logger.error("数据采集异常", e);
        }
    }

    /**
     * 解析十六进制地址字符串
     * @param addressStr 地址字符串（如 "0x01" 或 "1"）
     * @return 整数地址
     */
    private int parseHexAddress(String addressStr) {
        if (addressStr.startsWith("0x") || addressStr.startsWith("0X")) {
            return Integer.parseInt(addressStr.substring(2), 16);
        } else {
            return Integer.parseInt(addressStr);
        }
    }

    /**
     * 处理网关下发的 Modbus 命令（配置下发）
     * @param modbusCommand Modbus 命令帧
     * @return Modbus 响应帧
     */
    public byte[] executeModbusCommand(byte[] modbusCommand) {
        try {
            logger.info("执行配置下发命令: {}", BinaryProtocol.bytesToHex(modbusCommand));

            // 检查串口是否打开
            if (!serialPortManager.isOpen()) {
                logger.error("串口未打开，无法执行命令");
                return null;
            }

            // 1. 发送命令到串口
            int written = serialPortManager.write(modbusCommand);
            if (written != modbusCommand.length) {
                logger.error("命令发送不完整: 期望={}, 实际={}", modbusCommand.length, written);
                return null;
            }

            // 2. 等待并读取响应（超时500ms）
            byte[] response = serialPortManager.read(500);

            if (response == null || response.length == 0) {
                logger.warn("未收到命令响应（超时）");
                return null;
            }

            logger.info("收到命令响应: {}", BinaryProtocol.bytesToHex(response));

            // 3. 验证响应
            if (!ModbusProtocol.verifyCRC(response)) {
                logger.error("命令响应 CRC 校验失败");
                return null;
            }

            return response;

        } catch (Exception e) {
            logger.error("执行 Modbus 命令异常", e);
            return null;
        }
    }
}
