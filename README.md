# IoT Link RTU - 物联网RTU网关系统

## 项目简介

IoT Link RTU 是一个分布式物联网数据采集网关系统，采用 Maven 多模块架构。系统从串口设备以 1 秒间隔采集温湿度数据，通过 RTU 网关使用 Netty 处理，并通过 MQTT 转发到 Web 服务器进行管理和可视化展示。

### 核心特性

- 分布式部署：边缘采集与中心处理分离
- 高频采集：1 秒间隔实时数据采集
- 可靠传输：断网缓存与自动重连机制
- 纯字节流通信：自定义二进制协议，无 JSON 依赖
- 一对一架构：一个 RTU 对应一个串口，一个串口对应一个 Modbus 设备
- 集中管理：统一的 RTU 网关注册、配置和监控
- 高性能：全栈 Netty 实现，支持高并发
- 上下线控制：支持远程启用/禁用 RTU 采集功能
- 双数据库架构：PostgreSQL 存储管理数据，TDEngine 存储时序数据

## 系统架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              物联网 RTU 网关系统                              │
└─────────────────────────────────────────────────────────────────────────────┘

边缘设备层                    网关层                      业务层
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│              │         │              │         │              │
│ 温湿度传感器  │         │ rtu-gateway  │         │ web-server   │
│ (Modbus设备) │         │ (协议转换)    │         │ (业务服务)    │
│              │         │              │         │              │
└──────┬───────┘         └──────┬───────┘         └──────┬───────┘
       │                        │                        │
       │ Modbus-RTU             │ MQTT (JSON)            │
       │ (串口通信)              │                        │
       │                        │                        │
┌──────▼───────┐         ┌──────▼───────┐         ┌──────▼───────┐
│              │         │              │         │              │
│serial-       │         │ MQTT Broker  │         │   MySQL      │
│collector     │◄────────┤ (EMQX/       │◄────────┤  (数据存储)   │
│(边缘采集)     │  订阅    │  Mosquitto)  │  查询    │              │
│              │  配置    │              │         │              │
└──────────────┘         └──────────────┘         └──────────────┘
       │                                                  │
       │                                                  │
       │  自定义二进制协议 (TCP长连接)                      │
       │                                                  │
       └──────────────────┬───────────────────────────────┘
                          │
                          ▼
                  ┌──────────────┐
                  │ rtu-gateway  │
                  │ Netty TCP    │
                  │   Server     │
                  └──────────────┘
```

### 数据流向

```
1. 数据采集流程：
   温湿度传感器 ──Modbus-RTU──► serial-collector ──二进制帧──► rtu-gateway
                  (串口)           (封装)          (TCP)      (解析)
                                                                 │
                                                                 ▼
   web-server ◄──订阅── MQTT Broker ◄──发布── rtu-gateway
   (存储到MySQL)                        (JSON格式)

2. 配置下发流程：
   web-server ──发布──► MQTT Broker ──订阅──► rtu-gateway ──二进制帧──► serial-collector
   (管理界面)                                  (解析JSON)    (TCP)      (转Modbus命令)
                                                                              │
                                                                              ▼
                                                                        温湿度传感器
                                                                        (执行配置)

3. 认证流程：
   serial-collector ──认证帧(rtuId+secret)──► rtu-gateway ──验证API──► web-server
                      (二进制)                  (解析)                  (查询数据库)
                                                   │
                                                   ◄──认证结果──
                                                   (建立映射)
