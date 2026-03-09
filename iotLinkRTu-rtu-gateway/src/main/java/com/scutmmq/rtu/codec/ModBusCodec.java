package com.scutmmq.rtu.codec;

import com.scutmmq.rtu.protocol.ModBusMessage;
import com.scutmmq.rtu.utils.ModBusUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * ModBus-RTU 编解码器
 *
 * <p>实现 ModBus-RTU 协议的编码和解码功能：</p>
 * <ul>
 *     <li>编码：将 ModBusMessage 对象转换为字节流（地址码 + 功能码 + 数据区+CRC16）</li>
 *     <li>解码：将接收到的字节流解析为 ModBusMessage 对象</li>
 * </ul>
 *
 * <h3>编码规则：</h3>
 * <ol>
 *     <li>地址码 (1 字节) + 功能码 (1 字节) + 数据区 (N 字节) + CRC16(2 字节)</li>
 *     <li>CRC16 低字节在前，高字节在后</li>
 *     <li>16 位数据高字节在前</li>
 * </ol>
 *
 * @author mo.mingqin@xlink
 * @since 2026-03-08
 */
@Slf4j
public class ModBusCodec extends ByteToMessageCodec<ModBusMessage> {

    /**
     * 最小帧长度：地址 (1) + 功能码 (1) + 数据 (至少 2) + CRC(2) = 6 字节
     */
    private static final int MIN_FRAME_LENGTH = 6;

    /**
     * 最大帧长度限制
     */
    private static final int MAX_FRAME_LENGTH = 256;

    /**
     * 编码方法：将 ModBusMessage 对象编码为字节流
     *
     * @param ctx Channel 上下文
     * @param msg 待编码的 ModBusMessage 对象
     * @param out 输出字节缓冲区
     * @throws Exception 编码过程中可能抛出的异常
     */

    @Override
    protected void encode(ChannelHandlerContext ctx, ModBusMessage msg, ByteBuf out) throws Exception {
        log.debug("开始编码 ModBus 消息 - 地址：{}, 功能码：0x{}, 数据长度：{}",
                msg.getAddress(),
                String.format("%02X", msg.getFunctionCode()),
                msg.getData() != null ? msg.getData().length : 0);

        // 1. 写入地址码
        out.writeByte(msg.getAddress());

        // 2. 写入功能码
        out.writeByte(msg.getFunctionCode());

        // 写入数据长度
        out.writeByte(msg.getData().length);

        // 3. 写入数据区
        if (msg.getData() != null && msg.getData().length > 0) {
            out.writeBytes(msg.getData());
        }

        // 4. 计算并写入 CRC16
        byte[] frameData = buildFrameWithoutCrc(msg);
        short crc = ModBusUtil.calculateCrc16(frameData);

        log.debug("CRC 计算结果：0x{} (低位：0x{}, 高位：0x{})",
                String.format("%04X", crc),
                String.format("%02X", crc & 0xFF),
                String.format("%02X", (crc >> 8) & 0xFF));

        // 低字节在前
        out.writeByte(crc & 0xFF);      // CRC 低位
        out.writeByte((crc >> 8) & 0xFF); // CRC 高位

        log.debug("编码完成，总字节数：{}", out.writerIndex());
    }

