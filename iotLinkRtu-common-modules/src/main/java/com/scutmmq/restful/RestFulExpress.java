package com.scutmmq.restful;

import com.scutmmq.core.BaseController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 路由组件，负责注册接口和路由匹配
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-13
 */
public class RestFulExpress{

    // 单例模式，确保只有一个 RestFulExpress 实例
    private static final RestFulExpress INSTANCE = new RestFulExpress();

    /**
     * 精确路由表：path → Controller Class
     * 用于存储不含路径参数的路由，如 "/user/create"
     */
    private final Map<String, Class<? extends BaseController>> exactRoutes = new HashMap<>();

    /**
     * 模式路由表：Pattern → (Controller Class, 参数名列表)
     * 用于存储含路径参数的路由，如 "/user/{id}"
     */
    private final Map<Pattern, RouteEntry> patternRoutes = new HashMap<>();

    RestFulExpress(){}

    public static RestFulExpress instance(){
        return INSTANCE;
    }

    /**
     * 注册精确路由（无路径参数）
     * 
     * @param path 路径，如 "/user/create"
     * @param controller Controller 类
     */
    public void register(String path, Class<? extends BaseController> controller){
        try {
            exactRoutes.put(path, controller);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 注册含路径参数的路由
     * 例如：registerPattern("/user/{id}", UserController.class)
     * 
     * @param path 含路径参数的路径，如 "/user/{id}"
     * @param controller Controller 类
     */
    public void registerPattern(String path, Class<? extends BaseController> controller) {
        try {
            // 将 /user/{id} 转为正则 /user/([^/]+)
            String regexPath = path.replaceAll("\\{[^/]+}", "([^/]+)");
            
            // 提取参数名列表
            List<String> paramNames = new ArrayList<>();
            Matcher nameMatcher = Pattern.compile("\\{([^/]+)}").matcher(path);
            while (nameMatcher.find()) {
                paramNames.add(nameMatcher.group(1));
            }

            System.out.println("Registering pattern route: " + path + " -> " + controller);
            // 编译正则模式
            Pattern pattern = Pattern.compile("^" + regexPath + "$");
            patternRoutes.put(pattern, new RouteEntry(controller, paramNames));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据路径查找匹配的 Controller
     * 
     * @param path 请求路径，如 "/user/123"
     * @return 匹配结果，包含 Controller 类和路径参数；如果未找到返回 null
     */
    public RouteMatch match(String path) {
        // 1. 优先精确匹配
        Class<? extends BaseController> controllerClass = exactRoutes.get(path);
        if (controllerClass != null) {
            return new RouteMatch(controllerClass, new HashMap<>());
        }
        
        // 2. 路径参数匹配
        for (Map.Entry<Pattern, RouteEntry> entry : patternRoutes.entrySet()) {
            Matcher matcher = entry.getKey().matcher(path);
            if (matcher.matches()) {
                Map<String, String> pathParams = new HashMap<>();
                RouteEntry routeEntry = entry.getValue();
                for (int i = 0; i < routeEntry.paramNames().size(); i++) {
                    pathParams.put(routeEntry.paramNames().get(i), matcher.group(i + 1));
                }
                return new RouteMatch(routeEntry.controllerClass(), pathParams);
            }
        }
        return null; // 未找到路由
    }

    /**
     * 向后兼容的旧方法（已废弃）
     * 默认注册为精确路由
     * 
     * @param url URL 路径
     * @param controller Controller 类
     * @deprecated 请使用 {@link #register(String, Class)} 代替
     */
    @Deprecated
    public void urlRegister(String url, Class<? extends BaseController> controller){
        register(url, controller);
    }

    /**
     * 路由表条目：存储 Controller 类和路径参数名列表
     */
    public record RouteEntry(Class<? extends BaseController> controllerClass, List<String> paramNames) {}

    /**
     * 路由匹配结果：包含 Controller 类和提取的路径参数
     */
    public record RouteMatch(Class<? extends BaseController> controllerClass, Map<String, String> pathParams) {}
}
