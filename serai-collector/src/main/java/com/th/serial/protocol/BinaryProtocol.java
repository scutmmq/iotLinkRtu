package com.th.serial.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 二进制协议工具类
 * 用于构建 serial-collector ↔ rtu-gateway 之间的自定义二进制帧
 */
public class BinaryProtocol {

    // 帧头：固定为 0xAA55
    private static final byte[] FRAME_HEADER = {(byte) 0xAA, (byte) 0x55};

    // 帧类型定义
    public static final byte TYPE_AUTH_REQUEST = 0x01;      // 认证请求
    public static final byte TYPE_AUTH_RESPONSE = 0x02;     // 认证响应
    public static final byte TYPE_MODBUS_DATA = 0x03;       // Modbus 数据（上报/响应）
    public static final byte TYPE_MODBUS_COMMAND = 0x04;    // Modbus 命令（下发）
    public static final byte TYPE_HEARTBEAT_REQUEST = 0x05; // 心跳请求
    public static final byte TYPE_HEARTBEAT_RESPONSE = 0x06;// 心跳响应

    /**
     * 构建认证请求帧
     * @param rtuId RTU 标识符（明文，最多16字节）
     * @param secret 密钥（原始明文，将被 SHA-256 加密）
     * @return 完整的认证请求帧
     */
    public static byte[] buildAuthRequest(String rtuId, String secret) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        // 1. 帧头（2字节）
        buffer.put(FRAME_HEADER);

        // 2. 帧类型（1字节）
        buffer.put(TYPE_AUTH_REQUEST);

        // 3. 数据长度（2字节，大端序）- 先占位，后面填充
        int lengthPos = buffer.position();
        buffer.putShort((short) 0);

        // 4. 数据体开始位置
        int dataStartPos = buffer.position();

        // 4.1 rtuId（16字节，UTF-8明文，不足补0）
        byte[] rtuIdBytes = new byte[16];
        byte[] rtuIdSrc = rtuId.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(rtuIdSrc, 0, rtuIdBytes, 0, Math.min(rtuIdSrc.length, 16));
        buffer.put(rtuIdBytes);

        // 4.2 获取当前时间戳（4字节，用于防重放）
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        byte[] timestampBytes = ByteBuffer.allocate(4).putInt(timestamp).array();

        // 4.3 计算 SHA-256 哈希：SHA256(secret + rtuId + timestamp前4字节)
        byte[] secretHash = calculateSecretHash(secret, rtuId, timestampBytes);
        buffer.put(secretHash); // 32字节

        // 5. 回填数据长度（16 + 32 = 48字节）
        int dataLength = buffer.position() - dataStartPos;
        buffer.putShort(lengthPos, (short) dataLength);

        // 6. 计算校验和（异或）
        byte checksum = calculateChecksum(buffer.array(), 0, buffer.position());
        buffer.put(checksum);

        // 7. 提取完整帧
        byte[] frame = new byte[buffer.position()];
        buffer.flip();
        buffer.get(frame);

        return frame;
    }

    /**
     * 构建 Modbus 数据帧（用于上报数据）
     * @param modbusData Modbus 原始响应帧
     * @return 完整的数据帧
     */
    public static byte[] buildModbusDataFrame(byte[] modbusData) {
        ByteBuffer buffer = ByteBuffer.allocate(modbusData.length + 10);

        // 帧头
        buffer.put(FRAME_HEADER);

        // 帧类型
        buffer.put(TYPE_MODBUS_DATA);

        // 数据长度
        buffer.putShort((short) modbusData.length);

        // Modbus 数据
        buffer.put(modbusData);

        // 校验和
        byte checksum = calculateChecksum(buffer.array(), 0, buffer.position());
        buffer.put(checksum);

        byte[] frame = new byte[buffer.position()];
        buffer.flip();
        buffer.get(frame);
        return frame;
    }

    /**
     * 构建心跳请求帧
     * @return 完整的心跳请求帧
     */
    public static byte[] buildHeartbeatRequest() {
        ByteBuffer buffer = ByteBuffer.allocate(11);

        // 帧头
        buffer.put(FRAME_HEADER);

        // 帧类型
        buffer.put(TYPE_HEARTBEAT_REQUEST);

        // 数据长度
        buffer.putShort((short) 4);

        // 时间戳（4字节）
        buffer.putInt((int) (System.currentTimeMillis() / 1000));

        // 校验和
        byte checksum = calculateChecksum(buffer.array(), 0, buffer.position());
        buffer.put(checksum);

        return buffer.array();
    }

    /**
     * 计算 secret 的 SHA-256 哈希值
     * 哈希输入：secret + rtuId + timestamp（前4字节）
     * @param secret 原始密钥
     * @param rtuId RTU标识符
     * @param timestampBytes 时间戳字节（4字节）
     * @return SHA-256 哈希值（32字节）
     */
    private static byte[] calculateSecretHash(String secret, String rtuId, byte[] timestampBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // 拼接：secret + rtuId + timestamp
            digest.update(secret.getBytes(StandardCharsets.UTF_8));
            digest.update(rtuId.getBytes(StandardCharsets.UTF_8));
            digest.update(timestampBytes);

            return digest.digest(); // 返回32字节哈希值
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 计算校验和（异或）
     * @param data 数据
     * @param offset 起始位置
     * @param length 长度
     * @return 校验和
     */
    private static byte calculateChecksum(byte[] data, int offset, int length) {
        byte checksum = 0;
        for (int i = offset; i < offset + length; i++) {
            checksum ^= data[i];
        }
        return checksum;
    }

    /**
     * 将字节数组转换为十六进制字符串（用于调试）
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
