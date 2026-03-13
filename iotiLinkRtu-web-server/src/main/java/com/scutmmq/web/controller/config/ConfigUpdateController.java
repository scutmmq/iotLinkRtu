package com.scutmmq.web.controller.config;

import com.scutmmq.BadRequestException;
import com.scutmmq.NotFoundException;
import com.scutmmq.ServerException;
import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.exception.ErrorCode;

import java.util.Map;

/**
 * 配置更新控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class ConfigUpdateController extends BaseController {
    
    @Override
    protected void put(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取路径参数和请求体
        String rtuId = req.pathParam("rtuId");
        requireNotBlank(rtuId, "rtuId");
        
        Map<String, Object> body = req.bodyJson();
        Integer samplingInterval = toInteger(body.get("samplingInterval"));
        
        // 2. 业务校验 - 检查 RTU 是否存在
        if (!"RTU001".equals(rtuId)) {
            throw new NotFoundException(ErrorCode.RTU_NOT_FOUND);
        }
        
        // 3. 校验采样间隔
        if (samplingInterval == null || samplingInterval < 1 || samplingInterval > 60) {
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
        
        // 6. 更新配置并下发（模拟）
        System.out.println("更新 RTU 配置：" + rtuId + ", samplingInterval=" + samplingInterval);
        
        // TODO: 实际项目中应调用 Service 层更新并下发到 MQTT
        // boolean success = configService.updateAndApply(rtuId, body);
        // if (!success) {
        //     throw new ServerException(ErrorCode.CONFIG_APPLY_FAILED);
        // }
        
        // 7. 返回响应
        resp.json(buildSuccessResponse());
    }
    
    /**
     * 将对象安全转换为 Float
     */
    private Float toFloat(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.floatValue();
        try { return Float.parseFloat(val.toString()); } catch (Exception e) { return null; }
    }
}
