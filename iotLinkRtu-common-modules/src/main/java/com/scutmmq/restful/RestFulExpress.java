package com.scutmmq.restful;

/**
 * 路由组件，负责注册接口
 */
public class RestFulExpress{

    private static final RestFulExpress INSTANCE = new RestFulExpress();

    RestFulExpress(){}

    public static RestFulExpress instance(){
        return INSTANCE;
    }



}
