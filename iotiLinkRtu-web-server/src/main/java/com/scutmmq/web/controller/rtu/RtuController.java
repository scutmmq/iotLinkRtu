package com.scutmmq.web.controller.rtu;

import com.scutmmq.BadRequestException;
import com.scutmmq.NotFoundException;
import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.exception.ErrorCode;

import java.util.Map;

/**
 * RTU 统一控制器（处理 /api/rtu/{rtuId} 路径的所有 HTTP 方法）
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class RtuController extends BaseController {
    
    /**
     * GET - 查询 RTU 详情
     */
    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取路径参数
        String rtuId = req.pathParam("rtuId");
        requireNotBlank(rtuId, "rtuId");
        
        // 2. 查询 RTU（模拟）
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
    
    /**
     * PUT - 更新 RTU 信息
     */
    @Override
    protected void put(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取路径参数和请求体
        String rtuId = req.pathParam("rtuId");
        requireNotBlank(rtuId, "rtuId");
        
        Map<String, Object> body = req.bodyJson();
        String name = req.bodyString("name");
        String location = req.bodyString("location");
        Integer samplingInterval = toInteger(body.get("samplingInterval"));
        
        // 2. 业务校验 - 检查 RTU 是否存在
        if (!"RTU001".equals(rtuId)) {
            throw new NotFoundException(ErrorCode.RTU_NOT_FOUND);
        }
        
        // 3. 更新 RTU（模拟）
        System.out.println("更新 RTU: " + rtuId + ", name=" + name + ", location=" + location);
        
        // 4. 返回响应
        Map<String, Object> data = Map.of(
            "id", 1,
            "rtuId", rtuId,
            "name", name != null ? name : "原名称",
            "location", location != null ? location : "原位置",
            "samplingInterval", samplingInterval != null ? samplingInterval : 1,
            "updateTime", java.time.LocalDateTime.now().toString()
        );
        resp.json(buildSuccessResponse(data));
    }
    
    /**
     * DELETE - 删除 RTU
     */
    @Override
    protected void delete(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取路径参数
        String rtuId = req.pathParam("rtuId");
        requireNotBlank(rtuId, "rtuId");
        
        // 2. 业务校验 - 检查 RTU 是否存在
        System.out.println("删除 RTU: " + rtuId);
        
        if (!"RTU001".equals(rtuId)) {
            throw new NotFoundException(ErrorCode.RTU_NOT_FOUND, "RTU 不存在：" + rtuId);
        }
        
        // 3. 检查 RTU 状态（模拟）
        boolean isOnline = false; // 模拟离线状态
        if (isOnline) {
            throw new BadRequestException(ErrorCode.RTU_OFFLINE, "RTU 在线，不允许删除");
        }
        
        // 4. 执行删除操作（模拟）
        System.out.println("RTU 删除成功：" + rtuId);
        
        // 5. 返回响应
        Map<String, Object> data = Map.of(
            "deleted", true,
            "rtuId", rtuId
        );
        resp.json(buildSuccessResponse(data));
    }
}
