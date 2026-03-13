package com.scutmmq.web.controller.alarm;

import com.scutmmq.BadRequestException;
import com.scutmmq.NotFoundException;
import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.exception.ErrorCode;

/**
 * 报警处理控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class AlarmHandleController extends BaseController {
    
    @Override
    protected void put(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取路径参数和请求体
        String alarmId = req.pathParam("alarmId");
        requireNotBlank(alarmId, "alarmId");
        
        String handleResult = req.bodyString("handleResult");
        String handler = req.bodyString("handler");
        
        // 2. 业务校验 - 检查报警是否存在（模拟）
        // TODO: 实际项目中应调用 Service 层检查
        if (!"1".equals(alarmId)) {
            throw new NotFoundException(ErrorCode.ALARM_NOT_FOUND);
        }
        
        // 3. 校验必填参数
        requireNotBlank(handleResult, "handleResult");
        requireNotBlank(handler, "handler");
        
        // 4. 处理报警（模拟）
        System.out.println("处理报警：" + alarmId + ", result=" + handleResult + ", handler=" + handler);
        
        // 5. 返回响应
        resp.json(buildSuccessResponse());
    }
}
