# Netty RESTful框架异常处理设计文档

## 1. 概述

本文档详细描述了 IoT Link RTU 项目中 Netty RESTful框架的异常处理机制，包括：
- 错误码枚举体系
- 异常类层次结构
- 异常到 HTTP 响应的映射
- 最佳实践指南

**版本**: v1.0.0  
**更新日期**: 2026-03-13

---

## 2. 错误码枚举 (ErrorCode)

### 2.1 设计理念

采用**分段式错误码设计**，便于快速定位问题所属模块：

```
错误码范围      模块           HTTP 状态码映射
200-499        通用错误       根据具体值映射
1000-1999      RTU 管理       400 Bad Request
2000-2999      数据管理       400 Bad Request
3000-3999      配置管理       400/500
4000-4999      报警管理       400/500
5000-5999      MQTT 通信      500 Internal Server Error
6000-6999      数据库         500 Internal Server Error
7000-7999      权限认证       401/403
```

### 2.2 完整错误码列表

#### 通用错误 (200-499)

| 错误码 | 说明 | HTTP 状态码 | 使用场景 |
|--------|------|-----------|----------|
| 200 | 操作成功 | 200 OK | 所有成功操作 |
| 400 | 请求参数错误 | 400 Bad Request | 通用参数错误 |
| 401 | 未授权访问 | 401 Unauthorized | Token 缺失或无效 |
| 403 | 禁止访问 | 403 Forbidden | 权限不足 |
| 404 | 资源不存在 | 404 Not Found | 通用资源不存在 |
| 405 | 方法不允许 | 405 Method Not Allowed | HTTP 方法不支持 |
| 500 | 系统内部错误 | 500 Internal Server Error | 通用系统错误 |

#### RTU 管理错误 (1000-1999)

| 错误码 | 说明 | 触发条件 |
|--------|------|----------|
| 1001 | RTU 不存在 | 查询不存在的 RTU ID |
| 1002 | RTU 已存在 | 注册重复的 RTU |
| 1003 | RTU 离线 | 对离线 RTU 下发指令 |
| 1004 | RTU 已在线 | 重复上线操作 |
| 1005 | RTU 配置错误 | 配置参数非法 |
| 1006 | RTU ID 不能为空 | 注册时 rtuId 为空 |
| 1007 | RTU 名称不能为空 | 注册时 name 为空 |
| 1008 | 无效的 RTU 状态 | status 不在枚举范围 |
| 1009 | RTU ID 重复 | 使用已存在的 ID |

**使用示例：**
```java
// 检查 RTU 是否存在
if (!rtuService.exists(rtuId)) {
    throw new NotFoundException(ErrorCode.RTU_NOT_FOUND);
}

// 检查 RTU ID 是否重复
if (rtuService.exists(rtuId)) {
    throw new BadRequestException(ErrorCode.RTU_DUPLICATE_ID);
}
```

#### 数据相关错误 (2000-2999)

| 错误码 | 说明 | 触发条件 |
|--------|------|----------|
| 2001 | 数据不存在 | 查询不到数据 |
| 2002 | 数据格式错误 | JSON 解析失败 |
| 2003 | 数据超出范围 | 数值超过合理范围 |
| 2004 | 数据解析失败 | Modbus 解析错误 |
| 2005 | 温度值超出范围 (-40~85°C) | temperature < -40 或 > 85 |
| 2006 | 湿度值超出范围 (0~100%) | humidity < 0 或 > 100 |
| 2007 | 时间戳无效 | 时间戳为负数或格式错误 |

**使用示例：**
```java
// 校验传感器数据范围
if (temperature < -40 || temperature > 85) {
    throw new BadRequestException(ErrorCode.TEMPERATURE_OUT_OF_RANGE);
}

if (humidity < 0 || humidity > 100) {
    throw new BadRequestException(ErrorCode.HUMIDITY_OUT_OF_RANGE);
}
```

#### 配置管理错误 (3000-3999)

