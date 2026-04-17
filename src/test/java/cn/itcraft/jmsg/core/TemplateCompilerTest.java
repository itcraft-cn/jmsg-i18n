package cn.itcraft.jmsg.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class TemplateCompilerTest {
    
    @Test
    public void testCompileSimpleNull() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple(null);
        assertEquals(CompiledTemplate.EMPTY, compiled);
    }
    
    @Test
    public void testCompileSimpleEmpty() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("");
        assertEquals(CompiledTemplate.EMPTY, compiled);
    }
    
    @Test
    public void testCompileSimpleNoPlaceholder() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("Hello World");
        
        assertEquals(1, compiled.getFragments().length);
        assertEquals("Hello World", compiled.getFragments()[0]);
        assertEquals(0, compiled.getParamCount());
        assertFalse(compiled.hasParams());
        assertEquals("Hello World", compiled.getOriginalTemplate());
    }
    
    @Test
    public void testCompileSimpleSinglePlaceholder() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("Hello {}");
        
        assertEquals(2, compiled.getFragments().length);
        assertEquals("Hello ", compiled.getFragments()[0]);
        assertEquals("", compiled.getFragments()[1]);
        assertEquals(1, compiled.getParamCount());
        assertEquals(0, compiled.getParamIndices()[0]);
        assertNull(compiled.getParamNames());
    }
    
    @Test
    public void testCompileSimpleMultiplePlaceholders() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("{} + {} = {}");
        
        assertEquals(4, compiled.getFragments().length);
        assertEquals("", compiled.getFragments()[0]);
        assertEquals(" + ", compiled.getFragments()[1]);
        assertEquals(" = ", compiled.getFragments()[2]);
        assertEquals("", compiled.getFragments()[3]);
        assertEquals(3, compiled.getParamCount());
        assertEquals(0, compiled.getParamIndices()[0]);
        assertEquals(1, compiled.getParamIndices()[1]);
        assertEquals(2, compiled.getParamIndices()[2]);
    }
    
    @Test
    public void testCompileSimplePlaceholderAtEnd() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("Value: {}");
        
        assertEquals(2, compiled.getFragments().length);
        assertEquals("Value: ", compiled.getFragments()[0]);
        assertEquals("", compiled.getFragments()[1]);
        assertEquals(1, compiled.getParamCount());
    }
    
    @Test
    public void testCompileSimplePlaceholderAtStart() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("{} is the value");
        
        assertEquals(2, compiled.getFragments().length);
        assertEquals("", compiled.getFragments()[0]);
        assertEquals(" is the value", compiled.getFragments()[1]);
        assertEquals(1, compiled.getParamCount());
    }
    
    @Test
    public void testCompileSimpleConsecutivePlaceholders() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("{}{}{}");
        
        assertEquals(4, compiled.getFragments().length);
        assertEquals("", compiled.getFragments()[0]);
        assertEquals("", compiled.getFragments()[1]);
        assertEquals("", compiled.getFragments()[2]);
        assertEquals("", compiled.getFragments()[3]);
        assertEquals(3, compiled.getParamCount());
    }
    
    @Test
    public void testCompileSimpleWithCurlyBraceInText() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("JSON: {key: {}");
        
        assertEquals(2, compiled.getFragments().length);
        assertEquals("JSON: {key: ", compiled.getFragments()[0]);
        assertEquals("", compiled.getFragments()[1]);
        assertEquals(1, compiled.getParamCount());
    }
    
    @Test
    public void testCompileSimpleRealWorld() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple(
            "用户{}于{}登录成功，IP：{}");
        
        assertEquals(4, compiled.getFragments().length);
        assertEquals("用户", compiled.getFragments()[0]);
        assertEquals("于", compiled.getFragments()[1]);
        assertEquals("登录成功，IP：", compiled.getFragments()[2]);
        assertEquals("", compiled.getFragments()[3]);
        assertEquals(3, compiled.getParamCount());
    }
    
    @Test
    public void testCompileNamedNull() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed(null);
        assertEquals(CompiledTemplate.EMPTY, compiled);
    }
    
    @Test
    public void testCompileNamedEmpty() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("");
        assertEquals(CompiledTemplate.EMPTY, compiled);
    }
    
    @Test
    public void testCompileNamedNoPlaceholder() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello World");
        
        assertEquals(1, compiled.getFragments().length);
        assertEquals("Hello World", compiled.getFragments()[0]);
        assertEquals(0, compiled.getParamCount());
        assertEquals(0, compiled.getParamNames().length);
    }
    
    @Test
    public void testCompileNamedSingleParam() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello {name}");
        
        assertEquals(2, compiled.getFragments().length);
        assertEquals("Hello ", compiled.getFragments()[0]);
        assertEquals("", compiled.getFragments()[1]);
        assertEquals(1, compiled.getParamCount());
        assertEquals("name", compiled.getParamNames()[0]);
    }
    
    @Test
    public void testCompileNamedMultipleParams() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed(
            "User {userId} logged in at {time}");
        
        assertEquals(3, compiled.getFragments().length);
        assertEquals("User ", compiled.getFragments()[0]);
        assertEquals(" logged in at ", compiled.getFragments()[1]);
        assertEquals("", compiled.getFragments()[2]);
        assertEquals(2, compiled.getParamCount());
        assertEquals("userId", compiled.getParamNames()[0]);
        assertEquals("time", compiled.getParamNames()[1]);
    }
    
    @Test
    public void testCompileNamedParamWithUnderscore() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Value: {user_id}");
        
        assertEquals(1, compiled.getParamCount());
        assertEquals("user_id", compiled.getParamNames()[0]);
    }
    
    @Test
    public void testCompileNamedParamWithDash() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Value: {user-name}");
        
        assertEquals(1, compiled.getParamCount());
        assertEquals("user-name", compiled.getParamNames()[0]);
    }
    
    @Test
    public void testCompileNamedParamAtStart() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("{name} is here");
        
        assertEquals(2, compiled.getFragments().length);
        assertEquals("", compiled.getFragments()[0]);
        assertEquals(" is here", compiled.getFragments()[1]);
        assertEquals("name", compiled.getParamNames()[0]);
    }
    
    @Test
    public void testCompileNamedParamAtEnd() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("The value is {value}");
        
        assertEquals(2, compiled.getFragments().length);
        assertEquals("The value is ", compiled.getFragments()[0]);
        assertEquals("", compiled.getFragments()[1]);
        assertEquals("value", compiled.getParamNames()[0]);
    }
    
    @Test
    public void testCompileNamedInvalidPlaceholderNoClose() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Test {name");
        
        assertEquals(1, compiled.getFragments().length);
        assertEquals("Test {name", compiled.getFragments()[0]);
        assertEquals(0, compiled.getParamCount());
    }
    
    @Test
    public void testCompileNamedInvalidPlaceholderWithSpace() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Test {name space}");
        
        assertEquals(1, compiled.getFragments().length);
        assertEquals("Test {name space}", compiled.getFragments()[0]);
        assertEquals(0, compiled.getParamCount());
    }
    
    @Test
    public void testCompileNamedEmptyPlaceholder() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Test {}");
        
        assertEquals(1, compiled.getFragments().length);
        assertEquals("Test {}", compiled.getFragments()[0]);
        assertEquals(0, compiled.getParamCount());
    }
    
    @Test
    public void testCompileNamedWithSimplePlaceholder() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Test {} and {name}");
        
        assertEquals(2, compiled.getFragments().length);
        assertEquals(1, compiled.getParamCount());
        assertEquals("name", compiled.getParamNames()[0]);
    }
    
    @Test
    public void testCompileNamedRealWorld() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed(
            "用户{userId}于{time}在{location}执行了{action}操作，结果：{result}");
        
        assertEquals(6, compiled.getFragments().length);
        assertEquals(5, compiled.getParamCount());
        assertEquals("userId", compiled.getParamNames()[0]);
        assertEquals("time", compiled.getParamNames()[1]);
        assertEquals("location", compiled.getParamNames()[2]);
        assertEquals("action", compiled.getParamNames()[3]);
        assertEquals("result", compiled.getParamNames()[4]);
    }
}