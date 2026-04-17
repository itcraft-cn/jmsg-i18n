package cn.itcraft.jmsg.core;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.Locale;
import java.util.Set;
import static org.junit.Assert.*;

public class SimpleMsgTemplateTest {
    
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
        SimpleMsgTemplate template = SimpleMsgTemplate.fromValueString(
            "TEST_001", "测试|Test|中文:{}|English:{}|1");
        
        assertEquals("TEST_001", template.getCode());
        assertEquals("测试/Test", template.getName());
        assertEquals(MsgTemplate.Style.SIMPLE, template.getStyle());
        assertEquals(1, template.getOrder());
    }
    
    @Test
    public void testFromValueStringInvalidFormat() {
        try {
            SimpleMsgTemplate.fromValueString("TEST", "too|short");
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid format"));
        }
    }
    
    @Test
    public void testRenderWithDefaultLocale() {
        SimpleMsgTemplate template = SimpleMsgTemplate.fromValueString(
            "TEST_001", "测试|Test|中文:{}|English:{}|1");
        
        String result = template.render("值");
        
        assertEquals("中文:值", result);
    }
    
    @Test
    public void testRenderWithExplicitLocale() {
        SimpleMsgTemplate template = SimpleMsgTemplate.fromValueString(
            "TEST_001", "测试|Test|中文:{}|English:{}|1");
        
        String result = template.render(Locale.US, "value");
        
        assertEquals("English:value", result);
    }
    
    @Test
    public void testRenderMultipleParams() {
        SimpleMsgTemplate template = SimpleMsgTemplate.fromValueString(
            "TEST_002", "日志|Log|用户{}于{}登录|User {} logged in at {}|2");
        
        String result = template.render("admin", "2024-04-17");
        
        assertEquals("用户admin于2024-04-17登录", result);
    }
    
    @Test
    public void testGetSupportedLocales() {
        SimpleMsgTemplate template = SimpleMsgTemplate.fromValueString(
            "TEST", "名|Name|模板1:{}|Template1:{}|1");
        
        Set<String> locales = template.getSupportedLocales();
        
        assertTrue(locales.contains("zh"));
        assertTrue(locales.contains("en"));
    }
    
    @Test
    public void testGetLocale() {
        SimpleMsgTemplate template = SimpleMsgTemplate.fromValueString(
            "TEST", "名|Name|模板:{}|Template:{}|1");
        
        Locale locale = template.getLocale();
        
        assertEquals(MsgTemplateConfig.getDefaultLocale(), locale);
    }
    
    @Test
    public void testLocaleFallback() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
        
        SimpleMsgTemplate template = SimpleMsgTemplate.fromValueString(
            "TEST", "名|Name|中文:{}|English:{}|1");
        
        String result = template.render(Locale.JAPANESE, "值");
        
        assertEquals("中文:值", result);
    }
}