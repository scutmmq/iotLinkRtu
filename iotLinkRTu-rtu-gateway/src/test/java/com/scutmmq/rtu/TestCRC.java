package com.scutmmq.rtu;

import com.scutmmq.rtu.utils.ModBusUtil;

class TestCRC {
    public static void main(String[] args) {
        // 测试数据：包含 CRC 的完整帧
        byte[] data = {
                0x01, // 地址码
                0x03, // 功能码
                0x04, // 数据长度
                0x02, (byte) 0x92, // 湿度
                (byte) 0xFF,(byte) 0x9B, // 温度
                (byte) 0xDC,  // CRC 低位
                (byte) 0xC3   // CRC 高位
        };

        // 方法 1：计算前 6 个字节的 CRC
        short calculatedCrc = ModBusUtil.calculateCrc16(data, 0, 7);
        System.out.println("计算的 CRC: " + String.format("%04X", calculatedCrc & 0xFFFF));

        // 方法 2：验证整个帧的 CRC（如果 CRC 正确，对完整数据计算应得 0）
        boolean isValid = ModBusUtil.verifyCrc16(data);
        System.out.println("CRC 验证结果：" + (isValid ? "✓ 正确" : "✗ 错误"));
    }

}