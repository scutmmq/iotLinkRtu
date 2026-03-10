package com.scutmmq.mqtt.message;

import lombok.Data;

/**
 * 配置响应消息
 *
 * @author Claude
 * @since 2026-03-10
 */
@Data
public class ConfigResponseMessage {
    private String msgId;
    private String msgType;
    private String rtuId;
    private Long timestamp;
    private String requestMsgId;
    private Boolean success;
    private String message;
    private Object appliedConfig;
}
