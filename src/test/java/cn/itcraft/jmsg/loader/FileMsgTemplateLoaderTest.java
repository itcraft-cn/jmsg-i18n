package cn.itcraft.jmsg.loader;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.core.SimpleMsgTemplate;
import cn.itcraft.jmsg.core.NamedMsgTemplate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import static org.junit.Assert.*;

public class FileMsgTemplateLoaderTest {
    
    @Before
    public void setUp() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
    }
    
    @After
    public void tearDown() {
        MsgTemplateConfig.reset();
        EnumRegistry.clear();
    }
    
    @Test
    public void testLoadSimple() {
        FileMsgTemplateLoader loader = FileMsgTemplateLoader.forSimple(
            "msg_templates_simple.properties");
        
        int count = loader.load(MsgTemplate.class, null);
        
        assertEquals(2, count);
    }
    
    @Test
    public void testLoadSimpleAndRegistry() {
        FileMsgTemplateLoader loader = FileMsgTemplateLoader.forSimple(
            "msg_templates_simple.properties");
        loader.load(MsgTemplate.class, null);
        
        SimpleMsgTemplate template = EnumRegistry.valueOf(SimpleMsgTemplate.class, "TEST_001")
            .orElse(null);
        
        assertNotNull(template);
        assertEquals("中文:值", template.render("值"));
    }
    
    @Test
    public void testLoadNamed() {
        FileMsgTemplateLoader loader = FileMsgTemplateLoader.forNamed(
            "msg_templates_named.properties");
        
        int count = loader.load(MsgTemplate.class, null);
        
        assertEquals(2, count);
    }
    
    @Test
    public void testLoadNamedAndRegistry() {
        FileMsgTemplateLoader loader = FileMsgTemplateLoader.forNamed(
            "msg_templates_named.properties");
        loader.load(MsgTemplate.class, null);
        
        NamedMsgTemplate template = EnumRegistry.valueOf(NamedMsgTemplate.class, "TEST_001")
            .orElse(null);
        
        assertNotNull(template);
        Map<String, Object> args = new HashMap<>();
        args.put("name", "值");
        assertEquals("中文:值", template.render(args));
    }
    
    @Test
    public void testValidateSourceExists() {
        FileMsgTemplateLoader loader = FileMsgTemplateLoader.forSimple(
            "msg_templates_simple.properties");
        
        assertTrue(loader.validateSource());
    }
    
    @Test
    public void testValidateSourceNotExists() {
        FileMsgTemplateLoader loader = FileMsgTemplateLoader.forSimple(
            "nonexistent.properties");
        
        assertFalse(loader.validateSource());
    }
    
    @Test
    public void testLoadFileNotFound() {
        FileMsgTemplateLoader loader = FileMsgTemplateLoader.forSimple(
            "nonexistent.properties");
        
        try {
            loader.load(MsgTemplate.class, null);
            fail("Should throw RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Failed to load") || e.getMessage().contains("not found"));
        }
    }
    
    @Test
    public void testDefaultConstructor() {
        FileMsgTemplateLoader loader = new FileMsgTemplateLoader("msg_templates_simple.properties");
        
        assertEquals(MsgTemplate.Style.SIMPLE, loader.getStyle());
    }
}