| 错误码 | 说明 | 触发条件 |
|--------|------|----------|
| 3001 | 配置不存在 | 查询不存在的配置 |
| 3002 | 配置格式错误 | 配置 JSON 格式错误 |
| 3003 | 采样间隔无效 (1-60 秒) | interval < 1 或 > 60 |
| 3004 | 阈值设置错误 | 阈值配置不完整 |
| 3005 | 温度最小值不能大于最大值 | tempMin >= tempMax |
| 3006 | 湿度最小值不能大于最大值 | humiMin >= humiMax |
| 3007 | 配置下发失败 | MQTT 下发配置失败 |

**使用示例：**
```java
// 校验采样间隔
if (samplingInterval == null || samplingInterval < 1 || samplingInterval > 60) {
    throw new BadRequestException(ErrorCode.SAMPLING_INTERVAL_INVALID);
}

// 校验温度阈值
if (tempMin != null && tempMax != null && tempMin >= tempMax) {
    throw new BadRequestException(ErrorCode.TEMP_THRESHOLD_MIN_MAX);
}
```

#### 报警管理错误 (4000-4999)

| 错误码 | 说明 | 触发条件 |
|--------|------|----------|
| 4001 | 报警记录不存在 | 处理不存在的报警 |
| 4002 | 报警已处理 | 重复处理报警 |
| 4003 | 报警类型无效 | alarmType 不在枚举范围 |
| 4004 | 报警级别无效 | alarmLevel 不在枚举范围 |
| 4005 | 报警处理失败 | 数据库更新失败 |

#### MQTT 通信错误 (5000-5999)

| 错误码 | 说明 | 触发条件 |
|--------|------|----------|
| 5001 | MQTT 连接错误 | 无法连接到 Broker |
| 5002 | MQTT消息发布失败 | publish 失败 |
| 5003 | MQTT 订阅失败 | subscribe 失败 |
| 5004 | MQTT Topic 无效 | Topic 格式不符合规范 |
| 5005 | MQTT消息格式错误 | 消息 JSON 格式错误 |

#### 数据库错误 (6000-6999)

| 错误码 | 说明 | 触发条件 |
|--------|------|----------|
| 6001 | 数据库错误 | 通用数据库错误 |
| 6002 | 数据库连接失败 | 无法连接到数据库 |
| 6003 | 查询失败 | SQL 执行失败 |
| 6004 | 更新失败 | UPDATE 执行失败 |
| 6005 | 删除失败 | DELETE 执行失败 |
| 6006 | 数据库约束违反 | 唯一键冲突等 |

#### 权限认证错误 (7000-7999)

| 错误码 | 说明 | 触发条件 |
|--------|------|----------|
| 7001 | Token 不能为空 | 请求头缺少 Token |
| 7002 | Token 无效 | Token 格式错误或失效 |
| 7003 | Token 已过期 | Token 超过有效期 |
| 7004 | 用户不存在 | 登录用户名不存在 |
| 7005 | 密码错误 | 登录密码错误 |
| 7006 | 权限不足 | 用户无此操作权限 |
| 7007 | 用户已禁用 | 用户被禁用 |

---

## 3. 异常类层次结构

### 3.1 继承关系

```
java.lang.Throwable
  └── java.lang.Exception
      └── java.lang.RuntimeException
          └── com.scutmmq.ApiException (基类)
              ├── com.scutmmq.BadRequestException (400)
              ├── com.scutmmq.NotFoundException (404)
              └── com.scutmmq.ServerException (500)
```

### 3.2 ApiException 基类

**文件路径:** `E:\Study\IT\RTU\iotLinkRtu\iotLinkRtu-common-modules\src\main\java\com\scutmmq\ApiException.java`

**核心属性：**
```java
private final HttpResponseStatus status;  // HTTP 状态码
private final int errorCode;             // 业务错误码
private final String errorMsg;           // 错误消息
```

**构造方法：**
```java
// 1. 传统构造方式（兼容旧代码）
public ApiException(HttpResponseStatus status, int errorCode, String message)

// 2. 使用 ErrorCode 枚举（推荐）
public ApiException(ErrorCode errorCode)

// 3. 使用 ErrorCode + 自定义消息（推荐）
public ApiException(ErrorCode errorCode, String customMessage)
```

