package com.scutmmq.integration;

import com.scutmmq.protocol.BinaryProtocol;
import com.scutmmq.protocol.ModbusProtocol;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据采集流程集成测试
 * 模拟完整的数据采集、封装、传输流程
 */
public class DataCollectionFlowTest {

    @Test
    public void testCompleteDataCollectionFlow() {
        System.out.println("\n=== 完整数据采集流程测试 ===");

        // 1. 构建 Modbus 读取命令
        System.out.println("\n步骤1: 构建 Modbus 读取温湿度命令");
        int deviceAddress = 0x01;
        byte[] modbusCommand = ModbusProtocol.buildReadTempHumidityCommand(deviceAddress);
        System.out.println("  Modbus 命令: " + BinaryProtocol.bytesToHex(modbusCommand));
        assertEquals(8, modbusCommand.length);

        // 2. 模拟设备响应（温度 26.5℃，湿度 58.4%RH）
        System.out.println("\n步骤2: 模拟设备返回 Modbus 响应");
        byte[] modbusResponse = buildMockModbusResponse(26.5, 58.4);
        System.out.println("  Modbus 响应: " + BinaryProtocol.bytesToHex(modbusResponse));
        assertEquals(9, modbusResponse.length);

        // 3. 验证 Modbus 响应的 CRC
        System.out.println("\n步骤3: 验证 Modbus 响应 CRC");
        boolean crcValid = ModbusProtocol.verifyCRC(modbusResponse);
        assertTrue(crcValid, "CRC 校验应通过");
        System.out.println("  CRC 校验: 通过");

        // 4. 解析温湿度数据
        System.out.println("\n步骤4: 解析温湿度数据");
        ModbusProtocol.TempHumidityData data = ModbusProtocol.parseTempHumidityResponse(modbusResponse);
        assertNotNull(data);
        assertEquals(26.5, data.getTemperature(), 0.01);
        assertEquals(58.4, data.getHumidity(), 0.01);
        System.out.println("  解析结果: " + data);

        // 5. 封装为二进制帧
        System.out.println("\n步骤5: 封装为二进制帧发送给网关");
        byte[] binaryFrame = BinaryProtocol.buildModbusDataFrame(modbusResponse);
        System.out.println("  二进制帧长度: " + binaryFrame.length + " 字节");
        System.out.println("  二进制帧: " + BinaryProtocol.bytesToHex(binaryFrame));

        // 6. 验证二进制帧结构
        System.out.println("\n步骤6: 验证二进制帧结构");
        assertEquals((byte) 0xAA, binaryFrame[0], "帧头第一字节");
        assertEquals((byte) 0x55, binaryFrame[1], "帧头第二字节");
        assertEquals(BinaryProtocol.TYPE_MODBUS_DATA, binaryFrame[2], "帧类型");
        int dataLength = ((binaryFrame[3] & 0xFF) << 8) | (binaryFrame[4] & 0xFF);
        assertEquals(9, dataLength, "数据长度");
        System.out.println("  ✓ 二进制帧结构验证通过");

        System.out.println("\n✓ 完整数据采集流程测试通过");
    }

    @Test
    public void testAuthenticationFlow() {
        System.out.println("\n=== 认证流程测试 ===");

        // 1. 构建认证请求
        System.out.println("\n步骤1: 构建认证请求帧");
        String rtuId = "RTU-001";
        String secret = "abc123xyz";
        byte[] authFrame = BinaryProtocol.buildAuthRequest(rtuId, secret);
        System.out.println("  认证帧长度: " + authFrame.length + " 字节");
        System.out.println("  认证帧（前50字节）: " + BinaryProtocol.bytesToHex(
            java.util.Arrays.copyOf(authFrame, Math.min(50, authFrame.length))));

        // 2. 验证认证帧结构
        System.out.println("\n步骤2: 验证认证帧结构");
        assertEquals((byte) 0xAA, authFrame[0]);
        assertEquals((byte) 0x55, authFrame[1]);
        assertEquals(BinaryProtocol.TYPE_AUTH_REQUEST, authFrame[2]);
        int dataLength = ((authFrame[3] & 0xFF) << 8) | (authFrame[4] & 0xFF);
        assertEquals(48, dataLength, "认证数据长度应为 48 字节");

        // 3. 提取 rtuId
        System.out.println("\n步骤3: 提取 rtuId");
        byte[] rtuIdBytes = new byte[16];
        System.arraycopy(authFrame, 5, rtuIdBytes, 0, 16);
        String extractedRtuId = new String(rtuIdBytes).trim().replace("\0", "");
        assertEquals(rtuId, extractedRtuId);
        System.out.println("  提取的 rtuId: " + extractedRtuId);

        // 4. 验证 secret 哈希存在
        System.out.println("\n步骤4: 验证 secret 哈希");
        byte[] secretHash = new byte[32];
        System.arraycopy(authFrame, 21, secretHash, 0, 32);
        assertNotEquals(0, secretHash[0], "secret 哈希不应全为 0");
        System.out.println("  secret 哈希（前16字节）: " + BinaryProtocol.bytesToHex(
            java.util.Arrays.copyOf(secretHash, 16)));

        System.out.println("\n✓ 认证流程测试通过");
    }

