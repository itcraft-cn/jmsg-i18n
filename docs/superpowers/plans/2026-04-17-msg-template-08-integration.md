# jmsg-i18n 实现计划 - Part 8: Integration

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development

---

## Task 19: 集成测试

**Files:**
- Create: `src/test/java/cn/itcraft/jmsg/integration/MsgTemplateIntegrationTest.java`
- Resource: `src/test/resources/msg_integration_simple.properties`
- Resource: `src/test/resources/msg_integration_named.properties`

- [ ] **Step 1: Create integration test resources**

```properties
# msg_integration_simple.properties
SYS_ERR_001=系统错误|System error|系统内部错误:{}|Internal system error:{}|1
SYS_ERR_002=参数错误|Parameter error|参数{}验证失败|Parameter {} validation failed|2
LOG_001=登录日志|Login log|用户{}于{}登录成功|User {} logged in at {}|10
LOG_002=操作日志|Operation log|用户{}执行了{}操作|User {} performed {} operation|11
BIZ_ERR_001=订单错误|Order error|订单{}创建失败|Order {} creation failed|20
```

```properties
# msg_integration_named.properties
SYS_ERR_001=系统错误|System error|系统内部错误:{reason}|Internal system error:{reason}|1
SYS_ERR_002=参数错误|Parameter error|参数{paramName}验证失败|Parameter {paramName} validation failed|2
LOG_001=登录日志|Login log|用户{userId}于{time}登录成功|User {userId} logged in at {time}|10
LOG_002=操作日志|Operation log|用户{userId}执行了{action}操作|User {userId} performed {action} operation|11
BIZ_ERR_001=订单错误|Order error|订单{orderId}创建失败|Order {orderId} creation failed|20
```

- [ ] **Step 2: Write integration test**

```java
package cn.itcraft.jmsg.integration;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.builder.MsgTemplateBuilder;
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.core.SimpleMsgTemplate;
import cn.itcraft.jmsg.core.NamedMsgTemplate;
import cn.itcraft.jmsg.loader.FileMsgTemplateLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import static org.junit.Assert.*;

public class MsgTemplateIntegrationTest {
    
    @Before
    public void setUp() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
        MsgTemplateConfig.setReflectCacheEnabled(true);
        
        FileMsgTemplateLoader.forSimple("msg_integration_simple.properties")
            .load(MsgTemplate.class, null);
        FileMsgTemplateLoader.forNamed("msg_integration_named.properties")
            .load(MsgTemplate.class, null);
    }
    
    @After
    public void tearDown() {
        MsgTemplateConfig.reset();
        EnumRegistry.clear();
    }
    
    @Test
    public void testFullWorkflowSimple() {
        String result = MsgTemplateBuilder.create()
            .code("SYS_ERR_001")
            .simple()
            .args("数据库连接超时")
            .render();
        
        assertEquals("系统内部错误：数据库连接超时", result);
        
        result = MsgTemplateBuilder.create()
            .code("SYS_ERR_001")
            .locale(Locale.US)
            .simple()
            .args("Database timeout")
            .render();
        
        assertEquals("Internal system error:Database timeout", result);
    }
    
    @Test
    public void testFullWorkflowNamedMap() {
        Map<String, Object> args = new HashMap<>();
        args.put("userId", "admin");
        args.put("time", "2024-04-17 10:30:00");
        
        String result = MsgTemplateBuilder.create()
            .code("LOG_001")
            .named()
            .args(args)
            .render();
        
        assertEquals("用户admin于2024-04-17 10:30:00登录成功", result);
        
        result = MsgTemplateBuilder.create()
            .code("LOG_001")
            .locale(Locale.US)
            .named()
            .args(args)
            .render();
        
        assertEquals("User admin logged in at 2024-04-17 10:30:00", result);
    }
    
    @Test
    public void testFullWorkflowNamedBean() {
        LoginEvent event = new LoginEvent();
        event.userId = "john";
        event.time = "2024-04-17";
        event.action = "delete";
        
        String result = MsgTemplateBuilder.create()
            .code("LOG_002")
            .named()
            .bean(event)
            .render();
        
        assertEquals("用户john执行了delete操作", result);
        
        result = MsgTemplateBuilder.create()
            .code("LOG_002")
            .locale(Locale.US)
            .named()
            .bean(event)
            .render();
        
        assertEquals("User john performed delete operation", result);
    }
    
    @Test
    public void testReflectCachePerformance() {
        LoginEvent event = new LoginEvent();
        event.userId = "admin";
        event.time = "2024-04-17";
        
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            MsgTemplateBuilder.create()
                .code("LOG_001")
                .named()
                .bean(event)
                .render();
        }
        long elapsed = System.nanoTime() - start;
        
        System.out.println("10000 renders with cache: " + elapsed / 1_000_000 + "ms");
        assertTrue(elapsed < 500_000_000);
    }
    
    @Test
    public void testDefaultLocaleConfig() {
        MsgTemplateConfig.setDefaultLocale(Locale.US);
        
        String result = MsgTemplateBuilder.create()
            .code("SYS_ERR_001")
            .simple()
            .args("error")
            .render();
        
        assertEquals("Internal system error:error", result);
        
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
        
        result = MsgTemplateBuilder.create()
            .code("SYS_ERR_001")
            .simple()
            .args("错误")
            .render();
        
        assertEquals("系统内部错误：错误", result);
    }
    
    @Test
    public void testMultipleTemplateStyles() {
        String simpleResult = MsgTemplateBuilder.create()
            .code("BIZ_ERR_001")
            .simple()
            .args("ORD-12345")
            .render();
        
        assertEquals("订单ORD-12345创建失败", simpleResult);
        
        Map<String, Object> namedArgs = new HashMap<>();
        namedArgs.put("orderId", "ORD-12345");
        
        String namedResult = MsgTemplateBuilder.create()
            .code("BIZ_ERR_001")
            .named()
            .args(namedArgs)
            .render();
        
        assertEquals("订单ORD-12345创建失败", namedResult);
    }
    
    @Test
    public void testLocaleFallbackChain() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
        
        String result = MsgTemplateBuilder.create()
            .code("SYS_ERR_001")
            .locale(Locale.JAPANESE)
            .simple()
            .args("test")
            .render();
        
        assertEquals("系统内部错误：test", result);
    }
    
    @Test
    public void testReflectCacheDisabled() {
        MsgTemplateConfig.setReflectCacheEnabled(false);
        
        LoginEvent event = new LoginEvent();
        event.userId = "test";
        event.time = "2024-04-17";
        
        String result = MsgTemplateBuilder.create()
            .code("LOG_001")
            .named()
            .bean(event)
            .render();
        
        assertEquals("用户test于2024-04-17登录成功", result);
        
        MsgTemplateConfig.setReflectCacheEnabled(true);
    }
    
    @Test
    public void testMixedBuilderUsage() {
        String simpleMsg = MsgTemplateBuilder.create()
            .code("SYS_ERR_002")
            .locale(Locale.US)
            .simple()
            .args("email")
            .render();
        
        String namedMsg = MsgTemplateBuilder.create()
            .code("SYS_ERR_002")
            .locale(Locale.US)
            .named()
            .arg("paramName", "email")
            .render();
        
        assertEquals("Parameter email validation failed", simpleMsg);
        assertEquals("Parameter email validation failed", namedMsg);
    }
    
    public static class LoginEvent {
        public String userId;
        public String time;
        public String action;
        
        public String getUserId() { return userId; }
        public String getTime() { return time; }
        public String getAction() { return action; }
    }
}
```

