# PostgreSQL 数据库操作功能实现指南

## 📋 实现概述

本次实现完成了 webServer 中 PostgreSQL 数据库操作的完整功能，采用 **DAO + Structs** 分层架构，包含 RTU 注册和认证功能。

## 🏗️ 架构设计

```
┌─────────────────────────────────────┐
│         Controller Layer            │
│   (RtuRegisterController, etc.)     │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│          Service Layer              │
│   (RtuGatewayService, RtuAuthService)│
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│            DAO Layer                │
│        (RtuGatewayDao)              │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│         BaseDao (基础封装)           │
│    - queryOne                       │
│    - queryList                      │
│    - update                         │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      DataSourceManager (连接池)      │
│         (HikariCP)                  │
└─────────────────────────────────────┘
```

## 📦 已实现的功能模块

### 1. 数据库连接层

#### **DataSourceManager.java**
- **位置**: `iotiLinkRtu-web-server/src/main/java/com/scutmmq/db/`
- **功能**: 
  - 使用 HikariCP 管理数据库连接池
  - 从 config.properties 读取配置
  - 单例模式，线程安全
- **配置项**:
  ```properties
  postgresql.url=jdbc:postgresql://localhost:5432/iot_link_rtu
  postgresql.username=postgres
  postgresql.password=2004momingqin
  ```

#### **BaseDao.java**
- **位置**: `iotiLinkRtu-web-server/src/main/java/com/scutmmq/db/`
- **功能**:
  - 通用的 CRUD 操作封装
  - 自动参数绑定（PreparedStatement）
  - ResultSet 自动映射到 Java 对象
  - 驼峰命名转下划线命名

### 2. 数据结构层（Structs）

#### **RtuGateway.java**
- **位置**: `iotiLinkRtu-web-server/src/main/java/com/scutmmq/web/model/`
- **对应表**: `rtu_gateway`
- **字段**:
  - `id`: 主键 ID
  - `rtuId`: RTU 唯一标识
  - `name`: RTU 名称
  - `location`: 安装位置
  - `status`: ENABLED/DISABLED
  - `online`: ONLINE/OFFLINE
  - `secret`: 认证密钥（明文存储）
  - `heartbeatTime`: 最后心跳时间
  - `createTime`, `updateTime`: 时间戳

### 3. 数据访问层（DAO）

#### **RtuGatewayDao.java**
- **位置**: `iotiLinkRtu-web-server/src/main/java/com/scutmmq/web/dao/`
- **提供方法**:
  - `findByRtuId(String rtuId)`: 根据 rtuId 查询
  - `findById(Long id)`: 根据 ID 查询
  - `findAll()`: 查询所有
  - `findPage(int offset, int limit)`: 分页查询
  - `findPageByStatus(...)`: 按状态筛选
  - `save(RtuGateway rtu)`: 保存
  - `update(RtuGateway rtu)`: 更新
  - `updateOnlineStatus(...)`: 更新在线状态
  - `deleteByRtuId(String rtuId)`: 删除
  - `exists(String rtuId)`: 检查是否存在

### 4. 业务服务层（Service）

#### **RtuGatewayService.java**
- **位置**: `iotiLinkRtu-web-server/src/main/java/com/scutmmq/web/service/`
- **核心功能**:
  - **RTU 注册**: `register(RtuGateway rtu)`
    - 自动生成 32 位 UUID 作为 secret
    - 检查 RTU 是否已存在
    - 设置默认状态（ENABLED/OFFLINE）
  - **RTU 查询**: `findByRtuId()`, `findPage()`
  - **RTU 更新**: `update()`
  - **RTU 删除**: `delete()`
  - **状态更新**: `updateOnlineStatus()`

#### **RtuAuthService.java**
- **位置**: `iotiLinkRtu-web-server/src/main/java/com/scutmmq/web/service/`
- **核心功能**:
  - **RTU 认证验证**: `verify(String rtuId, String secretHash, long timestamp)`
    - 验证时间戳有效性（±5 分钟，防止重放攻击）
    - 查询 RTU 信息
    - 检查 RTU 状态（必须为 ENABLED）
    - 验证密钥哈希：`SHA256(secret + rtuId + timestamp[0:4])`
  - **返回结果**: `VerifyResult` 对象
    - `valid`: 是否通过
    - `status`: RTU 状态
    - `message`: 验证消息

### 5. 工具类

#### **Sha256Util.java**
- **位置**: `iotiLinkRtu-web-server/src/main/java/com/scutmmq/utils/`
- **核心方法**:
  - `sha256(String input)`: 计算 SHA-256 哈希
  - `calculateSecretHash(secret, rtuId, timestamp)`: 计算认证密钥哈希
  - `verifySecret(secret, rtuId, receivedHash, timestamp)`: 验证密钥
  - `isTimestampValid(timestamp)`: 检查时间戳有效性

