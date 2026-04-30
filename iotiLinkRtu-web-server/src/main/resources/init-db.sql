-- ========================================
-- IoT Link RTU 系统 - PostgreSQL 初始化脚本
-- ========================================
-- 数据库：iot_link_rtu
-- 版本：v1.0.0
-- 创建日期：2026-03-15
-- ========================================

-- 1. 创建数据库（如果不存在）
SELECT 'CREATE DATABASE iot_link_rtu' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'iot_link_rtu')\gexec

-- 2. 切换到数据库
\c iot_link_rtu;

-- 3. 创建扩展（如果需要 UUID 生成）
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ========================================
-- 表：rtu_gateway（RTU 网关节点管理表）
-- ========================================
CREATE TABLE IF NOT EXISTS rtu_gateway (
    id BIGSERIAL PRIMARY KEY,                              -- 自增主键 ID
    rtu_id VARCHAR(64) NOT NULL,                           -- RTU 唯一标识（如 RTU-001）
    name VARCHAR(64) NOT NULL,                             -- RTU 名称（如"1 号车间工控机"）
    location VARCHAR(256),                                 -- 安装位置描述
    serial_port VARCHAR(64),                               -- 串口号（如 COM3）
    status VARCHAR(32) DEFAULT 'ENABLED' NOT NULL,         -- 网关状态：ENABLED/DISABLED
    online VARCHAR(32) DEFAULT 'OFFLINE' NOT NULL,         -- 在线状态：ONLINE/OFFLINE
    secret VARCHAR(128) NOT NULL,                          -- 认证密钥（明文存储，用于计算 SHA-256）
    heartbeat_time TIMESTAMP,                              -- 最后心跳时间
    create_time TIMESTAMP DEFAULT NOW() NOT NULL,          -- 创建时间
    update_time TIMESTAMP DEFAULT NOW() NOT NULL           -- 更新时间
);

-- 创建唯一索引（rtu_id 全局唯一）
CREATE UNIQUE INDEX IF NOT EXISTS idx_rtu_gateway_rtu_id ON rtu_gateway(rtu_id);
CREATE INDEX IF NOT EXISTS idx_rtu_gateway_status ON rtu_gateway(status);
CREATE INDEX IF NOT EXISTS idx_rtu_gateway_online ON rtu_gateway(online);
CREATE INDEX IF NOT EXISTS idx_rtu_gateway_create_time ON rtu_gateway(create_time);

-- 添加表注释
COMMENT ON TABLE rtu_gateway IS 'RTU 网关管理表（边缘设备注册信息）';
COMMENT ON COLUMN rtu_gateway.rtu_id IS 'RTU 唯一标识（如 RTU-001），全局唯一';
COMMENT ON COLUMN rtu_gateway.name IS '网关名称（如"1 号车间工控机"）';
COMMENT ON COLUMN rtu_gateway.location IS '安装位置描述（如"机房 A 区 -1 号柜"）';
COMMENT ON COLUMN rtu_gateway.serial_port IS '边缘采集器所在串口号';
COMMENT ON COLUMN rtu_gateway.status IS '网关状态：ENABLED（允许采集）/DISABLED（禁止采集）';
COMMENT ON COLUMN rtu_gateway.online IS '在线状态：ONLINE（在线）/OFFLINE（离线）';
COMMENT ON COLUMN rtu_gateway.secret IS '认证密钥（明文存储，用于 SHA-256 加密验证）';
COMMENT ON COLUMN rtu_gateway.heartbeat_time IS '最后心跳时间（用于判断 RTU 是否在线）';
COMMENT ON COLUMN rtu_gateway.create_time IS '创建时间';
COMMENT ON COLUMN rtu_gateway.update_time IS '更新时间';

ALTER TABLE rtu_gateway ADD COLUMN IF NOT EXISTS serial_port VARCHAR(64);

