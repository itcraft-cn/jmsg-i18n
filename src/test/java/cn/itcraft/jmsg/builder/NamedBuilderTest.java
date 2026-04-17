package cn.itcraft.jmsg.builder;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.core.NamedMsgTemplate;
import cn.itcraft.jmsg.loader.FileMsgTemplateLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import static org.junit.Assert.*;

public class NamedBuilderTest {
    
    @Before
    public void setUp() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
        FileMsgTemplateLoader.forNamed("msg_templates_named.properties")
            .load(MsgTemplate.class, null);
    }
    
    @After
    public void tearDown() {
        MsgTemplateConfig.reset();
        EnumRegistry.clear();
    }
    
    @Test
    public void testRenderMapWithDefaultLocale() {
        String result = MsgTemplateBuilder.create()
            .code("TEST_001")
            .named()
            .arg("name", "值")
            .render();
        
        assertEquals("中文:值", result);
    }
    
    @Test
    public void testRenderMapWithExplicitLocale() {
        String result = MsgTemplateBuilder.create()
            .code("TEST_001")
            .locale(Locale.US)
            .named()
            .arg("name", "value")
            .render();
        
        assertEquals("English:value", result);
    }
    
    @Test
    public void testRenderMapMultipleArgs() {
        Map<String, Object> args = new HashMap<>();
        args.put("userId", "admin");
        args.put("time", "2024-04-17");
        
        String result = MsgTemplateBuilder.create()
            .code("TEST_002")
            .named()
            .args(args)
            .render();
        
        assertEquals("用户admin于2024-04-17登录", result);
    }
    
    @Test
    public void testRenderBeanWithDefaultLocale() {
        LoginEventBean bean = new LoginEventBean();
        bean.userId = "admin";
        bean.time = "2024-04-17";
        
        String result = MsgTemplateBuilder.create()
            .code("TEST_002")
            .named()
            .bean(bean)
            .render();
        
        assertEquals("用户admin于2024-04-17登录", result);
    }
    
    @Test
    public void testRenderBeanWithExplicitLocale() {
        LoginEventBean bean = new LoginEventBean();
        bean.userId = "admin";
        bean.time = "2024-04-17";
        
        String result = MsgTemplateBuilder.create()
            .code("TEST_002")
            .locale(Locale.US)
            .named()
            .bean(bean)
            .render();
        
        assertEquals("User admin logged in at 2024-04-17", result);
    }
    
    @Test
    public void testRenderWithLocaleMethod() {
        String result = MsgTemplateBuilder.create()
            .code("TEST_001")
            .named()
            .arg("name", "value")
            .render(Locale.US);
        
        assertEquals("English:value", result);
    }
    
    @Test
    public void testRenderCodeRequired() {
        try {
            MsgTemplateBuilder.create()
                .named()
                .arg("name", "test")
                .render();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Code is required"));
        }
    }
    
    @Test
    public void testRenderTemplateNotFound() {
        try {
            MsgTemplateBuilder.create()
                .code("NONEXISTENT")
                .named()
                .arg("name", "test")
                .render();
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("not found"));
        }
    }
    
    @Test
    public void testBuilderChaining() {
        String result = MsgTemplateBuilder.create()
            .code("TEST_001")
            .locale(Locale.CHINA)
            .named()
            .arg("name", "测试值")
            .render();
        
        assertEquals("中文:测试值", result);
    }
    
    public static class LoginEventBean {
        public String userId;
        public String time;
        
        public String getUserId() { return userId; }
        public String getTime() { return time; }
    }
}