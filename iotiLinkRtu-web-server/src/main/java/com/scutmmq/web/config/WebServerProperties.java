package com.scutmmq.web.config;

import com.scutmmq.utils.MicroConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Web Server 专用配置，避免打包时与 common 模块的 config.properties 冲突。
 */
public final class WebServerProperties {

    private static final String CONFIG_FILE = "web-server.properties";
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = WebServerProperties.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                PROPERTIES.load(input);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + CONFIG_FILE, e);
        }
    }

    private WebServerProperties() {
    }

    public static String readString(String key) {
        String value = PROPERTIES.getProperty(key);
        return value != null ? value : MicroConfig.readString(key);
    }

    public static int readInt(String key) {
        String value = PROPERTIES.getProperty(key);
        return value != null ? Integer.parseInt(value.trim()) : MicroConfig.readInt(key);
    }
}
