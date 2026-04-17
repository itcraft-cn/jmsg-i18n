package cn.itcraft.jmsg.builder;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.core.SimpleMsgTemplate;
import cn.itcraft.jmsg.loader.FileMsgTemplateLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.Locale;
import static org.junit.Assert.*;

public class SimpleBuilderTest {
    
    @Before
    public void setUp() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
        FileMsgTemplateLoader.forSimple("msg_templates_simple.properties")
            .load(MsgTemplate.class, null);
    }
    
    @After
    public void tearDown() {
        MsgTemplateConfig.reset();
        EnumRegistry.clear();
    }
    
    @Test
    public void testRenderWithDefaultLocale() {
        String result = MsgTemplateBuilder.create()
            .code("TEST_001")
            .simple()
            .args("值")
            .render();
        
        assertEquals("中文:值", result);
    }
    
    @Test
    public void testRenderWithExplicitLocale() {
        String result = MsgTemplateBuilder.create()
            .code("TEST_001")
            .locale(Locale.US)
            .simple()
            .args("value")
            .render();
        
        assertEquals("English:value", result);
    }
    
    @Test
    public void testRenderMultipleArgs() {
        String result = MsgTemplateBuilder.create()
            .code("TEST_002")
            .simple()
            .args("admin", "2024-04-17")
            .render();
        
        assertEquals("用户admin于2024-04-17登录", result);
    }
    
    @Test
    public void testRenderWithLocaleMethod() {
        String result = MsgTemplateBuilder.create()
            .code("TEST_001")
            .simple()
            .args("value")
            .render(Locale.US);
        
        assertEquals("English:value", result);
    }
    
    @Test
    public void testRenderCodeRequired() {
        try {
            MsgTemplateBuilder.create()
                .simple()
                .args("test")
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
                .simple()
                .args("test")
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
            .simple()
            .args("测试值")
            .render();
        
        assertEquals("中文:测试值", result);
    }
}