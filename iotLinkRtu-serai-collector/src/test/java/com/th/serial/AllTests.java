package com.scutmmq;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * 测试套件 - 运行所有测试
 */
@Suite
@SuiteDisplayName("Serial Collector 完整测试套件")
@SelectPackages({
    "com.scutmmq.protocol",
    "com.scutmmq.utils",
    "com.scutmmq.cache",
    "com.scutmmq.integration"
})
public class AllTests {
    // 测试套件入口
}
