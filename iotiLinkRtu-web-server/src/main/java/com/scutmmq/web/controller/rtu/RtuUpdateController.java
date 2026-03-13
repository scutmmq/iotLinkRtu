package com.scutmmq.web.controller.rtu;

import com.scutmmq.BadRequestException;
import com.scutmmq.NotFoundException;
import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.exception.ErrorCode;

import java.util.Map;

/**
 * RTU 更新控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class RtuUpdateController extends BaseController {
    
    @Override
    protected void put(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取路径参数和请求体
        String rtuId = req.pathParam("rtuId");
        requireNotBlank(rtuId, "rtuId");
        
        Map<String, Object> body = req.bodyJson();
        String name = req.bodyString("name");
        String location = req.bodyString("location");
        
        // 2. 业务校验 - 检查是否存在（模拟）
        // TODO: 实际项目中应调用 Service 层检查
        if (!"RTU001".equals(rtuId)) {
            throw new NotFoundException(ErrorCode.RTU_NOT_FOUND);
        }
        
        // 3. 更新数据（模拟）
        System.out.println("更新 RTU: " + rtuId + ", name=" + name + ", location=" + location);
        
        // 4. 返回响应
        resp.json(buildSuccessResponse());
    }
}