    @Test
    public void testConfigDeliveryFlow() {
        System.out.println("\n=== 配置下发流程测试 ===");

        // 1. 构建 Modbus 写寄存器命令（修改波特率为 9600）
        System.out.println("\n步骤1: 构建 Modbus 写寄存器命令");
        int deviceAddress = 0x01;
        int registerAddress = 0x07D1; // 波特率寄存器
        int value = 0x0002; // 9600 波特率代码
        byte[] modbusCommand = ModbusProtocol.buildWriteRegisterCommand(deviceAddress, registerAddress, value);
        System.out.println("  Modbus 写命令: " + BinaryProtocol.bytesToHex(modbusCommand));
        assertEquals(8, modbusCommand.length);

        // 2. 封装为二进制帧（网关下发）
        System.out.println("\n步骤2: 封装为二进制命令帧");
        byte[] commandFrame = new byte[modbusCommand.length + 6];
        commandFrame[0] = (byte) 0xAA;
        commandFrame[1] = (byte) 0x55;
        commandFrame[2] = BinaryProtocol.TYPE_MODBUS_COMMAND;
        commandFrame[3] = 0x00;
        commandFrame[4] = (byte) modbusCommand.length;
        System.arraycopy(modbusCommand, 0, commandFrame, 5, modbusCommand.length);

        // 计算校验和
        byte checksum = 0;
        for (int i = 0; i < commandFrame.length - 1; i++) {
            checksum ^= commandFrame[i];
        }
        commandFrame[commandFrame.length - 1] = checksum;

        System.out.println("  命令帧: " + BinaryProtocol.bytesToHex(commandFrame));

        // 3. collector 解析命令帧，提取 Modbus 命令
        System.out.println("\n步骤3: collector 解析命令帧");
        assertEquals((byte) 0xAA, commandFrame[0]);
        assertEquals((byte) 0x55, commandFrame[1]);
        assertEquals(BinaryProtocol.TYPE_MODBUS_COMMAND, commandFrame[2]);

        int cmdLength = ((commandFrame[3] & 0xFF) << 8) | (commandFrame[4] & 0xFF);
        byte[] extractedCommand = new byte[cmdLength];
        System.arraycopy(commandFrame, 5, extractedCommand, 0, cmdLength);
        assertArrayEquals(modbusCommand, extractedCommand);
        System.out.println("  提取的 Modbus 命令: " + BinaryProtocol.bytesToHex(extractedCommand));

        // 4. 模拟设备响应（写成功，原样返回）
        System.out.println("\n步骤4: 设备响应（写成功）");
        byte[] deviceResponse = modbusCommand.clone();
        System.out.println("  设备响应: " + BinaryProtocol.bytesToHex(deviceResponse));

        // 5. 封装响应为数据帧返回网关
        System.out.println("\n步骤5: 封装响应返回网关");
        byte[] responseFrame = BinaryProtocol.buildModbusDataFrame(deviceResponse);
        System.out.println("  响应帧: " + BinaryProtocol.bytesToHex(responseFrame));

        System.out.println("\n✓ 配置下发流程测试通过");
    }

