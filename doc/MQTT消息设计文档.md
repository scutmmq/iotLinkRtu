# 物联网 RTU 网关系统 - MQTT 消息设计文档

## 目录

- [1. 文档概述](#1-文档概述)
- [2. MQTT架构设计](#2-mqtt架构设计)
  - [2.1 Broker选型](#21-broker选型)
  - [2.2 连接参数](#22-连接参数)
  - [2.3 QoS策略](#23-qos策略)
- [3. Topic设计规范](#3-topic设计规范)
  - [3.1 Topic命名规则](#31-topic命名规则)
  - [3.2 Topic层级结构](#32-topic层级结构)
- [4. 消息类型定义](#4-消息类型定义)
  - [4.1 数据上报消息](#41-数据上报消息)
  - [4.2 心跳消息](#42-心跳消息)
  - [4.3 状态变更消息](#43-状态变更消息)
  - [4.4 配置下发消息](#44-配置下发消息)
  - [4.5 配置响应消息](#45-配置响应消息)
  - [4.6 报警消息](#46-报警消息)
  - [4.7 控制指令消息](#47-控制指令消息)
- [5. 消息格式规范](#5-消息格式规范)
  - [5.1 通用消息结构](#51-通用消息结构)
  - [5.2 时间戳格式](#52-时间戳格式)
  - [5.3 数据精度要求](#53-数据精度要求)
- [6. 错误处理机制](#6-错误处理机制)
- [7. 安全设计](#7-安全设计)
- [8. 性能优化建议](#8-性能优化建议)

---

## 1. 文档概述

本文档定义了物联网RTU网关系统中MQTT消息的完整规范，包括：
- MQTT Broker的连接配置和QoS策略
- Topic的命名规则和层级结构
- 各类消息的格式定义和使用场景
- 错误处理和安全机制

**适用模块**:
- rtu-gateway（MQTT Publisher）
- web-server（MQTT Subscriber）

**版本**: v1.0.0
**更新日期**: 2026-03-09

---

## 2. MQTT架构设计

### 2.1 Broker选型

推荐使用以下MQTT Broker：

| Broker | 适用场景 | 优势 |
|--------|---------|------|
| **EMQX** | 生产环境 | 高性能、集群支持、丰富的管理功能 |
| **Mosquitto** | 开发/小规模部署 | 轻量级、易部署、开源免费 |
| **HiveMQ** | 企业级应用 | 商业支持、高可用、安全性强 |

**推荐配置**: EMQX 5.x（支持MQTT 5.0协议）

### 2.2 连接参数

#### rtu-gateway连接配置
```properties
# MQTT Broker地址
mqtt.broker.url=tcp://localhost:1883
# 客户端ID（使用RTU ID作为唯一标识）
mqtt.client.id=rtu-gateway-${rtuId}
# 用户名密码（生产环境必须配置）
mqtt.username=rtu_gateway
mqtt.password=your_secure_password
# 连接超时（秒）
mqtt.connect.timeout=30
# 保持连接间隔（秒）
mqtt.keepalive.interval=60
# 自动重连
mqtt.auto.reconnect=true
# 重连间隔（毫秒）
mqtt.reconnect.interval=5000
# 清除会话
mqtt.clean.session=false
```

#### web-server连接配置
```properties
mqtt.broker.url=tcp://localhost:1883
mqtt.client.id=web-server-${instanceId}
mqtt.username=web_server
mqtt.password=your_secure_password
mqtt.connect.timeout=30
mqtt.keepalive.interval=60
mqtt.auto.reconnect=true
mqtt.clean.session=false
```

### 2.3 QoS策略

| 消息类型 | QoS级别 | 原因 |
|---------|---------|------|
| 数据上报 | **QoS 1** | 确保数据至少送达一次，允许少量重复 |
| 心跳消息 | **QoS 0** | 高频发送，丢失可接受 |
| 状态变更 | **QoS 1** | 重要状态需确保送达 |
| 配置下发 | **QoS 2** | 配置指令必须且仅送达一次 |
| 报警消息 | **QoS 1** | 报警需确保送达，允许去重 |
| 控制指令 | **QoS 2** | 控制指令必须精确执行一次 |

---

## 3. Topic设计规范

### 3.1 Topic命名规则

**基本原则**:
- 使用小写字母和下划线
- 层级清晰，语义明确
- 避免使用特殊字符
- 长度控制在128字符以内

**通配符使用**:
- `+`: 单层通配符，匹配一个层级
- `#`: 多层通配符，匹配多个层级（只能用在末尾）

### 3.2 Topic层级结构

```
iot/
├── rtu/
│   ├── {rtuId}/
│   │   ├── data              # 数据上报
│   │   ├── heartbeat         # 心跳
│   │   ├── status            # 状态变更
│   │   ├── alarm             # 报警消息
│   │   ├── config/
│   │   │   ├── request       # 配置下发
│   │   │   └── response      # 配置响应
│   │   └── control/
│   │       ├── request       # 控制指令
│   │       └── response      # 控制响应
│   └── broadcast/
│       └── config            # 广播配置（所有RTU）
└── system/
    ├── online                # RTU上线通知
    └── offline               # RTU离线通知
```

#### Topic详细说明

| Topic | 发布者 | 订阅者 | 说明 |
|-------|--------|--------|------|
| `iot/rtu/{rtuId}/data` | rtu-gateway | web-server | 温湿度数据上报 |
| `iot/rtu/{rtuId}/heartbeat` | rtu-gateway | web-server | 心跳保活 |
| `iot/rtu/{rtuId}/status` | rtu-gateway | web-server | 状态变更（上线/离线/故障） |
| `iot/rtu/{rtuId}/alarm` | rtu-gateway | web-server | 报警消息 |
| `iot/rtu/{rtuId}/config/request` | web-server | rtu-gateway | 配置下发 |
| `iot/rtu/{rtuId}/config/response` | rtu-gateway | web-server | 配置响应 |
| `iot/rtu/{rtuId}/control/request` | web-server | rtu-gateway | 控制指令 |
| `iot/rtu/{rtuId}/control/response` | rtu-gateway | web-server | 控制响应 |
| `iot/rtu/broadcast/config` | web-server | rtu-gateway | 广播配置 |
| `iot/system/online` | rtu-gateway | web-server | RTU上线通知 |
| `iot/syste rtu-gateway | web-server | RTU离线通知 |

#### 订阅示例

```java
// web-server订阅所有RTU的数据
mqttClient.subscribe("iot/rtu/+/data", 1);

// web-server订阅所有RTU的报警
mqttClient.subscribe("iot/rtu/+/alarm", 1);

// web-server订阅系统消息
mqttClient.subscribe("iot/system/#", 1);

// rtu-gateway订阅自己的配置
mqttClient.subscribe("iot/rtu/" + rtuId + "/config/request", 2);

// rtu-gateway订阅广播配置
mqttClient.subscribe("iot/rtu/broadcast/config", 2);
```

---

## 4. 消息类型定义

### 4.1 数据上报消息

**Topic**: `iot/rtu/{rtuId}/data`
**QoS**: 1
**发布频率**: 1秒/次（可配置）
**Retain**: false

#### 消息格式
```json
{
  "msgId": "550e8400-e29b-41d4-a716-446655440000",
  "msgType": "data_report",
  "rtuId": "RTU001",
  "timestamp": 1709971200000,
  "deviceAddress": 1,
  "functionCode": 3,
  "data": {
    "temperature": 25.5,
    "humidity": 50.2
  },
  "quality": "good",
  "rawModbus": "0103040FA01F4B2C3"
}
```

#### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| msgId | String | 是 | 消息唯一ID（UUID） |
| msgType | String | 是 | 消息类型：data_report |
| rtuId | String | 是 | RTU唯一标识 |
| timestamp | Long | 是 | 采集时间戳（毫秒） |
| deviceAddress | Integer | 是 | Modbus设备|
| functionCode | Integer | 是 | Modbus功能码 |
| data.temperature | Float | 是 | 温度值（°C），保留1位小数 |
| data.humidity | Float | 是 | 湿度值（%），保留1位小数 |
| quality | String | 是 | 数据质量：good/bad/uncertain |
| rawModbus | String | 否 | Modbus原始数据（十六进制字符串） |

---

### 4.2 心跳消息

**Topic**: `iot/rtu/{rtuId}/heartbeat`
**QoS**: 0
**发布频率**: 30秒/次
**Retain**: false

#### 消息格式
```json
{
  "msgId": "550e8400-e29b-41d4-a716-446655440001",
  "msgType": "heartbeat",
  "rtuId": "RTU001",
  "timestamp": 1709971200000,
  "status": "online",
  "uptime": 86400,
  "memoryUsage": 45.2,
  "cpuUsage": 12.5
}
```

##
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| msgId | String | 是 | 消息唯一ID |
| msgType | String | 是 | 消息类型：heartbeat |
| rtuId | String | 是 | RTU唯一标识 |
| timestamp | Long | 是 | 心跳时间戳（毫秒） |
| status | String | 是 | 运行状态：online/busy/error |
| uptime | Long | 否 | 运行时长（秒） |
| memoryUsage | Float | 否 | 内存使用率（%） |
| cpuUsage | Float | 否 | CPU使用率（%） |

---

### 4.3 状态变更消息

**Topic**: `iot/rtu/{rtuId}/status`
**QoS**: 1
**发布时机**: 状态变化时
**Retain**: true（保留最后状态）

#### 消息格式
```json
{
  "msgId": "550e8400-e29b-41d4-a716-446655440002",
  "msgType": "status_change",
  "rtuId": "RTU001",
  "timestamp": 1709971200000,
  "oldStatus": "online",
  "newStatus": "offline",
  "reason": "network_disconnected",
  "details": "TCP连接断开"
}
```

#### 状态枚举

| 状态值 | 说明 |
|--------|------|
| online | 在线正常 |
| offline | 离线 |
| busy | 忙碌中 |
| error | 故障 |
| maintenance | 维护中 |

#### 原因枚举

| 原因值 | 说明 |
|--------|------|
| network_disconnected | 网络断开 |
| serial_port_error | 串口错误 |
| modbus_timeout | Modbus超时 |
| system_restart | 系统重启 |
| manual_shutdown | 手动关闭 |

---

### 4.4 配置下发消息

**Topic**: `iot/rtu/{rtuId}/config/request`
**QoS**: 2
**发布时机**:
**Retain**: false

#### 消息格式
```json
{
  "msgId": "550e8400-e29b-41d4-a716-446655440003",
  "msgType": "config_request",
  "rtuId": "RTU001",
  "timestamp": 1709971200000,
  "configType": "sampling_interval",
  "config": {
    "samplingInterval": 5,
    "temperatureThreshold": {
      "min": 18.0,
      "max": 28.0
    },
    "humidityThreshold": {
      "min": 40.0,
      "max": 60.0
    },
    "alarmEnabled": true
  }
}
```

#### 配置类型

| 配置类型 | 说明 |
|---------|------|
| sampling_interval | 采样间隔配置 |
| threshold | 阈值配置 |
| alarm | 报警配置 |
| network | 网络配置 |
| full_config | 完整配置 |

---

### 4.5 配置响应消息

**Topic**: `iot/rtu/{rtuId}/config/response`
**QoS**: 1
**发布时机**: 收到配置请求后
**Retain**: false

#### 消息格式
```json
{
  "msgId": "550e8400-e29b-41d4-a716-446655440004",
  "msgType": "config_response",
  "rtuId": "RTU001",
  "timestamp": 1709971200000,
  "requestMsgId": "550e8400-e29b-41d4-a716-446655440003",
  "success": true,
  "message": "配置应用成功",
  "appliedConfig": {
    "samplingInterval": 5
  }
}
```

#### 失败响应示例
```json
{
  "msgId": "550e8400-e29b-41d4-a716-446655440005",
  "msgType": _response",
  "rtuId": "RTU001",
  "timestamp": 1709971200000,
  "requestMsgId": "550e8400-e29b-41d4-a716-446655440003",
  "success": false,
  "errorCode": "INVALID_INTERVAL",
  "message": "采样间隔必须在1-60秒之间"
}
```

---

### 4.6 报警消息

**Topic**: `iot/rtu/{rtuId}/alarm`
**QoS**: 1
**发布时机**: 检测到异常时
**Retain**: false

#### 消息格式
```json
{
  "msgId": "550e8400-e29b-41d4-a716-446655440006",
  "msgType": "alarm",
  "rtuId": "RTU001",
  "timestamp": 1709971200000,
  "alarmType": "temperh",
  "alarmLevel": "warning",
  "currentValue": 32.5,
  "thresholdValue": 28.0,
  "description": "温度超过上限阈值",
  "suggestion": "请检查空调设备"
}
```

#### 报警类型

| 报警类型 | 说明 |
|---------|------|
| temperature_high | 温度过高 |
| temperature_low | 温度过低 |
| humidity_high | 湿度过高 |
| humidity_low | 湿度过低 |
| sensor_error | 传感器故障 |
| communication_error | 通信故障 |
| data_abnormal | 数据异常 |

#### 报警级别

| 级别 | 说明 | 处理建议 |
|------|------|---------|
| info | 信息 | 记录即可 |
| warning | 警告 | 需要关注 |
| error | 错误 | 需要处理 |
| critical | 严重 | 立即处理 |

---

### 4.7 控制指令消息

**Topic**: `iot/rtu/{rtuId}/rol/request`
**QoS**: 2
**发布时机**: 用户发起控制操作
**Retain**: false

#### 消息格式
```json
{
  "msgId": "550e8400-e29b-41d4-a716-446655440007",
  "msgType": "control_request",
  "rtuId": "RTU001",
  "timestamp": 1709971200000,
  "command": "restart_collection",
  "params": {
    "delay": 5
  }
}
```

#### 控制指令类型

| 指令 | 说明 | 参数 |
|------|------|------|
| restart_collection | 重启采集任务 | delay: 延迟秒数 |
| stop_collection | 停止采集任务 | - |
| start_collection | 启动采集任务 | - |
| clear_cache | 清空缓存数据 | t_system | 重启系统 | delay: 延迟秒数 |
| update_firmware | 固件升级 | url: 固件下载地址 |

#### 控制响应消息

**Topic**: `iot/rtu/{rtuId}/control/response`

```json
{
  "msgId": "550e8400-e29b-41d4-a716-446655440008",
  "msgType": "control_response",
  "rtuId": "RTU001",
  "timestamp": 1709971200000,
  "requestMsgId": "550e8400-e29b-41d4-a716-446655440007",
  "command": "restart_collection",
  "success": true,
  "message": "采集任务已重启"
}
```

---

## 5. 消息格式规范

### 5.1 通用消息结构

所有MQTT消息必须包含以下通用字段：

```json
{
  "msgId": "UUID",
  "msgType": "消息类型",
  "rtuId": "RTU标识",
  "timestamp": 1709971200000,..": "其他业务字段"
}
```

### 5.2 时间戳格式

- **统一使用**: Unix时间戳（毫秒）
- **Java生成**: `System.currentTimeMillis()`
- **精度要求**: 毫秒级
- **时区**: UTC+8（北京时间）

### 5.3 数据精度要求

| 数据类型 | 精度 | 示例 |
|---------|------|------|
| 温度 | 保留1位小数 | 25.5 |
| 湿度 | 保留1位小数 | 50.2 |
| CPU使用率 | 保留1位小数 | 12.5 |
| 内存使用率 | 保留1位小数 | 45.2 |

---

## 6. 错误处理机制

### 6.1 消息发送失败

```java
// rtu-gateway发送失败处理
try {
    mqttClient.publish(topic, message, qos, retained);
} catch (MqttException e) {
    // 1. 记录日志
    logger.error("MQTT发送失败: topic={}, error={}", topic, e.getMessage());

    // 2. 缓存消息
    messageCache.add(message);

    // 3. 触发重连
    reconnect();
}
```

### 6.2 消息接收异常

```java
// web-server接收异常处理
@Override
public void messageArrived(String topic, MqttMessage message) {
    try {
        String payload = new String(message.getPayload());
        processMessage(topic, payload);
    } catch (JsonParseException e) {
        logger.error("JSON解析失败: topic={}, payload={}", topic, payload);
        // 记录到错误表
        saveErrorMessage(topic, payload, e.getMessage());
    } catch (Exception e) {
        logger.error("消息处理失败: topic={}", topic, e);
    }
}
```

### 6接断开处理

```java
@Override
public void connectionLost(Throwable cause) {
    logger.warn("MQTT连接断开: {}", cause.getMessage());

    // 1. 标记离线状态
    updateStatus("offline");

    // 2. 启动重连任务
    scheduleReconnect();

    // 3. 缓存待发送消息
    enableMessageCache();
}
```

---

## 7. 安全设计

### 7.1 认证机制

```properties
# 启用用户名密码认证
mqtt.username=rtu_gateway_user
mqtt.password=your_secure_password

# 生产环境建议使用证书认证
mqtt.ssl.enabled=true
mqtt.ssl.keystore.path=/path/to/keystore.jks
mqtt.ssl.keystore.password=keystore_password
```

### 7.2 Topic权限控制

| 客户端 | 允许发布 | 允许订阅 |
|--------|---------|---------|
| rtu-gateway | `iot/rtu/{rtuId}/*` | `iot/rtu/{rtuId}/config/*`<br>`iot/rtu/broadcast/*` |
| web-server | `iot/rtu/+/config/*`<br>`iot/rtu/broadcast/*` | `iot/rtu/+/*`<br>`iot/system/*` |

### 7.3 消息加密

对于敏感配置，建议使用AES加密：

```json
{
  "msgType": "config_request",
  "rtuId": "RTU001",
  "encrypted": true,
  "algorithm": "AES-256-CBC",
  "config": "base64_encrypted_data"
}
```

---

## 8. 性能优化建议

### 8.1 批量发送

对于高频数据，可以批量发送：

```json
{
  "msgType": "data_batch",
  "rtuId": "RTU001",
  "count": 10,
  "dataList": [
    {"timestamp": 1709971200000, "temperature": 25.5,ity": 50.2},
    {"timestamp": 1709971201000, "temperature": 25.6, "humidity": 50.3}
  ]
}
```

### 8.2 消息压缩

对于大消息，启用压缩：

```properties
# 启用GZIP压缩（消息大小 > 1KB时）
mqtt.compression.enabled=true
mqtt.compression.threshold=1024
```

### 8.3 连接池

web-server使用连接池管理MQTT连接：

```java
// 配置连接池
MqttConnectionPool pool = new MqttConnectionPool();
pool.setMaxConnections(10);
pool.setMinIdleConnections(2);
```

---

## 9. 监控指标

### 9.1 关键指标

| 指标 | 说明 | 告警阈值 |
|------|------|---------|
| 消息发送成功率 | 成功/总数 | < 95% |
| 消息延迟 | 发送到接收的时间差 | > 5秒 |
| 连接断开次数 | 每小时断开次数 | > 3次 |
| 消息堆积数量 | 未处理消息数 | > 1000 |

### 9.2 监控实现

```java
// 使用Micrometer记录指标
@Component
public class MqttMetrics {
    private final Counter publishCounter;
    private final Timer publishLatency;

    public void recordPublish(boolean success, long latency) {
        publishCounter.increment();
        publishLatency.record(latency, TimeUnit.MILLISECONDS);
    }
}
```

---

## 10. 测试用例

### 10.1 功能测试

```bash
# 使用mosquitto_pub测试发送
mosquitto_pub -h localhost -p 1883 \
  -t "iot/rtu/RTU001/data" \
  -m '{"msgType":"data_report","rtuId":"R001","timestamp":1709971200000,"data":{"temperature":25.5,"humidity":50.2}}'

# 使用mosquitto_sub测试订阅
mosquitto_sub -h localhost -p 1883 \
  -t "iot/rtu/+/data" \
  -v
```

### 10.2 压力测试

```java
// 模拟100个RTU同时发送数据
for (int i = 1; i <= 100; i++) {
    String rtuId = "RTU" + String.format("%03d", i);
    executorService.submit(() -> {
        publishData(rtuId);
    });
}
```

---

## 文档变更记录

| 版本 | 日期 | 修改人 | 修改内容 |
|------|------|--------|----------|
| v1.0.0 | 2026-03-09 | Claude | 初始版本，定义MQTT消息规范 |

---

**文档维护**: 本文档应与接口设计文档保持同步，任何消息格式变更需及时更新。
