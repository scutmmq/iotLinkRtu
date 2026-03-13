package com.scutmmq.web.controller.data;

import com.scutmmq.BadRequestException;
import com.scutmmq.NotFoundException;
import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.exception.ErrorCode;

import java.util.Map;

/**
 * 实时数据查询控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class DataRealtimeController extends BaseController {
    
    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取查询参数
        String rtuId = req.queryParam("rtuId");
        requireNotBlank(rtuId, "rtuId");
        
        // 2. 业务校验 - 检查 RTU 是否存在（模拟）
        if (!"RTU001".equals(rtuId)) {
            throw new NotFoundException(ErrorCode.RTU_NOT_FOUND);
        }
        
        // 3. 查询实时数据（模拟）
        System.out.println("查询实时数据：" + rtuId);
        
        Map<String, Object> data = Map.of(
            "rtuId", rtuId,
            "temperature", 25.5,
            "humidity", 50.2,
            "timestamp", "2026-03-13 10:30:00",
            "status", "normal"
        );
        resp.json(buildSuccessResponse(data));
    }
}
