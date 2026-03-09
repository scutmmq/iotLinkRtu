package com.scutmmq.rtu.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ModBus-RTU 消息实体类
 * 
 * <p>封装 ModBus-RTU 协议的消息结构，包含设备地址、功能码、数据区和 CRC 校验码</p>
 * <p>支持请求帧和响应帧的表示</p>
 * 
 * @author mo.mingqin@xlink
 * @since 2026-03-08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModBusMessage {
    
    /**
     * 设备地址 (1-254)
     */
    private int address;
    
    /**
     * 功能码
     * - 0x03: 读保持寄存器
     * - 0x04: 读输入寄存器  
     * - 0x06: 写单个寄存器
     */
    private int functionCode;
    
    /**
     * 数据区字节数组
     */
    private byte[] data;
    
    /**
     * CRC16 校验码
     */
    private short crc16;
    
    /**
     * 是否为请求帧
     * <p>true 表示请求帧，false 表示响应帧</p>
     */
    private boolean isRequest = true;
    
    /**
     * 构建请求消息
     * 
     * <p>创建一个请求类型的 ModBusMessage 对象</p>
     *
     * @param address 设备地址（1-254）
     * @param functionCode 功能码（如 0x03 读保持寄存器、0x04 读输入寄存器、0x06 写单个寄存器）
     * @param data 数据区字节数组
     */
    public ModBusMessage(int address, int functionCode, byte[] data) {
        this.address = address;
        this.functionCode = functionCode;
        this.data = data;
        this.isRequest = true;
    }
}