- [ ] **Step 3: Run integration test**

Run: `mvn test -Dtest=MsgTemplateIntegrationTest -q`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add src/test/java/cn/itcraft/jmsg/integration/MsgTemplateIntegrationTest.java
git add src/test/resources/msg_integration_simple.properties
git add src/test/resources/msg_integration_named.properties
git commit -m "test: add integration tests for full workflow verification"
```

---

## Task 20: 运行全部测试并打包

- [ ] **Step 1: 运行全部测试**

Run: `mvn test -q`

Expected: 所有测试通过

- [ ] **Step 2: 检查测试覆盖率**

Run: `mvn test -Dtest=* -q 2>&1 | grep "Tests run"`

Expected: 确认所有测试类都执行

- [ ] **Step 3: 打包项目**

Run: `mvn clean package -DskipTests -q`

Expected: 生成 jar 文件

- [ ] **Step 4: 最终提交**

```bash
git add -A
git commit -m "feat: complete jmsg-i18n multi-language template system

Features:
- MsgTemplate interface with SIMPLE/NAMED styles
- SimpleMsgTemplate {} style implementation
- NamedMsgTemplate {name} style with Map/Bean support
- TemplateCompiler for pre-compiled templates
- TemplateRenderer with StringBuilderPool optimization
- ReflectCache for getter method caching
- MsgTemplateConfig for global locale/settings
- FileMsgTemplateLoader and PropMsgTemplateLoader
- MsgTemplateBuilder with SimpleBuilder/NamedBuilder
- Full integration test coverage"
```

---

## 完成检查清单

- [ ] 所有Task 01-20完成
- [ ] 全部测试通过: `mvn test -q`
- [ ] 项目打包成功: `mvn package -q`
- [ ] 最终提交完成
- [ ] 功能验证:
  - [ ] Simple风格 {} 模板渲染
  - [ ] Named风格 {name} Map参数
  - [ ] Named风格 {name} Bean参数（反射）
  - [ ] Locale配置和回退
  - [ ] ReflectCache启用/禁用
  - [ ] Builder链式调用
  - [ ] 文件加载和Properties加载

---

## 项目交付

实现完成后，项目提供以下功能：

**API使用示例**：

```java
// 初始化
MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
FileMsgTemplateLoader.forSimple("templates_simple.properties").load(MsgTemplate.class, null);
FileMsgTemplateLoader.forNamed("templates_named.properties").load(MsgTemplate.class, null);

// Simple风格
String msg = MsgTemplateBuilder.create()
    .code("ERR_001").simple().args("数据库").render();

// Named风格 - Map
Map<String, Object> args = new HashMap<>();
args.put("userId", "admin");
String msg = MsgTemplateBuilder.create()
    .code("LOG_001").named().args(args).render();

// Named风格 - Bean
LoginEvent event = new LoginEvent();
event.userId = "admin";
String msg = MsgTemplateBuilder.create()
    .code("LOG_001").named().bean(event).render();
```