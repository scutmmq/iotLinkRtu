package com.scutmmq.mqtt;

import com.google.gson.Gson;
import com.scutmmq.mqtt.message.*;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * MQTT 消息发布器
 * 负责将数据封装成 JSON 格式并发布到 MQTT
 *
 * @author Claude
 * @since 2026-03-10
 */
@Slf4j
public class MqttPublisher {

    private final MqttClientManager mqttClient;
    private final Gson gson;

    // Topic 前缀
    private static final String TOPIC_PREFIX = "iot/rtu/";
    private static final String TOPIC_SYSTEM = "iot/system/";

    public MqttPublisher(MqttClientManager mqttClient) {
        this.mqttClient = mqttClient;
        this.gson = new Gson();
    }

    /**
     * 发布数据上报消息
     *
     * @param rtuId         RTU ID
     * @param temperature   温度
     * @param humidity      湿度
     * @param deviceAddress Modbus 设备地址
     * @param functionCode  Modbus 功能码
     * @param rawModbus     原始 Modbus 数据（十六进制字符串）
     */
    public void publishDataReport(String rtuId, float temperature, float humidity,
                                   int deviceAddress, int functionCode, String rawModbus) {
        try {
            DataReportMessage message = new DataReportMessage();
            message.setMsgId(UUID.randomUUID().toString());
            message.setMsgType("data_report");
            message.setRtuId(rtuId);
            message.setTimestamp(System.currentTimeMillis());
            message.setDeviceAddress(deviceAddress);
            message.setFunctionCode(functionCode);

            DataReportMessage.SensorData data = new DataReportMessage.SensorData();
            data.setTemperature(temperature);
            data.setHumidity(humidity);
            message.setData(data);

            message.setQuality("good");
            message.setRawModbus(rawModbus);

            String topic = TOPIC_PREFIX + rtuId + "/data";
            String payload = gson.toJson(message);

            mqttClient.publish(topic, payload, 1); // QoS 1

            log.debug("已发布数据上报: rtuId={}, temp={}℃, humidity={}%", rtuId, temperature, humidity);

        } catch (Exception e) {
            log.error("发布数据上报失败: rtuId={}", rtuId, e);
        }
    }

    /**
     * 发布心跳消息
     *
     * @param rtuId       RTU ID
     * @param status      运行状态
     * @param uptime      运行时长（秒）
     * @param memoryUsage 内存使用率（%）
     * @param cpuUsage    CPU使用率（%）
     */
    public void publishHeartbeat(String rtuId, String status, long uptime,
                                  float memoryUsage, float cpuUsage) {
        try {
            HeartbeatMessage message = new HeartbeatMessage();
            message.setMsgId(UUID.randomUUID().toString());
            message.setMsgType("heartbeat");
            message.setRtuId(rtuId);
            message.setTimestamp(System.currentTimeMillis());
            message.setStatus(status);
            message.setUptime(uptime);
            message.setMemoryUsage(memoryUsage);
            message.setCpuUsage(cpuUsage);

            String topic = TOPIC_PREFIX + rtuId + "/heartbeat";
            String payload = gson.toJson(message);

            mqttClient.publish(topic, payload, 0); // QoS 0

            log.debug("已发布心跳: rtuId={}, status={}", rtuId, status);

        } catch (Exception e) {
            log.error("发布心跳失败: rtuId={}", rtuId, e);
        }
    }

    /**
     * 发布状态变更消息
     *
     * @param rtuId     RTU ID
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     * @param reason    变更原因
     * @param details   详细信息
     */
    public void publishStatusChange(String rtuId, String oldStatus, String newStatus,
                                     String reason, String details) {
        try {
            StatusChangeMessage message = new StatusChangeMessage();
            message.setMsgId(UUID.randomUUID().toString());
            message.setMsgType("status_change");
            message.setRtuId(rtuId);
            message.setTimestamp(System.currentTimeMillis());
            message.setOldStatus(oldStatus);
            message.setNewStatus(newStatus);
            message.setReason(reason);
            message.setDetails(details);

            String topic = TOPIC_PREFIX + rtuId + "/status";
            String payload = gson.toJson(message);

            mqttClient.publish(topic, payload, 1, true); // QoS 1, Retained

            log.info("已发布状态变更: rtuId={}, {}→{}, reason={}", rtuId, oldStatus, newStatus, reason);

        } catch (Exception e) {
            log.error("发布状态变更失败: rtuId={}", rtuId, e);
        }
    }

