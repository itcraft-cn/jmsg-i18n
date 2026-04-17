package cn.itcraft.jmsg.benchmark;

import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.core.SimpleMsgTemplate;
import cn.itcraft.jmsg.core.NamedMsgTemplate;
import cn.itcraft.jmsg.core.CompiledTemplate;
import cn.itcraft.jmsg.core.TemplateCompiler;
import cn.itcraft.jmsg.core.TemplateRenderer;
import cn.itcraft.jmsg.util.ReflectCache;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import java.text.MessageFormat;
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
public class TemplateCompareBenchmark {
    
    private CompiledTemplate jmsgSimpleCompiled;
    private CompiledTemplate jmsgNamedCompiled;
    private Object[] simpleArgs;
    private Map<String, Object> namedArgs;
    private TestBean testBean;
    private String messageFormatPattern;
    private String slf4jStylePattern;
    
    @Setup(Level.Trial)
    public void setUp() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
        MsgTemplateConfig.setReflectCacheEnabled(true);
        ReflectCache.clear();
        
        jmsgSimpleCompiled = TemplateCompiler.compileSimple("用户{}于{}登录成功");
        jmsgNamedCompiled = TemplateCompiler.compileNamed("用户{userId}于{time}登录成功");
        
        simpleArgs = new Object[]{"admin", "2024-04-17"};
        
        namedArgs = new HashMap<>();
        namedArgs.put("userId", "admin");
        namedArgs.put("time", "2024-04-17");
        
        testBean = new TestBean();
        testBean.userId = "admin";
        testBean.time = "2024-04-17";
        
        messageFormatPattern = "用户{0}于{1}登录成功";
        slf4jStylePattern = "用户{}于{}登录成功";
    }
    
    @TearDown(Level.Trial)
    public void tearDown() {
        MsgTemplateConfig.reset();
        ReflectCache.clear();
    }
    
    @Benchmark
    public void jmsg_simple(Blackhole bh) {
        String result = TemplateRenderer.renderSimple(jmsgSimpleCompiled, simpleArgs);
        bh.consume(result);
    }
    
    @Benchmark
    public void jmsg_namedMap(Blackhole bh) {
        String result = TemplateRenderer.renderNamedMap(jmsgNamedCompiled, namedArgs);
        bh.consume(result);
    }
    
    @Benchmark
    public void jmsg_namedBean(Blackhole bh) {
        String result = TemplateRenderer.renderNamedBean(jmsgNamedCompiled, testBean);
        bh.consume(result);
    }
    
    @Benchmark
    public void jdk_messageFormat(Blackhole bh) {
        String result = MessageFormat.format(messageFormatPattern, simpleArgs);
        bh.consume(result);
    }
    
    @Benchmark
    public void jdk_messageFormat_cached(Blackhole bh) {
        MessageFormat mf = new MessageFormat(messageFormatPattern, Locale.CHINA);
        String result = mf.format(simpleArgs);
        bh.consume(result);
    }
    
    @Benchmark
    public void slf4j_style_manual(Blackhole bh) {
        String result = formatSlf4jStyle(slf4jStylePattern, simpleArgs);
        bh.consume(result);
    }
    
    @Benchmark
    public void jdk_stringConcat(Blackhole bh) {
        String result = "用户" + simpleArgs[0] + "于" + simpleArgs[1] + "登录成功";
        bh.consume(result);
    }
    
    @Benchmark
    public void jdk_stringBuilder(Blackhole bh) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户").append(simpleArgs[0]).append("于").append(simpleArgs[1]).append("登录成功");
        String result = sb.toString();
        bh.consume(result);
    }
    
    private String formatSlf4jStyle(String pattern, Object... args) {
        if (args == null || args.length == 0) {
            return pattern;
        }
        
        StringBuilder sb = new StringBuilder(pattern.length() + 64);
        int argIndex = 0;
        int i = 0;
        
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            if (c == '{' && i + 1 < pattern.length() && pattern.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    sb.append(args[argIndex] != null ? args[argIndex].toString() : "");
                } else {
                    sb.append("{}");
                }
                argIndex++;
                i += 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        
        return sb.toString();
    }
    
    public static class TestBean {
        public String userId;
        public String time;
        
        public String getUserId() { return userId; }
        public String getTime() { return time; }
    }
}