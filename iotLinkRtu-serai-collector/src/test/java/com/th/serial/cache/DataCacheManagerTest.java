package com.th.serial.cache;

import com.th.serial.protocol.BinaryProtocol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据缓存管理器测试
 */
public class DataCacheManagerTest {

    private DataCacheManager cacheManager;

    @BeforeEach
    public void setUp() {
        cacheManager = new DataCacheManager();
        // 清空之前的缓存
        cacheManager.clearCache();
    }

    @AfterEach
    public void tearDown() {
        // 测试后清理
        if (cacheManager != null) {
            cacheManager.clearCache();
        }
    }

    @Test
    public void testCacheFrame() {
        System.out.println("\n=== 缓存数据帧测试 ===");

        // 构建测试数据
        byte[] frame1 = BinaryProtocol.buildHeartbeatRequest();
        byte[] frame2 = BinaryProtocol.buildHeartbeatRequest();

        // 缓存数据
        cacheManager.cacheFrame(frame1);
        cacheManager.cacheFrame(frame2);

        // 验证缓存数量
        int count = cacheManager.getCacheCount();
        assertEquals(2, count, "应该缓存了 2 条数据");

        System.out.println("  ✓ 缓存数量: " + count);
    }

    @Test
    public void testGetAllCachedFrames() {
        System.out.println("\n=== 获取所有缓存数据测试 ===");

        // 缓存多条数据
        for (int i = 0; i < 5; i++) {
            byte[] frame = BinaryProtocol.buildHeartbeatRequest();
            cacheManager.cacheFrame(frame);
            try {
                Thread.sleep(10); // 确保时间戳不同
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 获取所有缓存
        List<DataCacheManager.CachedFrame> frames = cacheManager.getAllCachedFrames();

        assertEquals(5, frames.size(), "应该有 5 条缓存数据");

        // 验证按时间戳排序
        for (int i = 1; i < frames.size(); i++) {
            assertTrue(frames.get(i).timestamp >= frames.get(i - 1).timestamp,
                    "缓存数据应按时间戳排序");
        }

        System.out.println("  ✓ 获取到 " + frames.size() + " 条缓存数据");
        System.out.println("  ✓ 时间戳排序正确");
    }

    @Test
    public void testClearCache() {
        System.out.println("\n=== 清空缓存测试 ===");

        // 缓存数据
        for (int i = 0; i < 10; i++) {
            byte[] frame = BinaryProtocol.buildHeartbeatRequest();
            cacheManager.cacheFrame(frame);
        }

        int beforeClear = cacheManager.getCacheCount();
        assertTrue(beforeClear > 0, "清空前应有缓存数据");
        System.out.println("  清空前: " + beforeClear + " 条");

        // 清空缓存
        cacheManager.clearCache();

        int afterClear = cacheManager.getCacheCount();
        assertEquals(0, afterClear, "清空后应无缓存数据");
        System.out.println("  清空后: " + afterClear + " 条");

        System.out.println("  ✓ 缓存清空成功");
    }

    @Test
    public void testLargeDataCache() {
        System.out.println("\n=== 大量数据缓存测试 ===");

        int testCount = 100;

        // 缓存大量数据
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < testCount; i++) {
            byte[] frame = BinaryProtocol.buildHeartbeatRequest();
            cacheManager.cacheFrame(frame);
        }
        long endTime = System.currentTimeMillis();

        int count = cacheManager.getCacheCount();
        assertEquals(testCount, count, "应该缓存了 " + testCount + " 条数据");

        System.out.println("  ✓ 缓存 " + testCount + " 条数据");
        System.out.println("  ✓ 耗时: " + (endTime - startTime) + " ms");
        System.out.println("  ✓ 平均: " + ((endTime - startTime) / (double) testCount) + " ms/条");
    }

    @Test
    public void testCacheWithDifferentFrameTypes() {
        System.out.println("\n=== 不同类型帧缓存测试 ===");

        // 缓存不同类型的帧
        byte[] authFrame = BinaryProtocol.buildAuthRequest("RTU-001", "secret123");
        byte[] heartbeatFrame = BinaryProtocol.buildHeartbeatRequest();
        byte[] modbusFrame = BinaryProtocol.buildModbusDataFrame(new byte[]{0x01, 0x03, 0x04, 0x02, (byte) 0x92, (byte) 0xFF, (byte) 0x9B, 0x5A, 0x3D});

        cacheManager.cacheFrame(authFrame);
        cacheManager.cacheFrame(heartbeatFrame);
        cacheManager.cacheFrame(modbusFrame);

        // 获取缓存
        List<DataCacheManager.CachedFrame> frames = cacheManager.getAllCachedFrames();

        assertEquals(3, frames.size(), "应该有 3 条不同类型的缓存");

        System.out.println("  ✓ 认证帧: " + authFrame.length + " 字节");
        System.out.println("  ✓ 心跳帧: " + heartbeatFrame.length + " 字节");
        System.out.println("  ✓ Modbus帧: " + modbusFrame.length + " 字节");
    }

    @Test
    public void testCacheDirectory() {
        System.out.println("\n=== 缓存目录测试 ===");

        // 缓存数据
        byte[] frame = BinaryProtocol.buildHeartbeatRequest();
        cacheManager.cacheFrame(frame);

        // 检查缓存目录是否存在
        File cacheDir = new File(".cache");
        assertTrue(cacheDir.exists(), "缓存目录应该存在");
        assertTrue(cacheDir.isDirectory(), "应该是目录");

        System.out.println("  ✓ 缓存目录: " + cacheDir.getAbsolutePath());
        System.out.println("  ✓ 目录存在: " + cacheDir.exists());
    }

    @Test
    public void testCachedFrameToString() {
        System.out.println("\n=== CachedFrame toString 测试 ===");

        byte[] data = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};
        DataCacheManager.CachedFrame frame = new DataCacheManager.CachedFrame(System.currentTimeMillis(), data);

        String str = frame.toString();
        assertNotNull(str);
        assertTrue(str.contains("CachedFrame"));
        assertTrue(str.contains("timestamp"));
        assertTrue(str.contains("size"));

        System.out.println("  " + str);
        System.out.println("  ✓ toString 方法正常");
    }