**使用示例：**
```java
// ✅ 推荐：使用 ErrorCode 枚举
throw new BadRequestException(ErrorCode.RTU_NOT_FOUND);

// ✅ 推荐：使用 ErrorCode + 自定义消息
throw new NotFoundException(ErrorCode.RTU_NOT_FOUND, "RTU ID 不存在：" + rtuId);

// ⚠️ 兼容旧代码：传统方式
throw new BadRequestException(40001, "参数错误");
```

### 3.3 子类异常

#### BadRequestException (400)
用于客户端参数错误、校验失败等场景。

```java
// 快捷构造
public BadRequestException(String message)  // 默认错误码 40001

// 使用 ErrorCode
public BadRequestException(ErrorCode errorCode)
public BadRequestException(ErrorCode errorCode, String customMessage)
```

**典型场景：**
- 必填参数为空
- 参数格式错误
- 参数超出范围
- 业务规则校验失败

#### NotFoundException (404)
用于资源不存在的场景。

```java
// 快捷构造
public NotFoundException(String message)  // 默认错误码 40401

// 使用 ErrorCode
public NotFoundException(ErrorCode errorCode)
public NotFoundException(ErrorCode errorCode, String customMessage)
```

**典型场景：**
- RTU 不存在
- 数据记录不存在
- 配置不存在
- 报警记录不存在

#### ServerException (500)
用于服务端内部错误。

```java
// 快捷构造
public ServerException(String message)  // 默认错误码 50001

// 使用 ErrorCode
public ServerException(ErrorCode errorCode)
public ServerException(ErrorCode errorCode, String customMessage)
```

**典型场景：**
- 数据库操作失败
- MQTT 通信失败
- 第三方服务调用失败
- 未预期的系统异常

---

## 4. 异常处理流程

### 4.1 框架层异常捕获

**位置:** `BaseController.handle()` 方法

**处理逻辑：**
```java
public final void handle(MyHttpRequest request, MyHttpResponse response) {
    try {
        // 1. 根据 HTTP 方法分发到对应处理方法
        switch (request.method()) {
            case "GET"    -> get(request, response);
            case "POST"   -> post(request, response);
            case "PUT"    -> put(request, response);
            case "DELETE" -> delete(request, response);
            default       -> 返回 405 错误;
        }
        
        // 2. 捕获业务异常
    } catch (ApiException e) {
        // 自动转为 HTTP 错误响应
        response.json(e.getStatus(), buildErrorResponse(e.getErrorCode(), e.getErrorMsg()));
        
        // 3. 捕获未预期异常
    } catch (Exception e) {
        // 统一返回 500
        response.json(
            HttpResponseStatus.INTERNAL_SERVER_ERROR,
            buildErrorResponse(50000, "Internal Server Error: " + e.getMessage())
        );
    }
}
```

### 4.2 错误响应格式

**统一响应体：**
```json
{
  "code": 1001,
  "message": "RTU 不存在"
}
```

**HTTP 状态码映射规则：**
```java
private static HttpResponseStatus mapToHttpStatus(int code) {
    if (code >= 200 && code < 300) {
        return HttpResponseStatus.OK;
    } else if (code >= 400 && code < 500) {
        return HttpResponseStatus.BAD_REQUEST;
    } else if (code == 401) {
        return HttpResponseStatus.UNAUTHORIZED;
    } else if (code == 403) {
        return HttpResponseStatus.FORBIDDEN;
    } else if (code == 404) {
        return HttpResponseStatus.NOT_FOUND;
    } else if (code >= 500) {
        return HttpResponseStatus.INTERNAL_SERVER_ERROR;
    }
    return HttpResponseStatus.BAD_REQUEST;
}
```

**映射表示例：**
| 业务错误码 | HTTP 状态码 | 说明 |
|------------|-----------|------|
| 200 | 200 OK | 成功 |
| 1001 | 400 Bad Request | RTU 不存在 |
| 2005 | 400 Bad Request | 温度超出范围 |
| 3003 | 400 Bad Request | 采样间隔无效 |
| 4001 | 400 Bad Request | 报警记录不存在 |
| 5001 | 500 Internal Server Error | MQTT 连接错误 |
| 6001 | 500 Internal Server Error | 数据库错误 |
| 7002 | 401 Unauthorized | Token 无效 |
| 7006 | 403 Forbidden | 权限不足 |

