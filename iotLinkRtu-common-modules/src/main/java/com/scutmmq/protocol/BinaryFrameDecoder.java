package com.scutmmq.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 二进制帧解码器
 * 用于解析 rtu-gateway 返回的二进制帧
 */
public class BinaryFrameDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(BinaryFrameDecoder.class);

    private static final byte FRAME_HEADER_1 = (byte) 0xAA;
    private static final byte FRAME_HEADER_2 = (byte) 0x55;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 至少需要 6 字节（帧头2 + 类型1 + 长度2 + 校验1）
        if (in.readableBytes() < 6) {
            return;
        }

        // 标记读指针
        in.markReaderIndex();

        // 检查帧头
        byte header1 = in.readByte();
        byte header2 = in.readByte();
        if (header1 != FRAME_HEADER_1 || header2 != FRAME_HEADER_2) {
            // 帧头错误，丢弃1字节并继续查找
            in.resetReaderIndex();
            in.readByte();
            logger.warn("帧头错误，丢弃字节: 0x{}", String.format("%02X", header1));
            return;
        }

        // 读取帧类型
        byte frameType = in.readByte();

        // 读取数据长度（大端序）
        short dataLength = in.readShort();

        // 检查是否有足够的数据（数据体 + 校验和）
        if (in.readableBytes() < dataLength + 1) {
            in.resetReaderIndex();
            return;
        }

        // 读取数据体
        byte[] data = new byte[dataLength];
        in.readBytes(data);

        // 读取校验和
        byte checksum = in.readByte();

        // 验证校验和
        in.resetReaderIndex();
        byte[] frameBytes = new byte[5 + dataLength]; // 帧头2 + 类型1 + 长度2 + 数据
        in.readBytes(frameBytes);
        in.readByte(); // skip checksum

        byte calculatedChecksum = calculateChecksum(frameBytes);
        if (calculatedChecksum != checksum) {
            logger.warn("校验和错误: 期望=0x{}, 实际=0x{}",
                String.format("%02X", calculatedChecksum),
                String.format("%02X", checksum));
            return;
        }

        // 输出解析后的帧
        BinaryFrame frame = new BinaryFrame(frameType, data);
        out.add(frame);

        logger.debug("解析帧成功: 类型=0x{}, 数据长度={}",
            String.format("%02X", frameType), dataLength);
    }

    /**
     * 计算校验和（异或）
     */
    private byte calculateChecksum(byte[] data) {
        byte checksum = 0;
        for (byte b : data) {
            checksum ^= b;
        }
        return checksum;
    }
}
