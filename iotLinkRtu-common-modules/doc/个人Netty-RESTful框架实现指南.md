# 用 Netty 手写轻量级 RESTful 框架

> 本文受 `xlink-restful` 框架结构启发，指导如何用纯 Netty 个人实现一套优雅的 RESTful HTTP 框架。
> 适合 Netty 初学者，从零到可用，所有代码均可直接运行。

---

## 一、整体架构设计

### 1.1 设计目标

仿照 `xlink-restful` 的优秀设计，实现以下能力：

| 功能 | 目标效果 |
|------|--------|
| 路由注册 | `router.register("POST", "/user/create", UserCreateController.class)` |
| Controller 抽象 | 继承 `BaseController`，重写 `post()` / `get()` 等方法 |
| 请求封装 | 统一的 `HttpRequest` 对象，方便取参数 |
| 响应封装 | 统一的 `HttpResponse` 对象，方便写响应 |
| 异常处理 | 抛出 `ApiException`，自动转为对应 HTTP 状态码 |
| 业务线程池 | I/O 线程与业务线程分离，避免阻塞 Netty |

### 1.2 整体架构图

```
HTTP 客户端
    │
    │ TCP 连接
    ▼
┌──────────────────────────────────────────────────┐
│              Netty HTTP Server                    │
│                                                  │
│  ┌─────────────── ChannelPipeline ─────────────┐ │
│  │  HttpServerCodec      (HTTP编解码)           │ │
│  │  HttpObjectAggregator (合并分片请求)         │ │
│  │  RouterHandler        (路由分发 ← 核心)      │ │
│  └─────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────┘
    │
    │ 提交到业务线程池（避免阻塞I/O线程）
    ▼
┌──────────────────────────────────────────────────┐
│              业务线程池                           │
│  Router → 找到 Controller → 执行业务方法          │
│           → 写回 HTTP 响应                        │
└──────────────────────────────────────────────────┘
```

### 1.3 项目结构

```
src/main/java/com/example/framework/
├── server/
│   └── HttpServer.java              # Netty 服务器启动
├── core/
│   ├── Router.java                  # 路由注册与查找
│   ├── RouterHandler.java           # Netty Handler，接收请求并分发
│   ├── BaseController.java          # Controller 抽象基类
│   ├── MyHttpRequest.java           # 请求封装
│   └── MyHttpResponse.java          # 响应封装
├── exception/
│   ├── ApiException.java            # 业务异常基类
│   ├── BadRequestException.java     # 400
│   ├── NotFoundException.java       # 404
│   └── ServerException.java         # 500
└── Application.java                 # 启动入口 + 路由注册
```

---

## 二、核心实现

### 2.1 服务器启动 —— `HttpServer.java`

```java
package com.example.framework.server;

import com.example.framework.core.RouterHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class HttpServer {

    private final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public HttpServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        bossGroup  = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                // 1. HTTP 编解码（处理粘包、HTTP协议解析）
                                .addLast(new HttpServerCodec())
                                // 2. 合并分片 HTTP 请求，最大 Body 10MB
                                .addLast(new HttpObjectAggregator(10 * 1024 * 1024))
                                // 3. 路由 Handler（核心）
                                .addLast(new RouterHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind(port).sync();
            System.out.println("HTTP Server started on port: " + port);
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```

---

### 2.2 请求封装 —— `MyHttpRequest.java`

```java
package com.example.framework.core;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;
import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * 对 Netty FullHttpRequest 的封装，提供便捷的参数获取方法
 */
public class MyHttpRequest {

    private final FullHttpRequest nettyRequest;
    private final Map<String, String> pathParams; // 路径参数（如 /user/{id} 中的 id）
    private JSONObject bodyJson; // 懒加载，避免重复解析

    public MyHttpRequest(FullHttpRequest nettyRequest, Map<String, String> pathParams) {
        this.nettyRequest = nettyRequest;
        this.pathParams   = pathParams;
    }

    /** 获取请求体 JSON（POST/PUT 常用）*/
    public JSONObject bodyJson() {
        if (bodyJson == null) {
            String bodyStr = nettyRequest.content().toString(CharsetUtil.UTF_8);
            bodyJson = bodyStr.isEmpty() ? new JSONObject() : JSONObject.parseObject(bodyStr);
        }
        return bodyJson;
    }

    /** 获取 URL 查询参数（GET 常用，如 ?name=xxx）*/
    public String queryParam(String name) {
        QueryStringDecoder decoder = new QueryStringDecoder(nettyRequest.uri());
        List<String> values = decoder.parameters().get(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    /** 获取路径参数（如 /user/{id} 中的 id）*/
    public String pathParam(String name) {
        return pathParams.getOrDefault(name, null);
    }

    /** 获取请求头 */
    public String header(String name) {
        return nettyRequest.headers().get(name);
    }

    /** 获取请求方法（GET/POST/PUT/DELETE）*/
    public String method() {
        return nettyRequest.method().name();
    }

    /** 获取请求路径 */
    public String path() {
        return new QueryStringDecoder(nettyRequest.uri()).path();
    }
}
```