---

## 5. 最佳实践

### 5.1 何时抛出异常

#### ✅ 推荐做法

```java
// 1. 必填参数校验
String rtuId = req.bodyString("rtuId");
requireNotBlank(rtuId, "rtuId");  // 为空自动抛 BadRequestException

// 2. 资源存在性校验
RtuGateway rtu = rtuService.findById(rtuId);
if (rtu == null) {
    throw new NotFoundException(ErrorCode.RTU_NOT_FOUND);
}

// 3. 业务规则校验
if (samplingInterval < 1 || samplingInterval > 60) {
    throw new BadRequestException(ErrorCode.SAMPLING_INTERVAL_INVALID);
}

// 4. 数据范围校验
if (temperature < -40 || temperature > 85) {
    throw new BadRequestException(ErrorCode.TEMPERATURE_OUT_OF_RANGE);
}

// 5. 重复性校验
if (rtuService.exists(rtuId)) {
    throw new BadRequestException(ErrorCode.RTU_ALREADY_EXISTS);
}

// 6. 系统异常处理
try {
    mqttClient.publish(topic, message);
} catch (MqttException e) {
    logger.error("MQTT 发布失败", e);
    throw new ServerException(ErrorCode.MQTT_PUBLISH_FAILED);
}
```

#### ❌ 错误做法

```java
// 1. 不要捕获异常后什么都不做
try {
    validateParam(param);
} catch (Exception e) {
    // 空的 catch 块 - 绝对禁止！
}

// 2. 不要返回 magic number
if (param == null) {
    return -1;  // 应该抛异常
}

// 3. 不要在 Controller 外抛异常
@Service
public class RtuService {
    public void save(RtuGateway rtu) {
        if (rtu == null) {
            throw new BadRequestException(ErrorCode.RTU_ID_EMPTY);
            // 应该在 Service 层捕获并返回错误码，或在 Controller 层抛
        }
    }
}

// 4. 不要混用不同的错误处理方式
if (error) {
    throw new RuntimeException("错误");  // 应该使用 ApiException 子类
}
```

### 5.2 异常消息设计原则

#### ✅ 好的异常消息

```java
// 1. 明确告知什么问题
throw new BadRequestException(ErrorCode.RTU_ID_EMPTY, "RTU ID 不能为空");

// 2. 包含关键信息
throw new NotFoundException(ErrorCode.RTU_NOT_FOUND, "RTU ID 不存在：" + rtuId);

// 3. 提供解决建议（如适用）
throw new BadRequestException(ErrorCode.SAMPLING_INTERVAL_INVALID, 
    "采样间隔必须在 1-60 秒之间，当前值：" + interval);

// 4. 友好且专业
throw new BadRequestException(ErrorCode.DATA_PARSE_ERROR, 
    "数据解析失败，请检查数据格式是否正确");
```

#### ❌ 不好的异常消息

```java
// 1. 过于简略
throw new BadRequestException("错误");

// 2. 技术术语堆砌
throw new ServerException("NullPointerException at line 42");

// 3. 泄露敏感信息
throw new BadRequestException("数据库密码错误：password=123456");

// 4. 推卸责任
throw new BadRequestException("你输入的参数不对");
```

### 5.3 Controller 中的异常处理模式

#### 模式 1：直接抛出（推荐）

```java
@Override
protected void post(MyHttpRequest req, MyHttpResponse resp) throws Exception {
    String rtuId = req.bodyString("rtuId");
    requireNotBlank(rtuId, "rtuId");  // 自动抛出 BadRequestException
    
    if (rtuService.exists(rtuId)) {
        throw new BadRequestException(ErrorCode.RTU_ALREADY_EXISTS);
    }
    
    // 业务逻辑...
}
```

**优点：**
- 代码简洁清晰
- 框架自动处理
- 响应格式统一

