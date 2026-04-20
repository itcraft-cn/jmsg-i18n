package cn.itcraft.jmsg.benchmark;

import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.core.SimpleMsgTemplate;
import cn.itcraft.jmsg.core.NamedMsgTemplate;
import cn.itcraft.jmsg.core.CompiledTemplate;
import cn.itcraft.jmsg.core.TemplateCompiler;
import cn.itcraft.jmsg.core.TemplateRenderer;
import cn.itcraft.jmsg.util.StringBuilderPool;
import cn.itcraft.jmsg.util.ReflectCache;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 20, timeUnit = TimeUnit.MILLISECONDS)
@Threads(4)
@Fork(3)
@State(Scope.Thread)
public class MsgTemplateBenchmark {
    
    private SimpleMsgTemplate simpleTemplate;
    private NamedMsgTemplate namedTemplate;
    private CompiledTemplate simpleCompiled;
    private CompiledTemplate namedCompiled;
    private Object[] simpleArgs;
    private Map<String, Object> namedArgs;
    private TestBean testBean;
    
    @Setup(Level.Trial)
    public void setUp() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
        MsgTemplateConfig.setReflectCacheEnabled(true);
        ReflectCache.clear();
        
        simpleCompiled = TemplateCompiler.compileSimple("用户{}于{}登录");
        namedCompiled = TemplateCompiler.compileNamed("用户{userId}于{time}登录");
        
        Map<String, CompiledTemplate> simpleCompiledMap = new HashMap<>();
        simpleCompiledMap.put("zh_CN", simpleCompiled);
        simpleCompiledMap.put("en_US", TemplateCompiler.compileSimple("User {} logged in at {}"));
        
        Map<String, CompiledTemplate> namedCompiledMap = new HashMap<>();
        namedCompiledMap.put("zh_CN", namedCompiled);
        namedCompiledMap.put("en_US", TemplateCompiler.compileNamed("User {userId} logged in at {time}"));
        
        simpleTemplate = new SimpleMsgTemplate("TEST", "测试", "", 1, Locale.CHINA, simpleCompiledMap);
        namedTemplate = new NamedMsgTemplate("TEST", "测试", "", 1, Locale.CHINA, namedCompiledMap);
        
        simpleArgs = new Object[]{"admin", "2024-04-17"};
        
        namedArgs = new HashMap<>();
        namedArgs.put("userId", "admin");
        namedArgs.put("time", "2024-04-17");
        
        testBean = new TestBean();
        testBean.userId = "admin";
        testBean.time = "2024-04-17";
    }
    
    @TearDown(Level.Trial)
    public void tearDown() {
        MsgTemplateConfig.reset();
        ReflectCache.clear();
    }
    
    @Benchmark
    public void simpleTemplate_render(Blackhole bh) {
        String result = simpleTemplate.render(simpleArgs);
        bh.consume(result);
    }
    
    @Benchmark
    public void namedTemplate_renderMap(Blackhole bh) {
        String result = namedTemplate.render(namedArgs);
        bh.consume(result);
    }
    
    @Benchmark
    public void namedTemplate_renderBean(Blackhole bh) {
        String result = namedTemplate.render(testBean);
        bh.consume(result);
    }
    
    @Benchmark
    public void templateRenderer_renderSimple(Blackhole bh) {
        String result = TemplateRenderer.renderSimple(simpleCompiled, simpleArgs);
        bh.consume(result);
    }
    
    @Benchmark
    public void templateRenderer_renderNamedMap(Blackhole bh) {
        String result = TemplateRenderer.renderNamedMap(namedCompiled, namedArgs);
        bh.consume(result);
    }
    
    @Benchmark
    public void templateRenderer_renderNamedBean(Blackhole bh) {
        String result = TemplateRenderer.renderNamedBean(namedCompiled, testBean);
        bh.consume(result);
    }
    
    @Benchmark
    public void stringBuilderPool_acquireRelease(Blackhole bh) {
        StringBuilder sb = StringBuilderPool.acquire();
        sb.append("test").append(123);
        String result = StringBuilderPool.releaseAndToString(sb);
        bh.consume(result);
    }
    
    @Benchmark
    @Threads(1)
    public void reflectCache_getProperty(Blackhole bh) {
        Object value = ReflectCache.getProperty(testBean, "userId");
        bh.consume(value);
    }
    
    @Benchmark
    @Threads(1)
    public void reflectCache_getPropertyNoCache(Blackhole bh) {
        MsgTemplateConfig.setReflectCacheEnabled(false);
        Object value = ReflectCache.getPropertyNoCache(testBean, "userId");
        bh.consume(value);
        MsgTemplateConfig.setReflectCacheEnabled(true);
    }
    
    @Benchmark
    public void templateCompiler_compileSimple(Blackhole bh) {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("用户{}于{}登录成功");
        bh.consume(compiled);
    }
    
    @Benchmark
    public void templateCompiler_compileNamed(Blackhole bh) {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("用户{userId}于{time}登录成功");
        bh.consume(compiled);
    }
    
    public static class TestBean {
        public String userId;
        public String time;
        
        public String getUserId() { return userId; }
        public String getTime() { return time; }
    }
}