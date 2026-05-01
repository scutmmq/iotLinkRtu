package com.scutmmq.web.dao;

import com.scutmmq.db.BaseDao;
import com.scutmmq.db.DataSourceManager;
import com.scutmmq.web.model.RtuData;
import com.scutmmq.web.model.RtuDataStatistics;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RTU 温湿度采集数据 DAO。
 */
public class RtuDataDao extends BaseDao {

    public boolean tableExists() {
        try (Connection conn = DataSourceManager.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, "rtu_data", new String[]{"TABLE"})) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("检查 rtu_data 表失败：" + e.getMessage(), e);
        }
    }

    public RtuData findLatestByRtuId(String rtuId) {
        String sql = """
                SELECT * FROM rtu_data
                WHERE rtu_id = ?
                ORDER BY collect_time DESC, id DESC
                LIMIT 1
                """;
        return queryOne(sql, RtuData.class, rtuId);
    }

    public List<RtuData> findPage(String rtuId, LocalDateTime startTime, LocalDateTime endTime, int offset, int limit) {
        String sql = """
                SELECT * FROM rtu_data
                WHERE rtu_id = ? AND collect_time >= ? AND collect_time <= ?
                ORDER BY collect_time DESC, id DESC
                LIMIT ? OFFSET ?
                """;
        return queryList(sql, RtuData.class, rtuId, startTime, endTime, limit, offset);
    }

    public long count(String rtuId, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = """
                SELECT COUNT(*) FROM rtu_data
                WHERE rtu_id = ? AND collect_time >= ? AND collect_time <= ?
                """;
        return queryLong(sql, rtuId, startTime, endTime);
    }

    public RtuDataStatistics statistics(String rtuId, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = """
                SELECT
                    AVG(temperature) AS avg_temperature,
                    MAX(temperature) AS max_temperature,
                    MIN(temperature) AS min_temperature,
                    AVG(humidity) AS avg_humidity,
                    MAX(humidity) AS max_humidity,
                    MIN(humidity) AS min_humidity,
                    COUNT(*) AS data_count
                FROM rtu_data
                WHERE rtu_id = ? AND collect_time >= ? AND collect_time <= ?
                """;
        return queryOne(sql, RtuDataStatistics.class, rtuId, startTime, endTime);
    }
}
