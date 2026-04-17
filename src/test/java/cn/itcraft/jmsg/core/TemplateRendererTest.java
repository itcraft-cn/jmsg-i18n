package cn.itcraft.jmsg.core;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class TemplateRendererTest {
    
    @Test
    public void testRenderSimpleNoParams() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("Hello World");
        
        String result = TemplateRenderer.renderSimple(compiled, null);
        
        assertEquals("Hello World", result);
    }
    
    @Test
    public void testRenderSimpleEmpty() {
        CompiledTemplate compiled = CompiledTemplate.EMPTY;
        
        String result = TemplateRenderer.renderSimple(compiled, null);
        
        assertEquals("", result);
    }
    
    @Test
    public void testRenderSimpleSingleArg() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("Hello {}");
        
        String result = TemplateRenderer.renderSimple(compiled, new Object[]{"World"});
        
        assertEquals("Hello World", result);
    }
    
    @Test
    public void testRenderSimpleMultipleArgs() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("{} + {} = {}");
        
        String result = TemplateRenderer.renderSimple(compiled, new Object[]{1, 2, 3});
        
        assertEquals("1 + 2 = 3", result);
    }
    
    @Test
    public void testRenderSimpleArgNull() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("Value: {}");
        
        String result = TemplateRenderer.renderSimple(compiled, new Object[]{null});
        
        assertEquals("Value: {}", result);
    }
    
    @Test
    public void testRenderSimpleNotEnoughArgs() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("{} + {} = {}");
        
        String result = TemplateRenderer.renderSimple(compiled, new Object[]{1});
        
        assertEquals("1 + {} = {}", result);
    }
    
    @Test
    public void testRenderSimpleChinese() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("用户{}于{}登录成功");
        
        String result = TemplateRenderer.renderSimple(compiled, new Object[]{"admin", "2024-04-17"});
        
        assertEquals("用户admin于2024-04-17登录成功", result);
    }
    
    @Test
    public void testRenderNamedMapNoParams() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello World");
        
        String result = TemplateRenderer.renderNamedMap(compiled, null);
        
        assertEquals("Hello World", result);
    }
    
    @Test
    public void testRenderNamedMapSingleParam() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello {name}");
        
        Map<String, Object> args = new HashMap<>();
        args.put("name", "World");
        
        String result = TemplateRenderer.renderNamedMap(compiled, args);
        
        assertEquals("Hello World", result);
    }
    
    @Test
    public void testRenderNamedMapMultipleParams() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed(
            "User {userId} logged in at {time}");
        
        Map<String, Object> args = new HashMap<>();
        args.put("userId", "admin");
        args.put("time", "2024-04-17 10:30:00");
        
        String result = TemplateRenderer.renderNamedMap(compiled, args);
        
        assertEquals("User admin logged in at 2024-04-17 10:30:00", result);
    }
    
    @Test
    public void testRenderNamedMapMissingParam() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello {name}");
        
        Map<String, Object> args = new HashMap<>();
        
        String result = TemplateRenderer.renderNamedMap(compiled, args);
        
        assertEquals("Hello {name}", result);
    }
    
    @Test
    public void testRenderNamedMapNullArgs() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello {name}");
        
        String result = TemplateRenderer.renderNamedMap(compiled, null);
        
        assertEquals("Hello {name}", result);
    }
    
    @Test
    public void testRenderNamedMapChinese() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed(
            "用户{userId}于{time}登录成功");
        
        Map<String, Object> args = new HashMap<>();
        args.put("userId", "管理员");
        args.put("time", "2024-04-17");
        
        String result = TemplateRenderer.renderNamedMap(compiled, args);
        
        assertEquals("用户管理员于2024-04-17登录成功", result);
    }
    
    @Test
    public void testRenderNamedBeanNoParams() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello World");
        
        Object bean = new Object();
        String result = TemplateRenderer.renderNamedBean(compiled, bean);
        
        assertEquals("Hello World", result);
    }
    
    @Test
    public void testRenderNamedBeanNullBean() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello {name}");
        
        String result = TemplateRenderer.renderNamedBean(compiled, null);
        
        assertEquals("Hello {name}", result);
    }
    
    @Test
    public void testRenderNamedBeanSingleParam() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello {name}");
        
        RenderTestBean bean = new RenderTestBean();
        bean.name = "World";
        
        String result = TemplateRenderer.renderNamedBean(compiled, bean);
        
        assertEquals("Hello World", result);
    }
    
    @Test
    public void testRenderNamedBeanMultipleParams() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed(
            "User {userId} logged in at {time}");
        
        LoginBean bean = new LoginBean();
        bean.userId = "admin";
        bean.time = "2024-04-17 10:30:00";
        
        String result = TemplateRenderer.renderNamedBean(compiled, bean);
        
        assertEquals("User admin logged in at 2024-04-17 10:30:00", result);
    }
    
    @Test
    public void testRenderNamedBeanBooleanProperty() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Status: {active}");
        
        BooleanBean bean = new BooleanBean();
        bean.active = true;
        
        String result = TemplateRenderer.renderNamedBean(compiled, bean);
        
        assertEquals("Status: true", result);
    }
    
    public static class RenderTestBean {
        public String name;
        
        public String getName() { return name; }
    }
    
    public static class LoginBean {
        public String userId;
        public String time;
        
        public String getUserId() { return userId; }
        public String getTime() { return time; }
    }
    
    public static class BooleanBean {
        public boolean active;
        
        public boolean isActive() { return active; }
    }
}