package com.scutmmq.web.controller.config;

import com.scutmmq.BadRequestException;
import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.exception.ErrorCode;
import com.scutmmq.web.model.RtuConfig;
import com.scutmmq.web.service.RtuConfigService;

import java.util.Map;

/**
 * 配置更新控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class ConfigUpdateController extends BaseController {

    private final RtuConfigService configService = new RtuConfigService();

    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        String rtuId = req.pathParam("rtuId");
        requireNotBlank(rtuId, "rtuId");

        RtuConfig config = configService.getByRtuId(rtuId);
        resp.json(buildSuccessResponse(toResponse(config)));
    }
    
    @Override
    protected void put(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取路径参数和请求体
        String rtuId = req.pathParam("rtuId");
        requireNotBlank(rtuId, "rtuId");
        
        Map<String, Object> body = req.bodyJson();
        Integer samplingInterval = toInteger(body.get("samplingInterval"));
        
        // 2. 校验采样间隔
        if (samplingInterval != null && (samplingInterval < 1 || samplingInterval > 60)) {
            throw new BadRequestException(ErrorCode.SAMPLING_INTERVAL_INVALID, 
                "采样间隔必须在 1-60 秒之间，当前值：" + samplingInterval);
        }
        
        // 4. 校验温度阈值
        Map<String, Object> tempThresh = (Map<String, Object>) body.get("temperatureThreshold");
        if (tempThresh != null) {
            Float tempMin = toFloat(tempThresh.get("min"));
            Float tempMax = toFloat(tempThresh.get("max"));
            if (tempMin != null && tempMax != null && tempMin >= tempMax) {
                throw new BadRequestException(ErrorCode.TEMP_THRESHOLD_MIN_MAX);
            }
        }
        
        // 5. 校验湿度阈值
        Map<String, Object> humiThresh = (Map<String, Object>) body.get("humidityThreshold");
        if (humiThresh != null) {
            Float humiMin = toFloat(humiThresh.get("min"));
            Float humiMax = toFloat(humiThresh.get("max"));
            if (humiMin != null && humiMax != null && humiMin >= humiMax) {
                throw new BadRequestException(ErrorCode.HUMI_THRESHOLD_MIN_MAX);
            }
        }
        
        RtuConfig patch = new RtuConfig();
        patch.setRtuId(rtuId);
        patch.setInterval(samplingInterval);
        patch.setAlarmEnabled(toBoolean(body.get("alarmEnabled")));

        if (tempThresh != null) {
            patch.setTempThresholdMin(toDouble(tempThresh.get("min")));
            patch.setTempThresholdMax(toDouble(tempThresh.get("max")));
        }
        if (humiThresh != null) {
            patch.setHumidityThresholdMin(toDouble(humiThresh.get("min")));
            patch.setHumidityThresholdMax(toDouble(humiThresh.get("max")));
        }
        if (body.containsKey("baudRate")) {
            patch.setBaudRate(toInteger(body.get("baudRate")));
        }
        if (body.containsKey("deviceAddress")) {
            patch.setModbusDeviceAddress(toInteger(body.get("deviceAddress")));
        }
        if (body.containsKey("tempCalibration")) {
            patch.setTempCalibration(toDouble(body.get("tempCalibration")));
        }
        if (body.containsKey("humidityCalibration")) {
            patch.setHumidityCalibration(toDouble(body.get("humidityCalibration")));
        }

        RtuConfig saved = configService.update(rtuId, patch);
        resp.json(buildSuccessResponse(toResponse(saved)));
    }

    @Override
    protected void post(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        put(req, resp);
    }
    
    /**
     * 将对象安全转换为 Float
     */
    private Float toFloat(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.floatValue();
        try { return Float.parseFloat(val.toString()); } catch (Exception e) { return null; }
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return null; }
    }

    private Boolean toBoolean(Object val) {
        if (val == null) return null;
        if (val instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(val.toString());
    }

    private Map<String, Object> toResponse(RtuConfig config) {
        return Map.ofEntries(
            Map.entry("rtuId", config.getRtuId()),
            Map.entry("samplingInterval", config.getInterval()),
            Map.entry("deviceAddress", config.getModbusDeviceAddress()),
            Map.entry("baudRate", config.getBaudRate()),
            Map.entry("tempCalibration", config.getTempCalibration() != null ? config.getTempCalibration() : 0.0),
            Map.entry("humidityCalibration", config.getHumidityCalibration() != null ? config.getHumidityCalibration() : 0.0),
            Map.entry("temperatureThreshold", Map.of(
                "min", config.getTempThresholdMin(),
                "max", config.getTempThresholdMax()
            )),
            Map.entry("humidityThreshold", Map.of(
                "min", config.getHumidityThresholdMin(),
                "max", config.getHumidityThresholdMax()
            )),
            Map.entry("alarmEnabled", config.getAlarmEnabled() != null ? config.getAlarmEnabled() : Boolean.TRUE),
            Map.entry("updateTime", config.getUpdateTime() != null ? config.getUpdateTime().toString() : "")
        );
    }
}