    @Test
    public void testHeartbeatFlow() {
        System.out.println("\n=== 心跳流程测试 ===");

        // 1. 构建心跳请求
        System.out.println("\n步骤1: 构建心跳请求帧");
        byte[] heartbeatFrame = BinaryProtocol.buildHeartbeatRequest();
        System.out.println("  心跳帧: " + BinaryProtocol.bytesToHex(heartbeatFrame));
        assertEquals(11, heartbeatFrame.length);

        // 2. 验证心跳帧结构
        System.out.println("\n步骤2: 验证心跳帧结构");
        assertEquals((byte) 0xAA, heartbeatFrame[0]);
        assertEquals((byte) 0x55, heartbeatFrame[1]);
        assertEquals(BinaryProtocol.TYPE_HEARTBEAT_REQUEST, heartbeatFrame[2]);

        int dataLength = ((heartbeatFrame[3] & 0xFF) << 8) | (heartbeatFrame[4] & 0xFF);
        assertEquals(4, dataLength, "心跳数据长度应为 4 字节");

        // 3. 提取时间戳
        System.out.println("\n步骤3: 提取时间戳");
        int timestamp = ((heartbeatFrame[5] & 0xFF) << 24) | ((heartbeatFrame[6] & 0xFF) << 16)
                      | ((heartbeatFrame[7] & 0xFF) << 8) | (heartbeatFrame[8] & 0xFF);
        assertTrue(timestamp > 0);
        System.out.println("  时间戳: " + timestamp);

        System.out.println("\n✓ 心跳流程测试通过");
    }

    @Test
    public void testMultipleDataCollectionCycles() {
        System.out.println("\n=== 多次数据采集周期测试 ===");

        double[][] testData = {
            {25.0, 60.0},
            {26.5, 58.4},
            {-10.1, 65.8},
            {0.0, 0.0},
            {100.0, 100.0}
        };

        for (int i = 0; i < testData.length; i++) {
            double temp = testData[i][0];
            double humidity = testData[i][1];

            System.out.println("\n周期 " + (i + 1) + ": 温度=" + temp + "℃, 湿度=" + humidity + "%RH");

            // 构建 Modbus 响应
            byte[] modbusResponse = buildMockModbusResponse(temp, humidity);

            // 验证 CRC
            assertTrue(ModbusProtocol.verifyCRC(modbusResponse));

            // 解析数据
            ModbusProtocol.TempHumidityData data = ModbusProtocol.parseTempHumidityResponse(modbusResponse);
            assertNotNull(data);
            assertEquals(temp, data.getTemperature(), 0.01);
            assertEquals(humidity, data.getHumidity(), 0.01);

            // 封装为二进制帧
            byte[] binaryFrame = BinaryProtocol.buildModbusDataFrame(modbusResponse);
            assertNotNull(binaryFrame);
            assertTrue(binaryFrame.length > 0);

            System.out.println("  ✓ 数据采集成功: " + data);
        }

        System.out.println("\n✓ 多次数据采集周期测试通过");
    }

    /**
     * 构建模拟的 Modbus 响应帧
     * @param temperature 温度（℃）
     * @param humidity 湿度（%RH）
     * @return Modbus 响应帧
     */
    private byte[] buildMockModbusResponse(double temperature, double humidity) {
        // 温湿度值扩大10倍
        int humidityValue = (int) (humidity * 10);
        int tempValue = (int) (temperature * 10);

        // 处理负温度（补码）
        if (tempValue < 0) {
            tempValue = 65536 + tempValue;
        }

        byte[] response = new byte[9];
        response[0] = 0x01; // 设备地址
        response[1] = 0x03; // 功能码
        response[2] = 0x04; // 字节数
        response[3] = (byte) ((humidityValue >> 8) & 0xFF); // 湿度高字节
        response[4] = (byte) (humidityValue & 0xFF);        // 湿度低字节
        response[5] = (byte) ((tempValue >> 8) & 0xFF);     // 温度高字节
        response[6] = (byte) (tempValue & 0xFF);            // 温度低字节

        // 计算 CRC
        byte[] crc = ModbusProtocol.calculateCRC16(response, 0, 7);
        response[7] = crc[0];
        response[8] = crc[1];

        return response;
    }
}
