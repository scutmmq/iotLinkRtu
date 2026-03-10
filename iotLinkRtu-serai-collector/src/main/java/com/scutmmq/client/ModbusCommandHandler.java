package com.scutmmq.client;

/**
 * Modbus 命令处理器接口
 * 用于处理网关下发的配置命令
 */
@FunctionalInterface
public interface ModbusCommandHandler {

    /**
     * 处理 Modbus 命令
     * @param modbusCommand Modbus 命令帧
     * @return Modbus 响应帧，如果执行失败返回 null
     */
    byte[] handleCommand(byte[] modbusCommand);
}