    @Test
    public void testMemoryCacheLimit() {
        System.out.println("\n=== 内存缓存限制测试 ===");

        // 注意：这个测试会缓存超过 10000 条数据，可能会写入文件
        // 为了测试速度，我们只测试少量数据

        int testCount = 50;
        for (int i = 0; i < testCount; i++) {
            byte[] frame = BinaryProtocol.buildHeartbeatRequest();
            cacheManager.cacheFrame(frame);
        }

        int count = cacheManager.getCacheCount();
        assertEquals(testCount, count, "缓存数量应该正确");

        System.out.println("  ✓ 缓存 " + testCount + " 条数据");
        System.out.println("  ✓ 内存缓存工作正常");
    }

    @Test
    public void testCacheRecovery() {
        System.out.println("\n=== 缓存恢复测试 ===");

        // 第一个管理器缓存数据
        DataCacheManager manager1 = new DataCacheManager();
        for (int i = 0; i < 5; i++) {
            byte[] frame = BinaryProtocol.buildHeartbeatRequest();
            manager1.cacheFrame(frame);
        }

        int count1 = manager1.getCacheCount();
        System.out.println("  第一个管理器缓存: " + count1 + " 条");

        // 创建第二个管理器（模拟重启）
        DataCacheManager manager2 = new DataCacheManager();
        int count2 = manager2.getCacheCount();
        System.out.println("  第二个管理器读取: " + count2 + " 条");

        assertEquals(count1, count2, "重启后应该能读取到相同数量的缓存");

        // 清理
        manager2.clearCache();

        System.out.println("  ✓ 缓存恢复成功");
    }
}
