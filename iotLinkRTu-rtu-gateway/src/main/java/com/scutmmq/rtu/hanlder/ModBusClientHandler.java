package com.scutmmq.rtu.hanlder;

import com.scutmmq.rtu.parser.ModBusDataParser;
import com.scutmmq.rtu.protocol.ModBusMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * ModBus 客户端处理器
 * 
 * <p>负责处理从 ModBus 从机返回的响应消息，解析寄存器数据（如温湿度等传感器数据）</p>
 * <p>继承自 SimpleChannelInboundHandler，自动处理消息类型转换和资源释放</p>
 * 
 * @author mo.mingqin@xlink
 * @since 2026-03-08
 */
@Slf4j
public class ModBusClientHandler extends SimpleChannelInboundHandler<ModBusMessage> {

    /**
     * 读取并处理 ModBus 响应消息
     *
     * @param ctx Channel 上下文
     * @param msg 接收到的 ModBusMessage 对象
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ModBusMessage msg) throws Exception {
        log.info("收到从机应答 - 地址：{}, 功能码：0x{}, 数据长度：{}",
                msg.getAddress(), msg.getFunctionCode(), msg.getData().length);

        // 解析数据（以温湿度为例）
        if (msg.getFunctionCode() == 0x03 || msg.getFunctionCode() == 0x04) {
            parseRegisterData(msg);
        }
    }
    
    /**
     * 解析寄存器数据
     * 
     * <p>根据数据长度判断包含的寄存器数量，并分别解析湿度、温度等传感器数据</p>
     * <p>假设第一个寄存器为湿度，第二个为温度</p>
     *
     * @param msg 包含寄存器数据的 ModBusMessage 对象
     */
    private void parseRegisterData(ModBusMessage msg) {
        byte[] data = msg.getData();
        
        if (data.length < 2) {
            log.error("数据长度不足，无法解析");
            return;
        }
        
        // 根据数据长度判断包含几个寄存器值
        int registerCount = data.length / 2;
        
        log.info("解析 {} 个寄存器值", registerCount);
        
        for (int i = 0; i < registerCount; i++) {
            byte[] registerData = new byte[2];
            System.arraycopy(data, i * 2, registerData, 0, 2);
            
            // 计算实际寄存器地址（需要根据请求的寄存器地址确定）
            // 这里简单处理：假设第一个是湿度，第二个是温度
            if (i == 0 && registerCount >= 1) {
                // 解析湿度
                float humidity = ModBusDataParser.parseHumidity(registerData);
                log.info("【湿度】原始数据：{} {}, 实际值：{}%RH", 
                        String.format("%02X", registerData[0]), 
                        String.format("%02X", registerData[1]), 
                        humidity);
            } else if (i == 1 && registerCount >= 2) {
                // 解析温度
                float temperature = ModBusDataParser.parseTemperature(registerData);
                log.info("【温度】原始数据：{} {}, 实际值：{}℃", 
                        String.format("%02X", registerData[0]), 
                        String.format("%02X", registerData[1]), 
                        temperature);
            } else {
                // 其他寄存器值
                int highByte = registerData[0] & 0xFF;
                int lowByte = registerData[1] & 0xFF;
                int rawValue = (highByte << 8) | lowByte;
                short signedValue = (short) rawValue;
                double value = signedValue / 10.0;
                log.info("【寄存器{}】原始数据：{} {}, 实际值：{}", 
                        i, 
                        String.format("%02X", registerData[0]), 
                        String.format("%02X", registerData[1]), 
                        value);
            }
        }
    }
    
    /**
     * 处理通讯过程中的异常情况
     *
     * @param ctx Channel 上下文
     * @param cause 异常原因
     * @throws Exception 关闭连接时可能抛出的异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("通讯异常", cause);
        ctx.close();
    }
}
