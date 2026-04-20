package cn.itcraft.jmsg.core;

import org.junit.Test;
import java.util.Locale;
import java.util.Set;
import static org.junit.Assert.*;

public class MsgTemplateTest {
    
    @Test
    public void testStyleEnumExists() {
        MsgTemplate.Style simple = MsgTemplate.Style.SIMPLE;
        MsgTemplate.Style named = MsgTemplate.Style.NAMED;
        
        assertEquals("SIMPLE", simple.name());
        assertEquals("NAMED", named.name());
    }
    
    @Test
    public void testInterfaceMethods() {
        MsgTemplate template = new MockMsgTemplate();
        
        assertEquals("TEST_CODE", template.getCode());
        assertEquals("Test Template", template.getName());
        assertNotNull(template.getStyle());
        assertNotNull(template.getLocale());
        assertNotNull(template.getDefaultLocale());
        assertTrue(template.hasLocale(Locale.CHINA));
    }
    
    private static class MockMsgTemplate implements MsgTemplate {
        @Override
        public String getCode() { return "TEST_CODE"; }
        
        @Override
        public String getName() { return "Test Template"; }
        
        @Override
        public String getDescription() { return ""; }
        
        @Override
        public int getOrder() { return 0; }
        
        @Override
        public Locale getLocale() { return Locale.getDefault(); }
        
        @Override
        public Locale getDefaultLocale() { return Locale.CHINA; }
        
        @Override
        public MsgTemplate.Style getStyle() { return MsgTemplate.Style.SIMPLE; }
        
        @Override
        public String render(Locale locale, Object... args) { return ""; }
        
        @Override
        public String render(Object... args) { return ""; }
        
        @Override
        public Set<String> getSupportedLocales() { return java.util.Collections.singleton("zh_CN"); }
        
        @Override
        public boolean hasLocale(Locale locale) { return locale.equals(Locale.CHINA); }
    }
}