```

### 物理部署拓扑

```
┌─────────────────────────────────────────────────────────────────────┐
│                          中心服务器 (192.168.1.100)                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │ rtu-gateway  │  │ MQTT Broker  │  │ web-server   │              │
│  │   :8888      │  │   :1883      │  │   :8080      │              │
│  └──────────────┘  └──────────────┘  └──────────────┘              │
│                                        ┌──────────────┐              │
│                                        │    MySQL     │              │
│                                        │   :3306      │              │
│                                        └──────────────┘              │
└─────────────────────────────────────────────────────────────────────┘
                          │
                          │ Internet / LAN
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
┌───────▼────────┐ ┌──────▼───────┐ ┌──────▼───────┐
│ 边缘设备1       │ │ 边缘设备2     │ │ 边缘设备N     │
│ RTU-001        │ │ RTU-002      │ │ RTU-N        │
│                │ │              │ │              │
│ serial-        │ │ serial-      │ │ serial-      │
│ collector      │ │ collector    │ │ collector    │
│                │ │              │ │              │
│ ┌────────────┐ │ │ ┌──────────┐ │ │ ┌──────────┐ │
│ │温湿度传感器 │ │ │ │温湿度传感器│ │ │温湿度传感器│ │
│ │  (COM3)    │ │ │ │  (COM3)  │ │ │ │  (COM3)  │ │
│ └────────────┘ │ │ └──────────┘ │ │ └──────────┘ │
└────────────────┘ └──────────────┘ └──────────────┘
```

## 模块说明

### serai-collector（串口采集模块）

运行在边缘设备（工控机/树莓派）上的数据采集程序。

核心职责：
- 串口通信管理（jSerialComm）
- Modbus-RTU 协议实现
- 定时数据采集（1秒间隔）
- 断网数据缓存
- TCP 客户端（连接 rtu-gateway）

技术栈：
- Java 17
- Netty 4.1.107（TCP客户端）
- jSerialComm 2.10.4（串口通信）
- Hutool 5.8.26（工具类）

### rtu-gateway（RTU网关模块）

运行在中心服务器的协议转换网关。

核心职责：
- TCP 服务端（接收多个采集器连接）
- 二进制协议解析
- Modbus 数据解析
- MQTT 消息发布（JSON格式）
- 连接状态管理

技术栈：
- Java 17
- Netty 4.1.107（TCP服务端）
- Gson 2.10.1（JSON处理）
- Eclipse Paho MQTT（待集成）

### web-server（业务服务模块）

运行在中心服务器的 Web 服务，基于 Netty 实现。

核心职责：
- MQTT 订阅（接收网关数据）
- 数据持久化（MySQL）
- HTTP API 服务（Netty HTTP Server）
- RTU 管理和配置
- 报警处理和推送

技术栈：
- Java 17
- Netty 4.1.107（HTTP服务端）
- MySQL 8.0+
ent
- 后续自行实现

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- PostgreSQL 12+（管理数据）
- TDEngine 3.0+（时序数据）
- MQTT Broker（EMQX 或 Mosquitto）
- 串口设备（开发环境可使用虚拟串口）

### 数据库架构

本系统采用**双数据库架构**：

**PostgreSQL**（关系型数据库）
- 用途：存储管理类数据
- 表：rtu_gateway、rtu_config、rtu_alarm
- 特点：支持复杂查询和事务

**TDEngine**（时序数据库）
- 用途：存储时序采集数据
- 表：rtu_data（超表）
- 特点：高性能写入、高压缩比、自动分区

### 编译项目

```bash
# 克隆项目
git clone <repository-url>
cd iotLinkRtu

# 编译所有模块
mvn clean install

# 编译单个模块
cd serai-collector
mvn clean package
```

### 运行模块

#### 1. 启动 MQTT Broker

```bash
# 使用 Docker 启动 EMQX
docker run -d --name emqx \
  -p 1883:1883 \
  -p 8083:8083 \
  -p 8084:8084 \
  -p 8883:8883 \
  -p 18083:18083 \
  emqx/emqx:latest

# 访问管理界面：http://localhost:18083
# 默认用户名/密码：admin/public
```

#### 2. 启动数据库

**PostgreSQL**
```bash
# 使用 Docker 启动 PostgreSQL
docker run -d --name postgres \
  -p 5432:5432 \
  -e POSTGRES_DB=iot_rtu \
  -e POSTGRES_USER=iot_user \
  -e POSTGRES_PASSWORD=iot_pass \
  postgres:14

# 导入数据库脚本
psql -h localhost -U iot_user -d iot_rtu < doc/schema_postgres.sql
```

**TDEngine**
```bash
# 使用 Docker 启动 TDEngine
docker run -d --name tdengine \
  -p 6030:6030 \
  -p 6041:6041 \
  -p 6043-6049:6043-6049 \
  -p 6043-6049:6043-6049/udp \
  tdengine/tdengine:3.2.0.0

# 创建数据库和超级表
taos -h localhost -P 6030 < doc/schema_tdengine.sql
```

#### 3. 启动 rtu-gateway

```bash
cd rtu-gateway/target
java -jar rtu-gateway-1.0.0-SNAPSHOT-jar-with-dependencies.jar

# 默认监听端口：8888
```

#### 4. 启动 serai-collector

```bash
cd serai-collector/target
java -jar serai-collector-1.0.0-SNAPSHOT-jar-with-dependencies.jar

# 配置文件：src/main/resources/config.properties
```

#### 5. 启动 web-server（待实现）

```bash
cd web-server/target
java -jar web-server-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```

## 配置说明

### serai-collector 配置

编辑 `serai-collector/src/main/resources/config.properties`：

```properties
# RTU标识
rtu.id=RTU001

# 串口配置
serial.port=COM3
serial.baudrate=9600
serial.databits=8
serial.stopbits=1
serial.parity=0

# Modbus配置
modbus.device.address=1
modbus.function.code=3
modbus.register.address=0
modbus.register.count=2

# 采集配置
collect.interval=1000

# 网关连接
gateway.host=localhost
gateway.port=8888
```

### rtu-gateway 配置

编辑 `rtu-gateway/src/main/java/com/scutmmq/rtu/config/Config.java`：

```java
public class Config {
    public static final int SERVER_PORT = 8888;
    public static final String MQTT_BROKER = "tcp://localhost:1883";
    public static final String MQTT_CLIENT_ID = "rtu-gateway";
}
```

## 文档

- [项目设计文档](doc/项目设计文档.md) - 完整的系统架构和设计说明
- [接口设计文档](doc/接口设计文档.md) - 模块职责和接口规范
- [MQTT消息设计文档](doc/MQTT消息设计文档.md) - MQTT消息格式和Topic设计
- [通信协议文档](doc/通信协议文档.md) - 自定义二进制协议规范

## 测试

### 单元测试

```bash
# 运行所有测试
mvn test

