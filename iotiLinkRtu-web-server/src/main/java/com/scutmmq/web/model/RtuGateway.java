package com.scutmmq.web.model;

import java.time.LocalDateTime;

/**
 * RTU 网关节点实体
 * 对应数据库表：rtu_gateway
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-15
 */
public class RtuGateway {
    
    /** 自增主键 ID */
    private Long id;
    
    /** RTU 唯一标识（如 RTU-001） */
    private String rtuId;
    
    /** RTU 名称（如"1 号车间工控机"） */
    private String name;
    
    /** 安装位置描述 */
    private String location;
    
    /** 网关状态：ENABLED（允许采集）/DISABLED（禁止采集） */
    private String status;
    
    /** 在线状态：ONLINE（在线）/OFFLINE（离线） */
    private String online;
    
    /** 认证密钥（明文存储，用于 SHA-256 加密验证） */
    private String secret;
    
    /** 串口号（可选） */
    private String serialPort;
    
    /** 波特率（可选） */
    private Integer baudRate;
    
    /** Modbus 设备地址（可选） */
    private Integer deviceAddress;
    
    /** 最后心跳时间 */
    private LocalDateTime heartbeatTime;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
    
    // Getters and Setters
    
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
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getOnline() {
        return online;
    }
    
    public void setOnline(String online) {
        this.online = online;
    }
    
    public String getSecret() {
        return secret;
    }
    
    public void setSecret(String secret) {
        this.secret = secret;
    }
    
    public String getSerialPort() {
        return serialPort;
    }
    
    public void setSerialPort(String serialPort) {
        this.serialPort = serialPort;
    }
    
    public Integer getBaudRate() {
        return baudRate;
    }
    
    public void setBaudRate(Integer baudRate) {
        this.baudRate = baudRate;
    }
    
    public Integer getDeviceAddress() {
        return deviceAddress;
    }
    
    public void setDeviceAddress(Integer deviceAddress) {
        this.deviceAddress = deviceAddress;
    }
    
    public LocalDateTime getHeartbeatTime() {
        return heartbeatTime;
    }
    
    public void setHeartbeatTime(LocalDateTime heartbeatTime) {
        this.heartbeatTime = heartbeatTime;
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
    
    @Override
    public String toString() {
        return "RtuGateway{" +
                "id=" + id +
                ", rtuId='" + rtuId + '\'' +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", status='" + status + '\'' +
                ", online='" + online + '\'' +
                ", serialPort='" + serialPort + '\'' +
                ", baudRate=" + baudRate +
                ", deviceAddress=" + deviceAddress +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
