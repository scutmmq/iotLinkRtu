package com.th.serial.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Modbus 协议工具类测试
 */
public class ModbusProtocolTest {

    @Test
    public void testBuildReadTempHumidityCommand() {
        // 构建读取温湿度命令（设备地址 0x01）
        byte[] command = ModbusProtocol.buildReadTempHumidityCommand(0x01);

        // 验证命令长度
        assertEquals(8, command.length, "Modbus 命令长度应为 8 字节");

        // 验证地址码
        assertEquals(0x01, command[0], "设备地址应为 0x01");

        // 验证功能码
        assertEquals(0x03, command[1], "功能码应为 0x03（读保持寄存器）");

        // 验证起始地址
        assertEquals(0x00, command[2], "起始地址高字节应为 0x00");
        assertEquals(0x00, command[3], "起始地址低字节应为 0x00");

        // 验证数据长度
        assertEquals(0x00, command[4], "数据长度高字节应为 0x00");
        assertEquals(0x02, command[5], "数据长度低字节应为 0x02");

        // 验证 CRC（已知正确的 CRC 值）
        assertEquals((byte) 0xC4, command[6], "CRC 低字节应为 0xC4");
        assertEquals((byte) 0x0B, command[7], "CRC 高字节应为 0x0B");

        System.out.println("✓ 读取温湿度命令构建测试通过");
        System.out.println("  命令: " + bytesToHex(command));
    }

    @Test
    public void testBuildWriteRegisterCommand() {
        // 构建修改波特率命令（设备地址 0x01，寄存器 0x07D1，值 0x0002）
        byte[] command = ModbusProtocol.buildWriteRegisterCommand(0x01, 0x07D1, 0x0002);

        // 验证命令长度
        assertEquals(8, command.length, "Modbus 写命令长度应为 8 字节");

        // 验证地址码
        assertEquals(0x01, command[0], "设备地址应为 0x01");

        // 验证功能码
        assertEquals(0x06, command[1], "功能码应为 0x06（写单个寄存器）");

        // 验证寄存器地址
        assertEquals(0x07, command[2], "寄存器地址高字节应为 0x07");
        assertEquals((byte) 0xD1, command[3], "寄存器地址低字节应为 0xD1");

        // 验证写入值
        assertEquals(0x00, command[4], "写入值高字节应为 0x00");
        assertEquals(0x02, command[5], "写入值低字节应为 0x02");

        // 验证 CRC（已知正确的 CRC 值）
        assertEquals((byte) 0x88, command[6], "CRC 低字节应为 0x88");
        assertEquals((byte) 0x3A, command[7], "CRC 高字节应为 0x3A");

        System.out.println("✓ 写寄存器命令构建测试通过");
        System.out.println("  命令: " + bytesToHex(command));
    }

    @Test
    public void testVerifyCRC_ValidResponse() {
        // 正确的 Modbus 响应帧（温度-10.1℃，湿度65.8%RH）
        byte[] response = new byte[]{
            0x01, 0x03, 0x04, 0x02, (byte) 0x92, (byte) 0xFF, (byte) 0x9B, 0x5A, 0x3D
        };

        assertTrue(ModbusProtocol.verifyCRC(response), "CRC 校验应通过");
        System.out.println("✓ CRC 校验测试通过（有效响应）");
    }

    @Test
    public void testVerifyCRC_InvalidResponse() {
        // 错误的 CRC
        byte[] response = new byte[]{
            0x01, 0x03, 0x04, 0x02, (byte) 0x92, (byte) 0xFF, (byte) 0x9B, 0x00, 0x00
        };

        assertFalse(ModbusProtocol.verifyCRC(response), "CRC 校验应失败");
        System.out.println("✓ CRC 校验测试通过（无效响应）");
    }

    @Test
    public void testParseTempHumidityResponse_PositiveTemp() {
        // 温度 26.5℃，湿度 58.4%RH
        // 湿度：584 = 0x0248
        // 温度：265 = 0x0109
        byte[] response = new byte[]{
            0x01, 0x03, 0x04, 0x02, 0x48, 0x01, 0x09, 0x00, 0x00
        };

        // 计算正确的 CRC
        byte[] crc = ModbusProtocol.calculateCRC16(response, 0, 7);
        response[7] = crc[0];
        response[8] = crc[1];

        ModbusProtocol.TempHumidityData data = ModbusProtocol.parseTempHumidityResponse(response);

        assertNotNull(data, "解析结果不应为 null");
        assertEquals(1, data.getDeviceAddress(), "设备地址应为 1");
        assertEquals(58.4, data.getHumidity(), 0.01, "湿度应为 58.4%RH");
        assertEquals(26.5, data.getTemperature(), 0.01, "温度应为 26.5℃");

        System.out.println("✓ 温湿度解析测试通过（正温度）");
        System.out.println("  " + data);
    }

