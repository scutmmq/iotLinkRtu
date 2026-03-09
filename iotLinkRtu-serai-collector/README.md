# ModBus-RTU 串口采集服务

这是一个基于 Java 的串口采集服务，用于读取 ModBus-RTU 设备（如温湿度传感器）并转发给 RTU 网关。

## 功能特性

- **串口通信**: 使用 `jSerialComm` 进行跨平台串口通信。
- **协议支持**: 实现 ModBus-RTU 基础轮询与 CRC16 校验。
- **数据转发**: 使用 Netty 客户端将验证后的数据通过 TCP 转发至 RTU 网关。
- **自动重连**: 支持网关连接断开自动重连。

## 环境要求

- JDK 17+
- Maven 3.6+

## 配置参数

目前配置参数在代码中可以修改 (`com.th.serial.service.DataForwarderService`):

- 串口号: `COM3` (默认)
- 波特率: `9600`
- 网关地址: `127.0.0.1:502`
- 轮询间隔: `5000ms`

## 运行方式

1. 编译打包:
   ```bash
   mvn clean package
   ```

2. 运行:
   ```bash
   java -jar target/th-serial-1.0-SNAPSHOT.jar
   ```

## 项目结构

- `com.th.serial.manager`: 串口管理层
- `com.th.serial.client`: Netty TCP客户端
- `com.th.serial.service`: 业务逻辑层（轮询 scheduler）
- `com.th.serial.utils`: 工具类 (CRC16)

