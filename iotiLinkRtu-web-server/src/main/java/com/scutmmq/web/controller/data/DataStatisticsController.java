package com.scutmmq.web.controller.data;

import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.web.model.RtuDataStatistics;
import com.scutmmq.web.service.RtuDataService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数据统计控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class DataStatisticsController extends BaseController {

    private final RtuDataService dataService = new RtuDataService();
    
    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        String rtuId = req.queryParam("rtuId");
        String startTime = req.queryParam("startTime");
        String endTime = req.queryParam("endTime");
        
        requireNotBlank(rtuId, "rtuId");
        requireNotBlank(startTime, "startTime");
        requireNotBlank(endTime, "endTime");

        RtuDataStatistics statistics = dataService.statistics(rtuId, startTime, endTime);
        Map<String, Object> temperature = new LinkedHashMap<>();
        temperature.put("avg", statistics.getAvgTemperature());
        temperature.put("max", statistics.getMaxTemperature());
        temperature.put("min", statistics.getMinTemperature());
        
        Map<String, Object> humidity = new LinkedHashMap<>();
        humidity.put("avg", statistics.getAvgHumidity());
        humidity.put("max", statistics.getMaxHumidity());
        humidity.put("min", statistics.getMinHumidity());
        
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rtuId", rtuId);
        data.put("temperature", temperature);
        data.put("humidity", humidity);
        data.put("dataCount", statistics.getDataCount());
        resp.json(buildSuccessResponse(data));
    }
}
