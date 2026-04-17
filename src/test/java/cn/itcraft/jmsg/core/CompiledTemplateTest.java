package cn.itcraft.jmsg.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class CompiledTemplateTest {
    
    @Test
    public void testEmptyTemplate() {
        CompiledTemplate empty = CompiledTemplate.EMPTY;
        
        assertEquals(1, empty.getFragments().length);
        assertEquals("", empty.getFragments()[0]);
        assertEquals(0, empty.getParamCount());
        assertFalse(empty.hasParams());
        assertEquals("", empty.getOriginalTemplate());
    }
    
    @Test
    public void testSimpleTemplateNoParams() {
        CompiledTemplate compiled = new CompiledTemplate(
            new String[]{"Hello World"},
            new int[0],
            null,
            "Hello World"
        );
        
        assertEquals(1, compiled.getFragments().length);
        assertEquals("Hello World", compiled.getFragments()[0]);
        assertEquals(0, compiled.getParamCount());
        assertFalse(compiled.hasParams());
    }
    
    @Test
    public void testSimpleTemplateWithParams() {
        CompiledTemplate compiled = new CompiledTemplate(
            new String[]{"Hello ", " World"},
            new int[]{0},
            null,
            "Hello {} World"
        );
        
        assertEquals(2, compiled.getFragments().length);
        assertEquals("Hello ", compiled.getFragments()[0]);
        assertEquals(" World", compiled.getFragments()[1]);
        assertEquals(1, compiled.getParamCount());
        assertTrue(compiled.hasParams());
        assertEquals(0, compiled.getParamIndices()[0]);
    }
    
    @Test
    public void testNamedTemplate() {
        CompiledTemplate compiled = new CompiledTemplate(
            new String[]{"Hello ", " World"},
            new int[]{0},
            new String[]{"name"},
            "Hello {name} World"
        );
        
        assertEquals(2, compiled.getFragments().length);
        assertEquals(1, compiled.getParamCount());
        assertEquals("name", compiled.getParamNames()[0]);
        assertEquals("Hello {name} World", compiled.getOriginalTemplate());
    }
    
    @Test
    public void testMultipleParams() {
        CompiledTemplate compiled = new CompiledTemplate(
            new String[]{"User ", " logged in at ", ""},
            new int[]{0, 1},
            new String[]{"userId", "time"},
            "User {userId} logged in at {time}"
        );
        
        assertEquals(3, compiled.getFragments().length);
        assertEquals(2, compiled.getParamCount());
        assertEquals("userId", compiled.getParamNames()[0]);
        assertEquals("time", compiled.getParamNames()[1]);
    }
    
    @Test
    public void testParamIndicesNotNull() {
        CompiledTemplate compiled = new CompiledTemplate(
            new String[]{"test"},
            new int[]{0},
            null,
            "test{}"
        );
        
        assertNotNull(compiled.getParamIndices());
        assertEquals(1, compiled.getParamIndices().length);
    }
}