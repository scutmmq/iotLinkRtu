package com.scutmmq.web.controller.rtu;

import com.scutmmq.NotFoundException;
import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import com.scutmmq.exception.ErrorCode;
import com.scutmmq.web.service.RtuGatewayService;

import java.util.Map;

/**
 * Gateway 回调更新 RTU 在线状态
 */
public class RtuGatewayStatusController extends BaseController {

    private final RtuGatewayService rtuService = new RtuGatewayService();

    @Override
    protected void post(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        String rtuId = req.bodyString("rtuId");
        String online = req.bodyString("online");

        requireNotBlank(rtuId, "rtuId");
        requireNotBlank(online, "online");

        if (rtuService.findByRtuId(rtuId) == null) {
            throw new NotFoundException(ErrorCode.RTU_NOT_FOUND, "RTU 不存在：" + rtuId);
        }

        boolean success = rtuService.updateOnlineStatus(rtuId, online.trim().toUpperCase());
        resp.json(buildSuccessResponse(Map.of(
            "updated", success,
            "rtuId", rtuId,
            "online", online.trim().toUpperCase()
        )));
    }
}
