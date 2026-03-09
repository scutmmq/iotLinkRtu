package com.scutmmq.rtu.utils;

import com.scutmmq.rtu.protocol.ModBusMessage;

/**
 * ModBus 请求构建工具类
 * 
 * <p>提供便捷的 ModBus 请求消息构建方法，支持常用的功能码：</p>
 * <ul>
 *     <li>0x03 - 读保持寄存器</li>
 *     <li>0x04 - 读输入寄存器</li>
 *     <li>0x06 - 写单个寄存器</li>
 * </ul>
 * 
 * @author mo.mingqin@xlink
 * @since 2026-03-08
 */
public class ModBusRequestBuilder {
    
    /**
     * 构建读保持寄存器请求（功能码 0x03）
     * 
     * @param address 设备地址
     * @param registerAddress 寄存器起始地址
     * @param registerCount 寄存器数量
     * @return ModBusMessage 请求消息
     */
    public static ModBusMessage buildReadHoldingRegistersRequest(int address,
                                                                 int registerAddress,
                                                                 int registerCount) {
        byte[] data = new byte[4];
        // 寄存器地址：高字节在前
        data[0] = (byte) ((registerAddress >> 8) & 0xFF);
        data[1] = (byte) (registerAddress & 0xFF);
        // 寄存器数量：高字节在前
        data[2] = (byte) ((registerCount >> 8) & 0xFF);
        data[3] = (byte) (registerCount & 0xFF);
        
        return new ModBusMessage(address, 0x03, data);
    }
    
    /**
     * 构建读输入寄存器请求（功能码 0x04）
     * 
     * @param address 设备地址
     * @param registerAddress 寄存器起始地址
     * @param registerCount 寄存器数量
     * @return ModBusMessage 请求消息
     */
    public static ModBusMessage buildReadInputRegistersRequest(int address, 
                                                                int registerAddress, 
                                                                int registerCount) {
        byte[] data = new byte[4];
        data[0] = (byte) ((registerAddress >> 8) & 0xFF);
        data[1] = (byte) (registerAddress & 0xFF);
        data[2] = (byte) ((registerCount >> 8) & 0xFF);
        data[3] = (byte) (registerCount & 0xFF);
        
        return new ModBusMessage(address, 0x04, data);
    }
    
    /**
     * 构建写单个寄存器请求（功能码 0x06）
     * 
     * @param address 设备地址
     * @param registerAddress 寄存器地址
     * @param value 写入的值
     * @return ModBusMessage 请求消息
     */
    public static ModBusMessage buildWriteSingleRegisterRequest(int address, 
                                                                 int registerAddress, 
                                                                 int value) {
        byte[] data = new byte[4];
        data[0] = (byte) ((registerAddress >> 8) & 0xFF);
        data[1] = (byte) (registerAddress & 0xFF);
        data[2] = (byte) ((value >> 8) & 0xFF);
        data[3] = (byte) (value & 0xFF);
        
        return new ModBusMessage(address, 0x06, data);
    }
}
