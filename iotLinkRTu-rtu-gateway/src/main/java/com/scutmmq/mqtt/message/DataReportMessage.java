package com.scutmmq.mqtt.message;

import lombok.Data;

/**
 * 数据上报消息
 *
 * @author Claude
 * @since 2026-03-10
 */
@Data
public class DataReportMessage {
    private String msgId;
    private String msgType;
    private String rtuId;
    private Long timestamp;
    private Integer deviceAddress;
    private Integer functionCode;
    private SensorData data;
    private String quality;
    private String rawModbus;

    @Data
    public static class SensorData {
        private Float temperature;
        private Float humidity;
    }
}
