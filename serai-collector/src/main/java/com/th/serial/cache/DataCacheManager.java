package com.th.serial.cache;

import com.th.serial.protocol.BinaryProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 数据缓存管理器
 * 负责断网时的数据缓存和重连后的数据补发
 */
public class DataCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(DataCacheManager.class);

    private static final int MAX_MEMORY_CACHE = 10000; // 内存最多缓存 10000 条
    private static final String CACHE_DIR = ".cache"; // 缓存目录
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ConcurrentLinkedQueue<CachedFrame> memoryCache = new ConcurrentLinkedQueue<>();
    private final Path cacheDirectory;

    public DataCacheManager() {
        this.cacheDirectory = Paths.get(CACHE_DIR);
        initCacheDirectory();
    }

    /**
     * 初始化缓存目录
     */
    private void initCacheDirectory() {
        try {
            if (!Files.exists(cacheDirectory)) {
                Files.createDirectories(cacheDirectory);
                logger.info("创建缓存目录: {}", cacheDirectory.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("创建缓存目录失败", e);
        }
    }

    /**
     * 缓存数据帧
     * @param frame 二进制帧数据
     */
    public void cacheFrame(byte[] frame) {
        CachedFrame cachedFrame = new CachedFrame(System.currentTimeMillis(), frame);

        // 添加到内存缓存
        if (memoryCache.size() < MAX_MEMORY_CACHE) {
            memoryCache.offer(cachedFrame);
            logger.debug("数据已缓存到内存，当前缓存数量: {}", memoryCache.size());
        } else {
            // 内存缓存已满，写入文件
            writeToFile(cachedFrame);
        }
    }

    /**
     * 写入缓存文件
     * @param frame 缓存帧
     */
    private void writeToFile(CachedFrame frame) {
        String fileName = "cache_" + LocalDate.now().format(DATE_FORMATTER) + ".dat";
        Path filePath = cacheDirectory.resolve(fileName);

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(filePath.toFile(), true)))) {

            // 写入时间戳
            dos.writeLong(frame.timestamp);

            // 写入帧长度
            dos.writeInt(frame.data.length);

            // 写入帧数据
            dos.write(frame.data);

            logger.debug("数据已写入缓存文件: {}", fileName);

        } catch (IOException e) {
            logger.error("写入缓存文件失败", e);
        }
    }

    /**
     * 获取所有缓存的数据（用于补发）
     * @return 缓存的帧列表
     */
    public List<CachedFrame> getAllCachedFrames() {
        List<CachedFrame> allFrames = new ArrayList<>();

        // 1. 从文件读取
        try {
            File[] cacheFiles = cacheDirectory.toFile().listFiles((dir, name) -> name.startsWith("cache_") && name.endsWith(".dat"));

            if (cacheFiles != null) {
                for (File file : cacheFiles) {
                    allFrames.addAll(readFromFile(file));
                }
            }
        } catch (Exception e) {
            logger.error("读取缓存文件失败", e);
        }

        // 2. 从内存读取
        allFrames.addAll(new ArrayList<>(memoryCache));

        // 3. 按时间戳排序
        allFrames.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));

        logger.info("读取到 {} 条缓存数据", allFrames.size());
        return allFrames;
    }

    /**
     * 从文件读取缓存数据
     * @param file 缓存文件
     * @return 缓存帧列表
     */
    private List<CachedFrame> readFromFile(File file) {
        List<CachedFrame> frames = new ArrayList<>();

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {

            while (dis.available() > 0) {
                // 读取时间戳
                long timestamp = dis.readLong();

                // 读取帧长度
                int length = dis.readInt();

                // 读取帧数据
                byte[] data = new byte[length];
                dis.readFully(data);

                frames.add(new CachedFrame(timestamp, data));
            }

            logger.debug("从文件 {} 读取 {} 条数据", file.getName(), frames.size());

        } catch (EOFException e) {
            // 文件读取完毕
        } catch (IOException e) {
            logger.error("读取缓存文件失败: {}", file.getName(), e);
        }

        return frames;
    }

    /**
     * 清空所有缓存
     */
    public void clearCache() {
        // 清空内存缓存
        int memoryCount = memoryCache.size();
        memoryCache.clear();

        // 删除缓存文件
        int fileCount = 0;
        try {
            File[] cacheFiles = cacheDirectory.toFile().listFiles((dir, name) -> name.startsWith("cache_") && name.endsWith(".dat"));

            if (cacheFiles != null) {
                for (File file : cacheFiles) {
                    if (file.delete()) {
                        fileCount++;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("删除缓存文件失败", e);
        }

        logger.info("缓存已清空: 内存 {} 条, 文件 {} 个", memoryCount, fileCount);
    }

    /**
     * 获取缓存数量
     * @return 缓存数量
     */
    public int getCacheCount() {
        int fileCount = 0;

        try {
            File[] cacheFiles = cacheDirectory.toFile().listFiles((dir, name) -> name.startsWith("cache_") && name.endsWith(".dat"));

            if (cacheFiles != null) {
                for (File file : cacheFiles) {
                    fileCount += countFramesInFile(file);
                }
            }
        } catch (Exception e) {
            logger.error("统计缓存文件失败", e);
        }

        return memoryCache.size() + fileCount;
    }

    /**
     * 统计文件中的帧数量
     * @param file 缓存文件
     * @return 帧数量
     */
    private int countFramesInFile(File file) {
        int count = 0;

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {

            while (dis.available() > 0) {
                dis.readLong(); // 时间戳
                int length = dis.readInt(); // 帧长度
                dis.skipBytes(length); // 跳过帧数据
                count++;
            }

        } catch (EOFException e) {
            // 文件读取完毕
        } catch (IOException e) {
            logger.error("统计缓存文件失败: {}", file.getName(), e);
        }

        return count;
    }

    /**
     * 缓存帧对象
     */
    public static class CachedFrame {
        public final long timestamp;
        public final byte[] data;

        public CachedFrame(long timestamp, byte[] data) {
            this.timestamp = timestamp;
            this.data = data;
        }

        @Override
        public String toString() {
            return String.format("CachedFrame{timestamp=%d, size=%d bytes, data=%s}",
                    timestamp, data.length, BinaryProtocol.bytesToHex(data).substring(0, Math.min(30, data.length * 3)));
        }
    }
}
