package com.scutmmq.mqtt.message;

import lombok.Data;

/**
 * 报警消息
 *
 * @author Claude
 * @since 2026-03-10
 */
@Data
public class AlarmMessage {
    private String msgId;
    private String msgType;
    private String rtuId;
    private Long timestamp;
    private String alarmType;
    private String alarmLevel;
    private Float currentValue;
    private Float thresholdValue;
    private String description;
    private String suggestion;
}
