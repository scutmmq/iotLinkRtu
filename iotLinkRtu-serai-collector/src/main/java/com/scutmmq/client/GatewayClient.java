package com.scutmmq.client;

import com.scutmmq.cache.DataCacheManager;
import com.scutmmq.protocol.BinaryFrame;
import com.scutmmq.protocol.BinaryFrameDecoder;
import com.scutmmq.protocol.BinaryProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RTU 网关客户端
 * 负责建立 TCP 连接并将数据发送给 RTU 网关
 */
public class GatewayClient {
    private static final Logger logger = LoggerFactory.getLogger(GatewayClient.class);

    private final String host;
    private final int port;
    private Bootstrap bootstrap;
    private EventLoopGroup group;
    private Channel channel;
    private boolean isRunning = false;
    private boolean authenticated = false;
    private ScheduledFuture<?> heartbeatTask;

    // 配置下发回调接口
    private ModbusCommandHandler modbusCommandHandler;

    // 数据缓存管理器
    private final DataCacheManager cacheManager;

    // 数据补发标志
    private final AtomicBoolean isResending = new AtomicBoolean(false);

    public GatewayClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.cacheManager = new DataCacheManager();
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;

        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();

                        // 添加帧解码器
                        p.addLast(new BinaryFrameDecoder());

                        // 添加业务处理器
                        p.addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                super.channelActive(ctx);
                                logger.info("TCP连接建立成功，开始发送认证请求");

                                // 从配置文件读取 rtuId 和 secret
                                String rtuId = com.scutmmq.utils.MicroConfig.readString("rtu.id");
                                String secret = com.scutmmq.utils.MicroConfig.readString("rtu.secret");

                                // 构建认证请求帧
                                byte[] authFrame = BinaryProtocol.buildAuthRequest(rtuId, secret);

                                // 发送认证帧
                                ctx.writeAndFlush(Unpooled.wrappedBuffer(authFrame));

