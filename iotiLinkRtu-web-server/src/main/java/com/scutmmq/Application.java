package com.scutmmq;

import com.scutmmq.restful.RestFulExpress;
import com.scutmmq.restful.HttpServer;
import com.scutmmq.web.controller.config.HttpServerConfig;
import com.scutmmq.web.controller.rtu.RtuRegisterController;
import com.scutmmq.web.controller.rtu.RtuListController;
import com.scutmmq.web.controller.rtu.RtuController;
import com.scutmmq.web.controller.rtu.RtuVerifyController;
import com.scutmmq.web.controller.data.DataRealtimeController;
import com.scutmmq.web.controller.data.DataHistoryController;
import com.scutmmq.web.controller.data.DataStatisticsController;
import com.scutmmq.web.controller.config.ConfigUpdateController;
import com.scutmmq.web.controller.alarm.AlarmListController;
import com.scutmmq.web.controller.alarm.AlarmHandleController;

/**
 * Web Server 启动类
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class Application {
    
    public static void main(String[] args) throws Exception {
        // 1. 注册所有路由
        RestFulExpress router = RestFulExpress.instance();
        
        // ===== RTU 管理接口 =====
        router.register("/api/rtu/register", RtuRegisterController.class);
        router.register("/api/rtu/list", RtuListController.class);
        router.registerPattern("/api/rtu/{rtuId}", RtuController.class);  // GET/PUT/DELETE
        router.register("/api/rtu/gateway/verify", RtuVerifyController.class);
        
        // ===== 数据查询接口 =====
        router.register("/api/data/realtime", DataRealtimeController.class);
        router.register("/api/data/history", DataHistoryController.class);
        router.register("/api/data/statistics", DataStatisticsController.class);
        
        // ===== 配置管理接口 =====
        router.registerPattern("/api/rtu/{rtuId}/config", ConfigUpdateController.class);
        
        // ===== 报警管理接口 =====
        router.register("/api/alarm/list", AlarmListController.class);
        router.registerPattern("/api/alarm/{alarmId}/handle", AlarmHandleController.class);
        
        // 2. 启动 HTTP 服务器（默认端口 8080）
        System.out.println("正在启动 Web Server...");
        new HttpServer(HttpServerConfig.port).start();
    }
}
