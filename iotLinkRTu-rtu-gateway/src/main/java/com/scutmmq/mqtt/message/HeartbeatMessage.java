package com.scutmmq.mqtt.message;

import lombok.Data;

/**
 * 心跳消息
 *
 * @author Claude
 * @since 2026-03-10
 */
@Data
public class HeartbeatMessage {
    private String msgId;
    private String msgType;
    private String rtuId;
    private Long timestamp;
    private String status;
    private Long uptime;
    private Float memoryUsage;
    private Float cpuUsage;
}