                                logger.info("已发送认证请求: rtuId={}, 帧长度={} 字节", rtuId, authFrame.length);
                                logger.debug("认证帧内容: {}", BinaryProtocol.bytesToHex(authFrame));
                            }

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                if (msg instanceof BinaryFrame) {
                                    BinaryFrame frame = (BinaryFrame) msg;
                                    handleFrame(ctx, frame);
                                }
                            }

                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                super.channelInactive(ctx);
                                logger.warn("与网关断开连接");
                                authenticated = false;
                                stopHeartbeat();
                                scheduleReconnect();
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                logger.error("网关连接异常", cause);
                                ctx.close();
                            }
                        });
                    }
                });

        connect();
    }

    private void connect() {
        if (!isRunning) return;

        // 如果已经连接或正则连接中，跳过
        if (channel != null && channel.isActive()) {
            return;
        }

        logger.info("尝试连接到网关 {}:{}", host, port);
        try {
            bootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    channel = future.channel();
                    logger.info("已连接到网关 {}:{}", host, port);
                } else {
                    logger.warn("连接网关失败，将在 5 秒后重试");
                    // 只有在连接失败时才调度重连 (Inactive 也会触发重连)
                    future.channel().eventLoop().schedule(this::connect, 5, TimeUnit.SECONDS);
                }
            });
        } catch (Exception e) {
             logger.error("连接异常", e);
             scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (!isRunning) return;

        if (group != null && !group.isShuttingDown()) {
             group.schedule(this::connect, 5, TimeUnit.SECONDS);
        }
    }

    public void send(byte[] data) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(Unpooled.wrappedBuffer(data));
            logger.debug("已发送 {} 字节到网关", data.length);
        } else {
            logger.warn("无法发送数据，未连接到网关");
        }
    }

    public void stop() {
        isRunning = false;
        stopHeartbeat();

        // 优雅关闭缓存管理器
        if (cacheManager != null) {
            cacheManager.shutdown();
        }

        if (group != null) {
            group.shutdownGracefully();
        }
    }

    /**
     * 处理接收到的帧
     */
    private void handleFrame(ChannelHandlerContext ctx, BinaryFrame frame) {
        if (frame.isAuthResponse()) {
            // 处理认证响应
            handleAuthResponse(ctx, frame);
        } else if (frame.isHeartbeatResponse()) {
            // 处理心跳响应
            logger.debug("收到心跳响应");
        } else if (frame.isModbusCommand()) {
            // 处理 Modbus 命令（配置下发）
            handleModbusCommand(ctx, frame);
        } else {
            logger.warn("收到未知类型的帧: 0x{}", String.format("%02X", frame.getType()));
        }
    }

    /**
     * 处理认证响应
     */
    private void handleAuthResponse(ChannelHandlerContext ctx, BinaryFrame frame) {
        boolean success = frame.getAuthResult();

        if (success) {
            logger.info("认证成功！");
            authenticated = true;

            // 启动心跳任务（每30秒发送一次）
            startHeartbeat(ctx);

            // 补发缓存数据
            resendCachedData(ctx);
        } else {
            logger.error("认证失败！连接将被关闭");
            authenticated = false;
            ctx.close();
        }
    }

    /**
     * 处理 Modbus 命令（配置下发）
     */
    private void handleModbusCommand(ChannelHandlerContext ctx, BinaryFrame frame) {
        byte[] modbusCommand = frame.getData();
        logger.info("收到配置下发命令: {} 字节", modbusCommand.length);
        logger.debug("Modbus命令内容: {}", BinaryProtocol.bytesToHex(modbusCommand));

        // 如果没有设置命令处理器，直接返回
        if (modbusCommandHandler == null) {
            logger.warn("未设置 Modbus 命令处理器，忽略配置下发");
            return;
        }

        // 调用处理器执行命令
        byte[] response = modbusCommandHandler.handleCommand(modbusCommand);

        if (response != null && response.length > 0) {
            // 将响应封装成二进制帧发送回网关
            byte[] responseFrame = BinaryProtocol.buildModbusDataFrame(response);
            ctx.writeAndFlush(Unpooled.wrappedBuffer(responseFrame));
            logger.info("已返回配置执行结果: {} 字节", response.length);
        } else {
            logger.error("配置命令执行失败");
        }
    }

    /**
     * 设置 Modbus 命令处理器
     * @param handler 命令处理器
     */
    public void setModbusCommandHandler(ModbusCommandHandler handler) {
        this.modbusCommandHandler = handler;
    }

    /**
     * 启动心跳任务
     */
    private void startHeartbeat(ChannelHandlerContext ctx) {
        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            return;
        }

        heartbeatTask = ctx.executor().scheduleAtFixedRate(() -> {
            if (authenticated && ctx.channel().isActive()) {
                byte[] heartbeatFrame = BinaryProtocol.buildHeartbeatRequest();
                ctx.writeAndFlush(Unpooled.wrappedBuffer(heartbeatFrame));
                logger.debug("发送心跳请求");
            }
        }, 30, 30, TimeUnit.SECONDS);

        logger.info("心跳任务已启动（间隔30秒）");
    }

    /**
     * 停止心跳任务
     */
    private void stopHeartbeat() {
        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
            logger.info("心跳任务已停止");
        }
    }

    /**
     * 发送 Modbus 数据到网关
     * @param modbusData Modbus 原始响应帧
     */
    public void sendModbusData(byte[] modbusData) {
        byte[] frame = BinaryProtocol.buildModbusDataFrame(modbusData);

        if (!authenticated || channel == null || !channel.isActive()) {
            // 未认证或未连接，缓存数据
            logger.warn("未连接到网关，数据已缓存");
            cacheManager.cacheFrame(frame);
            return;
        }

        // 已连接且已认证，直接发送
        channel.writeAndFlush(Unpooled.wrappedBuffer(frame));
        logger.debug("已发送 Modbus 数据: {} 字节", modbusData.length);
    }

    /**
     * 补发缓存数据
     */
    private void resendCachedData(ChannelHandlerContext ctx) {
        if (isResending.get()) {
            logger.warn("数据补发正在进行中，跳过");
            return;
        }

        int cacheCount = cacheManager.getCacheCount();
        if (cacheCount == 0) {
            logger.info("没有缓存数据需要补发");
            return;
        }

        logger.info("开始补发缓存数据，共 {} 条", cacheCount);
        isResending.set(true);

        // 在后台线程补发数据
        ctx.executor().execute(() -> {
            try {
                List<DataCacheManager.CachedFrame> cachedFrames = cacheManager.getAllCachedFrames();

                int successCount = 0;
                int failCount = 0;

                for (DataCacheManager.CachedFrame cachedFrame : cachedFrames) {
                    if (!authenticated || !ctx.channel().isActive()) {
                        logger.warn("连接已断开，停止补发");
                        break;
                    }

                    try {
                        ctx.writeAndFlush(Unpooled.wrappedBuffer(cachedFrame.data)).sync();
                        successCount++;

                        // 控制补发速度，避免网络拥塞
                        if (successCount % 100 == 0) {
                            Thread.sleep(100);
                            logger.debug("已补发 {} 条数据", successCount);
                        }

                    } catch (Exception e) {
                        logger.error("补发数据失败", e);
                        failCount++;
                    }
                }

                logger.info("数据补发完成: 成功 {} 条, 失败 {} 条", successCount, failCount);

                // 补发成功后清空缓存
                if (failCount == 0) {
                    cacheManager.clearCache();
                    logger.info("缓存已清空");
                }

            } catch (Exception e) {
                logger.error("补发缓存数据异常", e);
            } finally {
                isResending.set(false);
            }
        });
    }

    /**
     * 检查是否已认证
     */
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * 获取缓存数量
     */
    public int getCacheCount() {
        return cacheManager.getCacheCount();
    }
}

