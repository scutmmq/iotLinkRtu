package com.scutmmq.cache;

import com.scutmmq.protocol.BinaryProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 数据缓存管理器（优化版）
 * 负责断网时的数据缓存和重连后的数据补发
 *
 * 优化特性：
 * 1. 批量写入：减少文件IO次数
 * 2. 异步刷盘：避免阻塞主线程
 * 3. 定期清理：自动删除过期缓存文件
 * 4. 优雅关闭：确保数据不丢失
 */
public class DataCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(DataCacheManager.class);

    private static final int MAX_MEMORY_CACHE = 10000; // 内存最多缓存 10000 条
    private static final int BATCH_WRITE_SIZE = 100; // 批量写入阈值
    private static final int CACHE_FILE_EXPIRE_DAYS = 7; // 缓存文件过期天数
    private static final String CACHE_DIR = ".cache"; // 缓存目录
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ConcurrentLinkedQueue<CachedFrame> memoryCache = new ConcurrentLinkedQueue<>();
    private final List<CachedFrame> writeBuffer = new ArrayList<>(BATCH_WRITE_SIZE);
    private final Path cacheDirectory;

    // 异步写入线程池
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "cache-writer");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean isShutdown = false;

    public DataCacheManager() {
        this.cacheDirectory = Paths.get(CACHE_DIR);
        initCacheDirectory();
        startPeriodicFlush();
        startPeriodicCleanup();
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
     * 启动定期刷盘任务（每5秒）
     */
    private void startPeriodicFlush() {
        executor.scheduleWithFixedDelay(() -> {
            try {
                flushWriteBuffer();
            } catch (Exception e) {
                logger.error("定期刷盘失败", e);
            }
        }, 5, 5, TimeUnit.SECONDS);
        logger.info("定期刷盘任务已启动（间隔5秒）");
    }

    /**
     * 启动定期清理任务（每天凌晨2点）
     */
    private void startPeriodicCleanup() {
        long initialDelay = calculateInitialDelay();
        executor.scheduleWithFixedDelay(() -> {
            try {
                cleanupExpiredFiles();
            } catch (Exception e) {
                logger.error("定期清理失败", e);
            }
        }, initialDelay, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
        logger.info("定期清理任务已启动（每天凌晨2点）");
    }

    /**
     * 计算到凌晨2点的延迟时间
     */
    private long calculateInitialDelay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.toLocalDate().atTime(2, 0);
        if (now.isAfter(nextRun)) {
            nextRun = nextRun.plusDays(1);
        }
        return java.time.Duration.between(now, nextRun).getSeconds();
    }

    /**
     * 缓存数据帧（优化版：批量写入）
     * @param frame 二进制帧数据
     */
    public void cacheFrame(byte[] frame) {
        if (isShutdown) {
            logger.warn("缓存管理器已关闭，拒绝缓存数据");
            return;
        }

        CachedFrame cachedFrame = new CachedFrame(System.currentTimeMillis(), frame);

        // 添加到内存缓存
        if (memoryCache.size() < MAX_MEMORY_CACHE) {
            memoryCache.offer(cachedFrame);
            logger.debug("数据已缓存到内存，当前缓存数量: {}", memoryCache.size());
        } else {
            // 内存缓存已满，加入写入缓冲区
            addToWriteBuffer(cachedFrame);
        }
    }

    /**
     * 添加到写入缓冲区（批量写入）
     */
    private synchronized void addToWriteBuffer(CachedFrame frame) {
        writeBuffer.add(frame);

        // 达到批量写入阈值，异步刷盘
        if (writeBuffer.size() >= BATCH_WRITE_SIZE) {
            List<CachedFrame> toWrite = new ArrayList<>(writeBuffer);
            writeBuffer.clear();

            executor.execute(() -> batchWriteToFile(toWrite));
        }
    }

    /**
     * 刷新写入缓冲区（确保数据不丢失）
     */
    private synchronized void flushWriteBuffer() {
        if (writeBuffer.isEmpty()) {
            return;
        }

        List<CachedFrame> toWrite = new ArrayList<>(writeBuffer);
        writeBuffer.clear();

        batchWriteToFile(toWrite);
    }

    /**
     * 批量写入缓存文件
     * @param frames 缓存帧列表
     */
    private void batchWriteToFile(List<CachedFrame> frames) {
        if (frames.isEmpty()) {
            return;
        }

        String fileName = "cache_" + LocalDate.now().format(DATE_FORMATTER) + ".dat";
        Path filePath = cacheDirectory.resolve(fileName);

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(filePath.toFile(), true)))) {

            for (CachedFrame frame : frames) {
                // 写入时间戳
                dos.writeLong(frame.timestamp);

                // 写入帧长度
                dos.writeInt(frame.data.length);

                // 写入帧数据
                dos.write(frame.data);
            }

            logger.debug("批量写入 {} 条数据到缓存文件: {}", frames.size(), fileName);

        } catch (IOException e) {
            logger.error("批量写入缓存文件失败", e);
        }
    }

    /**
     * 写入缓存文件（单条，保留用于兼容）
     * @param frame 缓存帧
     */
    private void writeToFile(CachedFrame frame) {
        batchWriteToFile(List.of(frame));
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
     * 清理过期的缓存文件
     */
    private void cleanupExpiredFiles() {
        try {
            File[] cacheFiles = cacheDirectory.toFile().listFiles((dir, name) ->
                name.startsWith("cache_") && name.endsWith(".dat"));

            if (cacheFiles == null || cacheFiles.length == 0) {
                return;
            }

            LocalDate expireDate = LocalDate.now().minusDays(CACHE_FILE_EXPIRE_DAYS);
            int deletedCount = 0;

            for (File file : cacheFiles) {
                try {
                    // 从文件名提取日期
                    String fileName = file.getName();
                    String dateStr = fileName.substring(6, 14); // cache_20260310.dat
                    LocalDate fileDate = LocalDate.parse(dateStr, DATE_FORMATTER);

                    // 删除过期文件
                    if (fileDate.isBefore(expireDate)) {
                        if (file.delete()) {
                            deletedCount++;
                            logger.info("删除过期缓存文件: {}", fileName);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("解析缓存文件日期失败: {}", file.getName(), e);
                }
            }

            if (deletedCount > 0) {
                logger.info("清理完成，删除 {} 个过期缓存文件", deletedCount);
            }

        } catch (Exception e) {
            logger.error("清理过期缓存文件失败", e);
        }
    }

    /**
     * 优雅关闭（确保数据不丢失）
     */
    public void shutdown() {
        if (isShutdown) {
            return;
        }

        logger.info("缓存管理器开始关闭...");
        isShutdown = true;

        try {
            // 刷新写入缓冲区
            flushWriteBuffer();

            // 关闭线程池
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }

            logger.info("缓存管理器已关闭");
        } catch (InterruptedException e) {
            logger.error("关闭缓存管理器时被中断", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 清空所有缓存
     */
    public void clearCache() {
        // 先刷新写入缓冲区
        flushWriteBuffer();

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
