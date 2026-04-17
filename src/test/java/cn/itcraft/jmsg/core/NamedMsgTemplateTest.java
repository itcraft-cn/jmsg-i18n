package cn.itcraft.jmsg.core;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

public class NamedMsgTemplateTest {
    
    @Before
    public void setUp() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
    }
    
    @After
    public void tearDown() {
        MsgTemplateConfig.reset();
    }
    
    @Test
    public void testFromValueStringBasic() {
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST_001", "测试|Test|中文:{name}|English:{name}|1");
        
        assertEquals("TEST_001", template.getCode());
        assertEquals("测试/Test", template.getName());
        assertEquals(MsgTemplate.Style.NAMED, template.getStyle());
        assertEquals(1, template.getOrder());
    }
    
    @Test
    public void testFromValueStringInvalidFormat() {
        try {
            NamedMsgTemplate.fromValueString("TEST", "too|short");
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid format"));
        }
    }
    
    @Test
    public void testRenderMapWithDefaultLocale() {
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST_001", "测试|Test|中文:{name}|English:{name}|1");
        
        Map<String, Object> args = new HashMap<>();
        args.put("name", "值");
        
        String result = template.render(args);
        
        assertEquals("中文:值", result);
    }
    
    @Test
    public void testRenderMapWithExplicitLocale() {
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST_001", "测试|Test|中文:{name}|English:{name}|1");
        
        Map<String, Object> args = new HashMap<>();
        args.put("name", "value");
        
        String result = template.render(Locale.US, args);
        
        assertEquals("English:value", result);
    }
    
    @Test
    public void testRenderBeanWithDefaultLocale() {
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST_002", "日志|Log|用户{userId}登录|User {userId} logged in|2");
        
        TestUserBean bean = new TestUserBean();
        bean.userId = "admin";
        
        String result = template.render(bean);
        
        assertEquals("用户admin登录", result);
    }
    
    @Test
    public void testRenderBeanWithExplicitLocale() {
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST_002", "日志|Log|用户{userId}登录|User {userId} logged in|2");
        
        TestUserBean bean = new TestUserBean();
        bean.userId = "admin";
        
        String result = template.render(Locale.US, bean);
        
        assertEquals("User admin logged in", result);
    }
    
    @Test
    public void testRenderMultipleParamsMap() {
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST_003", "日志|Log|用户{userId}于{time}登录|User {userId} logged in at {time}|3");
        
        Map<String, Object> args = new HashMap<>();
        args.put("userId", "admin");
        args.put("time", "2024-04-17");
        
        String result = template.render(args);
        
        assertEquals("用户admin于2024-04-17登录", result);
    }
    
    @Test
    public void testRenderMultipleParamsBean() {
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST_003", "日志|Log|用户{userId}于{time}登录|User {userId} logged in at {time}|3");
        
        TestLoginBean bean = new TestLoginBean();
        bean.userId = "admin";
        bean.time = "2024-04-17";
        
        String result = template.render(bean);
        
        assertEquals("用户admin于2024-04-17登录", result);
    }
    
    @Test
    public void testGetSupportedLocales() {
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST", "名|Name|模板:{x}|Template:{x}|1");
        
        Set<String> locales = template.getSupportedLocales();
        
        assertTrue(locales.contains("zh"));
        assertTrue(locales.contains("en"));
    }
    
    @Test
    public void testRenderNullArgs() {
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST", "名|Name|模板:{x}|Template:{x}|1");
        
        String result = template.render();
        
        assertEquals("模板:{x}", result);
    }
    
    @Test
    public void testLocaleFallback() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
        
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST", "名|Name|中文:{x}|English:{x}|1");
        
        Map<String, Object> args = new HashMap<>();
        args.put("x", "值");
        
        String result = template.render(Locale.JAPANESE, args);
        
        assertEquals("中文:值", result);
    }
    
    public static class TestUserBean {
        public String userId;
        
        public String getUserId() { return userId; }
    }
    
    public static class TestLoginBean {
        public String userId;
        public String time;
        
        public String getUserId() { return userId; }
        public String getTime() { return time; }
    }
}