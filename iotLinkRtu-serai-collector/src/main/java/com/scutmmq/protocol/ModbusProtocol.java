package com.scutmmq.protocol;

import java.nio.ByteBuffer;

/**
 * Modbus-RTU 协议工具类
 * 用于构建和解析 Modbus 命令和响应
 */
public class ModbusProtocol {

    /**
     * 构建读取温湿度的 Modbus 命令（功能码 0x03）
     * 读取寄存器地址 0x0000 开始的 2 个寄存器（湿度 + 温度）
     *
     * @param deviceAddress 设备地址（0x01-0xFE）
     * @return Modbus-RTU 命令帧
     */
    public static byte[] buildReadTempHumidityCommand(int deviceAddress) {
        ByteBuffer buffer = ByteBuffer.allocate(8);

        // 地址码
        buffer.put((byte) deviceAddress);

        // 功能码：0x03（读保持寄存器）
        buffer.put((byte) 0x03);

        // 起始地址：0x0000（湿度寄存器）
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);

        // 数据长度：0x0002（读取2个寄存器：湿度 + 温度）
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x02);

        // 计算 CRC16 校验
        byte[] crc = calculateCRC16(buffer.array(), 0, 6);
        buffer.put(crc[0]); // CRC 低字节
        buffer.put(crc[1]); // CRC 高字节

        return buffer.array();
    }

    /**
     * 构建修改设备配置的 Modbus 命令（功能码 0x06）
     *
     * @param deviceAddress 设备地址
     * @param registerAddress 寄存器地址
     * @param value 要写入的值
     * @return Modbus-RTU 命令帧
     */
    public static byte[] buildWriteRegisterCommand(int deviceAddress, int registerAddress, int value) {
        ByteBuffer buffer = ByteBuffer.allocate(8);

        // 地址码
        buffer.put((byte) deviceAddress);

        // 功能码：0x06（写单个寄存器）
        buffer.put((byte) 0x06);

        // 寄存器地址（2字节，大端序）
        buffer.put((byte) ((registerAddress >> 8) & 0xFF));
        buffer.put((byte) (registerAddress & 0xFF));

        // 寄存器值（2字节，大端序）
        buffer.put((byte) ((value >> 8) & 0xFF));
        buffer.put((byte) (value & 0xFF));

        // 计算 CRC16 校验
        byte[] crc = calculateCRC16(buffer.array(), 0, 6);
        buffer.put(crc[0]); // CRC 低字节
        buffer.put(crc[1]); // CRC 高字节

        return buffer.array();
    }

    /**
     * 验证 Modbus 响应帧的 CRC 校验
     *
     * @param response Modbus 响应帧
     * @return true 如果校验通过
     */
    public static boolean verifyCRC(byte[] response) {
        if (response == null || response.length < 4) {
            return false;
        }

        int dataLength = response.length - 2;
        byte[] calculatedCRC = calculateCRC16(response, 0, dataLength);

        return response[dataLength] == calculatedCRC[0]
            && response[dataLength + 1] == calculatedCRC[1];
    }

    /**
     * 解析温湿度响应帧
     *
     * @param response Modbus 响应帧
     * @return 温湿度数据对象，如果解析失败返回 null
     */
    public static TempHumidityData parseTempHumidityResponse(byte[] response) {
        // 验证响应帧格式
        // 正常响应：地址码(1) + 功能码(1) + 字节数(1) + 数据(4) + CRC(2) = 9字节
        if (response == null || response.length != 9) {
            return null;
        }

        // 验证功能码
        if (response[1] != 0x03) {
            return null;
        }

        // 验证字节数
        if (response[2] != 0x04) {
            return null;
        }

        // 验证 CRC
        if (!verifyCRC(response)) {
            return null;
        }

        // 提取设备地址
        int deviceAddress = response[0] & 0xFF;

        // 提取湿度值（寄存器0，扩大10倍）
        int humidityRaw = ((response[3] & 0xFF) << 8) | (response[4] & 0xFF);
        double humidity = humidityRaw / 10.0;

        // 提取温度值（寄存器1，扩大10倍，补码表示）
        int tempRaw = ((response[5] & 0xFF) << 8) | (response[6] & 0xFF);
        // 处理补码（负数）
        if (tempRaw > 32767) {
            tempRaw = tempRaw - 65536;
        }
        double temperature = tempRaw / 10.0;

        return new TempHumidityData(deviceAddress, temperature, humidity);
    }

    /**
     * 计算 Modbus CRC16 校验码
     *
     * @param data 数据
     * @param offset 起始位置
     * @param length 长度
     * @return CRC16 校验码（2字节，低字节在前）
     */
    public static byte[] calculateCRC16(byte[] data, int offset, int length) {
        int crc = 0xFFFF;

        for (int i = offset; i < offset + length; i++) {
            crc ^= (data[i] & 0xFF);

            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc >>= 1;
                    crc ^= 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }

        // 返回低字节在前，高字节在后
        return new byte[]{
            (byte) (crc & 0xFF),
            (byte) ((crc >> 8) & 0xFF)
        };
    }

    /**
     * 温湿度数据对象
     */
    public static class TempHumidityData {
        private final int deviceAddress;
        private final double temperature;
        private final double humidity;

        public TempHumidityData(int deviceAddress, double temperature, double humidity) {
            this.deviceAddress = deviceAddress;
            this.temperature = temperature;
            this.humidity = humidity;
        }

        public int getDeviceAddress() {
            return deviceAddress;
        }

        public double getTemperature() {
            return temperature;
        }

        public double getHumidity() {
            return humidity;
        }

        @Override
        public String toString() {
            return String.format("Device[0x%02X] Temp=%.1f℃, Humidity=%.1f%%RH",
                deviceAddress, temperature, humidity);
        }
    }
}
