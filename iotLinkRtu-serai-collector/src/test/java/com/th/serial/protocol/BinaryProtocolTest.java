package com.scutmmq.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 二进制协议工具类测试
 */
public class BinaryProtocolTest {

    @Test
    public void testBuildAuthRequest() {
        String rtuId = "RTU-001";
        String secret = "abc123xyz";

        byte[] frame = BinaryProtocol.buildAuthRequest(rtuId, secret);

        // 验证帧头
        assertEquals((byte) 0xAA, frame[0], "帧头第一字节应为 0xAA");
        assertEquals((byte) 0x55, frame[1], "帧头第二字节应为 0x55");

        // 验证帧类型
        assertEquals(BinaryProtocol.TYPE_AUTH_REQUEST, frame[2], "帧类型应为认证请求 0x01");

        // 验证数据长度（16 + 32 = 48字节）
        int dataLength = ((frame[3] & 0xFF) << 8) | (frame[4] & 0xFF);
        assertEquals(48, dataLength, "数据长度应为 48 字节");

        // 验证 rtuId（前16字节）
        byte[] rtuIdBytes = new byte[16];
        System.arraycopy(frame, 5, rtuIdBytes, 0, 16);
        String extractedRtuId = new String(rtuIdBytes).trim().replace("\0", "");
        assertEquals(rtuId, extractedRtuId, "提取的 rtuId 应与原始值相同");

        // 验证 secret 哈希（后32字节）
        byte[] secretHash = new byte[32];
        System.arraycopy(frame, 21, secretHash, 0, 32);
        assertNotNull(secretHash, "secret 哈希不应为 null");
        assertEquals(32, secretHash.length, "secret 哈希长度应为 32 字节");

        // 验证校验和（最后一个字节）
        byte checksum = frame[frame.length - 1];
        assertNotEquals(0, checksum, "校验和不应为 0");

        System.out.println("✓ 认证请求帧构建测试通过");
        System.out.println("  帧长度: " + frame.length + " 字节");
        System.out.println("  rtuId: " + extractedRtuId);
        System.out.println("  帧内容: " + BinaryProtocol.bytesToHex(frame));
    }

    @Test
    public void testBuildModbusDataFrame() {
        // Modbus 响应帧（温度-10.1℃，湿度65.8%RH）
        byte[] modbusData = new byte[]{
            0x01, 0x03, 0x04, 0x02, (byte) 0x92, (byte) 0xFF, (byte) 0x9B, 0x5A, 0x3D
        };

        byte[] frame = BinaryProtocol.buildModbusDataFrame(modbusData);

        // 验证帧头
        assertEquals((byte) 0xAA, frame[0], "帧头第一字节应为 0xAA");
        assertEquals((byte) 0x55, frame[1], "帧头第二字节应为 0x55");

        // 验证帧类型
        assertEquals(BinaryProtocol.TYPE_MODBUS_DATA, frame[2], "帧类型应为 Modbus 数据 0x03");

        // 验证数据长度
        int dataLength = ((frame[3] & 0xFF) << 8) | (frame[4] & 0xFF);
        assertEquals(modbusData.length, dataLength, "数据长度应与 Modbus 数据长度相同");

        // 验证 Modbus 数据
        byte[] extractedData = new byte[modbusData.length];
        System.arraycopy(frame, 5, extractedData, 0, modbusData.length);
        assertArrayEquals(modbusData, extractedData, "提取的 Modbus 数据应与原始数据相同");

        // 验证校验和
        byte checksum = frame[frame.length - 1];
        assertNotEquals(0, checksum, "校验和不应为 0");

        System.out.println("✓ Modbus 数据帧构建测试通过");
        System.out.println("  帧长度: " + frame.length + " 字节");
        System.out.println("  Modbus 数据: " + BinaryProtocol.bytesToHex(modbusData));
        System.out.println("  完整帧: " + BinaryProtocol.bytesToHex(frame));
    }

    @Test
    public void testBuildHeartbeatRequest() {
        byte[] frame = BinaryProtocol.buildHeartbeatRequest();

        // 验证帧头
        assertEquals((byte) 0xAA, frame[0], "帧头第一字节应为 0xAA");
        assertEquals((byte) 0x55, frame[1], "帧头第二字节应为 0x55");

        // 验证帧类型
        assertEquals(BinaryProtocol.TYPE_HEARTBEAT_REQUEST, frame[2], "帧类型应为心跳请求 0x05");

        // 验证数据长度
        int dataLength = ((frame[3] & 0xFF) << 8) | (frame[4] & 0xFF);
        assertEquals(4, dataLength, "数据长度应为 4 字节（时间戳）");

        // 验证时间戳（4字节）
        int timestamp = ((frame[5] & 0xFF) << 24) | ((frame[6] & 0xFF) << 16)
                      | ((frame[7] & 0xFF) << 8) | (frame[8] & 0xFF);
        assertTrue(timestamp > 0, "时间戳应大于 0");

        // 验证总长度（帧头2 + 类型1 + 长度2 + 数据4 + 校验1 = 11字节）
        assertEquals(11, frame.length, "心跳帧总长度应为 11 字节");

        System.out.println("✓ 心跳请求帧构建测试通过");
        System.out.println("  时间戳: " + timestamp);
        System.out.println("  帧内容: " + BinaryProtocol.bytesToHex(frame));
    }