#### 模式 2：try-catch 包装

```java
@Override
protected void post(MyHttpRequest req, MyHttpResponse resp) throws Exception {
    try {
        // 可能抛出非业务异常的操作
        mqttClient.publish(topic, message);
    } catch (MqttException e) {
        logger.error("MQTT 发布失败", e);
        throw new ServerException(ErrorCode.MQTT_PUBLISH_FAILED, 
            "MQTT消息发布失败：" + e.getMessage());
    }
}
```

**适用场景：**
- 需要记录详细日志
- 需要将底层异常转换为业务异常
- 需要添加额外上下文信息

### 5.4 Service 层的异常处理

#### 方案 1：Service 不抛异常（推荐简单项目）

```java
@Service
public class RtuService {
    
    public RtuGateway findById(String rtuId) {
        // 找不到返回 null，由 Controller 决定是否抛异常
        return rtuMapper.selectById(rtuId);
    }
    
    public boolean exists(String rtuId) {
        // 返回布尔值，不抛异常
        return rtuMapper.countById(rtuId) > 0;
    }
}
```

**Controller 使用：**
```java
RtuGateway rtu = rtuService.findById(rtuId);
if (rtu == null) {
    throw new NotFoundException(ErrorCode.RTU_NOT_FOUND);
}
```

#### 方案 2：Service 抛业务异常（推荐复杂项目）

```java
@Service
public class RtuService {
    
    public RtuGateway getById(String rtuId) {
        RtuGateway rtu = rtuMapper.selectById(rtuId);
        if (rtu == null) {
            throw new NotFoundException(ErrorCode.RTU_NOT_FOUND);
        }
        return rtu;
    }
    
    public void save(RtuGateway rtu) {
        if (rtuService.exists(rtu.getRtuId())) {
            throw new BadRequestException(ErrorCode.RTU_ALREADY_EXISTS);
        }
        rtuMapper.insert(rtu);
    }
}
```

**Controller 使用：**
```java
// Service 已处理异常，Controller 直接使用
RtuGateway rtu = rtuService.getById(rtuId);
rtuService.save(rtu);
resp.json(buildSuccessResponse(rtu));
```

**优点：**
- Service 层复用性强
- Controller 更简洁
- 异常处理集中

### 5.5 全局异常日志

建议在 `RouterHandler` 中添加全局异常日志：

```java
@Override
protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
    // ... 省略其他代码
    
    BUSINESS_EXECUTOR.execute(() -> {
        try {
            BaseController controller = match.controllerClass().newInstance();
            controller.handle(request, response);
        } catch (Exception e) {
            // 全局异常日志
            logger.error("HTTP 请求处理失败：path={}, method={}, error={}", 
                request.path(), request.method(), e.getMessage(), e);
            
            response.json(
                HttpResponseStatus.INTERNAL_SERVER_ERROR,
                buildErrorResponse(50000, "系统内部错误")
            );
        } finally {
            httpRequest.release();
        }
    });
}
```

---

## 6. 异常使用速查表

### 6.1 快速选择异常类型

| 场景 | 异常类型 | 错误码段 |
|------|---------|---------|
| 参数校验失败 | BadRequestException | 1000-4999 |
| 资源不存在 | NotFoundException | 对应模块 |
| 系统内部错误 | ServerException | 5000-7999 |
| 权限认证失败 | BadRequestException | 7000-7999 |

### 6.2 常用错误码速查

```java
// RTU 相关
ErrorCode.RTU_NOT_FOUND          // 1001 - RTU 不存在
ErrorCode.RTU_ALREADY_EXISTS     // 1002 - RTU 已存在
ErrorCode.RTU_OFFLINE            // 1003 - RTU 离线

// 数据相关
ErrorCode.DATA_NOT_FOUND         // 2001 - 数据不存在
ErrorCode.TEMPERATURE_OUT_OF_RANGE  // 2005 - 温度超出范围
ErrorCode.HUMIDITY_OUT_OF_RANGE     // 2006 - 湿度超出范围

// 配置相关
ErrorCode.SAMPLING_INTERVAL_INVALID  // 3003 - 采样间隔无效
ErrorCode.TEMP_THRESHOLD_MIN_MAX     // 3005 - 温度阈值错误

// MQTT 相关
ErrorCode.MQTT_PUBLISH_FAILED    // 5002 - MQTT 发布失败

// 数据库相关
ErrorCode.DATABASE_ERROR         // 6001 - 数据库错误
```

