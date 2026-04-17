package cn.itcraft.jmsg.util;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class StringBuilderPoolTest {
    
    @After
    public void tearDown() {
        StringBuilderPool.clear();
    }
    
    @Test
    public void testAcquireBasic() {
        StringBuilder sb = StringBuilderPool.acquire();
        
        assertNotNull(sb);
        assertEquals(0, sb.length());
    }
    
    @Test
    public void testAcquireWithSize() {
        StringBuilder sb = StringBuilderPool.acquire(500);
        
        assertNotNull(sb);
        assertTrue(sb.capacity() >= 500);
        assertEquals(0, sb.length());
    }
    
    @Test
    public void testAcquireReuse() {
        StringBuilder sb1 = StringBuilderPool.acquire();
        sb1.append("test content");
        String result1 = StringBuilderPool.releaseAndToString(sb1);
        
        StringBuilder sb2 = StringBuilderPool.acquire();
        
        assertEquals(0, sb2.length());
        assertEquals(sb1, sb2);
        assertEquals("test content", result1);
    }
    
    @Test
    public void testReleaseAndToString() {
        StringBuilder sb = StringBuilderPool.acquire();
        sb.append("Hello").append(" ").append("World");
        
        String result = StringBuilderPool.releaseAndToString(sb);
        
        assertEquals("Hello World", result);
    }
    
    @Test
    public void testGetCurrentCapacity() {
        StringBuilderPool.acquire(100);
        int capacity = StringBuilderPool.getCurrentCapacity();
        
        assertTrue(capacity >= 256);
    }
    
    @Test
    public void testClear() {
        StringBuilderPool.acquire(1000);
        
        StringBuilderPool.clear();
        
        StringBuilder sb2 = StringBuilderPool.acquire();
        int cap2 = sb2.capacity();
        
        assertEquals(256, cap2);
    }
}