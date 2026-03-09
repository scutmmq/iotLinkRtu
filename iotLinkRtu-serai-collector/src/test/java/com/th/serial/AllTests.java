package com.th.serial;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * 测试套件 - 运行所有测试
 */
@Suite
@SuiteDisplayName("Serial Collector 完整测试套件")
@SelectPackages({
    "com.th.serial.protocol",
    "com.th.serial.utils",
    "com.th.serial.cache",
    "com.th.serial.integration"
})
public class AllTests {
    // 测试套件入口
}
