package com.scutmmq.rtu;

import com.scutmmq.rtu.codec.ModBusCodec;
import com.scutmmq.rtu.hanlder.ModBusClientHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class TestCodec {
    public static void main(String[] args) {
        EmbeddedChannel channel = new EmbeddedChannel(
                // Inbound 方向：数据从右向左处理
                new ModBusCodec(),
                new ModBusClientHandler(),
                new LoggingHandler(LogLevel.DEBUG)
        );
        // 地址码 功能码 返回有效字节数 湿度值 温度值 校验码低位 校验码高位
        // 0x01 0x03 0x04 0x02 0x92 0xFF 0x9B 0xDC 0xC3
        channel.writeInbound(Unpooled.copiedBuffer(new byte[]{
                0x01, //地址码
                0x03, //功能码
                0x04, // 返回有效字节数
                0x02, (byte) 0x92, // 湿度
                (byte) 0xFF,(byte) 0x9B, // 温度
                (byte) 0xDC,  // 校验码低位
                (byte) 0xC3   // 校验码高位
        }));
    }
}
