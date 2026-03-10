package com.scutmmq.mqtt.message;

import lombok.Data;

/**
 * 状态变更消息
 *
 * @author Claude
 * @since 2026-03-10
 */
@Data
public class StatusChangeMessage {
    private String msgId;
    private String msgType;
    private String rtuId;
    private Long timestamp;
    private String oldStatus;
    private String newStatus;
    private String reason;
    private String details;
}
