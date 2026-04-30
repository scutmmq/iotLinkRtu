package com.scutmmq.web.dao;

import com.scutmmq.db.BaseDao;
import com.scutmmq.web.model.RtuAlarm;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * RTU 报警 DAO
 */
public class RtuAlarmDao extends BaseDao {

    public RtuAlarm findById(Long id) {
        String sql = "SELECT * FROM rtu_alarm WHERE id = ?";
        return queryOne(sql, RtuAlarm.class, id);
    }

    public List<RtuAlarm> findPage(String rtuId, String status, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM rtu_alarm WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (rtuId != null && !rtuId.isBlank()) {
            sql.append(" AND rtu_id = ?");
            params.add(rtuId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        sql.append(" ORDER BY alarm_time DESC, id DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return queryList(sql.toString(), RtuAlarm.class, params.toArray());
    }

    public long count(String rtuId, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM rtu_alarm WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (rtuId != null && !rtuId.isBlank()) {
            sql.append(" AND rtu_id = ?");
            params.add(rtuId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        return queryLong(sql.toString(), params.toArray());
    }

    public boolean handle(Long id, String handleResult, String handler, LocalDateTime handleTime) {
        String sql = """
                UPDATE rtu_alarm
                SET status = 'HANDLED', handle_result = ?, handler = ?, handle_time = ?
                WHERE id = ?
                """;
        return update(sql, handleResult, handler, handleTime, id) > 0;
    }
}
