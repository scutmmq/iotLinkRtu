package com.scutmmq.utils;

/**
 * ModBus CRC16 校验工具类
 */
public class Crc16Util {

    /**
     * 计算 CRC16 校验码
     * @param data 数据
     * @return 校验码
     */
    public static short calculateCrc16(byte[] data) {
        return calculateCrc16(data, 0, data.length);
    }

    /**
     * 计算 CRC16 校验码
     * @param data 数据
     * @param offset 起始位置
     * @param length 长度
     * @return 校验码
     */
    public static short calculateCrc16(byte[] data, int offset, int length) {
        short crc = (short) 0xFFFF;

        for (int i = offset; i < offset + length; i++) {
            crc ^= (short) (data[i] & 0xFF);

            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc >>>= 1;
                    crc ^= (short) 0xA001;
                } else {
                    crc >>>= 1;
                }
            }
        }

        return crc;
    }

    /**
     * 校验数据是否符合 CRC16
     * @param data 包含 CRC 的完整数据包
     * @return true 如果校验通过
     */
    public static boolean checkCrc16(byte[] data) {
        if (data == null || data.length < 2) {
            return false;
        }

        // 提取接收到的 CRC（低字节在前）
        short receivedCrc = (short) ((data[data.length - 1] << 8) | (data[data.length - 2] & 0xFF));

        // 计算数据的 CRC
        short calculatedCrc = calculateCrc16(data, 0, data.length - 2);

        return receivedCrc == calculatedCrc;
    }

    /**
     * 将 CRC 追加到数据末尾
     * @param data 原始数据
     * @return 带有 CRC 的新数组
     */
    public static byte[] appendCrc16(byte[] data) {
        short crc = calculateCrc16(data);
        byte[] result = new byte[data.length + 2];
        System.arraycopy(data, 0, result, 0, data.length);

        // ModBus RTU CRC 是低位在前，高位在后
        result[data.length] = (byte) (crc & 0xFF);     // 低位
        result[data.length + 1] = (byte) ((crc >> 8) & 0xFF); // 高位

        return result;
    }
}

