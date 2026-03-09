package com.scutmmq.rtu.config;

/**
 * 系统配置类
 * 
 * <p>存放系统运行所需的配置参数</p>
 * 
 * @author mo.mingqin@xlink
 * @since 2026-03-08
 */
public class Config {
    /**
     * RTU 服务器端口号
     * <p>从系统属性 "rtu.port" 中读取，并转换为整数类型</p>
     */
    public static final int rtuPort = Integer.parseInt(System.getProperty("rtu.port"));
}
