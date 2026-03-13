package com.scutmmq.exception;

/**
 * 错误码枚举 - 定义系统中所有业务错误码
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public enum ErrorCode {
    
    // ==================== 通用错误 (200-499) ====================
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "方法不允许"),
    INTERNAL_ERROR(500, "系统内部错误"),
    
    // ==================== RTU 管理错误 (1000-1999) ====================
    RTU_NOT_FOUND(1001, "RTU 不存在"),
    RTU_ALREADY_EXISTS(1002, "RTU 已存在"),
    RTU_OFFLINE(1003, "RTU 离线"),
    RTU_ONLINE(1004, "RTU 已在线"),
    RTU_CONFIG_ERROR(1005, "RTU 配置错误"),
    RTU_ID_EMPTY(1006, "RTU ID 不能为空"),
    RTU_NAME_EMPTY(1007, "RTU 名称不能为空"),
    RTU_INVALID_STATUS(1008, "无效的 RTU 状态"),
    RTU_DUPLICATE_ID(1009, "RTU ID 重复"),
    
    // ==================== 数据相关错误 (2000-2999) ====================
    DATA_NOT_FOUND(2001, "数据不存在"),
    DATA_INVALID(2002, "数据格式错误"),
    DATA_OUT_OF_RANGE(2003, "数据超出范围"),
    DATA_PARSE_ERROR(2004, "数据解析失败"),
    TEMPERATURE_OUT_OF_RANGE(2005, "温度值超出范围 (-40~85°C)"),
    HUMIDITY_OUT_OF_RANGE(2006, "湿度值超出范围 (0~100%)"),
    TIMESTAMP_INVALID(2007, "时间戳无效"),
    
    // ==================== 配置管理错误 (3000-3999) ====================
    CONFIG_NOT_FOUND(3001, "配置不存在"),
    CONFIG_INVALID(3002, "配置格式错误"),
    SAMPLING_INTERVAL_INVALID(3003, "采样间隔无效 (1-60 秒)"),
    THRESHOLD_INVALID(3004, "阈值设置错误"),
    TEMP_THRESHOLD_MIN_MAX(3005, "温度最小值不能大于最大值"),
    HUMI_THRESHOLD_MIN_MAX(3006, "湿度最小值不能大于最大值"),
    CONFIG_APPLY_FAILED(3007, "配置下发失败"),
    
    // ==================== 报警管理错误 (4000-4999) ====================
    ALARM_NOT_FOUND(4001, "报警记录不存在"),
    ALARM_ALREADY_HANDLED(4002, "报警已处理"),
    ALARM_TYPE_INVALID(4003, "报警类型无效"),
    ALARM_LEVEL_INVALID(4004, "报警级别无效"),
    ALARM_HANDLE_FAILED(4005, "报警处理失败"),
    
    // ==================== MQTT 通信错误 (5000-5999) ====================
    MQTT_CONNECTION_ERROR(5001, "MQTT 连接错误"),
    MQTT_PUBLISH_FAILED(5002, "MQTT消息发布失败"),
    MQTT_SUBSCRIBE_FAILED(5003, "MQTT 订阅失败"),
    MQTT_TOPIC_INVALID(5004, "MQTT Topic 无效"),
    MQTT_MESSAGE_FORMAT_ERROR(5005, "MQTT消息格式错误"),
    
    // ==================== 数据库错误 (6000-6999) ====================
    DATABASE_ERROR(6001, "数据库错误"),
    DATABASE_CONNECTION_FAILED(6002, "数据库连接失败"),
    DATABASE_QUERY_FAILED(6003, "查询失败"),
    DATABASE_UPDATE_FAILED(6004, "更新失败"),
    DATABASE_DELETE_FAILED(6005, "删除失败"),
    DATABASE_CONSTRAINT_VIOLATION(6006, "数据库约束违反"),
    
    // ==================== 权限认证错误 (7000-7999) ====================
    TOKEN_EMPTY(7001, "Token 不能为空"),
    TOKEN_INVALID(7002, "Token 无效"),
    TOKEN_EXPIRED(7003, "Token 已过期"),
    USER_NOT_FOUND(7004, "用户不存在"),
    PASSWORD_ERROR(7005, "密码错误"),
    PERMISSION_DENIED(7006, "权限不足"),
    USER_DISABLED(7007, "用户已禁用");
    
    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}
