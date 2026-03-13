package com.scutmmq.web.controller.alarm;

import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;

import java.util.List;
import java.util.Map;

/**
 * 报警列表查询控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class AlarmListController extends BaseController {
    
    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取查询参数
        String rtuId = req.queryParam("rtuId");
        String status = req.queryParam("status");
        String pageStr = req.queryParam("page");
        String sizeStr = req.queryParam("size");
        
        int page = parseIntParam(pageStr, 1);
        int size = parseIntParam(sizeStr, 10);
        
        // 2. 查询报警数据（模拟）
        System.out.println("查询报警列表：" + rtuId + ", status=" + status + ", page=" + page + ", size=" + size);
        
        Map<String, Object> alarm1 = Map.of(
            "id", 1,
            "rtuId", "RTU001",
            "alarmType", "temperature_high",
            "alarmLevel", "warning",
            "currentValue", 32.0,
            "thresholdValue", 28.0,
            "alarmTime", "2026-03-13 10:30:00",
            "status", "unhandled"
        );
        
        Map<String, Object> alarm2 = Map.of(
            "id", 2,
            "rtuId", "RTU001",
            "alarmType", "humidity_low",
            "alarmLevel", "info",
            "currentValue", 35.0,
            "thresholdValue", 40.0,
            "alarmTime", "2026-03-13 09:15:00",
            "status", "handled"
        );
        
        List<Map<String, Object>> list = List.of(alarm1, alarm2);
        
        Map<String, Object> data = Map.of(
            "total", list.size(),
            "list", list
        );
        resp.json(buildSuccessResponse(data));
    }
}
