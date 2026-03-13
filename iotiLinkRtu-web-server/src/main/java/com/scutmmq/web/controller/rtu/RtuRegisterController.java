package com.scutmmq.web.controller.rtu;

import com.scutmmq.BadRequestException;
import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.exception.ErrorCode;

import java.util.Map;

/**
 * RTU 注册控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class RtuRegisterController extends BaseController {
    
    @Override
    protected void post(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取请求参数
        Map<String, Object> body = req.bodyJson();
        String rtuId = req.bodyString("rtuId");
        String name = req.bodyString("name");
        String location = req.bodyString("location");
        String serialPort = req.bodyString("serialPort");
        Integer baudRate = toInteger(body.get("baudRate"));
        Integer deviceAddress = toInteger(body.get("deviceAddress"));
        
        // 2. 必填参数校验
        requireNotBlank(rtuId, "rtuId");
        requireNotBlank(name, "name");
        
        // 3. 业务校验 - 检查是否已存在（模拟）
        // TODO: 实际项目中应调用 Service 层检查
        // if (rtuService.exists(rtuId)) {
        //     throw new BadRequestException(ErrorCode.RTU_ALREADY_EXISTS);
        // }
        
        // 4. 创建 RTU 对象（模拟保存）
        // TODO: 实际项目中应调用 Service 层保存
        System.out.println("注册 RTU: " + rtuId + ", name=" + name + ", location=" + location);
        
        // 5. 返回响应
        Map<String, Object> data = Map.of(
            "id", System.currentTimeMillis(),  // 模拟 ID
            "rtuId", rtuId,
            "status", "offline",
            "createTime", java.time.LocalDateTime.now().toString()
        );
        resp.json(buildSuccessResponse(data));
    }
}
