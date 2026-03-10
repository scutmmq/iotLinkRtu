package com.scutmmq.manager.impl;

import com.fazecast.jSerialComm.SerialPort;
import com.scutmmq.manager.SerialPortManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于 jSerialComm 的串口管理器实现
 */
public class JSerialCommManager implements SerialPortManager {
    private static final Logger logger = LoggerFactory.getLogger(JSerialCommManager.class);

    private SerialPort serialPort;
    private final Object lock = new Object();

    @Override
    public boolean open(String portName, int baudRate) {
        synchronized (lock) {
            try {
                // 打印当前所有可用串口，方便调试
                SerialPort[] commPorts = SerialPort.getCommPorts();
                String availablePorts = (commPorts.length > 0)
                    ? java.util.Arrays.stream(commPorts).map(SerialPort::getSystemPortName).collect(java.util.stream.Collectors.joining(", "))
                    : "无";
                logger.info("系统当前可用串口: [{}]", availablePorts);

                if (isOpen()) {
                    logger.warn("串口 {} 已经打开，将先关闭再重新打开", portName);
                    close();
                }

                serialPort = SerialPort.getCommPort(portName);
                if (serialPort == null) {
                    logger.error("无法找到串口对象: {}", portName);
                    return false;
                }

                // 尝试打开串口
                // 注意：某些情况下，建议先打开再配置参数，或者反之。jSerialComm 通常建议配置后打开。
                // 使用 setComPortParameters 一次性设置
                serialPort.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);

                // 设置流控制为无
                serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

                if (serialPort.openPort()) {
                    logger.info("串口 {} 打开成功，波特率: {}", portName, baudRate);
                    // 再次确认参数设置（有些驱动在打开后会重置）
                    serialPort.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);

                    // 某些设备/虚拟驱动器可能需要 DTR/RTS 信号才能开始正常收发
                    serialPort.setDTR();
                    serialPort.setRTS();

                    return true;
                } else {
                    logger.error("串口 {} 打开失败 (可能被占用或不存在)", portName);
                    return false;
                }
            } catch (Exception e) {
                logger.error("打开串口异常: {}", e.getMessage(), e);
                return false;
            }
        }
    }

    @Override
    public byte[] read(int timeout) {
        synchronized (lock) { // Use synchronized properly
            if (serialPort == null || !serialPort.isOpen()) {
                logger.warn("尝试从关闭的串口读取数据");
                return new byte[0];
            }

            try {
                // 1. 设置半阻塞模式，超时时间为 timeout
                // SEMI_BLOCKING: 会阻塞直到至少读到一个字节，或者超时
                serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeout, 0);

                // 2. 尝试读取第一个数据块（这步是阻塞的）
                byte[] tempBuffer = new byte[1024];
                int bytesRead = serialPort.readBytes(tempBuffer, tempBuffer.length);

                if (bytesRead <= 0) {
                    return new byte[0]; // 超时，确实没收到任何数据
                }

                // 3. 读到了数据！ModBus 帧往往是连续发送的
                // 稍微等待一下（50ms），让剩余的帧字节（如果存在）到达接收缓冲区
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {}

                // 4. 检查是否还有更多数据到达
                // 将刚刚读到的和后面到达的合并
                int available = serialPort.bytesAvailable();
                if (available > 0) {
                    byte[] extraData = new byte[available];
                    int extraRead = serialPort.readBytes(extraData, available);

                    if (extraRead > 0) {
                        byte[] fullData = new byte[bytesRead + extraRead];
                        System.arraycopy(tempBuffer, 0, fullData, 0, bytesRead);
                        System.arraycopy(extraData, 0, fullData, bytesRead, extraRead);
                        return fullData;
                    }
                }

                // 仅返回第一次读到的数据
                byte[] result = new byte[bytesRead];
                System.arraycopy(tempBuffer, 0, result, 0, bytesRead);
                return result;

            } catch (Exception e) {
                logger.error("读取串口数据异常: {}", e.getMessage(), e);
                return new byte[0];
            }
        }
    }

    @Override
    public int write(byte[] data) {
        synchronized (lock) {
            if (!isOpen()) {
                logger.warn("尝试向关闭的串口写入数据");
                return 0;
            }
            try {
                int bytesWritten = serialPort.writeBytes(data, data.length);
                if (bytesWritten == -1) {
                    logger.error("写入串口失败");
                    return 0;
                }
                return bytesWritten;
            } catch (Exception e) {
                logger.error("写入串口异常: {}", e.getMessage(), e);
                return 0;
            }
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
                logger.info("串口已关闭");
            }
            serialPort = null;
        }
    }

    @Override
    public boolean isOpen() {
        return serialPort != null && serialPort.isOpen();
    }
}