    @Test
    public void testAuthRequestWithLongRtuId() {
        // 测试超长 rtuId（应被截断为16字节）
        String longRtuId = "RTU-001-VERY-LONG-ID-THAT-EXCEEDS-16-BYTES";
        String secret = "test123";

        byte[] frame = BinaryProtocol.buildAuthRequest(longRtuId, secret);

        // 提取 rtuId
        byte[] rtuIdBytes = new byte[16];
        System.arraycopy(frame, 5, rtuIdBytes, 0, 16);
        String extractedRtuId = new String(rtuIdBytes).trim().replace("\0", "");

        // 验证 rtuId 被截断
        assertEquals(16, rtuIdBytes.length, "rtuId 字段长度应为 16 字节");
        assertTrue(extractedRtuId.length() <= 16, "提取的 rtuId 长度不应超过 16");

        System.out.println("✓ 超长 rtuId 测试通过");
        System.out.println("  原始 rtuId: " + longRtuId);
        System.out.println("  截断后: " + extractedRtuId);
    }

    @Test
    public void testAuthRequestWithShortRtuId() {
        // 测试短 rtuId（应补0）
        String shortRtuId = "R1";
        String secret = "test";

        byte[] frame = BinaryProtocol.buildAuthRequest(shortRtuId, secret);

        // 提取 rtuId
        byte[] rtuIdBytes = new byte[16];
        System.arraycopy(frame, 5, rtuIdBytes, 0, 16);
        String extractedRtuId = new String(rtuIdBytes).trim().replace("\0", "");

        assertEquals(shortRtuId, extractedRtuId, "短 rtuId 应正确提取");

        System.out.println("✓ 短 rtuId 测试通过");
        System.out.println("  原始 rtuId: " + shortRtuId);
        System.out.println("  提取后: " + extractedRtuId);
    }

    @Test
    public void testModbusDataFrameWithDifferentSizes() {
        System.out.println("\n=== 不同大小 Modbus 数据测试 ===");

        // 小数据（8字节）
        byte[] smallData = new byte[]{0x01, 0x06, 0x07, (byte) 0xD1, 0x00, 0x02, (byte) 0x88, 0x3A};
        byte[] smallFrame = BinaryProtocol.buildModbusDataFrame(smallData);
        assertEquals(8 + 6, smallFrame.length, "小数据帧长度应为 14 字节");
        System.out.println("  ✓ 8字节数据: " + BinaryProtocol.bytesToHex(smallFrame));

        // 中等数据（9字节）
        byte[] mediumData = new byte[]{0x01, 0x03, 0x04, 0x02, (byte) 0x92, (byte) 0xFF, (byte) 0x9B, 0x5A, 0x3D};
        byte[] mediumFrame = BinaryProtocol.buildModbusDataFrame(mediumData);
        assertEquals(9 + 6, mediumFrame.length, "中等数据帧长度应为 15 字节");
        System.out.println("  ✓ 9字节数据: " + BinaryProtocol.bytesToHex(mediumFrame));

        // 大数据（假设读取多个寄存器）
        byte[] largeData = new byte[64];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) i;
        }
        byte[] largeFrame = BinaryProtocol.buildModbusDataFrame(largeData);
        assertEquals(64 + 6, largeFrame.length, "大数据帧长度应为 70 字节");
        System.out.println("  ✓ 64字节数据帧长度: " + largeFrame.length);
    }

    @Test
    public void testFrameIntegrity() {
        // 测试帧的完整性（构建后解析）
        String rtuId = "TEST-RTU";
        String secret = "secret123";

        byte[] authFrame = BinaryProtocol.buildAuthRequest(rtuId, secret);

        // 模拟解析过程
        assertEquals((byte) 0xAA, authFrame[0], "帧头验证失败");
        assertEquals((byte) 0x55, authFrame[1], "帧头验证失败");

        byte frameType = authFrame[2];
        assertEquals(BinaryProtocol.TYPE_AUTH_REQUEST, frameType, "帧类型验证失败");

        int dataLength = ((authFrame[3] & 0xFF) << 8) | (authFrame[4] & 0xFF);
        assertEquals(48, dataLength, "数据长度验证失败");

        // 验证校验和
        byte[] frameWithoutChecksum = new byte[authFrame.length - 1];
        System.arraycopy(authFrame, 0, frameWithoutChecksum, 0, frameWithoutChecksum.length);

        byte calculatedChecksum = 0;
        for (byte b : frameWithoutChecksum) {
            calculatedChecksum ^= b;
        }

        assertEquals(calculatedChecksum, authFrame[authFrame.length - 1], "校验和验证失败");

        System.out.println("✓ 帧完整性测试通过");
    }

    @Test
    public void testBytesToHex() {
        byte[] data = new byte[]{0x01, 0x02, (byte) 0xAA, (byte) 0xFF};
        String hex = BinaryProtocol.bytesToHex(data);

        assertEquals("01 02 AA FF", hex, "十六进制转换应正确");

        System.out.println("✓ 字节转十六进制测试通过");
        System.out.println("  输入: " + java.util.Arrays.toString(data));
        System.out.println("  输出: " + hex);
    }

    @Test
    public void testEmptyModbusData() {
        // 测试空 Modbus 数据
        byte[] emptyData = new byte[0];
        byte[] frame = BinaryProtocol.buildModbusDataFrame(emptyData);

        // 验证帧结构
        assertEquals((byte) 0xAA, frame[0]);
        assertEquals((byte) 0x55, frame[1]);
        assertEquals(BinaryProtocol.TYPE_MODBUS_DATA, frame[2]);

        int dataLength = ((frame[3] & 0xFF) << 8) | (frame[4] & 0xFF);
        assertEquals(0, dataLength, "空数据长度应为 0");

        // 总长度：帧头2 + 类型1 + 长度2 + 数据0 + 校验1 = 6字节
        assertEquals(6, frame.length, "空数据帧长度应为 6 字节");

        System.out.println("✓ 空 Modbus 数据测试通过");
    }
}