-- ========================================
-- 表：rtu_config（RTU 配置表）
-- ========================================
CREATE TABLE IF NOT EXISTS rtu_config (
    id BIGSERIAL PRIMARY KEY,                              -- 自增主键 ID
    rtu_id VARCHAR(64) NOT NULL,                           -- RTU 唯一标识
    interval INT DEFAULT 1 NOT NULL,                       -- 采集间隔（秒），范围 1-60
    modbus_device_address INT DEFAULT 1 NOT NULL,          -- Modbus 设备地址（1-254）
    baud_rate INT DEFAULT 4800 NOT NULL,                   -- 串口波特率
    temp_calibration DECIMAL(10,2) DEFAULT 0.0,            -- 温度校准值（℃）
    humidity_calibration DECIMAL(10,2) DEFAULT 0.0,        -- 湿度校准值（%RH）
    temp_threshold_min DECIMAL(10,2),                      -- 温度报警下限（℃）
    temp_threshold_max DECIMAL(10,2),                      -- 温度报警上限（℃）
    humidity_threshold_min DECIMAL(10,2),                  -- 湿度报警下限（%RH）
    humidity_threshold_max DECIMAL(10,2),                  -- 湿度报警上限（%RH）
    alarm_enabled BOOLEAN DEFAULT TRUE,                    -- 是否启用报警
    create_time TIMESTAMP DEFAULT NOW() NOT NULL,          -- 创建时间
    update_time TIMESTAMP DEFAULT NOW() NOT NULL           -- 更新时间
);

-- 创建唯一索引（一个 RTU 对应一份配置）
CREATE UNIQUE INDEX IF NOT EXISTS idx_rtu_config_rtu_id ON rtu_config(rtu_id);
CREATE INDEX IF NOT EXISTS idx_rtu_config_interval ON rtu_config(interval);

-- 添加表注释
COMMENT ON TABLE rtu_config IS 'RTU 配置表（采集参数和报警阈值）';
COMMENT ON COLUMN rtu_config.rtu_id IS 'RTU 唯一标识（与 rtu_gateway 关联）';
COMMENT ON COLUMN rtu_config.interval IS '采集间隔（秒），范围 1-60 秒';
COMMENT ON COLUMN rtu_config.modbus_device_address IS 'Modbus 设备地址（1-254），通常为 0x01';
COMMENT ON COLUMN rtu_config.baud_rate IS '串口波特率：1200/2400/4800/9600/19200/38400/57600/115200';
COMMENT ON COLUMN rtu_config.temp_calibration IS '温度校准值（℃），用于修正传感器误差';
COMMENT ON COLUMN rtu_config.humidity_calibration IS '湿度校准值（%RH），用于修正传感器误差';
COMMENT ON COLUMN rtu_config.temp_threshold_min IS '温度报警下限（℃）';
COMMENT ON COLUMN rtu_config.temp_threshold_max IS '温度报警上限（℃）';
COMMENT ON COLUMN rtu_config.humidity_threshold_min IS '湿度报警下限（%RH）';
COMMENT ON COLUMN rtu_config.humidity_threshold_max IS '湿度报警上限（%RH）';
COMMENT ON COLUMN rtu_config.alarm_enabled IS '是否启用报警（true=启用，false=禁用）';
COMMENT ON COLUMN rtu_config.create_time IS '创建时间';
COMMENT ON COLUMN rtu_config.update_time IS '更新时间';

