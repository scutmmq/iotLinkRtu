package com.scutmmq.mqtt;

import com.scutmmq.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * MQTT 客户端管理器
 * 负责 MQTT 连接管理、消息发布和订阅
 *
 * @author Claude
 * @since 2026-03-10
 */
@Slf4j
public class MqttClientManager {

    private MqttClient mqttClient;
    private final String brokerUrl;
    private final String clientId;
    private final MqttConnectOptions connectOptions;
    private volatile boolean isConnected = false;

    // 消息缓存队列（连接断开时缓存）
    private final ConcurrentLinkedQueue<CachedMessage> messageCache = new ConcurrentLinkedQueue<>();
    private static final int MAX_CACHE_SIZE = 10000;

    public MqttClientManager() {
        this.brokerUrl = Config.getMqttBrokerUrl();
        this.clientId = Config.getMqttClientId() + "-" + UUID.randomUUID().toString().substring(0, 8);
        this.connectOptions = buildConnectOptions();
    }

    /**
     * 构建连接选项
     */
    private MqttConnectOptions buildConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false); // 保留会话
        options.setAutomaticReconnect(true); // 自动重连
        options.setConnectionTimeout(30); // 连接超时30秒
        options.setKeepAliveInterval(60); // 心跳间隔60秒
        options.setMaxInflight(1000); // 最大未确认消息数

        // 如果配置了用户名密码
        String username = Config.getMqttUsername();
        String password = Config.getMqttPassword();
        if (username != null && !username.isEmpty()) {
            options.setUserName(username);
            options.setPassword(password.toCharArray());
        }

        return options;
    }

    /**
     * 启动 MQTT 客户端
     */
    public void start() {
        try {
            mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

            // 设置回调
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("MQTT 连接断开: {}", cause.getMessage());
                    isConnected = false;
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    handleIncomingMessage(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    log.debug("消息发送完成: {}", token.getMessageId());
                }
            });

            // 连接到 Broker
            connect();

            log.info("MQTT 客户端已启动: clientId={}, broker={}", clientId, brokerUrl);

        } catch (MqttException e) {
            log.error("启动 MQTT 客户端失败", e);
            throw new RuntimeException("MQTT 客户端启动失败", e);
        }
    }

    /**
     * 连接到 MQTT Broker
     */
    private void connect() throws MqttException {
        if (mqttClient != null && !mqttClient.isConnected()) {
            mqttClient.connect(connectOptions);
            isConnected = true;
            log.info("已连接到 MQTT Broker: {}", brokerUrl);

            // 连接成功后，补发缓存的消息
            resendCachedMessages();
        }
    }

    /**
     * 发布消息
     *
     * @param topic   主题
     * @param payload 消息内容（JSON字符串）
     * @param qos     QoS级别（0, 1, 2）
     */
    public void publish(String topic, String payload, int qos) {
        publish(topic, payload, qos, false);
    }

    /**
     * 发布消息
     *
     * @param topic    主题
     * @param payload  消息内容（JSON字符串）
     * @param qos      QoS级别（0, 1, 2）
     * @param retained 是否保留消息
     */
    public void publish(String topic, String payload, int qos, boolean retained) {
        try {
            if (!isConnected || mqttClient == null || !mqttClient.isConnected()) {
                log.warn("MQTT 未连接，消息已缓存: topic={}", topic);
                cacheMessage(topic, payload, qos, retained);
                return;
            }

            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(qos);
            message.setRetained(retained);

            mqttClient.publish(topic, message);
            log.debug("消息已发布: topic={}, qos={}, size={} bytes", topic, qos, payload.length());

        } catch (MqttException e) {
            log.error("发布消息失败: topic={}, error={}", topic, e.getMessage());
            cacheMessage(topic, payload, qos, retained);
        }
    }

    /**
     * 订阅主题
     *
     * @param topic 主题
     * @param qos   QoS级别
     */
    public void subscribe(String topic, int qos) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.subscribe(topic, qos);
                log.info("已订阅主题: topic={}, qos={}", topic, qos);
            } else {
                log.warn("MQTT 未连接，无法订阅主题: {}", topic);
            }
        } catch (MqttException e) {
            log.error("订阅主题失败: topic={}", topic, e);
        }
    }

    /**
     * 取消订阅
     *
     * @param topic 主题
     */
    public void unsubscribe(String topic) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.unsubscribe(topic);
                log.info("已取消订阅: {}", topic);
            }
        } catch (MqttException e) {
            log.error("取消订阅失败: topic={}", topic, e);
        }
    }

    /**
     * 处理接收到的消息
     */
    private void handleIncomingMessage(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload());
            log.info("收到 MQTT 消息: topic={}, payload={}", topic, payload);

            // TODO: 根据 topic 分发到不同的处理器
            // 例如：配置下发、控制指令等

        } catch (Exception e) {
            log.error("处理 MQTT 消息失败: topic={}", topic, e);
        }
    }

    /**
     * 缓存消息（连接断开时）
     */
    private void cacheMessage(String topic, String payload, int qos, boolean retained) {
        if (messageCache.size() >= MAX_CACHE_SIZE) {
            log.warn("消息缓存已满，丢弃最早的消息");
            messageCache.poll();
        }

        messageCache.offer(new CachedMessage(topic, payload, qos, retained));
        log.debug("消息已缓存，当前缓存数量: {}", messageCache.size());
    }

    /**
     * 补发缓存的消息
     */
    private void resendCachedMessages() {
        if (messageCache.isEmpty()) {
            return;
        }

        log.info("开始补发缓存消息，共 {} 条", messageCache.size());
        int successCount = 0;
        int failCount = 0;

        while (!messageCache.isEmpty()) {
            CachedMessage cached = messageCache.poll();
            try {
                MqttMessage message = new MqttMessage(cached.payload.getBytes());
                message.setQos(cached.qos);
                message.setRetained(cached.retained);

                mqttClient.publish(cached.topic, message);
                successCount++;

            } catch (MqttException e) {
                log.error("补发消息失败: topic={}", cached.topic, e);
                failCount++;
                // 重新放回队列
                messageCache.offer(cached);
                break; // 停止补发，等待下次重连
            }
        }

        log.info("消息补发完成: 成功 {} 条, 失败 {} 条", successCount, failCount);
    }

    /**
     * 停止 MQTT 客户端
     */
    public void stop() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
                isConnected = false;
                log.info("MQTT 客户端已停止");
            }
        } catch (MqttException e) {
            log.error("停止 MQTT 客户端失败", e);
        }
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return isConnected && mqttClient != null && mqttClient.isConnected();
    }

    /**
     * 缓存消息内部类
     */
    private static class CachedMessage {
        final String topic;
        final String payload;
        final int qos;
        final boolean retained;

        CachedMessage(String topic, String payload, int qos, boolean retained) {
            this.topic = topic;
            this.payload = payload;
            this.qos = qos;
            this.retained = retained;
        }
    }
}
