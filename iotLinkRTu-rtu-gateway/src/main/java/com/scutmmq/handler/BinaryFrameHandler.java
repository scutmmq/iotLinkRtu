package com.scutmmq.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.scutmmq.config.Config;
import com.scutmmq.manager.RtuConnectionManager;
import com.scutmmq.mqtt.MqttPublisher;
import com.scutmmq.parser.ModBusDataParser;
import com.scutmmq.protocol.BinaryFrame;
import com.scutmmq.protocol.BinaryProtocol;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private final MqttPublisher mqttPublisher;
    private final HttpClient httpClient;
    private final Map<String, CachedThresholdConfig> thresholdCache;

    public BinaryFrameHandler(RtuConnectionManager connectionManager, MqttPublisher mqttPublisher) {
        this.connectionManager = connectionManager;
        this.mqttPublisher = mqttPublisher;
        this.httpClient = HttpClient.newHttpClient();
        this.thresholdCache = new ConcurrentHashMap<>();
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

        // 数据格式：rtuId(16字节) + timestamp(4字节) + secretHash(32字节)
        if (data.length != 52) {
            log.error("认证请求数据长度错误: {}, 期望52字节", data.length);
            sendAuthResponse(ctx, false);
            return;
        }

        // 提取 rtuId（16字节，UTF-8明文）
        byte[] rtuIdBytes = new byte[16];
        System.arraycopy(data, 0, rtuIdBytes, 0, 16);
        String rtuId = new String(rtuIdBytes, StandardCharsets.UTF_8).trim().replace("\0", "");

        byte[] timestampBytes = new byte[4];
        System.arraycopy(data, 16, timestampBytes, 0, 4);
        long timestamp = ByteBuffer.wrap(timestampBytes).getInt() & 0xFFFFFFFFL;

        // 提取 secretHash（32字节）
        byte[] receivedHash = new byte[32];
        System.arraycopy(data, 20, receivedHash, 0, 32);

        log.info("收到认证请求: rtuId={}, timestamp={}", rtuId, timestamp);

        AuthResult authResult = verifyAuth(rtuId, receivedHash, timestamp);

        if (authResult.isValid() && "ENABLED".equals(authResult.getStatus())) {
            // 注册连接
            connectionManager.register(rtuId, ctx.channel());
            log.info("RTU {} 认证成功并已启用", rtuId);
            sendAuthResponse(ctx, true);
            syncOnlineStatus(rtuId, "ONLINE");

            // 发布上线通知到 MQTT
            mqttPublisher.publishOnlineNotification(rtuId);
            mqttPublisher.publishStatusChange(rtuId, "offline", "online", "authentication_success", "RTU认证成功并上线");
        } else if (authResult.isValid() && "DISABLED".equals(authResult.getStatus())) {
            log.warn("RTU {} 认证成功但已被禁用，拒绝连接", rtuId);
            sendAuthResponse(ctx, false);
        } else {
            log.warn("RTU {} 认证失败", rtuId);
            sendAuthResponse(ctx, false);
        }
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
        syncOnlineStatus(rtuId, "ONLINE");

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
    private AuthResult verifyAuth(String rtuId, byte[] receivedHash, long timestamp) {
        String url = Config.webServerApiUrl + "/api/rtu/gateway/verify";
        String secretHashHex = bytesToHex(receivedHash).toLowerCase();
        String requestBody = String.format(
            "{\"rtuId\":\"%s\",\"secretHash\":\"%s\",\"timestamp\":%d}",
            escapeJson(rtuId), secretHashHex, timestamp
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("RTU 认证失败，web-server 返回状态码: {}, body={}", response.statusCode(), response.body());
                return new AuthResult(false, "DISABLED");
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject data = root.getAsJsonObject("data");
            boolean valid = data != null && data.has("valid") && data.get("valid").getAsBoolean();
            String status = data != null && data.has("status") ? data.get("status").getAsString() : "DISABLED";
            return new AuthResult(valid, status);
        } catch (Exception e) {
            log.error("调用 web-server 验证 RTU 失败: rtuId={}", rtuId, e);
            return new AuthResult(false, "DISABLED");
        }
    }

    private void syncOnlineStatus(String rtuId, String online) {
        if (rtuId == null || rtuId.isBlank()) {
            return;
        }

        String url = Config.webServerApiUrl + "/api/rtu/gateway/status";
        String requestBody = String.format(
            "{\"rtuId\":\"%s\",\"online\":\"%s\"}",
            escapeJson(rtuId), escapeJson(online)
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.warn("同步 RTU 在线状态失败: rtuId={}, online={}", rtuId, online, e);
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 认证结果内部类
     */
    @Data
    private static class AuthResult {
        private  boolean valid;
        private  String status;

        public AuthResult(boolean valid, String status) {
            this.valid = valid;
            this.status = status;
        }

        public boolean isValid() {
            return valid;
        }

        public String getStatus() {
            return status;
        }
        public void setValid(boolean valid) {
            this.valid = valid;
        }
        public void setStatus(String status) {
            this.status = status;
        }
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

                // 发布到 MQTT
                String rawModbusHex = bytesToHex(modbusData);
                mqttPublisher.publishDataReport(rtuId, temperature, humidity, address, functionCode, rawModbusHex);

                // 检查阈值并发送报警
                checkThresholdAndAlarm(rtuId, temperature, humidity);
            }

        } catch (Exception e) {
            log.error("解析 Modbus 数据失败", e);
        }
    }

    /**
     * 检查阈值并发送报警
     */
    private void checkThresholdAndAlarm(String rtuId, float temperature, float humidity) {
        ThresholdConfig thresholdConfig = loadThresholdConfig(rtuId);
        if (!thresholdConfig.alarmEnabled()) {
            return;
        }

        float tempMin = thresholdConfig.tempMin();
        float tempMax = thresholdConfig.tempMax();
        float humidityMin = thresholdConfig.humidityMin();
        float humidityMax = thresholdConfig.humidityMax();

        if (temperature > tempMax) {
            mqttPublisher.publishAlarm(rtuId, "temperature_high", "warning",
                temperature, tempMax, "温度超过上限阈值", "请检查空调设备");
        } else if (temperature < tempMin) {
            mqttPublisher.publishAlarm(rtuId, "temperature_low", "warning",
                temperature, tempMin, "温度低于下限阈值", "请检查加热设备");
        }

        if (humidity > humidityMax) {
            mqttPublisher.publishAlarm(rtuId, "humidity_high", "warning",
                humidity, humidityMax, "湿度超过上限阈值", "请检查除湿设备");
        } else if (humidity < humidityMin) {
            mqttPublisher.publishAlarm(rtuId, "humidity_low", "warning",
                humidity, humidityMin, "湿度低于下限阈值", "请检查加湿设备");
        }
    }

    private ThresholdConfig loadThresholdConfig(String rtuId) {
        CachedThresholdConfig cached = thresholdCache.get(rtuId);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.cachedAt() < 30_000) {
            return cached.config();
        }

        ThresholdConfig fallback = ThresholdConfig.defaultConfig();
        try {
            String encodedRtuId = URLEncoder.encode(rtuId, StandardCharsets.UTF_8);
            String url = Config.webServerApiUrl + "/api/rtu/" + encodedRtuId + "/config";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return fallback;
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject data = root.getAsJsonObject("data");
            if (data == null) {
                return fallback;
            }

            JsonObject temperatureThreshold = data.getAsJsonObject("temperatureThreshold");
            JsonObject humidityThreshold = data.getAsJsonObject("humidityThreshold");
            ThresholdConfig config = new ThresholdConfig(
                getFloat(temperatureThreshold, "min", 18.0f),
                getFloat(temperatureThreshold, "max", 28.0f),
                getFloat(humidityThreshold, "min", 40.0f),
                getFloat(humidityThreshold, "max", 60.0f),
                data.has("alarmEnabled") && !data.get("alarmEnabled").isJsonNull() ? data.get("alarmEnabled").getAsBoolean() : true
            );

            thresholdCache.put(rtuId, new CachedThresholdConfig(config, now));
            return config;
        } catch (Exception e) {
            log.warn("加载 RTU 阈值配置失败，使用默认值: rtuId={}", rtuId, e);
            return fallback;
        }
    }

    private float getFloat(JsonObject object, String key, float defaultValue) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return defaultValue;
        }
        return object.get(key).getAsFloat();
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 获取 RTU ID
        String rtuId = connectionManager.getRtuId(ctx.channel());

        // 连接断开，注销
        connectionManager.unregister(ctx.channel());

        // 发布离线通知到 MQTT
        if (rtuId != null) {
            syncOnlineStatus(rtuId, "OFFLINE");
            mqttPublisher.publishOfflineNotification(rtuId);
            mqttPublisher.publishStatusChange(rtuId, "online", "offline", "connection_lost", "TCP连接断开");
        }

        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("处理异常", cause);
        ctx.close();
    }

    private record ThresholdConfig(float tempMin, float tempMax, float humidityMin, float humidityMax, boolean alarmEnabled) {
        private static ThresholdConfig defaultConfig() {
            return new ThresholdConfig(18.0f, 28.0f, 40.0f, 60.0f, true);
        }
    }

    private record CachedThresholdConfig(ThresholdConfig config, long cachedAt) {
    }
}
