package com.scutmmq.web.controller.data;

import com.scutmmq.BadRequestException;
import com.scutmmq.NotFoundException;
import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.exception.ErrorCode;

import java.util.List;
import java.util.Map;

/**
 * 历史数据查询控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class DataHistoryController extends BaseController {
    
    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取查询参数
        String rtuId = req.queryParam("rtuId");
        String startTime = req.queryParam("startTime");
        String endTime = req.queryParam("endTime");
        String pageStr = req.queryParam("page");
        String sizeStr = req.queryParam("size");
        
        int page = parseIntParam(pageStr, 1);
        int size = parseIntParam(sizeStr, 10);
        
        // 2. 必填参数校验
        requireNotBlank(rtuId, "rtuId");
        requireNotBlank(startTime, "startTime");
        requireNotBlank(endTime, "endTime");
        
        // 3. 业务校验 - 检查 RTU 是否存在
        if (!"RTU001".equals(rtuId)) {
            throw new NotFoundException(ErrorCode.RTU_NOT_FOUND);
        }
        
        // 4. 校验时间范围
        try {
            long start = System.currentTimeMillis(); // TODO: 实际应解析时间字符串
            long end = System.currentTimeMillis();
            
            if (start > end) {
                throw new BadRequestException(2007, "开始时间不能大于结束时间");
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ErrorCode.TIMESTAMP_INVALID, "时间格式错误：" + e.getMessage());
        }
        
        // 5. 查询历史数据（模拟）
        System.out.println("查询历史数据：" + rtuId + ", startTime=" + startTime + ", endTime=" + endTime);
        
        Map<String, Object> data1 = Map.of(
            "id", 1,
            "rtuId", rtuId,
            "temperature", 25.5,
            "humidity", 50.2,
            "timestamp", "2026-03-13 10:30:00"
        );
        
        Map<String, Object> data2 = Map.of(
            "id", 2,
            "rtuId", rtuId,
            "temperature", 26.0,
            "humidity", 51.0,
            "timestamp", "2026-03-13 10:31:00"
        );
        
        List<Map<String, Object>> list = List.of(data1, data2);
        
        Map<String, Object> response = Map.of(
            "total", list.size(),
            "list", list
        );
        resp.json(buildSuccessResponse(response));
    }
}
