# Netty RESTful框架使用指南

## 📖 快速开始

### 1. 路由注册

在 Application 启动类中注册你的 Controller：

```java
import com.scutmmq.restful.RestFulExpress;
import com.scutmmq.restful.HttpServer;

public class Application {
    public static void main(String[] args) throws Exception {
        // 获取路由实例
        RestFulExpress router = RestFulExpress.instance();
        
        // ① 注册精确路由（无路径参数）
        router.register("/user/create", UserCreateController.class);
        router.register("/user/query", UserQueryController.class);
        
        // ② 注册带路径参数的路由
        router.registerPattern("/user/{id}", UserController.class);
        router.registerPattern("/order/{orderId}/items", OrderItemsController.class);
        
        // ③ 启动 HTTP Server（默认端口 8080）
        new HttpServer(8080).start();
    }
}
```

---

### 2. 编写 Controller

继承 `BaseController` 并重写对应的 HTTP 方法：

#### 示例 1：创建用户（POST）

```java
package com.example.controller;

import com.scutmmq.core.BaseController;
import com.scutmmq.core.MyHttpRequest;
import com.scutmmq.core.MyHttpResponse;
import java.util.Map;

public class UserCreateController extends BaseController {
    
    @Override
    protected void post(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 从请求体获取参数
        String name = req.bodyString("name");
        Integer age = toInteger(req.bodyJson().get("age"));
        
        // 参数校验
        requireNotBlank(name, "name");
        requireNotNull(age, "age");
        
        if (age < 0 || age > 150) {
            throw new BadRequestException("age 范围不合法");
        }
        
        // 业务处理（此处省略 DAO 层调用）
        User user = new User(name, age);
        // userService.save(user);
        
        // 返回响应
        resp.json(Map.of(
            "code", 0,
            "message", "success",
            "data", user
        ));
    }
}
```

#### 示例 2：查询用户列表（GET + 查询参数）

```java
public class UserQueryController extends BaseController {
    
    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 从 URL 查询参数获取
        String name = req.queryParam("name");
        String pageStr = req.queryParam("page");
        int page = parseIntParam(pageStr, 1); // 默认第 1 页
        
        // 业务处理
        List<User> users = userService.findByName(name, page);
        
        resp.json(Map.of(
            "total", users.size(),
            "list", users
        ));
    }
}
```

#### 示例 3：用户详情管理（支持多种 HTTP 方法）

```java
public class UserController extends BaseController {
    
    // GET /user/{id} - 获取用户详情
    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 从路径参数获取 id
        String id = req.pathParam("id");
        requireNotBlank(id, "id");
        
        User user = userService.findById(id);
        if (user == null) {
            throw new NotFoundException("用户不存在：" + id);
        }
        
        resp.json(user);
    }
    
    // PUT /user/{id} - 更新用户
    @Override
    protected void put(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        String id = req.pathParam("id");
        String name = req.bodyString("name");
        Integer age = toInteger(req.bodyJson().get("age"));
        
        User user = userService.findById(id);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        
        // 更新字段
        if (name != null) user.setName(name);
        if (age != null) user.setAge(age);
        
        userService.save(user);
        resp.json(user);
    }
    
    // DELETE /user/{id} - 删除用户
    @Override
    protected void delete(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        String id = req.pathParam("id");
        requireNotBlank(id, "id");
        
        User user = userService.findById(id);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        
        userService.deleteById(id);
        resp.json(Map.of("deleted", 1));
    }
}
```

---

### 3. 高级用法

#### 3.1 多路径参数

```java
// 注册路由
router.registerPattern("/order/{orderId}/items/{itemId}", OrderItemController.class);

// Controller
public class OrderItemController extends BaseController {
    
    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        // 获取多个路径参数
        String orderId = req.pathParam("orderId");
        String itemId = req.pathParam("itemId");
        
        OrderItem item = orderItemService.findByKeys(orderId, itemId);
        resp.json(item);
    }
}
```

#### 3.2 统一响应格式

在 `BaseController` 中已经提供了统一的错误处理，你也可以自定义响应包装：

```java
protected void success(MyHttpResponse resp, Object data) {
    resp.json(Map.of(
        "code", 0,
        "message", "success",
        "data", data
    ));
}

protected void error(MyHttpResponse resp, int code, String message) {
    resp.json(Map.of(
        "code", code,
        "message", message,
        "data", null
    ));
}
```

