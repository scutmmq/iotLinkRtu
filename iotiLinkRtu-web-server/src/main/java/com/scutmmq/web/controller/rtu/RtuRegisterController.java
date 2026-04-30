package com.scutmmq.web.controller.rtu;

import com.scutmmq.BadRequestException;
import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.exception.ErrorCode;
import com.scutmmq.web.model.RtuGateway;
import com.scutmmq.web.service.RtuGatewayService;

import java.util.Map;

/**
 * RTU 注册控制器
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class RtuRegisterController extends BaseController {
    
    private final RtuGatewayService rtuService = new RtuGatewayService();
    
    @Override
    protected void post(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 获取请求参数
        Map<String, Object> body = req.bodyJson();
        String rtuId = req.bodyString("rtuId");
        String name = req.bodyString("name");
        String location = req.bodyString("location");
        String serialPort = req.bodyString("serialPort");
        Integer baudRate = toInteger(body.get("baudRate"));
        Integer deviceAddress = toInteger(body.get("deviceAddress"));
        Integer samplingInterval = toInteger(body.get("samplingInterval"));
        
        // 2. 必填参数校验
        requireNotBlank(rtuId, "rtuId");
        requireNotBlank(name, "name");
        
        // 3. 创建 RTU 对象
        RtuGateway rtu = new RtuGateway();
        rtu.setRtuId(rtuId);
        rtu.setName(name);
        rtu.setLocation(location);
        rtu.setSerialPort(serialPort);
        rtu.setBaudRate(baudRate);
        rtu.setDeviceAddress(deviceAddress);
        
        // 4. 调用 Service 层注册（自动检查是否已存在）
        RtuGateway savedRtu;
        try {
            savedRtu = rtuService.register(rtu, samplingInterval);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ErrorCode.RTU_ALREADY_EXISTS, e.getMessage());
        }
        var config = rtuService.getConfig(savedRtu.getRtuId());
        
        // 5. 返回响应
        Map<String, Object> data = Map.ofEntries(
            Map.entry("id", savedRtu.getId()),
            Map.entry("rtuId", savedRtu.getRtuId()),
            Map.entry("name", savedRtu.getName()),
            Map.entry("location", savedRtu.getLocation() != null ? savedRtu.getLocation() : ""),
            Map.entry("status", savedRtu.getOnline()),
            Map.entry("serialPort", savedRtu.getSerialPort() != null ? savedRtu.getSerialPort() : ""),
            Map.entry("baudRate", config.getBaudRate()),
            Map.entry("deviceAddress", config.getModbusDeviceAddress()),
            Map.entry("samplingInterval", config.getInterval()),
            Map.entry("secret", savedRtu.getSecret()),
            Map.entry("createTime", savedRtu.getCreateTime() != null ? savedRtu.getCreateTime().toString() : ""),
            Map.entry("updateTime", savedRtu.getUpdateTime() != null ? savedRtu.getUpdateTime().toString() : "")
        );
        resp.json(buildSuccessResponse(data));
    }
}
