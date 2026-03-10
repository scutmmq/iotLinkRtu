package com.scutmmq.handler;

import com.scutmmq.manager.RtuConnectionManager;
import com.scutmmq.parser.ModBusDataParser;
import com.scutmmq.protocol.BinaryFrame;
import com.scutmmq.protocol.BinaryProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 二进制帧处理器
 *
 * <p>负责处理 serial-collector 发送的二进制帧</p>
 * <p>支持的帧类型：认证请求、Modbus数据、心跳请求</p>
 *
 * @author Claude
 * @since 2026-03-10
 */
@Slf4j
public class BinaryFrameHandler extends SimpleChannelInboundHandler<BinaryFrame> {

    private final RtuConnectionManager connectionManager;

    public BinaryFrameHandler(RtuConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BinaryFrame frame) throws Exception {
        byte frameType = frame.getType();

        log.debug("收到二进制帧: 类型=0x{}, 数据长度={}",
            String.format("%02X", frameType), frame.getData().length);

        switch (frameType) {
            case BinaryProtocol.TYPE_AUTH_REQUEST:
                handleAuthRequest(ctx, frame);
                break;

            case BinaryProtocol.TYPE_MODBUS_DATA:
                handleModbusData(ctx, frame);
                break;

            case BinaryProtocol.TYPE_HEARTBEAT_REQUEST:
                handleHeartbeat(ctx, frame);
                break;

            default:
                log.warn("未知的帧类型: 0x{}", String.format("%02X", frameType));
        }
    }

    /**
     * 处理认证请求
     */
    private void handleAuthRequest(ChannelHandlerContext ctx, BinaryFrame frame) {
        byte[] data = frame.getData();

        // 数据格式：rtuId(16字节) + secretHash(32字节)
        if (data.length != 48) {
            log.error("认证请求数据长度错误: {}, 期望48字节", data.length);
            sendAuthResponse(ctx, false);
            return;
        }

        // 提取 rtuId（16字节，UTF-8明文）
        byte[] rtuIdBytes = new byte[16];
        System.arraycopy(data, 0, rtuIdBytes, 0, 16);
        String rtuId = new String(rtuIdBytes, StandardCharsets.UTF_8).trim().replace("\0", "");

        // 提取 secretHash（32字节）
        byte[] receivedHash = new byte[32];
        System.arraycopy(data, 16, receivedHash, 0, 32);

        log.info("收到认证请求: rtuId={}", rtuId);

        // TODO: 调用 web-server API 验证 rtuId 和 secretHash
        // 这里暂时简单验证（实际应该调用 HTTP API）
        boolean authSuccess = verifyAuth(rtuId, receivedHash);

        // TODO 临时表示验证成功
        authSuccess = true;

        if (authSuccess) {
            // 注册连接
            connectionManager.register(rtuId, ctx.channel());
            log.info("RTU {} 认证成功", rtuId);
        } else {
            log.warn("RTU {} 认证失败", rtuId);
        }

        // 发送认证响应
        sendAuthResponse(ctx, authSuccess);
    }

    /**
     * 处理 Modbus 数据帧
     */
    private void handleModbusData(ChannelHandlerContext ctx, BinaryFrame frame) {
        // 检查是否已认证
        if (!connectionManager.isAuthenticated(ctx.channel())) {
            log.warn("未认证的连接尝试发送数据，关闭连接");
            ctx.close();
            return;
        }

        String rtuId = connectionManager.getRtuId(ctx.channel());
        byte[] modbusData = frame.getData();

        log.debug("收到 RTU {} 的 Modbus 数据: {} 字节", rtuId, modbusData.length);

        // 解析 Modbus 数据
        parseAndPublishModbusData(rtuId, modbusData);
    }

    /**
     * 处理心跳请求
     */
    private void handleHeartbeat(ChannelHandlerContext ctx, BinaryFrame frame) {
        if (!connectionManager.isAuthenticated(ctx.channel())) {
            log.warn("未认证的连接发送心跳，忽略");
            return;
        }

        String rtuId = connectionManager.getRtuId(ctx.channel());
        log.debug("收到 RTU {} 的心跳", rtuId);

        // 发送心跳响应
        byte[] response = BinaryProtocol.buildHeartbeatResponse();
        ctx.writeAndFlush(Unpooled.wrappedBuffer(response));
    }

    /**
     * 发送认证响应
     */
    private void sendAuthResponse(ChannelHandlerContext ctx, boolean success) {
        byte[] response = BinaryProtocol.buildAuthResponse(success);
        ctx.writeAndFlush(Unpooled.wrappedBuffer(response));

        if (!success) {
            // 认证失败，延迟关闭连接
            ctx.channel().eventLoop().schedule(() -> ctx.close(), 1, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    /**
     * 验证认证信息（临时实现，实际应调用 web-server API）
     */
    private boolean verifyAuth(String rtuId, byte[] receivedHash) {
        // TODO: 调用 web-server API
        // GET /api/rtu/gateway/{rtuId}/verify?secretHash=xxx

        // 临时实现：简单验证 rtuId 格式
        return rtuId != null && rtuId.startsWith("RTU");
    }

    /**
     * 解析 Modbus 数据并发布到 MQTT
     */
    private void parseAndPublishModbusData(String rtuId, byte[] modbusData) {
        try {
            // 验证 Modbus 帧格式（至少包含：地址1 + 功能码1 + 字节数1 + 数据N + CRC2）
            if (modbusData.length < 5) {
                log.error("Modbus 数据长度不足: {}", modbusData.length);
                return;
            }

            int address = modbusData[0] & 0xFF;
            int functionCode = modbusData[1] & 0xFF;
            int byteCount = modbusData[2] & 0xFF;

            log.debug("Modbus 帧: 地址={}, 功能码=0x{}, 字节数={}",
                address, String.format("%02X", functionCode), byteCount);

            // 提取实际数据（跳过地址、功能码、字节数，去掉末尾CRC）
            if (modbusData.length < 3 + byteCount + 2) {
                log.error("Modbus 数据不完整");
                return;
            }

            byte[] registerData = new byte[byteCount];
            System.arraycopy(modbusData, 3, registerData, 0, byteCount);

            // 解析温湿度（假设寄存器0=湿度，寄存器1=温度）
            if (byteCount >= 4) {
                byte[] humidityBytes = new byte[2];
                byte[] tempBytes = new byte[2];
                System.arraycopy(registerData, 0, humidityBytes, 0, 2);
                System.arraycopy(registerData, 2, tempBytes, 0, 2);

                float humidity = ModBusDataParser.parseHumidity(humidityBytes);
                float temperature = ModBusDataParser.parseTemperature(tempBytes);

                log.info("RTU {} - 温度: {}℃, 湿度: {}%RH", rtuId, temperature, humidity);

                // TODO: 发布到 MQTT
                // publishToMqtt(rtuId, temperature, humidity);
            }

        } catch (Exception e) {
            log.error("解析 Modbus 数据失败", e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 连接断开，注销
        connectionManager.unregister(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("处理异常", cause);
        ctx.close();
    }
}
