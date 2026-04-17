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
        
        assertEquals("系统内部错误:数据库连接超时", result);
        
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
        
        assertEquals("系统内部错误:错误", result);
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
        
        assertEquals("系统内部错误:test", result);
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