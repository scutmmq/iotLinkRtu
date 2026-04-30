package com.scutmmq.web.controller.rtu;

import com.scutmmq.BadRequestException;
import com.scutmmq.NotFoundException;
import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.exception.ErrorCode;
import com.scutmmq.web.model.RtuConfig;
import com.scutmmq.web.model.RtuGateway;
import com.scutmmq.web.service.RtuConfigService;
import com.scutmmq.web.service.RtuGatewayService;

import java.util.Map;

/**
 * RTU 统一控制器（处理 /api/rtu/{rtuId} 路径的所有 HTTP 方法）
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class RtuController extends BaseController {

    private final RtuGatewayService rtuService = new RtuGatewayService();
    private final RtuConfigService configService = new RtuConfigService();
    
    /**
     * GET - 查询 RTU 详情
     */
    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取路径参数
        String rtuId = req.pathParam("rtuId");
        requireNotBlank(rtuId, "rtuId");
        
        RtuGateway gateway = rtuService.findByRtuId(rtuId);
        if (gateway == null) {
            throw new NotFoundException(ErrorCode.RTU_NOT_FOUND, "RTU 不存在：" + rtuId);
        }
        RtuConfig config = configService.getByRtuId(rtuId);

        Map<String, Object> data = Map.ofEntries(
            Map.entry("id", gateway.getId()),
            Map.entry("rtuId", gateway.getRtuId()),
            Map.entry("name", gateway.getName()),
            Map.entry("location", gateway.getLocation() != null ? gateway.getLocation() : ""),
            Map.entry("status", gateway.getOnline()),
            Map.entry("gatewayStatus", gateway.getStatus()),
            Map.entry("serialPort", gateway.getSerialPort() != null ? gateway.getSerialPort() : ""),
            Map.entry("baudRate", config.getBaudRate()),
            Map.entry("deviceAddress", config.getModbusDeviceAddress()),
            Map.entry("samplingInterval", config.getInterval()),
            Map.entry("lastOnlineTime", gateway.getHeartbeatTime() != null ? gateway.getHeartbeatTime().toString() : ""),
            Map.entry("createTime", gateway.getCreateTime() != null ? gateway.getCreateTime().toString() : ""),
            Map.entry("updateTime", gateway.getUpdateTime() != null ? gateway.getUpdateTime().toString() : "")
        );
        resp.json(buildSuccessResponse(data));
    }
    
    /**
     * PUT - 更新 RTU 信息
     */
    @Override
    protected void put(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取路径参数和请求体
        String rtuId = req.pathParam("rtuId");
        requireNotBlank(rtuId, "rtuId");
        
        Map<String, Object> body = req.bodyJson();
        String name = req.bodyString("name");
        String location = req.bodyString("location");
        Integer samplingInterval = toInteger(body.get("samplingInterval"));
        String gatewayStatus = req.bodyString("gatewayStatus");

        RtuGateway existing = rtuService.findByRtuId(rtuId);
        if (existing == null) {
            throw new NotFoundException(ErrorCode.RTU_NOT_FOUND, "RTU 不存在：" + rtuId);
        }

        RtuGateway update = new RtuGateway();
        update.setRtuId(rtuId);
        update.setName(name);
        update.setLocation(location);
        update.setStatus(gatewayStatus);
        update.setSerialPort(existing.getSerialPort());
        rtuService.update(update);

        if (samplingInterval != null) {
            configService.updateInterval(rtuId, samplingInterval);
        }

        RtuGateway gateway = rtuService.findByRtuId(rtuId);
        RtuConfig config = configService.getByRtuId(rtuId);
        Map<String, Object> data = Map.ofEntries(
            Map.entry("id", gateway.getId()),
            Map.entry("rtuId", rtuId),
            Map.entry("name", gateway.getName()),
            Map.entry("location", gateway.getLocation() != null ? gateway.getLocation() : ""),
            Map.entry("samplingInterval", config.getInterval()),
            Map.entry("updateTime", gateway.getUpdateTime() != null ? gateway.getUpdateTime().toString() : "")
        );
        resp.json(buildSuccessResponse(data));
    }
    
    /**
     * DELETE - 删除 RTU
     */
    @Override
    protected void delete(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取路径参数
        String rtuId = req.pathParam("rtuId");
        requireNotBlank(rtuId, "rtuId");
        
        RtuGateway gateway = rtuService.findByRtuId(rtuId);
        if (gateway == null) {
            throw new NotFoundException(ErrorCode.RTU_NOT_FOUND, "RTU 不存在：" + rtuId);
        }

        if ("ONLINE".equalsIgnoreCase(gateway.getOnline())) {
            throw new BadRequestException(ErrorCode.RTU_ONLINE, "RTU 在线，不允许删除");
        }

        try {
            rtuService.delete(rtuId);
        } catch (IllegalStateException e) {
            throw new BadRequestException(ErrorCode.RTU_ONLINE, e.getMessage());
        }

        Map<String, Object> data = Map.of(
            "deleted", true,
            "rtuId", rtuId
        );
        resp.json(buildSuccessResponse(data));
    }
}