# 运行单个模块测试
cd serai-collector
mvn test
```

### 集成测试

```bash
# serai-collector 集成测试
cd serai-collector
mvn test -Dtest=DataCollectionFlowTest
```

## 开发指南

### 项目结构

```
iotLinkRtu/
├── serai-collector/          # 串口采集模块
│   ├── src/main/java/
│   │   └── comal/
│   │       ├── Application.java
│   │       ├── manager/      # 串口管理
│   │       ├── protocol/     # 协议实现
│   │       ├── client/       # 网关客户端
│   │       └── service/      # 业务服务
│   └── pom.xml
├── rtu-gateway/              # RTU网关模块
│   ├── src/main/java/
│   │   └── com/scutmmq/rtu/
│   │       ├── Application.java
│   │       ├── server/       # TCP服务端
│   │       ├── codec/        # 编解码器
│   │       ├── parser/       # 数据解析
│   │       └── handler/      # 业务处理
│   └── pom.xml
├── web-server/               # Web服务模块（待实现）
│   └── pom.xml
├── doc/                      # 项目文档
│   ├── 项目设计文档.md
│   ├── 接口设计文档.md
│   ├── MQTT消息设计文档.md
│   └── 通信协议文档.md
├── pom.xml                   # 父项目POM
└── README.md
```

### 代码规范

- 使用 Java 17 特性
- 遵循阿里巴巴 Java 开发手册
- 使用 Lombok 简化代码
- 单元测试覆盖率 > 70%
- 关键业务逻辑必须有注释

### 提交规范

```bash
# 功能开发
git commit -m "feat: 添加XXX功能"

# Bug修复
git commit -m "fix: 修复XXX问题"

# 文档更新
git commit -m "docs: 更新XXX文档"

# 代码重构
git commit -m "refactor: 重构XXX模块"

# 性能优化
git commit -m "perf: 优化XXX性能"

# 测试相关
git commit -m "test: 添加XXX测试"
```

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 开发语言 |
| Maven | 3.8+ | 项目管理 |
| Netty | 4.1.107 | 网络通信框架 |
| jSerialComm | 2.10.4 | 串口通信 |
| Hutool | 5.8.26 | Java工具类库 |
| Gson | 2.10.1 | JSON处理 |
| Logback | 1.4.14 | 日志框架 |
| JUnit 5 | 5.10.1 | 单元测试 |
| PostgreSQL | 12+ | 管理数据存储 |
| TDEngine | 3.0+ | 时序数据存储 |
| MQTT | 3.1.1 | 消息队列 |

## 核心功能

### 上下线控制机制

系统支持远程控制 RTU 的采集和上报功能：

**状态字段**：
- `status`：控制是否允许采集（ENABLED/DISABLED）
- `online`：显示当前在线状态（ONLINE/OFFLINE）

**控制流程**：
1. 用户在 web 界面注册 RTU，默认 `status=ENABLED`
2. RTU 连接并认证时，rtu-gateway 检查 `status` 字段
3. 如果 `status=DISABLED`，拒绝认证，RTU 无法上报数据
4. 如果 `status=ENABLED`，允许连接，设置 `online=ONLINE`
5. 用户可以在 web 界面修改 `status` 来远程启用/禁用 RTU

**使用场景**：
- 设备维护时临时禁用数据采集
- 异常设备的远程隔离
- 分批次启用新设备

### 双数据库架构

**数据分离存储**：
```
采集数据流
    ↓
web-server
    ├─→ PostgreSQL（管理数据）
    │   - rtu_gateway：RTU 注册信息
    │   - rtu_config：配置参数
    │   - rtu_alarm：报警记录
    │
    └─→ TDEngine（时序数据）
        - rtu_data：温湿度采集数据
```

**优势**：
- PostgreSQL：支持复杂查询、事务、关联查询
- TDEngine：高性能写入（百万级/秒）、高压缩比（1/10）、自动分区

## 性能指标

- 数据采集频率：1秒/次
- TCP连接数：支持 1000+ 并发连接
- 消息吞吐量：10000+ msg/s
- 数据延迟：< 100ms（端到端）
- 断网缓存：支持 10000+ 条数据缓存

## 安全特性

- MQTT 用户名密码认证
- TCP 连接认证机制
- 数据传输 CRC 校验
- 敏感配置加密存储
- SQL 注入防护

## 后续规划

### v1.1.0
- 完成 web-server 模块（Netty HTTP Server）
- 集成 MQTT 客户端
- 实现数据持久化
- 实现 RESTful API

### v1.2.0
- 支持多种传感器类型
- 实现数据可视化大屏
- 添加报警推送（邮件/短信/钉钉）n
### v2.0.0
- 支持 Modbus-TCP 协议
- 支持集群部署
- 实现数据分析和预测
- 移动端 App

## 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 作者

- 开发者 - 初始工作

## 致谢

- Netty - 高性能网络框架
- jSerialComm - Java串口通信库
- Hutool - Java工具类库
- EMQX - 高性能MQTT Broker

## 联系方式

- 项目主页：<repository-url>
- 问题反馈：<repository-url>/issues