## 🔐 SecretHash 解决方案

### 加密算法

**公式**: `SHA256(原始 secret + rtuId + timestamp 前 4 字节)`

**实现细节**:
1. 提取时间戳前 4 字节（大端序，秒级时间戳）
2. 拼接字符串：`input = secret + rtuId + timeBytes`
3. 计算 SHA-256: `hash = SHA256(input)`
4. 转为十六进制字符串（64 字符）

### 认证流程

```
1. serial-collector 发送认证帧
   └─> 包含：rtuId (明文) + secretHash (SHA-256) + timestamp

2. rtu-gateway 调用 web-server API
   └─> POST /api/rtu/gateway/verify
       请求体：{"rtuId": "RTU-001", "secretHash": "...", "timestamp": 1234567890}

3. web-server 验证逻辑
   ├─> 检查时间戳有效性（±5 分钟）
   ├─> 根据 rtuId 查询数据库获取 secret
   ├─> 计算期望的 hash: expectedHash = SHA256(secret + rtuId + timestamp[0:4])
   ├─> 对比：receivedHash == expectedHash
   └─> 检查 status=ENABLED

4. 返回验证结果
   └─> {"valid": true, "status": "ENABLED", "message": "认证成功"}
```

### 安全性保障

1. **防重放攻击**: 时间戳必须在±5 分钟内
2. **单向加密**: secret 不直接传输，只传输 hash 值
3. **动态验证**: 每次认证的 hash 都不同（时间戳参与计算）
4. **数据库存储**: secret 明文存储（用于计算 hash，非密码场景）

## 📊 数据库表结构

### rtu_gateway（RTU 网关节点管理表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 自增主键 |
| rtu_id | VARCHAR(64) | NOT NULL UNIQUE | RTU 唯一标识 |
| name | VARCHAR(64) | NOT NULL | RTU 名称 |
| location | VARCHAR(256) | - | 安装位置 |
| status | VARCHAR(32) | DEFAULT 'ENABLED' | 网关状态 |
| online | VARCHAR(32) | DEFAULT 'OFFLINE' | 在线状态 |
| secret | VARCHAR(128) | NOT NULL | 认证密钥 |
| heartbeat_time | TIMESTAMP | - | 最后心跳 |
| create_time | TIMESTAMP | DEFAULT NOW() | 创建时间 |
| update_time | TIMESTAMP | DEFAULT NOW() | 更新时间 |

**索引**:
- `idx_rtu_gateway_rtu_id`: 唯一索引
- `idx_rtu_gateway_status`: 普通索引
- `idx_rtu_gateway_online`: 普通索引
- `idx_rtu_gateway_create_time`: 普通索引

### rtu_config（RTU 配置表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 自增主键 |
| rtu_id | VARCHAR(64) | NOT NULL UNIQUE | RTU 唯一标识 |
| interval | INT | DEFAULT 1 | 采集间隔（秒） |
| modbus_device_address | INT | DEFAULT 1 | Modbus 地址 |
| baud_rate | INT | DEFAULT 4800 | 波特率 |
| temp_calibration | DECIMAL(10,2) | DEFAULT 0.0 | 温度校准 |
| humidity_calibration | DECIMAL(10,2) | DEFAULT 0.0 | 湿度校准 |
| temp_threshold_min | DECIMAL(10,2) | - | 温度下限 |
| temp_threshold_max | DECIMAL(10,2) | - | 温度上限 |
| humidity_threshold_min | DECIMAL(10,2) | - | 湿度下限 |
| humidity_threshold_max | DECIMAL(10,2) | - | 湿度上限 |
| alarm_enabled | BOOLEAN | DEFAULT TRUE | 启用报警 |
| create_time | TIMESTAMP | DEFAULT NOW() | 创建时间 |
| update_time | TIMESTAMP | DEFAULT NOW() | 更新时间 |

**索引**:
- `idx_rtu_config_rtu_id`: 唯一索引

### rtu_alarm（RTU 报警表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 自增主键 |
| rtu_id | VARCHAR(64) | NOT NULL | RTU 唯一标识 |
| alarm_type | VARCHAR(32) | NOT NULL | 报警类型 |
| alarm_level | VARCHAR(32) | DEFAULT 'WARNING' | 报警级别 |
| current_value | DECIMAL(10,2) | NOT NULL | 实际值 |
| threshold_value | DECIMAL(10,2) | NOT NULL | 阈值 |
| alarm_message | TEXT | - | 报警描述 |
| alarm_time | TIMESTAMP | DEFAULT NOW() | 报警时间 |
| status | VARCHAR(32) | DEFAULT 'UNHANDLED' | 处理状态 |
| handle_result | TEXT | - | 处理结果 |
| handler | VARCHAR(64) | - | 处理人 |
| handle_time | TIMESTAMP | - | 处理时间 |

