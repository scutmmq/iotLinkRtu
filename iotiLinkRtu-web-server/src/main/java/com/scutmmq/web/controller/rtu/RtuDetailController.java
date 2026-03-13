package com.scutmmq.web.controller.rtu;

import com.scutmmq.NotFoundException;
import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.exception.ErrorCode;

import java.util.Map;

/**
 * RTU 详情查询控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class RtuDetailController extends BaseController {
    
    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取路径参数
        String rtuId = req.pathParam("rtuId");
        requireNotBlank(rtuId, "rtuId");
        
        // 2. 查询 RTU（模拟）
        // TODO: 实际项目中应调用 Service 层查询
        System.out.println("查询 RTU 详情：" + rtuId);
        
        // 3. 模拟数据 - 假设 RTU001 存在
        if (!"RTU001".equals(rtuId)) {
            throw new NotFoundException(ErrorCode.RTU_NOT_FOUND, "RTU 不存在：" + rtuId);
        }
        
        Map<String, Object> data = Map.ofEntries(
            Map.entry("id", 1),
            Map.entry("rtuId", rtuId),
            Map.entry("name", "1 号温湿度采集器"),
            Map.entry("location", "机房 A 区"),
            Map.entry("status", "online"),
            Map.entry("serialPort", "COM3"),
            Map.entry("baudRate", 9600),
            Map.entry("deviceAddress", 1),
            Map.entry("samplingInterval", 1),
            Map.entry("lastOnlineTime", "2026-03-13 10:30:00"),
            Map.entry("createTime", "2026-03-13 10:00:00")
        );
        resp.json(buildSuccessResponse(data));
    }
}
