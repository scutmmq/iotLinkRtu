# Web Server API 接口文档

## 目录

- [1. 文档概述](#1-文档概述)
- [2. 统一响应格式](#2-统一响应格式)
- [3. 错误码定义](#3-错误码定义)
- [4. RTU 管理接口](#4-rtu 管理接口)
- [5. 数据查询接口](#5-数据查询接口)
- [6. 配置管理接口](#6-配置管理接口)
- [7. 报警管理接口](#7-报警管理接口)
- [8. 使用示例](#8-使用示例)

---

## 1. 文档概述

本文档详细描述了 IoT Link RTU 系统 Web Server 提供的 RESTful API 接口，包括：
- 接口的 URL、方法、请求参数、响应格式
- 统一的错误码定义和异常处理机制
- 完整的使用示例

**版本**: v1.0.0  
**更新日期**: 2026-03-13  
**适用模块**: web-server

---

## 2. 统一响应格式

### 2.1 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

**字段说明：**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| code | Integer | 是 | 业务错误码，200 表示成功 |
| message | String | 是 | 响应消息 |
| data | Object | 否 | 业务数据（可选） |

### 2.2 错误响应

```json
{
  "code": 1001,
  "message": "RTU 不存在"
}
```

**字段说明：**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| code | Integer | 是 | 业务错误码 |
| message | String | 是 | 错误消息 |

### 2.3 HTTP 状态码映射

框架会自动将业务错误码映射到对应的 HTTP 状态码：

| 业务错误码范围 | HTTP 状态码 | 说明 |
|--------------|-----------|------|
| 200 | 200 OK | 成功 |
| 400-499 | 400 Bad Request | 客户端错误 |
| 401 | 401 Unauthorized | 未授权 |
| 403 | 403 Forbidden | 禁止访问 |
| 404 | 404 Not Found | 资源不存在 |
| 500-7999 | 500 Internal Server Error | 服务端错误 |

---

## 3. 错误码定义

### 3.1 通用错误 (200-499)

| 错误码 | HTTP 状态码 | 说明 |
|--------|-----------|------|
| 200 | 200 | 操作成功 |
| 400 | 400 | 请求参数错误 |
| 401 | 401 | 未授权访问 |
| 403 | 403 | 禁止访问 |
| 404 | 404 | 资源不存在 |
| 405 | 405 | 方法不允许 |
| 500 | 500 | 系统内部错误 |

### 3.2 RTU 管理错误 (1000-1999)

| 错误码 | HTTP 状态码 | 说明 | 触发场景 |
|--------|-----------|------|----------|
| 1001 | 404 | RTU 不存在 | 查询不存在的 RTU |
| 1002 | 400 | RTU 已存在 | 注册重复的 RTU ID |
| 1003 | 400 | RTU 离线 | 对离线 RTU 下发指令 |
| 1004 | 400 | RTU 已在线 | 重复上线 |
| 1005 | 400 | RTU 配置错误 | 配置参数非法 |
| 1006 | 400 | RTU ID 不能为空 | 注册时未提供 RTU ID |
| 1007 | 400 | RTU 名称不能为空 | 注册时未提供名称 |
| 1008 | 400 | 无效的 RTU 状态 | 状态值不在枚举范围内 |
| 1009 | 400 | RTU ID 重复 | 使用已存在的 ID 注册 |

**使用示例：**
```java
// Controller 中抛出异常
if (rtuService.exists(rtuId)) {
    throw new BadRequestException(ErrorCode.RTU_ALREADY_EXISTS);
}

// 或使用自定义消息
if (rtuId == null || rtuId.trim().isEmpty()) {
    throw new BadRequestException(ErrorCode.RTU_ID_EMPTY, "RTU ID 不能为空");
}
```

### 3.3 数据相关错误 (2000-2999)

| 错误码 | HTTP 状态码 | 说明 | 触发场景 |
|--------|-----------|------|----------|
| 2001 | 404 | 数据不存在 | 查询不到历史数据 |
| 2002 | 400 | 数据格式错误 | JSON 解析失败 |
| 2003 | 400 | 数据超出范围 | 数值超过合理范围 |
| 2004 | 400 | 数据解析失败 | Modbus 数据解析错误 |
| 2005 | 400 | 温度值超出范围 (-40~85°C) | 温度值不合法 |
| 2006 | 400 | 湿度值超出范围 (0~100%) | 湿度值不合法 |
| 2007 | 400 | 时间戳无效 | 时间戳格式错误或为负数 |

**使用示例：**
```java
// 校验温度范围
if (temperature < -40 || temperature > 85) {
    throw new BadRequestException(ErrorCode.TEMPERATURE_OUT_OF_RANGE);
}

// 校验湿度范围
if (humidity < 0 || humidity > 100) {
    throw new BadRequestException(ErrorCode.HUMIDITY_OUT_OF_RANGE);
}
```

### 3.4 配置管理错误 (3000-3999)

| 错误码 | HTTP 状态码 | 说明 | 触发场景 |
|--------|-----------|------|----------|
| 3001 | 404 | 配置不存在 | 查询不存在的配置 |
| 3002 | 400 | 配置格式错误 | 配置 JSON 格式错误 |
| 3003 | 400 | 采样间隔无效 (1-60 秒) | 采样间隔不在有效范围 |
| 3004 | 400 | 阈值设置错误 | 阈值配置不完整 |
| 3005 | 400 | 温度最小值不能大于最大值 | tempMin > tempMax |
| 3006 | 400 | 湿度最小值不能大于最大值 | humiMin > humiMax |
| 3007 | 500 | 配置下发失败 | MQTT 下发配置失败 |

**使用示例：**
```java
// 校验采样间隔
if (samplingInterval < 1 || samplingInterval > 60) {
    throw new BadRequestException(ErrorCode.SAMPLING_INTERVAL_INVALID);
}

// 校验温度阈值
if (tempMin >= tempMax) {
    throw new BadRequestException(ErrorCode.TEMP_THRESHOLD_MIN_MAX);
}
```

### 3.5 报警管理错误 (4000-4999)

| 错误码 | HTTP 状态码 | 说明 | 触发场景 |
|--------|-----------|------|----------|
| 4001 | 404 | 报警记录不存在 | 处理不存在的报警 |
| 4002 | 400 | 报警已处理 | 重复处理报警 |
| 4003 | 400 | 报警类型无效 | 报警类型不在枚举范围 |
| 4004 | 400 | 报警级别无效 | 报警级别不在枚举范围 |
| 4005 | 500 | 报警处理失败 | 数据库更新失败 |

### 3.6 MQTT 通信错误 (5000-5999)

| 错误码 | HTTP 状态码 | 说明 | 触发场景 |
|--------|-----------|------|----------|
| 5001 | 500 | MQTT 连接错误 | 无法连接到 Broker |
| 5002 | 500 | MQTT消息发布失败 | publish 失败 |
| 5003 | 500 | MQTT 订阅失败 | subscribe 失败 |
| 5004 | 400 | MQTT Topic 无效 | Topic 格式不符合规范 |
| 5005 | 400 | MQTT消息格式错误 | 消息 JSON 格式错误 |

### 3.7 数据库错误 (6000-6999)

| 错误码 | HTTP 状态码 | 说明 | 触发场景 |
|--------|-----------|------|----------|
| 6001 | 500 | 数据库错误 | 通用数据库错误 |
| 6002 | 500 | 数据库连接失败 | 无法连接到数据库 |
| 6003 | 500 | 查询失败 | SQL 执行失败 |
| 6004 | 500 | 更新失败 | UPDATE 执行失败 |
| 6005 | 500 | 删除失败 | DELETE 执行失败 |
| 6006 | 400 | 数据库约束违反 | 唯一键冲突等 |

### 3.8 权限认证错误 (7000-7999)

| 错误码 | HTTP 状态码 | 说明 | 触发场景 |
|--------|-----------|------|----------|
| 7001 | 400 | Token 不能为空 | 请求头缺少 Token |
| 7002 | 401 | Token 无效 | Token 格式错误或已失效 |
| 7003 | 401 | Token 已过期 | Token 超过有效期 |
| 7004 | 404 | 用户不存在 | 登录用户名不存在 |
| 7005 | 401 | 密码错误 | 登录密码错误 |
| 7006 | 403 | 权限不足 | 用户无此操作权限 |
| 7007 | 403 | 用户已禁用 | 用户被禁用 |

---

## 4. RTU 管理接口

### 4.1 注册 RTU

**接口：** `POST /api/rtu/register`

**请求参数：**
```json
{
  "rtuId": "RTU001",
  "name": "1 号温湿度采集器",
  "location": "机房 A 区",
  "serialPort": "COM3",
  "baudRate": 9600,
  "deviceAddress": 1
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "rtuId": "RTU001",
    "status": "offline",
    "createTime": "2026-03-13 10:00:00"
  }
}
```

**可能的错误码：**
| 错误码 | 说明 |
|--------|------|
| 1002 | RTU 已存在 |
| 1006 | RTU ID 不能为空 |
| 1007 | RTU 名称不能为空 |
| 1009 | RTU ID 重复 |

**Controller 实现示例：**
```java
public class RtuRegisterController extends BaseController {
    
    @Override
    protected void post(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 获取参数
        String rtuId = req.bodyString("rtuId");
        String name = req.bodyString("name");
        String location = req.bodyString("location");
        Integer baudRate = toInteger(req.bodyJson().get("baudRate"));
        Integer deviceAddress = toInteger(req.bodyJson().get("deviceAddress"));
        
        // 参数校验
        requireNotBlank(rtuId, "rtuId");
        requireNotBlank(name, "name");
        
        // 检查是否已存在
        if (rtuService.exists(rtuId)) {
            throw new BadRequestException(ErrorCode.RTU_ALREADY_EXISTS);
        }
        
        // 创建 RTU
        RtuGateway rtu = new RtuGateway();
        rtu.setRtuId(rtuId);
        rtu.setName(name);
        rtu.setLocation(location);
        rtu.setBaudRate(baudRate);
        rtu.setDeviceAddress(deviceAddress);
        rtu.setStatus("offline");
        
        rtuService.save(rtu);
        
        // 返回响应
        resp.json(buildSuccessResponse(Map.of(
            "id", rtu.getId(),
            "rtuId", rtu.getRtuId(),
            "status", rtu.getStatus(),
            "createTime", rtu.getCreateTime()
        )));
    }
}
```

### 4.2 查询 RTU 列表

**接口：** `GET /api/rtu/list?page=1&size=10&status=online`

**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页数量，默认 10 |
| status | String | 否 | 状态筛选：online/offline |

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 25,
    "list": [
      {
        "id": 1,
        "rtuId": "RTU001",
        "name": "1 号温湿度采集器",
        "status": "online",
        "lastOnlineTime": "2026-03-13 10:30:00",
        "location": "机房 A 区"
      }
    ]
  }
}
```

**可能的错误码：**
| 错误码 | 说明 |
|--------|------|
| 2003 | 数据超出范围（page/size 不合法） |

### 4.3 查询 RTU 详情

**接口：** `GET /api/rtu/{rtuId}`

**路径参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| rtuId | String | 是 | RTU ID |

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "rtuId": "RTU001",
    "name": "1 号温湿度采集器",
    "location": "机房 A 区",
    "status": "online",
    "serialPort": "COM3",
    "baudRate": 9600,
    "deviceAddress": 1,
    "samplingInterval": 1,
    "lastOnlineTime": "2026-03-13 10:30:00",
    "createTime": "2026-03-13 10:00:00"
  }
}
```

**可能的错误码：**
| 错误码 | 说明 |
|--------|------|
| 1001 | RTU 不存在 |

**Controller 实现示例：**
```java
public class RtuDetailController extends BaseController {
    
    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        String rtuId = req.pathParam("rtuId");
        requireNotBlank(rtuId, "rtuId");
        
        RtuGateway rtu = rtuService.findById(rtuId);
        if (rtu == null) {
            throw new NotFoundException(ErrorCode.RTU_NOT_FOUND);
        }
        
        resp.json(buildSuccessResponse(rtu));
    }
}
```

### 4.4 更新 RTU 信息

**接口：** `PUT /api/rtu/{rtuId}`

**请求参数：**
```json
{
  "name": "1 号温湿度采集器（已更新）",
  "location": "机房 B 区"
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "success"
}
```

**可能的错误码：**
| 错误码 | 说明 |
|--------|------|
| 1001 | RTU 不存在 |
| 1007 | RTU 名称不能为空 |

### 4.5 删除 RTU

**接口：** `DELETE /api/rtu/{rtuId}`

**响应示例：**
```json
{
  "code": 200,
  "message": "success"
}
```

**可能的错误码：**
| 错误码 | 说明 |
|--------|------|
| 1001 | RTU 不存在 |

---

## 5. 数据查询接口

### 5.1 查询实时数据

**接口：** `GET /api/data/realtime?rtuId=RTU001`

**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| rtuId | String | 是 | RTU ID |

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "rtuId": "RTU001",
    "temperature": 25.5,
    "humidity": 50.2,
    "timestamp": "2026-03-13 10:30:00",
    "status": "normal"
  }
}
```

**可能的错误码：**
| 错误码 | 说明 |
|--------|------|
| 1001 | RTU 不存在 |
| 2001 | 数据不存在 |

### 5.2 查询历史数据

**接口：** `GET /api/data/history?rtuId=RTU001&startTime=2026-03-13 00:00:00&endTime=2026-03-13 23:59:59&page=1&size=100`

**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| rtuId | String | 是 | RTU ID |
| startTime | String | 是 | 开始时间 |
| endTime | String | 是 | 结束时间 |
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 86400,
    "list": [
      {
        "id": 1,
        "rtuId": "RTU001",
        "temperature": 25.5,
        "humidity": 50.2,
        "timestamp": "2026-03-13 10:30:00"
      }
    ]
  }
}
```

**可能的错误码：**
| 错误码 | 说明 |
|--------|------|
| 1001 | RTU 不存在 |
| 2001 | 数据不存在 |
| 2007 | 时间戳无效 |

### 5.3 数据统计

**接口：** `GET /api/data/statistics?rtuId=RTU001&startTime=2026-03-13 00:00:00&endTime=2026-03-13 23:59:59`

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "rtuId": "RTU001",
    "temperature": {
      "avg": 25.5,
      "max": 30.0,
      "min": 20.0
    },
    "humidity": {
      "avg": 50.5,
      "max": 60.0,
      "min": 40.0
    },
    "dataCount": 86400
  }
}
```

---

## 6. 配置管理接口

### 6.1 查询 RTU 配置

**接口：** `GET /api/config/{rtuId}`

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "rtuId": "RTU001",
    "samplingInterval": 1,
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

**可能的错误码：**
| 错误码 | 说明 |
|--------|------|
| 1001 | RTU 不存在 |
| 3001 | 配置不存在 |

### 6.2 更新 RTU 配置

**接口：** `PUT /api/config/{rtuId}`

**请求参数：**
```json
{
  "samplingInterval": 5,
  "temperatureThreshold": {
    "min": 20.0,
    "max": 30.0
  },
  "humidityThreshold": {
    "min": 45.0,
    "max": 65.0
  },
  "alarmEnabled": true
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "配置更新成功，已下发到 RTU"
}
```

**可能的错误码：**
| 错误码 | 说明 |
|--------|------|
| 1001 | RTU 不存在 |
| 3003 | 采样间隔无效 |
| 3005 | 温度最小值不能大于最大值 |
| 3006 | 湿度最小值不能大于最大值 |
| 3007 | 配置下发失败 |

**Controller 实现示例：**
```java
public class ConfigUpdateController extends BaseController {
    
    @Override
    protected void put(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        String rtuId = req.pathParam("rtuId");
        requireNotBlank(rtuId, "rtuId");
        
        // 获取配置参数
        Map<String, Object> body = req.bodyJson();
        Integer samplingInterval = toInteger(body.get("samplingInterval"));
        Map<String, Object> tempThresh = (Map<String, Object>) body.get("temperatureThreshold");
        Map<String, Object> humiThresh = (Map<String, Object>) body.get("humidityThreshold");
        
        // 校验采样间隔
        if (samplingInterval == null || samplingInterval < 1 || samplingInterval > 60) {
            throw new BadRequestException(ErrorCode.SAMPLING_INTERVAL_INVALID);
        }
        
        // 校验温度阈值
        if (tempThresh != null) {
            Float tempMin = toFloat(tempThresh.get("min"));
            Float tempMax = toFloat(tempThresh.get("max"));
            if (tempMin != null && tempMax != null && tempMin >= tempMax) {
                throw new BadRequestException(ErrorCode.TEMP_THRESHOLD_MIN_MAX);
            }
        }
        
        // 更新配置并下发到 RTU
        boolean success = configService.updateAndApply(rtuId, body);
        if (!success) {
            throw new ServerException(ErrorCode.CONFIG_APPLY_FAILED);
        }
        
        resp.json(buildSuccessResponse());
    }
    
    private Float toFloat(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.floatValue();
        try { return Float.parseFloat(val.toString()); } catch (Exception e) { return null; }
    }
}
```

---

## 7. 报警管理接口

### 7.1 查询报警列表

**接口：** `GET /api/alarm/list?rtuId=RTU001&status=unhandled&page=1&size=10`

**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| rtuId | String | 否 | RTU ID |
| status | String | 否 | 状态：unhandled/handled |
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 5,
    "list": [
      {
        "id": 1,
        "rtuId": "RTU001",
        "alarmType": "temperature_high",
        "alarmLevel": "warning",
        "currentValue": 32.0,
        "thresholdValue": 28.0,
        "alarmTime": "2026-03-13 10:30:00",
        "status": "unhandled"
      }
    ]
  }
}
```

### 7.2 处理报警

**接口：** `PUT /api/alarm/{alarmId}/handle`

**请求参数：**
```json
{
  "handleResult": "已调整空调温度",
  "handler": "张三"
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "报警处理成功"
}
```

**可能的错误码：**
| 错误码 | 说明 |
|--------|------|
| 4001 | 报警记录不存在 |
| 4002 | 报警已处理 |
| 4005 | 报警处理失败 |

---

## 8. 使用示例

### 8.1 完整的 Controller 示例

```java
package com.scutmmq.web.controller.rtu;

import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.exception.ErrorCode;
import com.scutmmq.BadRequestException;
import com.scutmmq.NotFoundException;

import java.util.Map;

/**
 * RTU 注册控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class RtuRegisterController extends BaseController {
    
    /**
     * 处理 POST 请求 - 注册 RTU
     * 
     * @param req 请求对象
     * @param resp 响应对象
     * @throws Exception 业务异常
     */
    @Override
    protected void post(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取请求参数
        Map<String, Object> body = req.bodyJson();
        String rtuId = req.bodyString("rtuId");
        String name = req.bodyString("name");
        String location = req.bodyString("location");
        Integer baudRate = toInteger(body.get("baudRate"));
        Integer deviceAddress = toInteger(body.get("deviceAddress"));
        
        // 2. 参数校验
        requireNotBlank(rtuId, "rtuId");
        requireNotBlank(name, "name");
        
        // 3. 业务校验
        if (rtuService.exists(rtuId)) {
            throw new BadRequestException(ErrorCode.RTU_ALREADY_EXISTS, "RTU ID 已存在：" + rtuId);
        }
        
        // 4. 创建实体
        RtuGateway rtu = new RtuGateway();
        rtu.setRtuId(rtuId);
        rtu.setName(name);
        rtu.setLocation(location);
        rtu.setBaudRate(baudRate);
        rtu.setDeviceAddress(deviceAddress);
        rtu.setStatus("offline");
        
        // 5. 保存数据
        rtuService.save(rtu);
        
        // 6. 返回响应
        Map<String, Object> data = Map.of(
            "id", rtu.getId(),
            "rtuId", rtu.getRtuId(),
            "status", rtu.getStatus(),
            "createTime", rtu.getCreateTime()
        );
        resp.json(buildSuccessResponse(data));
    }
}
```

### 8.2 路由注册示例

```java
package com.scutmmq.web;

import com.scutmmq.restful.RestFulExpress;
import com.scutmmq.restful.HttpServer;
import com.scutmmq.web.controller.rtu.*;

/**
 * Web Server 启动类
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class Application {
    
    public static void main(String[] args) throws Exception {
        // 1. 注册路由
        RestFulExpress router = RestFulExpress.instance();
        
        // RTU 管理接口
        router.register("/api/rtu/register", RtuRegisterController.class);
        router.register("/api/rtu/list", RtuListController.class);
        router.registerPattern("/api/rtu/{rtuId}", RtuDetailController.class);
        
        // 数据查询接口
        router.register("/api/data/realtime", DataRealtimeController.class);
        router.register("/api/data/history", DataHistoryController.class);
        router.register("/api/data/statistics", DataStatisticsController.class);
        
        // 配置管理接口
        router.registerPattern("/api/config/{rtuId}", ConfigController.class);
        
        // 报警管理接口
        router.register("/api/alarm/list", AlarmListController.class);
        router.registerPattern("/api/alarm/{alarmId}/handle", AlarmHandleController.class);
        
        // 2. 启动 HTTP 服务器
        new HttpServer(8080).start();
    }
}
```

### 8.3 异常处理最佳实践

```java
public class DataQueryController extends BaseController {
    
    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        String rtuId = req.queryParam("rtuId");
        String startTime = req.queryParam("startTime");
        String endTime = req.queryParam("endTime");
        
        // 必填参数校验
        requireNotBlank(rtuId, "rtuId");
        requireNotBlank(startTime, "startTime");
        requireNotBlank(endTime, "endTime");
        
        // 校验 RTU 是否存在
        if (!rtuService.exists(rtuId)) {
            throw new NotFoundException(ErrorCode.RTU_NOT_FOUND);
        }
        
        // 校验时间范围
        try {
            long start = parseTimestamp(startTime);
            long end = parseTimestamp(endTime);
            
            if (start < 0 || end < 0) {
                throw new BadRequestException(ErrorCode.TIMESTAMP_INVALID);
            }
            
            if (start > end) {
                throw new BadRequestException(2007, "开始时间不能大于结束时间");
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ErrorCode.TIMESTAMP_INVALID, "时间格式错误：" + e.getMessage());
        }
        
        // 查询数据
        List<RtuData> dataList = dataService.queryHistory(rtuId, startTime, endTime);
        if (dataList == null || dataList.isEmpty()) {
            throw new NotFoundException(ErrorCode.DATA_NOT_FOUND);
        }
        
        // 返回响应
        resp.json(buildSuccessResponse(Map.of(
            "total", dataList.size(),
            "list", dataList
        )));
    }
    
    private long parseTimestamp(String timestamp) {
        // 解析时间戳逻辑
        return System.currentTimeMillis();
    }
}
```

---

## 附录

### A. 数据模型

#### A.1 RTU 网关实体
```java
@Data
@Table(name = "rtu_gateway")
public class RtuGateway {
    private Long id;
    private String rtuId;           // RTU 唯一标识
    private String name;            // RTU 名称
    private String location;        // 安装位置
    private String status;          // 状态：online/offline
    private String serialPort;      // 串口号
    private Integer baudRate;       // 波特率
    private Integer deviceAddress;  // Modbus 设备地址
    private Integer samplingInterval; // 采样间隔（秒）
    private LocalDateTime lastOnlineTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

#### A.2 RTU 数据实体
```java
@Data
@Table(name = "rtu_data")
public class RtuData {
    private Long id;
    private String rtuId;
    private Float temperature;      // 温度
    private Float humidity;         // 湿度
    private LocalDateTime timestamp; // 采集时间
    private String rawModbus;       // Modbus 原始数据
    private LocalDateTime createTime;
}
```

### B. 快速参考

#### B.1 常用工具方法（BaseController 提供）

```java
// 参数校验
requireNotNull(value, "fieldName");      // 对象不能为 null
requireNotBlank(str, "fieldName");       // 字符串不能为空

// 类型转换
Integer age = toInteger(obj);            // 转换为 Integer
int page = parseIntParam(str, 1);        // 字符串转 int

// 获取参数
String name = req.bodyString("name");    // 从 Body 获取字符串
String keyword = req.queryParam("kw");   // 从 URL 参数获取
String id = req.pathParam("id");         // 从路径参数获取

// 构建响应
Map<String, Object> resp = buildSuccessResponse(data);  // 成功响应
Map<String, Object> err = buildErrorResponse(code, msg); // 错误响应
```

#### B.2 异常抛出速查

```java
// 404 错误 - 资源不存在
throw new NotFoundException(ErrorCode.RTU_NOT_FOUND);

// 400 错误 - 参数错误
throw new BadRequestException(ErrorCode.RTU_ID_EMPTY);
throw new BadRequestException(ErrorCode.TEMPERATURE_OUT_OF_RANGE);

// 500 错误 - 服务器错误
throw new ServerException(ErrorCode.MQTT_PUBLISH_FAILED);
throw new ServerException(ErrorCode.DATABASE_ERROR);

// 带自定义消息
throw new NotFoundException(ErrorCode.RTU_NOT_FOUND, "RTU ID 不存在：" + rtuId);
```

---

**文档维护**: 任何接口变更需及时更新此文档。
