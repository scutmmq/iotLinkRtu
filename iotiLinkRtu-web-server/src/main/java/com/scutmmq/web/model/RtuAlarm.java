package com.scutmmq.web.model;

import java.time.LocalDateTime;

/**
 * RTU 报警实体
 */
public class RtuAlarm {

    private Long id;
    private String rtuId;
    private String alarmType;
    private String alarmLevel;
    private Double currentValue;
    private Double thresholdValue;
    private String alarmMessage;
    private LocalDateTime alarmTime;
    private String status;
    private String handleResult;
    private String handler;
    private LocalDateTime handleTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRtuId() {
        return rtuId;
    }

    public void setRtuId(String rtuId) {
        this.rtuId = rtuId;
    }

    public String getAlarmType() {
        return alarmType;
    }

    public void setAlarmType(String alarmType) {
        this.alarmType = alarmType;
    }

    public String getAlarmLevel() {
        return alarmLevel;
    }

    public void setAlarmLevel(String alarmLevel) {
        this.alarmLevel = alarmLevel;
    }

    public Double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Double currentValue) {
        this.currentValue = currentValue;
    }

    public Double getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(Double thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public String getAlarmMessage() {
        return alarmMessage;
    }

    public void setAlarmMessage(String alarmMessage) {
        this.alarmMessage = alarmMessage;
    }

    public LocalDateTime getAlarmTime() {
        return alarmTime;
    }

    public void setAlarmTime(LocalDateTime alarmTime) {
        this.alarmTime = alarmTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHandleResult() {
        return handleResult;
    }

    public void setHandleResult(String handleResult) {
        this.handleResult = handleResult;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public LocalDateTime getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(LocalDateTime handleTime) {
        this.handleTime = handleTime;
    }
}
