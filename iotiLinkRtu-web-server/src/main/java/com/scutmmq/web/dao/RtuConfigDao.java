package com.scutmmq.web.dao;

import com.scutmmq.db.BaseDao;
import com.scutmmq.web.model.RtuConfig;

/**
 * RTU 配置 DAO
 */
public class RtuConfigDao extends BaseDao {

    public RtuConfig findByRtuId(String rtuId) {
        String sql = "SELECT * FROM rtu_config WHERE rtu_id = ?";
        return queryOne(sql, RtuConfig.class, rtuId);
    }

    public boolean save(RtuConfig config) {
        String sql = """
                INSERT INTO rtu_config (
                    rtu_id, interval, modbus_device_address, baud_rate,
                    temp_calibration, humidity_calibration,
                    temp_threshold_min, temp_threshold_max,
                    humidity_threshold_min, humidity_threshold_max,
                    alarm_enabled
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        int rows = update(sql,
                config.getRtuId(),
                config.getInterval(),
                config.getModbusDeviceAddress(),
                config.getBaudRate(),
                config.getTempCalibration(),
                config.getHumidityCalibration(),
                config.getTempThresholdMin(),
                config.getTempThresholdMax(),
                config.getHumidityThresholdMin(),
                config.getHumidityThresholdMax(),
                config.getAlarmEnabled());
        return rows > 0;
    }

    public boolean update(RtuConfig config) {
        String sql = """
                UPDATE rtu_config SET
                    interval = ?, modbus_device_address = ?, baud_rate = ?,
                    temp_calibration = ?, humidity_calibration = ?,
                    temp_threshold_min = ?, temp_threshold_max = ?,
                    humidity_threshold_min = ?, humidity_threshold_max = ?,
                    alarm_enabled = ?
                WHERE rtu_id = ?
                """;
        int rows = update(sql,
                config.getInterval(),
                config.getModbusDeviceAddress(),
                config.getBaudRate(),
                config.getTempCalibration(),
                config.getHumidityCalibration(),
                config.getTempThresholdMin(),
                config.getTempThresholdMax(),
                config.getHumidityThresholdMin(),
                config.getHumidityThresholdMax(),
                config.getAlarmEnabled(),
                config.getRtuId());
        return rows > 0;
    }

    public boolean deleteByRtuId(String rtuId) {
        String sql = "DELETE FROM rtu_config WHERE rtu_id = ?";
        return update(sql, rtuId) > 0;
    }
}
