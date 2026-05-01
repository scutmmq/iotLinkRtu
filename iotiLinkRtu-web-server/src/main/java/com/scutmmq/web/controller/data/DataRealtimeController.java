package com.scutmmq.web.controller.data;

import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.web.model.RtuData;
import com.scutmmq.web.service.RtuDataService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 实时数据查询控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class DataRealtimeController extends BaseController {

    private final RtuDataService dataService = new RtuDataService();
    
    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        String rtuId = req.queryParam("rtuId");
        requireNotBlank(rtuId, "rtuId");

        RtuData latest = dataService.getRealtime(rtuId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rtuId", latest.getRtuId());
        data.put("temperature", latest.getTemperature());
        data.put("humidity", latest.getHumidity());
        data.put("timestamp", latest.getCollectTime() != null ? latest.getCollectTime().toString().replace('T', ' ') : "");
        data.put("status", latest.getStatus() != null ? latest.getStatus() : "normal");
        resp.json(buildSuccessResponse(data));
    }
}
