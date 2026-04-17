package cn.itcraft.jmsg.util;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReflectCacheTest {
    
    @After
    public void tearDown() {
        ReflectCache.clear();
        ReflectCache.setMaxSize(1024);
    }
    
    @Test
    public void testGetPropertyBasicGetter() {
        TestBean bean = new TestBean();
        bean.name = "test";
        
        Object value = ReflectCache.getProperty(bean, "name");
        
        assertEquals("test", value);
    }
    
    @Test
    public void testGetPropertyIsGetter() {
        TestBean bean = new TestBean();
        bean.active = true;
        
        Object value = ReflectCache.getProperty(bean, "active");
        
        assertEquals(true, value);
    }
    
    @Test
    public void testGetPropertyNullBean() {
        Object value = ReflectCache.getProperty(null, "name");
        
        assertNull(value);
    }
    
    @Test
    public void testGetPropertyNullPropertyName() {
        TestBean bean = new TestBean();
        
        Object value = ReflectCache.getProperty(bean, null);
        
        assertNull(value);
    }
    
    @Test
    public void testGetPropertyEmptyPropertyName() {
        TestBean bean = new TestBean();
        
        Object value = ReflectCache.getProperty(bean, "");
        
        assertNull(value);
    }
    
    @Test
    public void testGetPropertyNonExistent() {
        TestBean bean = new TestBean();
        
        Object value = ReflectCache.getProperty(bean, "nonexistent");
        
        assertNull(value);
    }
    
    @Test
    public void testGetPropertyCached() {
        ReflectCache.clear();
        
        TestBean bean = new TestBean();
        bean.name = "first";
        
        ReflectCache.getProperty(bean, "name");
        boolean cached = ReflectCache.contains(TestBean.class, "name");
        
        assertTrue(cached);
        
        TestBean bean2 = new TestBean();
        bean2.name = "second";
        
        Object value = ReflectCache.getProperty(bean2, "name");
        
        assertEquals("second", value);
    }
    
    @Test
    public void testGetMaxSize() {
        assertEquals(1024, ReflectCache.getMaxSize());
    }
    
    @Test
    public void testSetMaxSize() {
        ReflectCache.setMaxSize(500);
        
        assertEquals(500, ReflectCache.getMaxSize());
    }
    
    @Test
    public void testSetMaxSizeInvalid() {
        ReflectCache.setMaxSize(-100);
        
        assertEquals(1024, ReflectCache.getMaxSize());
    }
    
    @Test
    public void testClear() {
        TestBean bean = new TestBean();
        ReflectCache.getProperty(bean, "name");
        
        ReflectCache.clear();
        
        assertEquals(0, ReflectCache.size());
    }
    
    @Test
    public void testGetPropertyFromSuperclass() {
        ChildBean child = new ChildBean();
        child.name = "child";
        
        Object value = ReflectCache.getProperty(child, "name");
        
        assertEquals("child", value);
    }
    
    public static class TestBean {
        public String name;
        public boolean active;
        
        public String getName() { return name; }
        public boolean isActive() { return active; }
    }
    
    public static class ChildBean extends TestBean {
    }
}