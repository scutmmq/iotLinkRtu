package com.scutmmq.mqtt.message;

import lombok.Data;

/**
 * 系统消息（上线/离线通知）
 *
 * @author Claude
 * @since 2026-03-10
 */
@Data
public class SystemMessage {
    private String msgId;
    private String msgType;
    private String rtuId;
    private Long timestamp;
}
