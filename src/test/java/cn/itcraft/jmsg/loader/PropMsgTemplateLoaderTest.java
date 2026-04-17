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
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import static org.junit.Assert.*;

public class PropMsgTemplateLoaderTest {
    
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
    public void testLoadSimpleFromProperties() {
        Properties props = new Properties();
        props.setProperty("TEST_001", "测试|Test|中文:{}|English:{}|1");
        
        PropMsgTemplateLoader loader = new PropMsgTemplateLoader(props, MsgTemplate.Style.SIMPLE);
        
        int count = loader.load(MsgTemplate.class, null);
        
        assertEquals(1, count);
    }
    
    @Test
    public void testLoadSimpleAndRegistry() {
        Properties props = new Properties();
        props.setProperty("TEST_001", "测试|Test|中文:{}|English:{}|1");
        
        PropMsgTemplateLoader loader = new PropMsgTemplateLoader(props, MsgTemplate.Style.SIMPLE);
        loader.load(MsgTemplate.class, null);
        
        SimpleMsgTemplate template = EnumRegistry.valueOf(SimpleMsgTemplate.class, "TEST_001")
            .orElse(null);
        
        assertNotNull(template);
        assertEquals("中文:值", template.render("值"));
    }
    
    @Test
    public void testLoadNamedFromProperties() {
        Properties props = new Properties();
        props.setProperty("TEST_001", "测试|Test|中文:{name}|English:{name}|1");
        
        PropMsgTemplateLoader loader = new PropMsgTemplateLoader(props, MsgTemplate.Style.NAMED);
        
        int count = loader.load(MsgTemplate.class, null);
        
        assertEquals(1, count);
    }
    
    @Test
    public void testLoadNamedAndRegistry() {
        Properties props = new Properties();
        props.setProperty("TEST_001", "测试|Test|中文:{name}|English:{name}|1");
        
        PropMsgTemplateLoader loader = new PropMsgTemplateLoader(props, MsgTemplate.Style.NAMED);
        loader.load(MsgTemplate.class, null);
        
        NamedMsgTemplate template = EnumRegistry.valueOf(NamedMsgTemplate.class, "TEST_001")
            .orElse(null);
        
        assertNotNull(template);
        Map<String, Object> args = new HashMap<>();
        args.put("name", "值");
        assertEquals("中文:值", template.render(args));
    }
    
    @Test
    public void testValidateSourceEmpty() {
        Properties props = new Properties();
        
        PropMsgTemplateLoader loader = new PropMsgTemplateLoader(props, MsgTemplate.Style.SIMPLE);
        
        assertFalse(loader.validateSource());
    }
    
    @Test
    public void testValidateSourceNull() {
        PropMsgTemplateLoader loader = new PropMsgTemplateLoader(null, MsgTemplate.Style.SIMPLE);
        
        assertFalse(loader.validateSource());
    }
    
    @Test
    public void testValidateSourceNotEmpty() {
        Properties props = new Properties();
        props.setProperty("TEST", "value");
        
        PropMsgTemplateLoader loader = new PropMsgTemplateLoader(props, MsgTemplate.Style.SIMPLE);
        
        assertTrue(loader.validateSource());
    }
    
    @Test
    public void testLoadMultipleTemplates() {
        Properties props = new Properties();
        props.setProperty("TEST_001", "测试1|Test1|中文1:{}|English1:{}|1");
        props.setProperty("TEST_002", "测试2|Test2|中文2:{}|English2:{}|2");
        props.setProperty("TEST_003", "测试3|Test3|中文3:{}|English3:{}|3");
        
        PropMsgTemplateLoader loader = new PropMsgTemplateLoader(props, MsgTemplate.Style.SIMPLE);
        
        int count = loader.load(MsgTemplate.class, null);
        
        assertEquals(3, count);
    }
}