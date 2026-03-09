package com.th.serial.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 微型配置文件读取工具类
 * 用于读取 resources/config.properties 配置文件
 */
public class MicroConfig {

    private static final String CONFIG_FILE = "config.properties";
    private static final Properties properties = new Properties();
    private static boolean loaded = false;

    static {
        loadConfig();
    }

    /**
     * 加载配置文件
     */
    private static void loadConfig() {
        if (loaded) {
            return;
        }

        try (InputStream input = MicroConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new RuntimeException("Unable to find " + CONFIG_FILE);
            }
            properties.load(input);
            loaded = true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file: " + CONFIG_FILE, e);
        }
    }

    /**
     * 读取字符串配置
     * @param key 配置键
     * @return 配置值，如果不存在返回 null
     */
    public static String readString(String key) {
        return properties.getProperty(key);
    }

    /**
     * 读取字符串配置（带默认值）
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值，如果不存在返回默认值
     */
    public static String readString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * 读取整数配置
     * @param key 配置键
     * @return 配置值
     * @throws NumberFormatException 如果配置值不是有效的整数
     * @throws NullPointerException 如果配置键不存在
     */
    public static int readInt(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new NullPointerException("Config key not found: " + key);
        }
        return Integer.parseInt(value.trim());
    }

    /**
     * 读取整数配置（带默认值）
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值，如果不存在或解析失败返回默认值
     */
    public static int readInt(String key, int defaultValue) {
        try {
            String value = properties.getProperty(key);
            if (value == null) {
                return defaultValue;
            }
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 读取长整数配置
     * @param key 配置键
     * @return 配置值
     * @throws NumberFormatException 如果配置值不是有效的长整数
     * @throws NullPointerException 如果配置键不存在
     */
    public static long readLong(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new NullPointerException("Config key not found: " + key);
        }
        return Long.parseLong(value.trim());
    }

    /**
     * 读取长整数配置（带默认值）
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值，如果不存在或解析失败返回默认值
     */
    public static long readLong(String key, long defaultValue) {
        try {
            String value = properties.getProperty(key);
            if (value == null) {
                return defaultValue;
            }
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 读取布尔配置
     * @param key 配置键
     * @return 配置值
     * @throws NullPointerException 如果配置键不存在
     */
    public static boolean readBoolean(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new NullPointerException("Config key not found: " + key);
        }
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * 读取布尔配置（带默认值）
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值，如果不存在返回默认值
     */
    public static boolean readBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * 读取点数配置
     * @param key 配置键
     * @return 配置值
     * @throws NumberFormatException 如果配置值不是有效的浮点数
     * @throws NullPointerException 如果配置键不存在
     */
    public static double readDouble(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new NullPointerException("Config key not found: " + key);
        }
        return Double.parseDouble(value.trim());
    }

    /**
     * 读取双精度浮点数配置（带默认值）
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值，如果不存在或解析失败返回默认值
     */
    public static double readDouble(String key, double defaultValue) {
        try {
            String value = properties.getProperty(key);
            if (value == null) {
                return defaultValue;
            }
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 检查配置键是否存在
     * @param key 配置键
     * @return 如果存在返回 true，否则返回 false
     */
    public static boolean containsKey(String key) {
        return properties.containsKey(key);
    }

    /**
     * 获取所有配置属性
     * @return Properties 对象
     */
    public static Properties getProperties() {
        return (Properties) properties.clone();
    }

    /**
     * 重新加载配置文件
     */
    public static void reload() {
        properties.clear();
        loaded = false;
        loadConfig();
    }
}