-- ========================================
-- 表：rtu_alarm（RTU 报警表）
-- ========================================
CREATE TABLE IF NOT EXISTS rtu_alarm (
    id BIGSERIAL PRIMARY KEY,                              -- 自增主键 ID
    rtu_id VARCHAR(64) NOT NULL,                           -- RTU 唯一标识
    alarm_type VARCHAR(32) NOT NULL,                       -- 报警类型
    alarm_level VARCHAR(32) DEFAULT 'WARNING' NOT NULL,    -- 报警级别：INFO/WARNING/ERROR
    current_value DECIMAL(10,2) NOT NULL,                  -- 实际触发报警的值
    threshold_value DECIMAL(10,2) NOT NULL,                -- 阈值
    alarm_message TEXT,                                    -- 报警详细描述
    alarm_time TIMESTAMP DEFAULT NOW() NOT NULL,           -- 报警时间
    status VARCHAR(32) DEFAULT 'UNHANDLED' NOT NULL,       -- 处理状态：UNHANDLED/HANDLED
    handle_result TEXT,                                    -- 处理结果描述
    handler VARCHAR(64),                                   -- 处理人
    handle_time TIMESTAMP                                  -- 处理时间
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_rtu_alarm_rtu_id ON rtu_alarm(rtu_id);
CREATE INDEX IF NOT EXISTS idx_rtu_alarm_time ON rtu_alarm(alarm_time);
CREATE INDEX IF NOT EXISTS idx_rtu_alarm_status ON rtu_alarm(status);
CREATE INDEX IF NOT EXISTS idx_rtu_alarm_type ON rtu_alarm(alarm_type);

-- 添加表注释
COMMENT ON TABLE rtu_alarm IS 'RTU 报警记录表';
COMMENT ON COLUMN rtu_alarm.rtu_id IS 'RTU 唯一标识';
COMMENT ON COLUMN rtu_alarm.alarm_type IS '报警类型：TEMP_HIGH（高温）/TEMP_LOW（低温）/HUMIDITY_HIGH（高湿）/HUMIDITY_LOW（低湿）';
COMMENT ON COLUMN rtu_alarm.alarm_level IS '报警级别：INFO（提示）/WARNING（警告）/ERROR（错误）';
COMMENT ON COLUMN rtu_alarm.current_value IS '实际触发报警的值';
COMMENT ON COLUMN rtu_alarm.threshold_value IS '阈值（超过或低于此值触发报警）';
COMMENT ON COLUMN rtu_alarm.alarm_message IS '报警详细描述';
COMMENT ON COLUMN rtu_alarm.alarm_time IS '报警时间';
COMMENT ON COLUMN rtu_alarm.status IS '处理状态：UNHANDLED（未处理）/HANDLED（已处理）';
COMMENT ON COLUMN rtu_alarm.handle_result IS '处理结果描述';
COMMENT ON COLUMN rtu_alarm.handler IS '处理人';
COMMENT ON COLUMN rtu_alarm.handle_time IS '处理时间';

-- ========================================
-- 触发器：自动更新 update_time
-- ========================================
-- rtu_gateway 表
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_rtu_gateway_update_time
    BEFORE UPDATE ON rtu_gateway
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- rtu_config 表
CREATE TRIGGER trg_rtu_config_update_time
    BEFORE UPDATE ON rtu_config
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ========================================
-- 插入测试数据
-- ========================================
INSERT INTO rtu_gateway (rtu_id, name, location, serial_port, status, online, secret) VALUES
('RTU-001', '1 号车间工控机', '机房 A 区 -1 号柜', 'COM3', 'ENABLED', 'ONLINE', 'abc123xyz789'),
('RTU-002', '2 号车间工控机', '机房 B 区 -2 号柜', 'COM4', 'ENABLED', 'OFFLINE', 'def456uvw012'),
('RTU-003', '3 号车间工控机', '机房 C 区 -3 号柜', 'COM5', 'DISABLED', 'OFFLINE', 'ghi789rst345')
ON CONFLICT (rtu_id) DO NOTHING;

INSERT INTO rtu_config (rtu_id, interval, modbus_device_address, baud_rate, temp_threshold_min, temp_threshold_max, humidity_threshold_min, humidity_threshold_max) VALUES
('RTU-001', 1, 1, 9600, 18.0, 28.0, 40.0, 60.0),
('RTU-002', 5, 1, 4800, 15.0, 35.0, 30.0, 70.0),
('RTU-003', 10, 1, 9600, 20.0, 30.0, 45.0, 65.0)
ON CONFLICT (rtu_id) DO NOTHING;

INSERT INTO rtu_alarm (rtu_id, alarm_type, alarm_level, current_value, threshold_value, alarm_message, status) VALUES
('RTU-001', 'TEMP_HIGH', 'WARNING', 32.0, 28.0, '温度超过上限（32.0℃ > 28.0℃）', 'UNHANDLED'),
('RTU-001', 'HUMIDITY_LOW', 'INFO', 35.0, 40.0, '湿度低于下限（35.0% < 40.0%）', 'HANDLED'),
('RTU-002', 'TEMP_LOW', 'ERROR', 12.0, 15.0, '温度低于下限（12.0℃ < 15.0℃）', 'UNHANDLED');

-- ========================================
-- 查询验证
-- ========================================
-- 查看所有表
SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;

-- 查看 rtu_gateway 数据
SELECT * FROM rtu_gateway;

-- 查看 rtu_config 数据
SELECT * FROM rtu_config;

-- 查看 rtu_alarm 数据
SELECT * FROM rtu_alarm;
