package com.scutmmq.web.controller.data;

import com.scutmmq.BadRequestException;
import com.scutmmq.NotFoundException;
import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.exception.ErrorCode;

import java.util.Map;

/**
 * 数据统计控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class DataStatisticsController extends BaseController {
    
    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取查询参数
        String rtuId = req.queryParam("rtuId");
        String startTime = req.queryParam("startTime");
        String endTime = req.queryParam("endTime");
        
        // 2. 必填参数校验
        requireNotBlank(rtuId, "rtuId");
        requireNotBlank(startTime, "startTime");
        requireNotBlank(endTime, "endTime");
        
        // 3. 业务校验 - 检查 RTU 是否存在
        if (!"RTU001".equals(rtuId)) {
            throw new NotFoundException(ErrorCode.RTU_NOT_FOUND);
        }
        
        // 4. 统计数据（模拟）
        System.out.println("数据统计：" + rtuId + ", startTime=" + startTime + ", endTime=" + endTime);
        
        Map<String, Object> temperature = Map.of(
            "avg", 25.5,
            "max", 30.0,
            "min", 20.0
        );
        
        Map<String, Object> humidity = Map.of(
            "avg", 50.5,
            "max", 60.0,
            "min", 40.0
        );
        
        Map<String, Object> data = Map.of(
            "rtuId", rtuId,
            "temperature", temperature,
            "humidity", humidity,
            "dataCount", 86400
        );
        resp.json(buildSuccessResponse(data));
    }
}
