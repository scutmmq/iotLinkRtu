package com.scutmmq;

import com.fazecast.jSerialComm.SerialPort;
import com.scutmmq.client.GatewayClient;
import com.scutmmq.manager.SerialPortManager;
import com.scutmmq.manager.impl.JSerialCommManager;
import com.scutmmq.service.DataCollectorService;
import com.scutmmq.utils.MicroConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 应用程序入口
 */
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("   ModBus-RTU 串口采集服务 v2.0");
        System.out.println("==========================================");

        // 1. 列出可用串口
        SerialPort[] ports = SerialPort.getCommPorts();
        System.out.println("检测到系统可用串口:");
        if (ports.length == 0) {
            System.out.println("   [警告] 未检测到任何串口！请确认虚拟串口或驱动是否安装。");
        } else {
            for (SerialPort port : ports) {
                System.out.println("   - " + port.getSystemPortName() + " (" + port.getDescriptivePortName() + ")");
            }
        }
        System.out.println("==========================================");

        // 2. 读取配置
        String rtuId = MicroConfig.readString("rtu.id");
        String gatewayHost = MicroConfig.readString("gateway.host");
        int gatewayPort = MicroConfig.readInt("gateway.port");
        String serialPort = MicroConfig.readString("serial.port");
        int baudRate = MicroConfig.readInt("serial.baudrate");

        logger.info("配置信息:");
        logger.info("  RTU ID: {}", rtuId);
        logger.info("  网关地址: {}:{}", gatewayHost, gatewayPort);
        logger.info("  串口: {}, 波特率: {}", serialPort, baudRate);

        // 3. 初始化串口管理器
        SerialPortManager serialPortManager = new JSerialCommManager();
        if (!serialPortManager.open(serialPort, baudRate)) {
            logger.error("串口打开失败，程序退出");
            System.exit(1);
        }

        // 4. 初始化网关客户端
        GatewayClient gatewayClient = new GatewayClient(gatewayHost, gatewayPort);

        // 5. 初始化数据采集服务
        DataCollectorService dataCollectorService = new DataCollectorService(serialPortManager, gatewayClient);

        // 6. 设置配置下发处理器（将网关命令转发到串口）
        gatewayClient.setModbusCommandHandler(dataCollectorService::executeModbusCommand);

        // 7. 启动网关客户端
        gatewayClient.start();

        // 8. 启动数据采集服务
        dataCollectorService.start();

        logger.info("==========================================");
        logger.info("服务启动成功！");
        logger.info("==========================================");

        // 添加 Shutdown Hook 优雅停机
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("正在关闭服务...");
            dataCollectorService.stop();
            gatewayClient.stop();
            serialPortManager.close();
            logger.info("服务已关闭");
        }));

        // 保持主线程运行
        try {
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            logger.error("主线程被中断", e);
        }
    }
}