    /**
     * 发布报警消息
     *
     * @param rtuId          RTU ID
     * @param alarmType      报警类型
     * @param alarmLevel     报警级别
     * @param currentValue   当前值
     * @param thresholdValue 阈值
     * @param description    描述
     * @param suggestion     建议
     */
    public void publishAlarm(String rtuId, String alarmType, String alarmLevel,
                             float currentValue, float thresholdValue,
                             String description, String suggestion) {
        try {
            AlarmMessage message = new AlarmMessage();
            message.setMsgId(UUID.randomUUID().toString());
            message.setMsgType("alarm");
            message.setRtuId(rtuId);
            message.setTimestamp(System.currentTimeMillis());
            message.setAlarmType(alarmType);
            message.setAlarmLevel(alarmLevel);
            message.setCurrentValue(currentValue);
            message.setThresholdValue(thresholdValue);
            message.setDescription(description);
            message.setSuggestion(suggestion);

            String topic = TOPIC_PREFIX + rtuId + "/alarm";
            String payload = gson.toJson(message);

            mqttClient.publish(topic, payload, 1); // QoS 1

            log.warn("已发布报警: rtuId={}, type={}, level={}", rtuId, alarmType, alarmLevel);

        } catch (Exception e) {
            log.error("发布报警失败: rtuId={}", rtuId, e);
        }
    }

    /**
     * 发布配置响应消息
     *
     * @param rtuId        RTU ID
     * @param requestMsgId 请求消息ID
     * @param success      是否成功
     * @param message      响应消息
     * @param appliedConfig 已应用的配置
     */
    public void publishConfigResponse(String rtuId, String requestMsgId, boolean success,
                                       String message, Object appliedConfig) {
        try {
            ConfigResponseMessage response = new ConfigResponseMessage();
            response.setMsgId(UUID.randomUUID().toString());
            response.setMsgType("config_response");
            response.setRtuId(rtuId);
            response.setTimestamp(System.currentTimeMillis());
            response.setRequestMsgId(requestMsgId);
            response.setSuccess(success);
            response.setMessage(message);
            response.setAppliedConfig(appliedConfig);

            String topic = TOPIC_PREFIX + rtuId + "/config/response";
            String payload = gson.toJson(response);

            mqttClient.publish(topic, payload, 1); // QoS 1

            log.info("已发布配置响应: rtuId={}, success={}", rtuId, success);

        } catch (Exception e) {
            log.error("发布配置响应失败: rtuId={}", rtuId, e);
        }
    }

    /**
     * 发布 RTU 上线通知
     *
     * @param rtuId RTU ID
     */
    public void publishOnlineNotification(String rtuId) {
        try {
            SystemMessage message = new SystemMessage();
            message.setMsgId(UUID.randomUUID().toString());
            message.setMsgType("online");
            message.setRtuId(rtuId);
            message.setTimestamp(System.currentTimeMillis());

            String topic = TOPIC_SYSTEM + "online";
            String payload = gson.toJson(message);

            mqttClient.publish(topic, payload, 1); // QoS 1

            log.info("已发布上线通知: rtuId={}", rtuId);

        } catch (Exception e) {
            log.error("发布上线通知失败: rtuId={}", rtuId, e);
        }
    }

    /**
     * 发布 RTU 离线通知
     *
     * @param rtuId RTU ID
     */
    public void publishOfflineNotification(String rtuId) {
        try {
            SystemMessage message = new SystemMessage();
            message.setMsgId(UUID.randomUUID().toString());
            message.setMsgType("offline");
            message.setRtuId(rtuId);
            message.setTimestamp(System.currentTimeMillis());

            String topic = TOPIC_SYSTEM + "offline";
            String payload = gson.toJson(message);

            mqttClient.publish(topic, payload, 1); // QoS 1

            log.info("已发布离线通知: rtuId={}", rtuId);

        } catch (Exception e) {
            log.error("发布离线通知失败: rtuId={}", rtuId, e);
        }
    }
}
