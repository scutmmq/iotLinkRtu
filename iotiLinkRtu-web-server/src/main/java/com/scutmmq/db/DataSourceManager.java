package com.scutmmq.db;

import com.scutmmq.web.config.WebServerProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库连接池管理器
 * 使用 HikariCP 管理 PostgreSQL 数据库连接
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-15
 */
public class DataSourceManager {
    
    private static final Logger log = LoggerFactory.getLogger(DataSourceManager.class);
    private static volatile HikariDataSource dataSource;
    
    /**
     * 私有构造函数，防止实例化
     */
    private DataSourceManager() {}
    
    /**
     * 获取数据源（单例模式）
     * 
     * @return HikariDataSource 数据源
     */
    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (DataSourceManager.class) {
                if (dataSource == null) {
                    initDataSource();
                }
            }
        }
        return dataSource;
    }
    
    /**
     * 初始化数据源
     */
    private static void initDataSource() {
        log.info("正在初始化 PostgreSQL 数据源...");
        
        try {
            // 从配置文件读取数据库配置
            String url = WebServerProperties.readString("postgresql.url");
            String username = WebServerProperties.readString("postgresql.username");
            String password = WebServerProperties.readString("postgresql.password");
            
            // 验证配置
            if (url == null || username == null || password == null) {
                throw new RuntimeException("数据库配置不完整，请检查 config.properties 文件");
            }
            
            // 创建 HikariCP 配置
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            
            // PostgreSQL 驱动类名
            config.setDriverClassName("org.postgresql.Driver");
            
            // 连接池参数配置
            config.setMinimumIdle(5);           // 最小空闲连接数
            config.setMaximumPoolSize(20);      // 最大连接数
            config.setConnectionTimeout(30000); // 连接超时时间（30 秒）
            config.setIdleTimeout(600000);      // 空闲连接超时（10 分钟）
            config.setMaxLifetime(1800000);     // 连接最大生命周期（30 分钟）
            
            // PostgreSQL 特定优化
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            
            // 创建数据源
            dataSource = new HikariDataSource(config);
            
            // 测试连接
            try (Connection conn = dataSource.getConnection()) {
                if (conn != null) {
                    log.info("PostgreSQL 数据源初始化成功！");
                    log.info("数据库 URL: {}", url);
                    log.info("连接池配置：minIdle={}, maxPoolSize={}", 
                             config.getMinimumIdle(), config.getMaximumPoolSize());
                }
            }
            
        } catch (SQLException e) {
            log.error("PostgreSQL 数据源初始化失败！", e);
            throw new RuntimeException("数据库连接失败：" + e.getMessage(), e);
        } catch (Exception e) {
            log.error("数据库配置加载失败！", e);
            throw new RuntimeException("数据库配置加载失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 获取数据库连接
     * 
     * @return 数据库连接
     * @throws SQLException SQL 异常
     */
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }
    
    /**
     * 关闭数据源（应用关闭时调用）
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("正在关闭数据库连接池...");
            dataSource.close();
            log.info("数据库连接池已关闭");
        }
    }
}
