package cn.itcraft.jmsg.loader;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.core.NamedMsgTemplate;
import cn.itcraft.jmsg.builder.MsgTemplateBuilder;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class YamlMsgTemplateLoaderTest {
    
    @Before
    public void setup() {
        EnumRegistry.clear();
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
    }
    
    @After
    public void teardown() {
        EnumRegistry.clear();
    }
    
    @Test
    public void testLoadSimple() {
        YamlMsgTemplateLoader loader = YamlMsgTemplateLoader.forSimple("templates_simple.yaml");
        int count = loader.load(MsgTemplate.class, null);
        assertEquals(2, count);
        
        String msg = MsgTemplateBuilder.create()
            .code("ERR_001")
            .simple()
            .args("数据库超时")
            .render();
        assertEquals("内部错误:数据库超时", msg);
    }
    
    @Test
    public void testLoadNamed() {
        YamlMsgTemplateLoader loader = YamlMsgTemplateLoader.forNamed("templates_named.yaml");
        int count = loader.load(MsgTemplate.class, null);
        assertEquals(2, count);
        
        Map<String, Object> args = new HashMap<>();
        args.put("reason", "timeout");
        
        String msg = MsgTemplateBuilder.create()
            .code("ERR_001")
            .named()
            .args(args)
            .render();
        assertEquals("内部错误:timeout", msg);
    }
    
    @Test
    public void testLocaleFallback() {
        YamlMsgTemplateLoader.forSimple("templates_simple.yaml").load(MsgTemplate.class, null);
        
        String msg = MsgTemplateBuilder.create()
            .code("ERR_001")
            .locale(Locale.US)
            .simple()
            .args("timeout")
            .render();
        assertEquals("Internal error:timeout", msg);
        
        msg = MsgTemplateBuilder.create()
            .code("ERR_001")
            .locale(Locale.UK)
            .simple()
            .args("timeout")
            .render();
        assertEquals("Internal error:timeout", msg);
    }
    
    @Test
    public void testSupportedLocales() {
        YamlMsgTemplateLoader.forNamed("templates_named.yaml").load(MsgTemplate.class, null);
        
        NamedMsgTemplate template = EnumRegistry.valueOf(NamedMsgTemplate.class, "ERR_001").orElse(null);
        assertNotNull(template);
        
        assertTrue(template.hasLocale(Locale.CHINA));
        assertTrue(template.hasLocale(Locale.US));
        assertTrue(template.hasLocale(Locale.UK));
    }
}