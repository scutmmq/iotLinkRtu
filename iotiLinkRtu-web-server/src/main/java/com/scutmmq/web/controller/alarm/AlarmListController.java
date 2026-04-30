package com.scutmmq.web.controller.alarm;

import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.web.model.RtuAlarm;
import com.scutmmq.web.service.RtuAlarmService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 报警列表查询控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class AlarmListController extends BaseController {

    private final RtuAlarmService alarmService = new RtuAlarmService();
    
    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取查询参数
        String rtuId = req.queryParam("rtuId");
        String status = req.queryParam("status");
        String pageStr = req.queryParam("page");
        String sizeStr = req.queryParam("size");
        
        int page = parseIntParam(pageStr, 1);
        int size = parseIntParam(sizeStr, 10);

        String normalizedStatus = status == null ? null : status.trim().toUpperCase();
        List<RtuAlarm> alarms = alarmService.findPage(rtuId, normalizedStatus, page, size);
        long total = alarmService.count(rtuId, normalizedStatus);
        List<Map<String, Object>> list = new ArrayList<>();

        for (RtuAlarm alarm : alarms) {
            list.add(Map.ofEntries(
                Map.entry("id", alarm.getId()),
                Map.entry("rtuId", alarm.getRtuId()),
                Map.entry("alarmType", alarm.getAlarmType()),
                Map.entry("alarmLevel", alarm.getAlarmLevel()),
                Map.entry("currentValue", alarm.getCurrentValue()),
                Map.entry("thresholdValue", alarm.getThresholdValue()),
                Map.entry("alarmMessage", alarm.getAlarmMessage() != null ? alarm.getAlarmMessage() : ""),
                Map.entry("alarmTime", alarm.getAlarmTime() != null ? alarm.getAlarmTime().toString() : ""),
                Map.entry("status", alarm.getStatus()),
                Map.entry("handleResult", alarm.getHandleResult() != null ? alarm.getHandleResult() : ""),
                Map.entry("handler", alarm.getHandler() != null ? alarm.getHandler() : "")
            ));
        }

        Map<String, Object> data = Map.of(
            "total", total,
            "list", list
        );
        resp.json(buildSuccessResponse(data));
    }
}
