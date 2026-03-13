package com.scutmmq.web.controller.rtu;

import com.scutmmq.BadRequestException;
import com.scutmmq.NotFoundException;
import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.exception.ErrorCode;

import java.util.Map;

/**
 * RTU 认证验证控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class RtuVerifyController extends BaseController {
    
    @Override
    protected void post(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取请求参数
        String rtuId = req.bodyString("rtuId");
        String secretHash = req.bodyString("secretHash");
        
        // 2. 必填参数校验
        requireNotBlank(rtuId, "rtuId");
        requireNotBlank(secretHash, "secretHash");
        
        // 3. 业务校验 - 验证 RTU 和密钥（模拟）
        // TODO: 实际项目中应调用 Service 层查询数据库
        System.out.println("验证 RTU: " + rtuId);
        
        // 4. 模拟验证逻辑
        // 假设 RTU001 存在且密钥正确
        if (!"RTU001".equals(rtuId)) {
            // RTU 不存在
            throw new NotFoundException(ErrorCode.RTU_NOT_FOUND, "RTU 不存在：" + rtuId);
        }
        
        // 模拟验证 secretHash (实际应该查询数据库对比)
        boolean isValid = "valid_hash".equals(secretHash);
        
        if (!isValid) {
            // 认证失败
            resp.json(Map.of(
                "code", 401,
                "message", "认证失败：RTU 不存在或密钥错误",
                "data", Map.of("valid", false)
            ));
            return;
        }
        
        // 5. 认证成功
        Map<String, Object> data = Map.of(
            "valid", true,
            "status", "ENABLED",
            "rtuId", rtuId
        );
        resp.json(buildSuccessResponse(data));
    }
}
