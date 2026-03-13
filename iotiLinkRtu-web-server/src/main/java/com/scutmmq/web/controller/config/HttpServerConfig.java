package com.scutmmq.web.controller.config;

import com.scutmmq.utils.MicroConfig;

public class HttpServerConfig {
    public static int port = MicroConfig.readInt("http.server.port");
}
