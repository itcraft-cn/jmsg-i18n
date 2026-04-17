package cn.itcraft.jmsg.core;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import java.util.Locale;
import static org.junit.Assert.*;

public class MsgTemplateConfigTest {
    
    @Before
    public void setUp() {
        MsgTemplateConfig.reset();
    }
    
    @After
    public void tearDown() {
        MsgTemplateConfig.reset();
    }
    
    @Test
    public void testDefaultLocale() {
        Locale initial = MsgTemplateConfig.getDefaultLocale();
        assertEquals(Locale.getDefault(), initial);
    }
    
    @Test
    public void testSetDefaultLocale() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
        assertEquals(Locale.CHINA, MsgTemplateConfig.getDefaultLocale());
    }
    
    @Test
    public void testSetDefaultLocaleString() {
        MsgTemplateConfig.setDefaultLocale("zh_CN");
        Locale locale = MsgTemplateConfig.getDefaultLocale();
        assertEquals("zh", locale.getLanguage());
        assertEquals("CN", locale.getCountry());
    }
    
    @Test
    public void testSetDefaultLocaleNull() {
        Locale before = MsgTemplateConfig.getDefaultLocale();
        MsgTemplateConfig.setDefaultLocale((Locale) null);
        assertEquals(before, MsgTemplateConfig.getDefaultLocale());
    }
    
    @Test
    public void testReflectCacheEnabled() {
        assertTrue(MsgTemplateConfig.isReflectCacheEnabled());
        
        MsgTemplateConfig.setReflectCacheEnabled(false);
        assertFalse(MsgTemplateConfig.isReflectCacheEnabled());
    }
    
    @Test
    public void testReflectCacheMaxSize() {
        assertEquals(1024, MsgTemplateConfig.getReflectCacheMaxSize());
        
        MsgTemplateConfig.setReflectCacheMaxSize(2048);
        assertEquals(2048, MsgTemplateConfig.getReflectCacheMaxSize());
    }
    
    @Test
    public void testReflectCacheMaxSizeInvalid() {
        MsgTemplateConfig.setReflectCacheMaxSize(-100);
        assertEquals(1024, MsgTemplateConfig.getReflectCacheMaxSize());
        
        MsgTemplateConfig.setReflectCacheMaxSize(0);
        assertEquals(1024, MsgTemplateConfig.getReflectCacheMaxSize());
    }
    
    @Test
    public void testReset() {
        MsgTemplateConfig.setDefaultLocale(Locale.US);
        MsgTemplateConfig.setReflectCacheMaxSize(5000);
        MsgTemplateConfig.setReflectCacheEnabled(false);
        
        MsgTemplateConfig.reset();
        
        assertEquals(Locale.getDefault(), MsgTemplateConfig.getDefaultLocale());
        assertEquals(1024, MsgTemplateConfig.getReflectCacheMaxSize());
        assertTrue(MsgTemplateConfig.isReflectCacheEnabled());
    }
}