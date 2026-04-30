package com.scutmmq.web.model;

import java.time.LocalDateTime;

/**
 * RTU 配置实体
 */
public class RtuConfig {

    private Long id;
    private String rtuId;
    private Integer interval;
    private Integer modbusDeviceAddress;
    private Integer baudRate;
    private Double tempCalibration;
    private Double humidityCalibration;
    private Double tempThresholdMin;
    private Double tempThresholdMax;
    private Double humidityThresholdMin;
    private Double humidityThresholdMax;
    private Boolean alarmEnabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

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

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public Integer getModbusDeviceAddress() {
        return modbusDeviceAddress;
    }

    public void setModbusDeviceAddress(Integer modbusDeviceAddress) {
        this.modbusDeviceAddress = modbusDeviceAddress;
    }

    public Integer getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(Integer baudRate) {
        this.baudRate = baudRate;
    }

    public Double getTempCalibration() {
        return tempCalibration;
    }

    public void setTempCalibration(Double tempCalibration) {
        this.tempCalibration = tempCalibration;
    }

    public Double getHumidityCalibration() {
        return humidityCalibration;
    }

    public void setHumidityCalibration(Double humidityCalibration) {
        this.humidityCalibration = humidityCalibration;
    }

    public Double getTempThresholdMin() {
        return tempThresholdMin;
    }

    public void setTempThresholdMin(Double tempThresholdMin) {
        this.tempThresholdMin = tempThresholdMin;
    }

    public Double getTempThresholdMax() {
        return tempThresholdMax;
    }

    public void setTempThresholdMax(Double tempThresholdMax) {
        this.tempThresholdMax = tempThresholdMax;
    }

    public Double getHumidityThresholdMin() {
        return humidityThresholdMin;
    }

    public void setHumidityThresholdMin(Double humidityThresholdMin) {
        this.humidityThresholdMin = humidityThresholdMin;
    }

    public Double getHumidityThresholdMax() {
        return humidityThresholdMax;
    }

    public void setHumidityThresholdMax(Double humidityThresholdMax) {
        this.humidityThresholdMax = humidityThresholdMax;
    }

    public Boolean getAlarmEnabled() {
        return alarmEnabled;
    }

    public void setAlarmEnabled(Boolean alarmEnabled) {
        this.alarmEnabled = alarmEnabled;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
