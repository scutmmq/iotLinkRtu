package com.scutmmq.web.service;

import com.scutmmq.NotFoundException;
import com.scutmmq.exception.ErrorCode;
import com.scutmmq.web.dao.RtuConfigDao;
import com.scutmmq.web.model.RtuConfig;
import com.scutmmq.web.model.RtuGateway;

/**
 * RTU 配置服务
 */
public class RtuConfigService {

    private final RtuConfigDao configDao = new RtuConfigDao();

    public RtuConfig createDefaultConfig(RtuGateway gateway, Integer samplingInterval) {
        RtuConfig config = new RtuConfig();
        config.setRtuId(gateway.getRtuId());
        config.setInterval(samplingInterval != null ? samplingInterval : 1);
        config.setModbusDeviceAddress(gateway.getDeviceAddress() != null ? gateway.getDeviceAddress() : 1);
        config.setBaudRate(gateway.getBaudRate() != null ? gateway.getBaudRate() : 9600);
        config.setTempCalibration(0.0);
        config.setHumidityCalibration(0.0);
        config.setTempThresholdMin(18.0);
        config.setTempThresholdMax(28.0);
        config.setHumidityThresholdMin(40.0);
        config.setHumidityThresholdMax(60.0);
        config.setAlarmEnabled(Boolean.TRUE);
        configDao.save(config);
        return configDao.findByRtuId(gateway.getRtuId());
    }

    public RtuConfig getByRtuId(String rtuId) {
        RtuConfig config = configDao.findByRtuId(rtuId);
        if (config == null) {
            throw new NotFoundException(ErrorCode.CONFIG_NOT_FOUND, "RTU 配置不存在：" + rtuId);
        }
        return config;
    }

    public RtuConfig update(String rtuId, RtuConfig patch) {
        RtuConfig existing = getByRtuId(rtuId);
        merge(existing, patch);
        configDao.update(existing);
        return getByRtuId(rtuId);
    }

    public RtuConfig updateInterval(String rtuId, Integer interval) {
        RtuConfig patch = new RtuConfig();
        patch.setInterval(interval);
        return update(rtuId, patch);
    }

    public void deleteByRtuId(String rtuId) {
        configDao.deleteByRtuId(rtuId);
    }

    private void merge(RtuConfig target, RtuConfig patch) {
        if (patch.getInterval() != null) {
            target.setInterval(patch.getInterval());
        }
        if (patch.getModbusDeviceAddress() != null) {
            target.setModbusDeviceAddress(patch.getModbusDeviceAddress());
        }
        if (patch.getBaudRate() != null) {
            target.setBaudRate(patch.getBaudRate());
        }
        if (patch.getTempCalibration() != null) {
            target.setTempCalibration(patch.getTempCalibration());
        }
        if (patch.getHumidityCalibration() != null) {
            target.setHumidityCalibration(patch.getHumidityCalibration());
        }
        if (patch.getTempThresholdMin() != null) {
            target.setTempThresholdMin(patch.getTempThresholdMin());
        }
        if (patch.getTempThresholdMax() != null) {
            target.setTempThresholdMax(patch.getTempThresholdMax());
        }
        if (patch.getHumidityThresholdMin() != null) {
            target.setHumidityThresholdMin(patch.getHumidityThresholdMin());
        }
        if (patch.getHumidityThresholdMax() != null) {
            target.setHumidityThresholdMax(patch.getHumidityThresholdMax());
        }
        if (patch.getAlarmEnabled() != null) {
            target.setAlarmEnabled(patch.getAlarmEnabled());
        }
    }
}
