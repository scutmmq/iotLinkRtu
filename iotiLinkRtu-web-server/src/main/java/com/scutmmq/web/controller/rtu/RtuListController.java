package com.scutmmq.web.controller.rtu;

import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;

import java.util.List;
import java.util.Map;

/**
 * RTU 列表查询控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class RtuListController extends BaseController {
    
    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取查询参数
        String pageStr = req.queryParam("page");
        String sizeStr = req.queryParam("size");
        String status = req.queryParam("status");
        
        int page = parseIntParam(pageStr, 1);
        int size = parseIntParam(sizeStr, 10);
        
        // 2. 查询数据（模拟）
        // TODO: 实际项目中应调用 Service 层查询
        System.out.println("查询 RTU 列表：page=" + page + ", size=" + size + ", status=" + status);
        
        // 3. 模拟数据
        Map<String, Object> rtu1 = Map.of(
            "id", 1,
            "rtuId", "RTU001",
            "name", "1 号温湿度采集器",
            "status", "online",
            "lastOnlineTime", "2026-03-13 10:30:00",
            "location", "机房 A 区"
        );
        
        Map<String, Object> rtu2 = Map.of(
            "id", 2,
            "rtuId", "RTU002",
            "name", "2 号温湿度采集器",
            "status", "offline",
            "lastOnlineTime", "2026-03-13 09:00:00",
            "location", "机房 B 区"
        );
        
        List<Map<String, Object>> list = List.of(rtu1, rtu2);
        
        // 4. 返回响应
        Map<String, Object> data = Map.of(
            "total", list.size(),
            "list", list
        );
        resp.json(buildSuccessResponse(data));
    }
}
