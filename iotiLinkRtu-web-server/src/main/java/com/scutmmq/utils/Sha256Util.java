package com.scutmmq.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 加密工具类
 * 用于 RTU 认证时的密钥哈希计算
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-15
 */
public class Sha256Util {
    
    /**
     * 计算 SHA-256 哈希值
     * 
     * @param input 输入字符串
     * @return 64 位十六进制哈希字符串
     * @throws RuntimeException 计算失败时抛出
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不存在", e);
        }
    }
    
    /**
     * 计算 RTU 认证密钥的哈希值
     * 公式：SHA256(原始 secret + rtuId + timestamp 前 4 字节)
     * 
     * @param secret RTU 的认证密钥（数据库中存储的明文）
     * @param rtuId RTU 唯一标识
     * @param timestamp 当前时间戳（毫秒）
     * @return 64 位十六进制哈希字符串
     */
    public static String calculateSecretHash(String secret, String rtuId, long timestamp) {
        // 1. 统一转换为秒级时间戳后取 4 字节（大端序）
        long epochSeconds = normalizeToEpochSeconds(timestamp);
        byte[] timeBytes = ByteBuffer.allocate(4)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt((int) epochSeconds)
                .array();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            digest.update(rtuId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            digest.update(timeBytes);
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不存在", e);
        }
    }
    
    /**
     * 验证 RTU 认证密钥
     * 
     * @param secret 数据库中的明文密钥
     * @param rtuId RTU 唯一标识
     * @param receivedHash 接收到的哈希值
     * @param timestamp 时间戳（毫秒）
     * @return 验证通过返回 true
     */
    public static boolean verifySecret(String secret, String rtuId, String receivedHash, long timestamp) {
        // 计算期望的哈希值
        String expectedHash = calculateSecretHash(secret, rtuId, timestamp);
        
        // 对比哈希值（忽略大小写）
        return expectedHash.equalsIgnoreCase(receivedHash);
    }
    
    /**
     * 检查时间戳是否有效（防止重放攻击）
     * 时间戳必须在当前时间的±5 分钟内
     * 
     * @param timestamp 待检查的时间戳（毫秒）
     * @return 有效返回 true
     */
    public static boolean isTimestampValid(long timestamp) {
        long normalizedTimestampMs = normalizeToEpochMillis(timestamp);
        long currentTime = System.currentTimeMillis();
        long fiveMinutes = 5 * 60 * 1000; // 5 分钟（毫秒）
        
        return Math.abs(currentTime - normalizedTimestampMs) <= fiveMinutes;
    }

    private static long normalizeToEpochSeconds(long timestamp) {
        if (timestamp > 1_000_000_000_000L) {
            return timestamp / 1000;
        }
        return timestamp;
    }

    private static long normalizeToEpochMillis(long timestamp) {
        if (timestamp > 1_000_000_000_000L) {
            return timestamp;
        }
        return timestamp * 1000;
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
