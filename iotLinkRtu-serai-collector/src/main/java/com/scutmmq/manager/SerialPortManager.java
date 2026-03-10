package com.scutmmq.manager;

/**
 * 串口管理器接口
 * 负责串口的打开、关闭、读取和写入操作
 */
public interface SerialPortManager {

    /**
     * 打开串口
     * @param portName 端口名称（如"COM3"）
     * @param baudRate 波特率（如 9600）
     * @return true 如果打开成功
     */
    boolean open(String portName, int baudRate);

    /**
     * 从串口读取数据
     * @param timeout 超时时间（毫秒）
     * @return 读取的字节数组，如果读取失败或超时返回空数组
     */
    byte[] read(int timeout);

    /**
     * 向串口写入数据
     * @param data 要发送的数据
     * @return 实际写入的字节数
     */
    int write(byte[] data);

    /**
     * 关闭串口
     */
    void close();

    /**
     * 检查串口是否打开
     * @return true 如果串口已打开
     */
    boolean isOpen();
}