---

### 2.3 响应封装 —— `MyHttpResponse.java`

```java
package com.example.framework.core;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

/**
 * HTTP 响应封装，提供便捷的响应写出方法
 */
public class MyHttpResponse {

    private final ChannelHandlerContext ctx;

    public MyHttpResponse(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    /** 写出 JSON 响应（200 OK）*/
    public void json(Object data) {
        String json = com.alibaba.fastjson.JSONObject.toJSONString(data);
        writeResponse(HttpResponseStatus.OK, json);
    }

    /** 写出纯字符串响应 */
    public void text(String text) {
        writeResponse(HttpResponseStatus.OK, text);
    }

    /** 写出指定状态码的 JSON 响应 */
    public void json(HttpResponseStatus status, Object data) {
        String json = com.alibaba.fastjson.JSONObject.toJSONString(data);
        writeResponse(status, json);
    }

    private void writeResponse(HttpResponseStatus status, String body) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            status,
            Unpooled.copiedBuffer(body, CharsetUtil.UTF_8)
        );
        response.headers()
            .set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8")
            .set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
            .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*"); // 允许跨域

        // writeAndFlush 是异步的，Netty 会在 I/O 线程发送
        ctx.writeAndFlush(response);
    }
}
```

---

### 2.4 异常体系 —— `exception/` 包

**`ApiException.java`（基类）：**

```java
package com.example.framework.exception;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * API 业务异常基类，抛出后框架自动转为对应 HTTP 错误响应
 */
public class ApiException extends RuntimeException {

    private final HttpResponseStatus status;
    private final int                errorCode;

    public ApiException(HttpResponseStatus status, int errorCode, String message) {
        super(message);
        this.status    = status;
        this.errorCode = errorCode;
    }

    public HttpResponseStatus getStatus()    { return status; }
    public int                getErrorCode() { return errorCode; }
}
```

**`BadRequestException.java`（400）：**

```java
package com.example.framework.exception;

import io.netty.handler.codec.http.HttpResponseStatus;

/** 请求参数错误（400）*/
public class BadRequestException extends ApiException {
    public BadRequestException(int errorCode, String message) {
        super(HttpResponseStatus.BAD_REQUEST, errorCode, message);
    }
    // 快捷构造
    public BadRequestException(String message) {
        this(40001, message);
    }
}
```

**`NotFoundException.java`（404）：**

```java
package com.example.framework.exception;

import io.netty.handler.codec.http.HttpResponseStatus;

/** 资源不存在（404）*/
public class NotFoundException extends ApiException {
    public NotFoundException(int errorCode, String message) {
        super(HttpResponseStatus.NOT_FOUND, errorCode, message);
    }
    public NotFoundException(String message) {
        this(40401, message);
    }
}
```

**`ServerException.java`（500）：**

```java
package com.example.framework.exception;

import io.netty.handler.codec.http.HttpResponseStatus;

/** 内部服务错误（500）*/
public class ServerException extends ApiException {
    public ServerException(int errorCode, String message) {
        super(HttpResponseStatus.INTERNAL_SERVER_ERROR, errorCode, message);
    }
    public ServerException(String message) {
        this(50001, message);
    }
}
```

---

### 2.5 Controller 抽象基类 —— `BaseController.java`