    @Test
    public void testParseTempHumidityResponse_NegativeTemp() {
        // 温度 -10.1℃，湿度 65.8%RH
        // 湿度：658 = 0x0292
        // 温度：-101 = 0xFF9B（补码）
        byte[] response = new byte[]{
            0x01, 0x03, 0x04, 0x02, (byte) 0x92, (byte) 0xFF, (byte) 0x9B, 0x5A, 0x3D
        };

        ModbusProtocol.TempHumidityData data = ModbusProtocol.parseTempHumidityResponse(response);

        assertNotNull(data, "解析结果不应为 null");
        assertEquals(1, data.getDeviceAddress(), "设备地址应为 1");
        assertEquals(65.8, data.getHumidity(), 0.01, "湿度应为 65.8%RH");
        assertEquals(-10.1, data.getTemperature(), 0.01, "温度应为 -10.1℃");

        System.out.println("✓ 温湿度解析测试通过（负温度）");
        System.out.println("  " + data);
    }

    @Test
    public void testParseTempHumidityResponse_InvalidLength() {
        // 长度不正确的响应
        byte[] response = new byte[]{0x01, 0x03, 0x04};

        ModbusProtocol.TempHumidityData data = ModbusProtocol.parseTempHumidityResponse(response);

        assertNull(data, "长度不正确的响应应返回 null");
        System.out.println("✓ 无效长度测试通过");
    }

    @Test
    public void testParseTempHumidityResponse_InvalidFunctionCode() {
        // 功能码错误
        byte[] response = new byte[]{
            0x01, 0x06, 0x04, 0x02, (byte) 0x92, (byte) 0xFF, (byte) 0x9B, 0x00, 0x00
        };

        ModbusProtocol.TempHumidityData data = ModbusProtocol.parseTempHumidityResponse(response);

        assertNull(data, "功能码错误的响应应返回 null");
        System.out.println("✓ 无效功能码测试通过");
    }

    @Test
    public void testParseTempHumidityResponse_InvalidCRC() {
        // CRC 错误
        byte[] response = new byte[]{
            0x01, 0x03, 0x04, 0x02, (byte) 0x92, (byte) 0xFF, (byte) 0x9B, 0x00, 0x00
        };

        ModbusProtocol.TempHumidityData data = ModbusProtocol.parseTempHumidityResponse(response);

        assertNull(data, "CRC 错误的响应应返回 null");
        System.out.println("✓ 无效 CRC 测试通过");
    }

    @Test
    public void testCalculateCRC16() {
        // 测试已知的 CRC 计算
        byte[] data = new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x02};
        byte[] crc = ModbusProtocol.calculateCRC16(data, 0, 6);

        assertEquals((byte) 0xC4, crc[0], "CRC 低字节应为 0xC4");
        assertEquals((byte) 0x0B, crc[1], "CRC 高字节应为 0x0B");

        System.out.println("✓ CRC16 计算测试通过");
        System.out.println("  输入: " + bytesToHex(data));
        System.out.println("  CRC: " + bytesToHex(crc));
    }

    @Test
    public void testEdgeCases() {
        // 测试边界值
        System.out.println("\n=== 边界值测试 ===");

        // 温度 0℃
        byte[] response1 = new byte[]{0x01, 0x03, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        byte[] crc1 = ModbusProtocol.calculateCRC16(response1, 0, 7);
        response1[7] = crc1[0];
        response1[8] = crc1[1];
        ModbusProtocol.TempHumidityData data1 = ModbusProtocol.parseTempHumidityResponse(response1);
        assertNotNull(data1);
        assertEquals(0.0, data1.getTemperature(), 0.01);
        assertEquals(0.0, data1.getHumidity(), 0.01);
        System.out.println("  ✓ 温度 0℃，湿度 0%: " + data1);

        // 温度 100℃，湿度 100%
        byte[] response2 = new byte[]{0x01, 0x03, 0x04, 0x03, (byte) 0xE8, 0x03, (byte) 0xE8, 0x00, 0x00};
        byte[] crc2 = ModbusProtocol.calculateCRC16(response2, 0, 7);
        response2[7] = crc2[0];
        response2[8] = crc2[1];
        ModbusProtocol.TempHumidityData data2 = ModbusProtocol.parseTempHumidityResponse(response2);
        assertNotNull(data2);
        assertEquals(100.0, data2.getTemperature(), 0.01);
        assertEquals(100.0, data2.getHumidity(), 0.01);
        System.out.println("  ✓ 温度 100℃，湿度 100%: " + data2);

        // 温度 -40℃（极低温）
        byte[] response3 = new byte[]{0x01, 0x03, 0x04, 0x00, 0x00, (byte) 0xFE, 0x70, 0x00, 0x00};
        byte[] crc3 = ModbusProtocol.calculateCRC16(response3, 0, 7);
        response3[7] = crc3[0];
        response3[8] = crc3[1];
        ModbusProtocol.TempHumidityData data3 = ModbusProtocol.parseTempHumidityResponse(response3);
        assertNotNull(data3);
        assertEquals(-40.0, data3.getTemperature(), 0.01);
        System.out.println("  ✓ 温度 -40℃: " + data3);
    }

    // 辅助方法：字节数组转十六进制字符串
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
