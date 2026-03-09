package com.scutmmq.rtu.utils;

/**
 * ModBus 工具类 - CRC16 校验计算
 * 
 * <p>提供 ModBus-RTU 协议标准的 CRC16 校验码计算和验证功能</p>
 * <p>采用多项式：0xA001（反转后的 0x8005）</p>
 * 
 * @author mo.mingqin@xlink
 * @since 2026-03-08
 */
public class ModBusUtil {

    /**
     * 计算 CRC16（ModBus-RTU 标准）
     *
     * @param data 数据字节数组
     * @return CRC16 校验码
     */
    public static short calculateCrc16(byte[] data) {
        return calculateCrc16(data, 0, data.length);
    }

    /**
     * 计算指定范围的 CRC16
     *
     * @param data 数据字节数组
     * @param offset 起始偏移量
     * @param length 长度
     * @return CRC16 校验码
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
     * 验证 CRC16
     *
     * <p>检查数据末尾的 CRC 校验码是否正确</p>
     *
     * @param data 包含 CRC 的完整数据字节数组（最后两字节为 CRC）
     * @return true 如果 CRC 正确，否则 false
     */
    public static boolean verifyCrc16(byte[] data) {
        if (data.length < 2) {
            return false;
        }

        // 提取接收到的 CRC（低字节在前）
        short receivedCrc = (short) ((data[data.length - 1] << 8) | (data[data.length - 2] & 0xFF));

        // 计算数据的 CRC
        short calculatedCrc = calculateCrc16(data, 0, data.length - 2);

        return receivedCrc == calculatedCrc;
    }
}