```java
package com.example.framework.core;

import com.example.framework.exception.ApiException;
import com.example.framework.exception.BadRequestException;

/**
 * Controller 抽象基类
 * 子类按需重写对应 HTTP 方法，不支持的方法默认返回 405
 */
public abstract class BaseController {

    /**
     * 执行请求（框架内部调用，不需要重写）
     */
    public final void handle(MyHttpRequest request, MyHttpResponse response) {
        try {
            switch (request.method()) {
                case "GET"    -> get(request, response);
                case "POST"   -> post(request, response);
                case "PUT"    -> put(request, response);
                case "DELETE" -> delete(request, response);
                default       -> response.json(
                        io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED,
                        ErrorBody.of(40500, "Method Not Allowed")
                );
            }
        } catch (ApiException e) {
            // 业务异常：转为对应 HTTP 错误响应
            response.json(e.getStatus(), ErrorBody.of(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            // 未预期异常：统一返回 500
            response.json(
                io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR,
                ErrorBody.of(50000, "Internal Server Error: " + e.getMessage())
            );
        }
    }

    // ---- 子类按需重写以下方法 ----

    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        throw new BadRequestException(40500, "GET method not supported");
    }

    protected void post(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        throw new BadRequestException(40500, "POST method not supported");
    }

    protected void put(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        throw new BadRequestException(40500, "PUT method not supported");
    }

    protected void delete(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        throw new BadRequestException(40500, "DELETE method not supported");
    }

    // ---- 工具方法（子类可直接调用）----

    /** 必填参数校验：为空则抛 400 */
    protected void requireNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " 不能为空");
        }
    }

    /** 必填字符串校验：为空或空字符串则抛 400 */
    protected void requireNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(fieldName + " 不能为空");
        }
    }

    /** 错误响应体格式 */
    record ErrorBody(int code, String message) {
        static ErrorBody of(int code, String msg) {
            return new ErrorBody(code, msg);
        }
    }
}
```

---

### 2.6 路由系统 —— `Router.java`

```java
package com.example.framework.core;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 路由注册与匹配
 * 支持路径参数，如 /user/{id}
 */
public class Router {

    private static final Router INSTANCE = new Router();

    public static Router instance() { return INSTANCE; }

    // 路由表：routeKey("POST", "/user/create") → Controller实例
    private final Map<String, BaseController> exactRoutes  = new HashMap<>();
    // 含路径参数的路由：Pattern → (Controller, 参数名列表)
    private final Map<Pattern, RouteEntry>    patternRoutes = new HashMap<>();

    /** 注册精确路由（无路径参数）*/
    public void register(String method, String path, BaseController controller) {
        exactRoutes.put(routeKey(method, path), controller);
    }

    /** 注册含路径参数的路由，如 /user/{id} */
    public void registerPattern(String method, String path, BaseController controller) {
        // 将 /user/{id} 转为正则 /user/([^/]+)，并记录参数名
        java.util.List<String> paramNames = new java.util.ArrayList<>();
        String regex = path.replaceAll("\\{([^/]+)}", m -> {
            paramNames.add(m.group(1).replaceAll("[{}]", ""));
            return "([^/]+)";
        });
        // 使用 replaceAll 的函数式版本在 Java 9+ 才支持，这里用简单替换
        String regexPath = path.replaceAll("\\{[^/]+}", "([^/]+)");
        // 提取参数名
        Matcher nameMatcher = Pattern.compile("\\{([^/]+)}").matcher(path);
        paramNames.clear();
        while (nameMatcher.find()) paramNames.add(nameMatcher.group(1));

        Pattern pattern = Pattern.compile("^" + regexPath + "$");
        patternRoutes.put(pattern, new RouteEntry(controller, paramNames, method));
    }

    /** 根据请求方法和路径查找 Controller */
    public RouteMatch match(String method, String path) {
        // 1. 优先精确匹配
        BaseController controller = exactRoutes.get(routeKey(method, path));
        if (controller != null) {
            return new RouteMatch(controller, new HashMap<>());
        }

        // 2. 路径参数匹配
        for (Map.Entry<Pattern, RouteEntry> entry : patternRoutes.entrySet()) {
            RouteEntry routeEntry = entry.getValue();
            if (!routeEntry.method().equals(method)) continue;

            Matcher matcher = entry.getKey().matcher(path);
            if (matcher.matches()) {
                Map<String, String> pathParams = new HashMap<>();
                for (int i = 0; i < routeEntry.paramNames().size(); i++) {
                    pathParams.put(routeEntry.paramNames().get(i), matcher.group(i + 1));
                }
                return new RouteMatch(routeEntry.controller(), pathParams);
            }
        }
        return null; // 未找到路由
    }

    private String routeKey(String method, String path) {
        return method.toUpperCase() + ":" + path;
    }

    // 内部记录类
    record RouteEntry(BaseController controller, java.util.List<String> paramNames, String method) {}
    public record RouteMatch(BaseController controller, Map<String, String> pathParams) {}
}
```

