package com.scutmmq.web.controller.data;

import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.web.model.RtuData;
import com.scutmmq.web.service.RtuDataService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 历史数据查询控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class DataHistoryController extends BaseController {

    private final RtuDataService dataService = new RtuDataService();
    
    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        String rtuId = req.queryParam("rtuId");
        String startTime = req.queryParam("startTime");
        String endTime = req.queryParam("endTime");
        String pageStr = req.queryParam("page");
        String sizeStr = req.queryParam("size");
        
        int page = parseIntParam(pageStr, 1);
        int size = parseIntParam(sizeStr, 10);
        
        requireNotBlank(rtuId, "rtuId");
        requireNotBlank(startTime, "startTime");
        requireNotBlank(endTime, "endTime");

        List<RtuData> records = dataService.findHistory(rtuId, startTime, endTime, page, size);
        long total = dataService.countHistory(rtuId, startTime, endTime);
        List<Map<String, Object>> list = new ArrayList<>();
        for (RtuData record : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", record.getId());
            item.put("rtuId", record.getRtuId());
            item.put("temperature", record.getTemperature());
            item.put("humidity", record.getHumidity());
            item.put("timestamp", record.getCollectTime() != null ? record.getCollectTime().toString().replace('T', ' ') : "");
            item.put("status", record.getStatus() != null ? record.getStatus() : "normal");
            list.add(item);
        }
        
        Map<String, Object> response = Map.of(
            "total", total,
            "list", list
        );
        resp.json(buildSuccessResponse(response));
    }
}
