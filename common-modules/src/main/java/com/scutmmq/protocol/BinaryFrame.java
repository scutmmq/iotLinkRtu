package com.scutmmq.protocol;

/**
 * 二进制帧对象
 * 表示解析后的帧数据
 */
public class BinaryFrame {

    private final byte type;
    private final byte[] data;

    public BinaryFrame(byte type, byte[] data) {
        this.type = type;
        this.data = data;
    }

    public byte getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }

    /**
     * 判断是否为认证响应帧
     */
    public boolean isAuthResponse() {
        return type == BinaryProtocol.TYPE_AUTH_RESPONSE;
    }

    /**
     * 判断是否为心跳响应帧
     */
    public boolean isHeartbeatResponse() {
        return type == BinaryProtocol.TYPE_HEARTBEAT_RESPONSE;
    }

    /**
     * 判断是否为 Modbus 命令帧
     */
    public boolean isModbusCommand() {
        return type == BinaryProtocol.TYPE_MODBUS_COMMAND;
    }

    /**
     * 获取认证结果（仅用于认证响应帧）
     * @return true=认证成功, false=认证失败
     */
    public boolean getAuthResult() {
        if (!isAuthResponse() || data.length < 1) {
            throw new IllegalStateException("Not an auth response frame");
        }
        return data[0] == 0x01;
    }

    @Override
    public String toString() {
        return String.format("BinaryFrame{type=0x%02X, dataLength=%d}", type, data.length);
    }
}
