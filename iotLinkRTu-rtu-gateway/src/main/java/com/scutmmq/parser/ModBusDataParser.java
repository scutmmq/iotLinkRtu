package com.scutmmq.parser;

import lombok.extern.slf4j.Slf4j;

/**
 * ModBus 数据解析工具类 - 温湿度数据解析
 * 
 * <p>提供 ModBus 寄存器数据的解析功能，支持湿度、温度等传感器数据的转换</p>
 * <p>所有解析方法均假设高字节在前（Big-Endian）</p>
 * 
 * @author mo.mingqin@xlink
 * @since 2026-03-08
 */
@Slf4j
public class ModBusDataParser {
    
    /**
     * 解析湿度值（寄存器地址 0x0000）
     * 
     * @param data 数据区字节数组（2 字节）
     * @return 湿度值（%RH）
     */
    public static float parseHumidity(byte[] data) {
        if (data == null || data.length != 2) {
            log.error("无效的湿度数据长度：{}", data != null ? data.length : 0);
            return Float.NaN;
        }
        
        // 高字节在前
        int highByte = data[0] & 0xFF;
        int lowByte = data[1] & 0xFF;
        int rawValue = (highByte << 8) | lowByte;
        
        // 扩大 10 倍的值，需要除以 10
        float humidity = rawValue / 10.0f;
        
        log.debug("原始值：{}, 湿度：{}%RH", rawValue, humidity);
        return humidity;
    }
    
    /**
     * 解析温度值（寄存器地址 0x0001）
     * 
     * @param data 数据区字节数组（2 字节）
     * @return 温度值（℃）
     */
    public static float parseTemperature(byte[] data) {
        if (data == null || data.length != 2) {
            log.error("无效的温度数据长度：{}", data != null ? data.length : 0);
            return Float.NaN;
        }
        
        // 高字节在前
        int highByte = data[0] & 0xFF;
        int lowByte = data[1] & 0xFF;
        int rawValue = (highByte << 8) | lowByte;
        
        // 处理有符号数（温度可能为负）
        short tempValue = (short) rawValue;
        
        // 扩大 10 倍的值，需要除以 10
        float temperature = tempValue / 10.0f;
        
        log.debug("原始值：{}, 温度：{}℃", tempValue, temperature);
        return temperature;
    }
    
    /**
     * 解析多个寄存器值
     * 
     * @param data 数据区字节数组
     * @param startRegister 起始寄存器地址
     * @param count 寄存器数量
     * @return 解析后的值数组
     */
    public static double[] parseMultipleRegisters(byte[] data, int startRegister, int count) {
        if (data == null || data.length < count * 2) {
            log.error("无效的数据长度，期望至少 {} 字节，实际 {} 字节", count * 2, 
                    data != null ? data.length : 0);
            return new double[0];
        }
        
        double[] values = new double[count];
        
        for (int i = 0; i < count; i++) {
            int highByte = data[i * 2] & 0xFF;
            int lowByte = data[i * 2 + 1] & 0xFF;
            int rawValue = (highByte << 8) | lowByte;
            
            // 处理有符号数
            short signedValue = (short) rawValue;
            values[i] = signedValue / 10.0;
        }
        
        return values;
    }
}