#### 3.3 工具方法使用

`BaseController` 提供了丰富的工具方法：

```java
// 参数校验
requireNotNull(value, "fieldName");      // 对象不能为 null
requireNotBlank(str, "fieldName");       // 字符串不能为空或空白

// 类型转换
Integer age = toInteger(obj);            // 安全转换为 Integer
int page = parseIntParam(str, 1);        // 字符串转 int，失败返回默认值

// 从请求体获取数据
String name = req.bodyString("name");    // 获取 JSON 字符串字段
Map<String, Object> body = req.bodyJson(); // 获取整个 JSON 对象

// 从查询参数获取
String keyword = req.queryParam("kw");   // 获取 ?kw=xxx

// 从路径参数获取
String id = req.pathParam("id");         // 获取 /user/{id} 中的 id

// 获取请求头
String token = req.header("Authorization");
```

---

## 📊 特性对比

| 特性 | 旧版本 | 新版本 |
|------|--------|--------|
| 路由注册 | `urlRegister(url, Class)` | `register(path, Class)` |
| 路径参数 | ❌ 不支持 | ✅ 支持 `/user/{id}` |
| HTTP 方法区分 | ✅ 自动分发 | ✅ 自动分发 |
| 向后兼容 | - | ✅ 保留 `urlRegister()` |

---

## ⚠️ 注意事项

### 1. isStart 标记问题

当前实现中，路由注册需要 `isStart` 为 true 才能生效。请在启动时设置：

```java
RestFulExpress router = RestFulExpress.instance();
// TODO: 需要在启动前设置 isStart 为 true
new HttpServer(8080).start();
```

**建议修改：** 移除 `isStart` 检查，允许在任何时候注册路由。

### 2. Controller实例化

每次请求都会创建新的 Controller实例（通过 `newInstance()`）。如果 Controller 较重，可以考虑改为单例模式。

### 3. 异常处理

框架会自动捕获并处理以下异常：
- `ApiException` 及其子类 → 对应 HTTP 状态码
- 其他未预期异常 → 返回 500 错误

---

## 🚀 完整示例

```java
// ====== Application.java ======
public class Application {
    public static void main(String[] args) throws Exception {
        RestFulExpress router = RestFulExpress.instance();
        
        // 注册路由
        router.register("/user/create", UserCreateController.class);
        router.register("/user/query", UserQueryController.class);
        router.registerPattern("/user/{id}", UserController.class);
        
        // 启动服务器
        new HttpServer(8080).start();
    }
}

// ====== UserCreateController.java ======
public class UserCreateController extends BaseController {
    @Override
    protected void post(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        String name = req.bodyString("name");
        Integer age = toInteger(req.bodyJson().get("age"));
        
        requireNotBlank(name, "name");
        requireNotNull(age, "age");
        
        User user = new User(name, age);
        // userService.save(user);
        
        resp.json(Map.of("code", 0, "message", "success", "data", user));
    }
}

// ====== UserController.java ======
public class UserController extends BaseController {
    @Override
    protected void get(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        String id = req.pathParam("id");
        User user = userService.findById(id);
        resp.json(user);
    }
    
    @Override
    protected void put(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        String id = req.pathParam("id");
        String name = req.bodyString("name");
        
        User user = userService.findById(id);
        if (name != null) user.setName(name);
        userService.save(user);
        
        resp.json(user);
    }
    
    @Override
    protected void delete(MyHttpRequest req, MyHttpResponse resp) throws Exception {
        String id = req.pathParam("id");
        userService.deleteById(id);
        resp.json(Map.of("deleted", 1));
    }
}
```

---

## 📝 总结

你的 Netty RESTful框架现在已经具备以下能力：

✅ **精确路由匹配** - `/user/create`  
✅ **路径参数支持** - `/user/{id}`  
✅ **HTTP 方法自动分发** - GET/POST/PUT/DELETE 自动调用对应方法  
✅ **统一异常处理** - ApiException 自动转为 HTTP 错误响应  
✅ **业务线程池** - I/O 与业务逻辑分离，避免阻塞  
✅ **请求/响应封装** - 便捷的参数获取和响应写出方法  
✅ **向后兼容** - 保留旧的 `urlRegister()` 方法

现在可以开始使用了！🎉
