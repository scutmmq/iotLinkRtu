package com.scutmmq.web.controller.rtu;

import com.scutmmq.BadRequestException;
import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.exception.ErrorCode;
import com.scutmmq.web.service.RtuAuthService;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Map;

/**
 * RTU 认证验证控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class RtuVerifyController extends BaseController {
    
    private final RtuAuthService authService = new RtuAuthService();
    
    @Override
    protected void post(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取请求参数
        String rtuId = req.bodyString("rtuId");
        String secretHash = req.bodyString("secretHash");
        Long timestamp = toLong(req.bodyJson().get("timestamp"));
        
        // 2. 必填参数校验
        requireNotBlank(rtuId, "rtuId");
        requireNotBlank(secretHash, "secretHash");
        requireNotNull(timestamp, "timestamp");
        
        // 3. 调用 Service 层验证
        RtuAuthService.VerifyResult result = authService.verify(rtuId, secretHash, timestamp);
        
        // 4. 返回响应
        if (result.isValid()) {
            // 认证成功
            Map<String, Object> data = Map.of(
                "valid", true,
                "status", result.getStatus(),
                "rtuId", result.getRtuId()
            );
            resp.json(buildSuccessResponse(data));
        } else {
            // 认证失败
            Map<String, Object> errorResp = Map.of(
                "code", 401,
                "message", result.getMessage(),
                "data", Map.of("valid", false)
            );
            resp.json(HttpResponseStatus.UNAUTHORIZED, errorResp);
        }
    }
    
    /**
     * 将对象转换为 Long 类型
     */
    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }
}
