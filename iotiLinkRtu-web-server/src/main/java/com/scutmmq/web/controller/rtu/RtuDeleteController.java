package com.scutmmq.web.controller.rtu;

import com.scutmmq.BadRequestException;
import com.scutmmq.NotFoundException;
import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.exception.ErrorCode;

import java.util.Map;

/**
 * RTU 删除控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class RtuDeleteController extends BaseController {
    
    @Override
    protected void delete(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取路径参数
        String rtuId = req.pathParam("rtuId");
        requireNotBlank(rtuId, "rtuId");
        
        // 2. 业务校验 - 检查 RTU 是否存在（模拟）
        // TODO: 实际项目中应调用 Service 层查询
        System.out.println("删除 RTU: " + rtuId);
        
        // 3. 模拟数据 - 假设只有 RTU001 存在且可以删除
        if (!"RTU001".equals(rtuId)) {
            throw new NotFoundException(ErrorCode.RTU_NOT_FOUND, "RTU 不存在：" + rtuId);
        }
        
        // 4. 检查 RTU 状态（模拟）
        // TODO: 实际项目中应检查 RTU 是否在线
        boolean isOnline = false; // 模拟离线状态
        if (isOnline) {
            throw new BadRequestException(ErrorCode.RTU_OFFLINE, "RTU 在线，不允许删除");
        }
        
        // 5. 执行删除操作（模拟）
        System.out.println("RTU 删除成功：" + rtuId);
        
        // 6. 返回响应
        Map<String, Object> data = Map.of(
            "deleted", true,
            "rtuId", rtuId
        );
        resp.json(buildSuccessResponse(data));
    }
}