    /**
     * 解码方法：将接收到的字节流解码为 ModBusMessage 对象
     *
     * @param ctx Channel 上下文
     * @param in 输入字节缓冲区
     * @param out 解码后的消息列表
     * @throws Exception 解码失败时抛出异常（如 CRC 校验失败）
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        log.debug("开始解码 ModBus 帧 - 可读字节数：{}", in.readableBytes());

        // 检查是否有足够的数据
        if (in.readableBytes() < MIN_FRAME_LENGTH) {
            log.debug("数据不足最小帧长度 (6 字节)，等待更多数据");
            return; // 等待更多数据
        }

        // 标记当前读取位置
        in.markReaderIndex();

        try {
            // 1. 读取地址码和函数码
            int address = in.readUnsignedByte();
            int functionCode = in.readUnsignedByte();

            log.debug("读取到地址码：0x{}, 功能码：0x{}", String.format("%02X", address), String.format("%02X", functionCode));

            // 2. 根据功能码确定数据长度（包括读取有效字节数字段）
            int dataLength = calculateDataLength(functionCode, in);

            log.debug("计算数据长度(包括有效字节数字段)：{} 字节", dataLength);

            // 3. 检查剩余字节是否足够
            if (in.readableBytes() < dataLength + 2) {
                log.debug("剩余字节不足 (需要：{}, 实际：{})，重置读取位置",
                        dataLength + 2, in.readableBytes());
                in.resetReaderIndex();
                return; // 数据不完整
            }

            // 4. 读取数据区（包含有效字节数字段 + 实际数据）
            byte[] fullData = new byte[dataLength];
            in.readBytes(fullData);

            log.debug("读取数据区：{} 字节", bytesToHex(fullData));

            // 5. 读取 CRC（低字节在前）
            int crcLow = in.readUnsignedByte();
            int crcHigh = in.readUnsignedByte();
            short receivedCrc = (short) ((crcHigh << 8) | crcLow);

            log.debug("读取到 CRC: 0x{} (低位：0x{}, 高位：0x{})",
                    String.format("%04X", receivedCrc),
                    String.format("%02X", crcLow),
                    String.format("%02X", crcHigh));

            // 6. 验证 CRC - 对完整帧（地址码 + 功能码 + 有效字节数 + 数据）计算 CRC
            byte[] frameWithoutCrc = buildFrameWithoutCrcWithByteCount(address, functionCode, fullData);
            short calculatedCrc = ModBusUtil.calculateCrc16(frameWithoutCrc);

            log.debug("计算的 CRC: 0x{}, 接收的 CRC: 0x{}",
                    String.format("%04X", calculatedCrc),
                    String.format("%04X", receivedCrc));

            if (calculatedCrc != receivedCrc) {
                log.error("CRC 校验失败！计算值：0x{}, 接收值：0x{}",
                        String.format("%04X", calculatedCrc),
                        String.format("%04X", receivedCrc));
                throw new IllegalArgumentException("CRC 校验失败");
            }

            log.info("CRC 校验通过 ✓");

            // 7. 提取实际数据（去掉有效字节数字段）
            byte[] actualData;
            if (functionCode == 0x03 || functionCode == 0x04) {
                // 去掉第一个字节（有效字节数），只保留实际数据
                actualData = new byte[dataLength - 1];
                System.arraycopy(fullData, 1, actualData, 0, actualData.length);
            } else {
                actualData = fullData;
            }

            // 8. 构建消息对象
            ModBusMessage message = new ModBusMessage();
            message.setAddress(address);
            message.setFunctionCode(functionCode);
            message.setData(actualData);
            message.setCrc16(receivedCrc);
            message.setRequest(false); // 应答帧

            log.debug("成功解析 ModBus 消息：地址={}, 功能码=0x{}, 数据={}",
                    message.getAddress(),
                    String.format("%02X", message.getFunctionCode()),
                    bytesToHex(message.getData()));

            out.add(message);

            log.debug("解码完成，添加消息到输出列表");

        } catch (Exception e) {
            log.error("解码失败，丢弃该帧", e);
            // CRC 校验失败或其他错误，丢弃该帧
            in.resetReaderIndex();
            throw e;
        }
    }

    /**
     * 根据功能码计算数据长度（包含有效字节数字段）
     *
     * @param functionCode 功能码
     * @param in 输入字节缓冲区
     * @return 数据长度（字节数）
     */
    private int calculateDataLength(int functionCode, ByteBuf in) {
        // 保存当前读取位置
        in.markReaderIndex();

        // 跳过地址码和功能码（已读）
        int remainingBytes = in.readableBytes();

        // 简单策略：根据功能码推断
        // 对于读寄存器响应，第一个字节是有效字节数
        switch (functionCode) {
            case 0x03: // 读保持寄存器
            case 0x04: // 读输入寄存器
                // 应答帧第一个字节是有效字节数（不包含这个字节本身）
                if (remainingBytes >= 1) {
                    int byteCount = in.readUnsignedByte();
                    in.resetReaderIndex(); // 重置，不消费有效字节数字段
                    return byteCount + 1; // +1 是因为要包含有效字节数这个字节本身
                }
                return 2; // 默认

            case 0x06: // 写单个寄存器
                return 4; // 地址 (2) + 值 (2)

            default:
                // 其他功能码，尝试从数据中推断
                return Math.max(2, remainingBytes - 2);
        }
    }

    /**
     * 字节数组转十六进制字符串
     *
     * @param bytes 待转换的字节数组
     * @return 十六进制字符串（字节间用空格分隔）
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * 构建包含有效字节数字段的不含 CRC 帧数据（用于 CRC 验证）
     *
     * @param address 设备地址
     * @param functionCode 功能码
     * @param fullData 完整数据（包含有效字节数字段）
     * @return 不含 CRC 的帧数据字节数组
     */
    private byte[] buildFrameWithoutCrcWithByteCount(int address, int functionCode, byte[] fullData) {
        // fullData 已经包含了有效字节数字段，直接使用
        byte[] frame = new byte[2 + fullData.length];

        frame[0] = (byte) address;
        frame[1] = (byte) functionCode;
        System.arraycopy(fullData, 0, frame, 2, fullData.length);

        return frame;
    }

    /**
     * 构建不包含 CRC 的帧数据
     *
     * @param address 设备地址
     * @param functionCode 功能码
     * @param data 数据区字节数组
     * @return 不含 CRC 的帧数据字节数组
     */
    private byte[] buildFrameWithoutCrc(int address, int functionCode, byte[] data) {
        int dataLen = data != null ? data.length : 0;
        byte[] frame = new byte[2 + dataLen];

        frame[0] = (byte) address;
        frame[1] = (byte) functionCode;

        if (data != null) {
            System.arraycopy(data, 0, frame, 2, dataLen);
        }

        return frame;
    }

    /**
     * 构建不包含 CRC 的帧数据（从 ModBusMessage）
     *
     * @param msg ModBusMessage 对象
     * @return 不含 CRC 的帧数据字节数组
     */
    private byte[] buildFrameWithoutCrc(ModBusMessage msg) {
        int dataLen = msg.getData() != null ? msg.getData().length : 0;
        byte[] frame = new byte[3 + dataLen];
        frame[0] = (byte) msg.getAddress();
        frame[1] = (byte) msg.getFunctionCode();
        frame[2] = (byte) (dataLen & 0xFF);

        if (msg.getData() != null) {
            System.arraycopy(msg.getData(), 0, frame, 3, dataLen);
        }

        return frame;
    }
}
