package com.scutmmq.web.controller.frontend;

import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 管理端首页。
 */
public class FrontendPageController extends BaseController {

    private static final String INDEX_RESOURCE = "/static/index.html";

    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        try (InputStream inputStream = FrontendPageController.class.getResourceAsStream(INDEX_RESOURCE)) {
            if (inputStream == null) {
                resp.text("Frontend resource not found: " + INDEX_RESOURCE);
                return;
            }
            resp.html(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