**索引**:
- `idx_rtu_alarm_rtu_id`: 普通索引
- `idx_rtu_alarm_time`: 普通索引
- `idx_rtu_alarm_status`: 普通索引
- `idx_rtu_alarm_type`: 普通索引

## 🚀 使用示例

### 1. 初始化数据库

```bash
# 连接到 PostgreSQL
psql -U postgres

# 执行初始化脚本
\i /path/to/init-db.sql
```

### 2. Controller 中使用 Service

#### RTU 注册 Controller 示例

```java
public class RtuRegisterController extends BaseController {
    
    private final RtuGatewayService rtuService = new RtuGatewayService();
    
    @Override
    protected void post(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取请求参数
        Map<String, Object> body = req.bodyJson();
        String rtuId = req.bodyString("rtuId");
        String name = req.bodyString("name");
        String location = req.bodyString("location");
        
        // 2. 参数校验
        requireNotBlank(rtuId, "rtuId");
        requireNotBlank(name, "name");
        
        // 3. 创建 RTU 对象
        RtuGateway rtu = new RtuGateway();
        rtu.setRtuId(rtuId);
        rtu.setName(name);
        rtu.setLocation(location);
        
        // 4. 调用 Service 注册
        RtuGateway savedRtu = rtuService.register(rtu);
        
        // 5. 返回响应
        Map<String, Object> data = Map.of(
            "id", savedRtu.getId(),
            "rtuId", savedRtu.getRtuId(),
            "secret", savedRtu.getSecret(),  // 返回生成的密钥
            "status", savedRtu.getStatus(),
            "createTime", savedRtu.getCreateTime().toString()
        );
        resp.json(buildSuccessResponse(data));
    }
}
```

#### RTU 认证验证 Controller 示例

```java
public class RtuVerifyController extends BaseController {
    
    private final RtuAuthService authService = new RtuAuthService();
    
    @Override
    protected void post(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取请求参数
        String rtuId = req.bodyString("rtuId");
        String secretHash = req.bodyString("secretHash");
        Long timestamp = toLong(req.bodyJson().get("timestamp"));
        
        // 2. 参数校验
        requireNotBlank(rtuId, "rtuId");
        requireNotBlank(secretHash, "secretHash");
        requireNotNull(timestamp, "timestamp");
        
        // 3. 调用 Service 验证
        RtuAuthService.VerifyResult result = authService.verify(rtuId, secretHash, timestamp);
        
        // 4. 返回响应
        if (result.isValid()) {
            Map<String, Object> data = Map.of(
                "valid", true,
                "status", result.getStatus(),
                "rtuId", result.getRtuId()
            );
            resp.json(buildSuccessResponse(data));
        } else {
            Map<String, Object> errorData = Map.of(
                "valid", false,
                "message", result.getMessage()
            );
            resp.json(HttpResponseStatus.UNAUTHORIZED, buildErrorResponse(401, result.getMessage()));
        }
    }
    
    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }
}
```

## ✅ 测试清单

### 单元测试
- [ ] DataSourceManager 连接测试
- [ ] BaseDao 参数绑定测试
- [ ] RtuGatewayDao CRUD 测试
- [ ] RtuGatewayService 注册测试
- [ ] RtuAuthService 认证测试
- [ ] Sha256Util 哈希计算测试

### 集成测试
- [ ] RTU 注册接口测试
- [ ] RTU 认证接口测试
- [ ] RTU 列表查询测试
- [ ] RTU 状态更新测试

## 📝 注意事项

1. **数据库配置安全**: 
   - 生产环境应使用环境变量或加密配置
   - 不要将密码提交到版本控制

2. **连接池调优**:
   - 最小空闲连接：5
   - 最大连接数：20
   - 根据实际并发量调整

3. **事务管理**:
   - 当前实现未包含事务管理
   - 涉及多表操作时需要手动添加事务

4. **SQL 注入防护**:
   - BaseDao 使用 PreparedStatement
   - 避免字符串拼接 SQL

5. **secretHash 时效性**:
   - 时间戳必须在±5 分钟内
   - 超过时间窗口将验证失败

## 🔄 后续扩展

### 待实现功能
- [ ] RtuConfigDao 和 RtuConfigService（配置管理）
- [ ] RtuAlarmDao 和 RtuAlarmService（报警管理）
- [ ] RtuDataDao 和 RtuDataService（数据采集 - TDEngine）
- [ ] 事务管理器（TransactionManager）
- [ ] 通用计数方法（BaseDao.count()）

### 性能优化
- [ ] 添加 Redis 缓存层
- [ ] 批量操作优化
- [ ] 连接池监控
- [ ] SQL 慢查询日志

---

**文档版本**: v1.0.0  
**创建日期**: 2026-03-15  
**作者**: mo.mingqin@xlink
