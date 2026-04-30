package com.scutmmq.web.controller.alarm;

import com.scutmmq.BadRequestException;
import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.web.service.RtuAlarmService;

/**
 * 报警处理控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class AlarmHandleController extends BaseController {

    private final RtuAlarmService alarmService = new RtuAlarmService();
    
    @Override
    protected void put(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取路径参数和请求体
        String alarmId = req.pathParam("alarmId");
        requireNotBlank(alarmId, "alarmId");
        
        String handleResult = req.bodyString("handleResult");
        String handler = req.bodyString("handler");

        // 2. 校验必填参数
        requireNotBlank(handleResult, "handleResult");
        requireNotBlank(handler, "handler");

        Long id;
        try {
            id = Long.parseLong(alarmId);
        } catch (NumberFormatException e) {
            throw new BadRequestException("alarmId 非法：" + alarmId);
        }

        alarmService.handle(id, handleResult, handler);
        resp.json(buildSuccessResponse(java.util.Map.of(
            "handled", true,
            "alarmId", id
        )));
    }
}
