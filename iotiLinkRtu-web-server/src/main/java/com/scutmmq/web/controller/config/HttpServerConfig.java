package com.scutmmq.web.controller.config;

import com.scutmmq.web.config.WebServerProperties;

public class HttpServerConfig {
    public static int port = WebServerProperties.readInt("http.server.port");
}
