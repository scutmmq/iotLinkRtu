package com.scutmmq.config;

import com.scutmmq.utils.MicroConfig;

/**
 * RTU 网关配置类
 *
 * <p>存放 RTU 网关运行所需的配置参数</p>
 *
 * @author mo.mingqin@xlink
 * @since 2026-03-08
 */
public class Config {
    /**
     * RTU 网关服务器端口号
     * <p>从配置文件 "rtu.port" 中读取，默认 8888</p>
     */
    public static final int rtuPort = MicroConfig.readInt("rtu.port", 8888);

    /**
     * MQTT Broker 地址
     * <p>从配置文件 "mqtt.broker.url" 中读取</p>
     */
    public static final String mqttBrokerUrl = MicroConfig.readString("mqtt.broker.url", "tcp://localhost:1883");

    /**
     * MQTT 客户端 ID 前缀
     */
    public static final String mqttClientIdPrefix = MicroConfig.readString("mqtt.client.id.prefix", "rtu-gateway");

    /**
     * Web Server API 地址
     * <p>用于验证 RTU 认证</p>
     */
    public static final String webServerApiUrl = MicroConfig.readString("web.server.api.url", "http://localhost:8080");
}
