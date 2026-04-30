package com.scutmmq.web.controller.rtu;

import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.web.model.RtuConfig;
import com.scutmmq.web.model.RtuGateway;
import com.scutmmq.web.service.RtuGatewayService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RTU 列表查询控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class RtuListController extends BaseController {

    private final RtuGatewayService rtuService = new RtuGatewayService();
    
    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取查询参数
        String pageStr = req.queryParam("page");
        String sizeStr = req.queryParam("size");
        String status = req.queryParam("status");
        String online = req.queryParam("online");
        String rtuIdKeyword = req.queryParam("rtuId");
        
        int page = parseIntParam(pageStr, 1);
        int size = parseIntParam(sizeStr, 10);

        String gatewayStatus = null;
        String onlineStatus = online;
        if (status != null) {
            String normalized = status.trim().toUpperCase();
            if ("ONLINE".equals(normalized) || "OFFLINE".equals(normalized)) {
                onlineStatus = normalized;
            } else if ("ENABLED".equals(normalized) || "DISABLED".equals(normalized)) {
                gatewayStatus = normalized;
            }
        }

        List<RtuGateway> gateways = rtuService.findPage(page, size, gatewayStatus, onlineStatus, rtuIdKeyword);
        long total = rtuService.count(gatewayStatus, onlineStatus, rtuIdKeyword);
        List<Map<String, Object>> list = new ArrayList<>();

        for (RtuGateway gateway : gateways) {
            RtuConfig config = rtuService.getConfig(gateway.getRtuId());
            list.add(Map.ofEntries(
                Map.entry("id", gateway.getId()),
                Map.entry("rtuId", gateway.getRtuId()),
                Map.entry("name", gateway.getName()),
                Map.entry("status", gateway.getOnline()),
                Map.entry("gatewayStatus", gateway.getStatus()),
                Map.entry("lastOnlineTime", gateway.getHeartbeatTime() != null ? gateway.getHeartbeatTime().toString() : ""),
                Map.entry("location", gateway.getLocation() != null ? gateway.getLocation() : ""),
                Map.entry("serialPort", gateway.getSerialPort() != null ? gateway.getSerialPort() : ""),
                Map.entry("baudRate", config.getBaudRate()),
                Map.entry("deviceAddress", config.getModbusDeviceAddress()),
                Map.entry("samplingInterval", config.getInterval())
            ));
        }

        Map<String, Object> data = Map.of(
            "total", total,
            "list", list
        );
        resp.json(buildSuccessResponse(data));
    }
}
