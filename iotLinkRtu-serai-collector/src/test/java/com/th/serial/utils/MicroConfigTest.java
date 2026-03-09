package com.th.serial.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 配置读取工具类测试
 */
public class MicroConfigTest {

    @Test
    public void testReadString() {
        String rtuId = MicroConfig.readString("rtu.id");
        assertNotNull(rtuId, "rtu.id 不应为 null");
        assertFalse(rtuId.isEmpty(), "rtu.id 不应为空");

        System.out.println("✓ 读取字符串配置测试通过");
        System.out.println("  rtu.id = " + rtuId);
    }

    @Test
    public void testReadStringWithDefault() {
        String value = MicroConfig.readString("non.existent.key", "default-value");
        assertEquals("default-value", value, "不存在的键应返回默认值");

        System.out.println("✓ 读取字符串配置（带默认值）测试通过");
    }

    @Test
    public void testReadInt() {
        int port = MicroConfig.readInt("gateway.port");
        assertTrue(port > 0, "端口号应大于 0");

        System.out.println("✓ 读取整数配置测试通过");
        System.out.println("  gateway.port = " + port);
    }

    @Test
    public void testReadIntWithDefault() {
        int value = MicroConfig.readInt("non.existent.key", 9999);
        assertEquals(9999, value, "不存在的键应返回默认值");

        System.out.println("✓ 读取整数配置（带默认值）测试通过");
    }

    @Test
    public void testReadIntInvalidFormat() {
        // 测试无效格式（应返回默认值）
        int value = MicroConfig.readInt("rtu.id", 0); // rtu.id 是字符串，无法解析为整数
        assertEquals(0, value, "无效格式应返回默认值");

        System.out.println("✓ 读取整数配置（无效格式）测试通过");
    }

    @Test
    public void testReadBaudRate() {
        int baudRate = MicroConfig.readInt("serial.baudrate");
        assertTrue(baudRate > 0, "波特率应大于 0");

        System.out.println("✓ 读取波特率配置测试通过");
        System.out.println("  serial.baudrate = " + baudRate);
    }

    @Test
    public void testReadSerialPort() {
        String port = MicroConfig.readString("serial.port");
        assertNotNull(port, "串口配置不应为 null");

        System.out.println("✓ 读取串口配置测试通过");
        System.out.println("  serial.port = " + port);
    }

    @Test
    public void testReadModbusAddress() {
        String address = MicroConfig.readString("modbus.device.address");
        assertNotNull(address, "Modbus 设备地址不应为 null");

        System.out.println("✓ 读取 Modbus 设备地址测试通过");
        System.out.println("  modbus.device.address = " + address);
    }

    @Test
    public void testReadCollectInterval() {
        int interval = MicroConfig.readInt("collect.interval");
        assertTrue(interval > 0, "采集间隔应大于 0");

        System.out.println("✓ 读取采集间隔配置测试通过");
        System.out.println("  collect.interval = " + interval + " 秒");
    }

    @Test
    public void testContainsKey() {
        assertTrue(MicroConfig.containsKey("rtu.id"), "应包含 rtu.id 键");
        assertFalse(MicroConfig.containsKey("non.existent.key"), "不应包含不存在的键");

        System.out.println("✓ 检查键存在性测试通过");
    }

    @Test
    public void testReadAllConfigs() {
        System.out.println("\n=== 读取所有配置 ===");

        String rtuId = MicroConfig.readString("rtu.id");
        String secret = MicroConfig.readString("rtu.secret");
        String gatewayHost = MicroConfig.readString("gateway.host");
        int gatewayPort = MicroConfig.readInt("gateway.port");
        String serialPort = MicroConfig.readString("serial.port");
        int baudRate = MicroConfig.readInt("serial.baudrate");
        String modbusAddress = MicroConfig.readString("modbus.device.address");
        int collectInterval = MicroConfig.readInt("collect.interval");

        System.out.println("  rtu.id = " + rtuId);
        System.out.println("  rtu.secret = " + (secret != null ? "***" : "null"));
        System.out.println("  gateway.host = " + gatewayHost);
        System.out.println("  gateway.port = " + gatewayPort);
        System.out.println("  serial.port = " + serialPort);
        System.out.println("  serial.baudrate = " + baudRate);
        System.out.println("  modbus.device.address = " + modbusAddress);
        System.out.println("  collect.interval = " + collectInterval);

        assertNotNull(rtuId);
        assertNotNull(secret);
        assertNotNull(gatewayHost);
        assertTrue(gatewayPort > 0);
        assertNotNull(serialPort);
        assertTrue(baudRate > 0);
        assertNotNull(modbusAddress);
        assertTrue(collectInterval > 0);

        System.out.println("✓ 所有配置读取测试通过");
    }
}
