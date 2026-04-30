package com.scutmmq.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 基础 DAO 抽象类
 * 提供通用的数据库操作方法，子类继承后可直接使用
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-15
 */
public abstract class BaseDao {
    
    protected static final Logger log = LoggerFactory.getLogger(BaseDao.class);
    
    /**
     * 执行 UPDATE/INSERT/DELETE 操作
     * 
     * @param sql SQL 语句
     * @param params 参数列表
     * @return 影响的行数
     */
    protected int update(String sql, Object... params) {
        try (Connection conn = DataSourceManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setParameters(stmt, params);
            int rows = stmt.executeUpdate();
            log.debug("SQL 执行成功，影响行数：{}", rows);
            return rows;
            
        } catch (SQLException e) {
            log.error("SQL 执行失败：{}, 参数：{}", sql, params, e);
            throw new RuntimeException("数据库操作失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 执行查询并返回单个对象
     * 
     * @param sql SQL 语句
     * @param clazz 目标类型
     * @param params 参数列表
     * @param <T> 泛型类型
     * @return 查询结果对象，不存在返回 null
     */
    protected <T> T queryOne(String sql, Class<T> clazz, Object... params) {
        try (Connection conn = DataSourceManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setParameters(stmt, params);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToObject(rs, clazz);
                }
                return null;
            }
            
        } catch (SQLException e) {
            log.error("SQL 查询失败：{}, 参数：{}", sql, params, e);
            throw new RuntimeException("数据库查询失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 执行查询并返回对象列表
     * 
     * @param sql SQL 语句
     * @param clazz 目标类型
     * @param params 参数列表
     * @param <T> 泛型类型
     * @return 查询结果列表
     */
    protected <T> List<T> queryList(String sql, Class<T> clazz, Object... params) {
        List<T> resultList = new ArrayList<>();
        
        try (Connection conn = DataSourceManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setParameters(stmt, params);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    T obj = mapResultSetToObject(rs, clazz);
                    if (obj != null) {
                        resultList.add(obj);
                    }
                }
            }
            
        } catch (SQLException e) {
            log.error("SQL 查询失败：{}, 参数：{}", sql, params, e);
            throw new RuntimeException("数据库查询失败：" + e.getMessage(), e);
        }
        
        return resultList;
    }

    /**
     * 查询 long 标量值
     */
    protected long queryLong(String sql, Object... params) {
        try (Connection conn = DataSourceManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setParameters(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Object value = rs.getObject(1);
                    if (value instanceof Number number) {
                        return number.longValue();
                    }
                }
                return 0L;
            }

        } catch (SQLException e) {
            log.error("SQL 查询失败：{}, 参数：{}", sql, params, e);
            throw new RuntimeException("数据库查询失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 设置 PreparedStatement 参数
     */
    private void setParameters(PreparedStatement stmt, Object[] params) throws SQLException {
        if (params == null) {
            return;
        }
        
        for (int i = 0; i < params.length; i++) {
            int paramIndex = i + 1;
            Object param = params[i];
            
            if (param == null) {
                stmt.setNull(paramIndex, Types.NULL);
            } else if (param instanceof String) {
                stmt.setString(paramIndex, (String) param);
            } else if (param instanceof Integer) {
                stmt.setInt(paramIndex, (Integer) param);
            } else if (param instanceof Long) {
                stmt.setLong(paramIndex, (Long) param);
            } else if (param instanceof Double) {
                stmt.setDouble(paramIndex, (Double) param);
            } else if (param instanceof Float) {
                stmt.setFloat(paramIndex, (Float) param);
            } else if (param instanceof Boolean) {
                stmt.setBoolean(paramIndex, (Boolean) param);
            } else if (param instanceof LocalDateTime) {
                stmt.setTimestamp(paramIndex, Timestamp.valueOf((LocalDateTime) param));
            } else if (param instanceof Timestamp) {
                stmt.setTimestamp(paramIndex, (Timestamp) param);
            } else if (param instanceof Date) {
                stmt.setDate(paramIndex, (Date) param);
            } else {
                stmt.setObject(paramIndex, param);
            }
        }
    }
    
    /**
     * 将 ResultSet 映射为 Java 对象
     */
    private <T> T mapResultSetToObject(ResultSet rs, Class<T> clazz) throws SQLException {
        try {
            T obj = clazz.getDeclaredConstructor().newInstance();
            Field[] fields = clazz.getDeclaredFields();
            
            for (Field field : fields) {
                String columnName = camelToSnake(field.getName());
                Object value;
                try {
                    value = getColumnValue(rs, columnName, field.getType());
                } catch (SQLException e) {
                    // 兼容实体字段比查询列更多的场景，例如渐进式数据库迁移
                    continue;
                }

                if (value != null) {
                    field.setAccessible(true);
                    field.set(obj, value);
                }
            }
            
            return obj;
            
        } catch (Exception e) {
            log.error("ResultSet 映射失败：clazz={}", clazz.getName(), e);
            throw new RuntimeException("数据映射失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 根据字段类型获取列值
     */
    private Object getColumnValue(ResultSet rs, String columnName, Class<?> fieldType) throws SQLException {
        Object value = rs.getObject(columnName);
        if (value == null) {
            return null;
        }

        if (fieldType == String.class) {
            return value.toString();
        } else if (fieldType == Integer.class || fieldType == int.class) {
            return ((Number) value).intValue();
        } else if (fieldType == Long.class || fieldType == long.class) {
            return ((Number) value).longValue();
        } else if (fieldType == Double.class || fieldType == double.class) {
            return ((Number) value).doubleValue();
        } else if (fieldType == Float.class || fieldType == float.class) {
            return ((Number) value).floatValue();
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            return (Boolean) value;
        } else if (fieldType == LocalDateTime.class) {
            if (value instanceof Timestamp timestamp) {
                return timestamp.toLocalDateTime();
            }
            return value;
        } else if (fieldType == Timestamp.class) {
            return rs.getTimestamp(columnName);
        } else if (fieldType == Date.class) {
            return rs.getDate(columnName);
        } else if (fieldType == BigDecimal.class) {
            return rs.getBigDecimal(columnName);
        }
        return value;
    }
    
    /**
     * 驼峰命名转下划线命名
     * 例如：rtuId -> rtu_id
     */
    private String camelToSnake(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : camelCase.toCharArray()) {
            if (Character.isUpperCase(c)) {
                result.append('_');
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