---

### 2.7 路由 Handler —— `RouterHandler.java`

```java
package com.example.framework.core;

import com.example.framework.exception.ApiException;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Netty Handler，负责：
 * 1. 接收 HTTP 请求
 * 2. 路由匹配
 * 3. 将业务逻辑提交到业务线程池执行（避免阻塞 I/O 线程）
 */
@ChannelHandler.Sharable
public class RouterHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final Router router = Router.instance();

    // 业务线程池：I/O 线程只做网络收发，业务逻辑在此线程池异步执行
    private static final ExecutorService BUSINESS_EXECUTOR = new ThreadPoolExecutor(
            4,                           // 核心线程数
            16,                          // 最大线程数
            60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            r -> {
                Thread t = new Thread(r, "http-business-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            }
    );

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        // 路径和方法
        String path   = new QueryStringDecoder(httpRequest.uri()).path();
        String method = httpRequest.method().name();

        // 路由查找
        Router.RouteMatch match = router.match(method, path);
        if (match == null) {
            // 404 响应
            MyHttpResponse response = new MyHttpResponse(ctx);
            response.json(HttpResponseStatus.NOT_FOUND,
                    java.util.Map.of("code", 40400, "message", "Route not found: " + method + " " + path));
            return;
        }

        // 封装请求/响应对象
        MyHttpRequest  request  = new MyHttpRequest(httpRequest, match.pathParams());
        MyHttpResponse response = new MyHttpResponse(ctx);

        // ★ 关键：提交到业务线程池，不阻塞 Netty I/O 线程
        httpRequest.retain(); // 引用计数+1，防止在异步执行前被释放
        BUSINESS_EXECUTOR.execute(() -> {
            try {
                match.controller().handle(request, response);
            } finally {
                httpRequest.release(); // 引用计数-1，释放内存
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("HTTP Handler exception: " + cause.getMessage());
        ctx.close();
    }
}
```

---

## 三、编写业务 Controller

有了框架，业务开发只需关注业务逻辑：

### 3.1 新增接口

```java
public class UserCreateController extends BaseController {

    @Override
    protected void post(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 1. 取参数
        String name  = req.bodyJson().getString("name");
        Integer age  = req.bodyJson().getInteger("age");

        // 2. 校验（直接用基类工具方法，错误时自动抛 400）
        requireNotBlank(name, "name");
        requireNotNull(age, "age");

        if (age < 0 || age > 150) {
            throw new BadRequestException("age 范围不合法");
        }

        // 3. 业务处理（此处省略 DAO 层调用）
        User user = new User(name, age);
        // userDao.save(user);

        // 4. 返回响应
        resp.json(user);
    }
}
```

### 3.2 查询接口（GET + 查询参数）

```java
public class UserQueryController extends BaseController {

    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // GET 请求从 URL 查询参数取值
        String name   = req.queryParam("name");
        String pageStr = req.queryParam("page");
        int page = pageStr != null ? Integer.parseInt(pageStr) : 1;

        // 查询业务逻辑...
        List<User> users = userDao.findByName(name, page);

        resp.json(Map.of(
            "total", users.size(),
            "list",  users
        ));
    }
}
```

### 3.3 更新接口（PUT）

```java
public class UserUpdateController extends BaseController {

    @Override
    protected void put(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        String id   = req.bodyJson().getString("id");
        String name = req.bodyJson().getString("name");

        requireNotBlank(id, "id");

        User user = userDao.findById(id);
        if (user == null) {
            throw new NotFoundException("用户不存在: " + id);
        }

        if (name != null) user.setName(name);
        userDao.save(user);

        resp.json(user);
    }
}
```

### 3.4 删除接口（DELETE + 路径参数）

```java
// 路径：DELETE /user/{id}
public class UserDeleteController extends BaseController {

    @Override
    protected void delete(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 路径参数：/user/{id} 中的 id
        String id = req.pathParam("id");

        requireNotBlank(id, "id");

        User user = userDao.findById(id);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }

        userDao.deleteById(id);
        resp.json(Map.of("deleted", 1));
    }
}
```

---

## 四、启动入口 —— `Application.java`