### 6.3 一行代码抛异常

```java
// 参数校验
requireNotBlank(value, "fieldName");  // 为空抛 400

// 资源不存在
if (resource == null) throw new NotFoundException(ErrorCode.XXX);

// 重复检查
if (exists) throw new BadRequestException(ErrorCode.XXX_ALREADY_EXISTS);

// 范围校验
if (value < min || value > max) throw new BadRequestException(ErrorCode.XXX_OUT_OF_RANGE);

// 系统异常
try { /* 危险操作 */ } catch (Exception e) { throw new ServerException(ErrorCode.XXX); }
```

---

## 7. 常见问题 FAQ

### Q1: 什么时候用 ErrorCode，什么时候自定义错误码？

**A:** 
- **优先使用 ErrorCode 枚举**（90% 场景）
- 只有 ErrorCode 确实无法覆盖时，才使用自定义错误码
- 自定义错误码需遵循分段规则

```java
// ✅ 推荐
throw new BadRequestException(ErrorCode.RTU_NOT_FOUND);

// ⚠️ 仅在特殊情况下使用
throw new BadRequestException(9999, "特殊的业务错误");
```

### Q2: 异常消息应该多详细？

**A:** 
- **对内（开发调试）**：详细，包含关键信息
- **对外（API 响应）**：简洁，不泄露敏感信息

```java
// 开发环境（详细）
throw new BadRequestException(ErrorCode.RTU_NOT_FOUND, 
    "RTU ID 不存在：rtuId=" + rtuId + ", currentTime=" + System.currentTimeMillis());

// 生产环境（简洁）
throw new BadRequestException(ErrorCode.RTU_NOT_FOUND);
```

### Q3: 如何处理多个参数校验错误？

**A:** 按顺序校验，遇到第一个错误就抛出

```java
// ✅ 推荐：逐个校验，依次抛出
requireNotBlank(rtuId, "rtuId");      // 先校验 rtuId
requireNotBlank(name, "name");        // 再校验 name
requireNotNull(age, "age");           // 最后校验 age

// ❌ 不推荐：一次性收集所有错误
List<String> errors = new ArrayList<>();
if (rtuId == null) errors.add("rtuId 不能为空");
if (name == null) errors.add("name 不能为空");
if (!errors.isEmpty()) throw new BadRequestException(String.join(",", errors));
```

### Q4: Service 层应该抛异常还是返回错误码？

**A:** 
- **简单项目**：Service 返回 null/false，Controller 抛异常
- **复杂项目**：Service 抛业务异常，Controller 统一捕获

根据项目规模选择，保持一致性即可。

### Q5: 如何记录异常日志？

**A:** 在框架层统一记录，业务代码不需要重复记录

```java
// RouterHandler 中统一记录
catch (Exception e) {
    logger.error("请求处理失败：path={}, error={}", request.path(), e.getMessage(), e);
    // ... 返回错误响应
}
```

---

## 8. 总结

### 8.1 核心要点

1. **统一使用 ErrorCode 枚举** - 规范、易维护
2. **选择合适的异常类型** - BadRequest/NotFound/Server
3. **框架自动处理异常** - Controller 只需抛出异常
4. **异常消息要清晰** - 明确问题和解决方案
5. **分层处理原则** - Service 可抛可返回，Controller 负责响应

### 8.2 异常处理流程图

```
请求 → Controller → 业务逻辑
                    ↓
                发生异常？
                    ↓
         是 → 抛出 ApiException 子类
                    ↓
         BaseController.handle() 捕获
                    ↓
         构建统一错误响应
                    ↓
         返回给客户端
```

---

**文档维护**: 新增错误码或修改异常处理逻辑时，请及时更新此文档。