```java
public class Application {

    public static void main(String[] args) throws Exception {

        // ① 注册路由（精确路由）
        Router router = Router.instance();
        router.register("POST",   "/user/create",  new UserCreateController());
        router.register("GET",    "/user/query",   new UserQueryController());
        router.register("PUT",    "/user/update",  new UserUpdateController());

        // ② 注册含路径参数的路由
        router.registerPattern("DELETE", "/user/{id}", new UserDeleteController());
        router.registerPattern("GET",    "/user/{id}", new UserGetByIdController());

        // ③ 启动 HTTP Server
        new HttpServer(8080).start();
    }
}
```

---

## 五、与公司框架的对比与总结

| 方面 | 公司 xlink-restful | 本文个人实现 |
|------|-------------------|------------|
| 路由注册 | `RestfulExpress.getInstance().urlRegister(url, Class)` | `Router.instance().register(method, path, instance)` |
| Controller 基类 | `BaseController extends RestfulController` | `BaseController`（自定义） |
| HTTP 方法重写 | `post(socketHash, request, response)` | `post(request, response)` |
| 路径参数 | `request.header("id")` | `request.pathParam("id")` |
| Body 解析 | `funcGetBodyJson(request)` | `request.bodyJson()` |
| 写出响应 | `funcResponse(socketHash, response, json)` | `response.json(data)` |
| 异常处理 | `throw new Rest400StatusException(code, msg)` | `throw new BadRequestException(msg)` |
| 业务线程池 | 框架内部管理 | `RouterHandler` 中的 `BUSINESS_EXECUTOR` |
| 权限校验 | 框架内置 `CORP\|EMPOWER\|XLINK` 掩码 | 需自行在 Handler 或 Controller 中实现 |

---

## 六、扩展方向（进阶优化）

掌握以上基础后，可按需扩展：

### 6.1 添加全局拦截器（鉴权）

```java
// 在 RouterHandler 中，调用 controller.handle() 前执行拦截器
public interface Interceptor {
    boolean preHandle(MyHttpRequest req, MyHttpResponse resp);
}

// 注册拦截器
List<Interceptor> interceptors = List.of(new AuthInterceptor());
for (Interceptor i : interceptors) {
    if (!i.preHandle(request, response)) return; // 拦截，直接返回
}
match.controller().handle(request, response);
```

### 6.2 统一响应格式包装

```java
// 让 resp.json(data) 自动包装为统一格式
// { "code": 0, "message": "success", "data": {...} }
public void json(Object data) {
    Map<String, Object> wrapper = Map.of("code", 0, "message", "success", "data", data);
    writeResponse(HttpResponseStatus.OK, JSONObject.toJSONString(wrapper));
}
```

### 6.3 添加 HTTPS 支持

```java
// 在 Pipeline 中第一个位置加入 SslHandler
SslContext sslContext = SslContextBuilder
    .forServer(certFile, keyFile)
    .build();
ch.pipeline().addFirst(sslContext.newHandler(ch.alloc()));
```

### 6.4 WebSocket 支持（长连接）

```java
// 在 Pipeline 中加入 WebSocketServerProtocolHandler
ch.pipeline()
    .addLast(new HttpServerCodec())
    .addLast(new HttpObjectAggregator(65536))
    .addLast(new WebSocketServerProtocolHandler("/ws"))  // 协议升级
    .addLast(new MyWebSocketHandler());                  // WS消息处理
```

---

## 七、学习路径建议

```
阶段一：理解基础（对照本项目 RTUServer.java）
  → 搭建最简单的 Netty TCP Echo Server（收到什么就回什么）
  → 理解 EventLoop、Channel、Pipeline、Handler 概念

阶段二：实现本文框架（对照本文代码）
  → 加入 HttpServerCodec，实现 HTTP Server
  → 实现 Router 路由匹配
  → 实现 BaseController 抽象和异常处理

阶段三：对照公司代码理解高级设计
  → 学习 xlink-restful 的分片线程池设计（MessageDispatcher）
  → 理解 @Sharable Handler 的设计原则
  → 学习 LengthFieldBasedFrameDecoder 处理自定义二进制协议

阶段四：扩展能力
  → 实现拦截器链（类比 Spring MVC 的 HandlerInterceptor）
  → 实现参数自动反序列化（类比 @RequestBody）
  → 实现 WebSocket 长连接支持
```
