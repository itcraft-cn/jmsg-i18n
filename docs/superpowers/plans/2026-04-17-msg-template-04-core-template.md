# jmsg-i18n 实现计划 - Part 4: Template Classes

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development

---

## Task 10: SimpleMsgTemplate

**Files:**
- Create: `src/main/java/cn/itcraft/jmsg/core/SimpleMsgTemplate.java`
- Test: `src/test/java/cn/itcraft/jmsg/core/SimpleMsgTemplateTest.java`

- [ ] **Step 1: Write the failing test**

```java
package cn.itcraft.jmsg.core;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.Locale;
import java.util.Set;
import static org.junit.Assert.*;

public class SimpleMsgTemplateTest {
    
    @Before
    public void setUp() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
    }
    
    @After
    public void tearDown() {
        MsgTemplateConfig.reset();
        cn.itcraft.dyenums.core.EnumRegistry.clear();
    }
    
    @Test
    public void testFromValueStringBasic() {
        SimpleMsgTemplate template = SimpleMsgTemplate.fromValueString(
            "TEST_001", "测试|Test|中文:{}|English:{}|1");
        
        assertEquals("TEST_001", template.getCode());
        assertEquals("测试/Test", template.getName());
        assertEquals(MsgTemplate.Style.SIMPLE, template.getStyle());
        assertEquals(1, template.getOrder());
    }
    
    @Test
    public void testFromValueStringInvalidFormat() {
        try {
            SimpleMsgTemplate.fromValueString("TEST", "too|short");
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid format"));
        }
    }
    
    @Test
    public void testRenderWithDefaultLocale() {
        SimpleMsgTemplate template = SimpleMsgTemplate.fromValueString(
            "TEST_001", "测试|Test|中文:{}|English:{}|1");
        
        String result = template.render("值");
        
        assertEquals("中文:值", result);
    }
    
    @Test
    public void testRenderWithExplicitLocale() {
        SimpleMsgTemplate template = SimpleMsgTemplate.fromValueString(
            "TEST_001", "测试|Test|中文:{}|English:{}|1");
        
        String result = template.render(Locale.US, "value");
        
        assertEquals("English:value", result);
    }
    
    @Test
    public void testRenderMultipleParams() {
        SimpleMsgTemplate template = SimpleMsgTemplate.fromValueString(
            "TEST_002", "日志|Log|用户{}于{}登录|User {} logged in at {}|2");
        
        String result = template.render("admin", "2024-04-17");
        
        assertEquals("用户admin于2024-04-17登录", result);
    }
    
    @Test
    public void testGetSupportedLocales() {
        SimpleMsgTemplate template = SimpleMsgTemplate.fromValueString(
            "TEST", "名|Name|模板1:{}|Template1:{}|1");
        
        Set<String> locales = template.getSupportedLocales();
        
        assertTrue(locales.contains("zh"));
        assertTrue(locales.contains("en"));
    }
    
    @Test
    public void testGetLocale() {
        SimpleMsgTemplate template = SimpleMsgTemplate.fromValueString(
            "TEST", "名|Name|模板:{}|Template:{}|1");
        
        Locale locale = template.getLocale();
        
        assertEquals(MsgTemplateConfig.getDefaultLocale(), locale);
    }
    
    @Test
    public void testLocaleFallback() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
        
        SimpleMsgTemplate template = SimpleMsgTemplate.fromValueString(
            "TEST", "名|Name|中文:{}|English:{}|1");
        
        String result = template.render(Locale.JAPANESE, "值");
        
        assertEquals("中文:值", result);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=SimpleMsgTemplateTest -q`
Expected: FAIL (SimpleMsgTemplate class not found)

- [ ] **Step 3: Write minimal implementation**

```java
package cn.itcraft.jmsg.core;

import cn.itcraft.dyenums.core.DyEnum;
import cn.itcraft.dyenums.core.EnumRegistry;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SimpleMsgTemplate implements MsgTemplate, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String code;
    private final String name;
    private final String description;
    private final int order;
    private final Map<String, CompiledTemplate> compiledTemplates;
    
    public SimpleMsgTemplate(String code, String name, String description, int order,
                             Map<String, CompiledTemplate> compiledTemplates) {
        this.code = code;
        this.name = name;
        this.description = description != null ? description : "";
        this.order = order;
        this.compiledTemplates = compiledTemplates;
    }
    
    @Override
    public String getCode() { return code; }
    
    @Override
    public String getName() { return name; }
    
    @Override
    public String getDescription() { return description; }
    
    @Override
    public int getOrder() { return order; }
    
    @Override
    public Locale getLocale() {
        return MsgTemplateConfig.getDefaultLocale();
    }
    
    @Override
    public MsgTemplate.Style getStyle() { return MsgTemplate.Style.SIMPLE; }
    
    @Override
    public String render(Locale locale, Object... args) {
        CompiledTemplate compiled = getCompiled(locale);
        return TemplateRenderer.renderSimple(compiled, args);
    }
    
    @Override
    public String render(Object... args) {
        return render(MsgTemplateConfig.getDefaultLocale(), args);
    }
    
    public CompiledTemplate getCompiled(Locale locale) {
        return compiledTemplates.getOrDefault(
            locale.getLanguage(),
            compiledTemplates.getOrDefault(MsgTemplateConfig.getDefaultLocale().getLanguage(), 
                compiledTemplates.getOrDefault("en", CompiledTemplate.EMPTY))
        );
    }
    
    public Set<String> getSupportedLocales() {
        return compiledTemplates.keySet();
    }
    
    public static SimpleMsgTemplate fromValueString(String code, String valueString) {
        String[] parts = valueString.split("\\|", -1);
        if (parts.length < 4) {
            throw new IllegalArgumentException(
                "Invalid format. Expected: name_zh|name_en|template_zh|template_en|order");
        }
        
        String nameZh = parts[0].trim();
        String nameEn = parts[1].trim();
        int order = parts.length >= 5 ? Integer.parseInt(parts[4].trim()) : 0;
        
        Map<String, CompiledTemplate> compiled = new HashMap<>();
        compiled.put("zh", TemplateCompiler.compileSimple(parts[2].trim()));
        compiled.put("en", TemplateCompiler.compileSimple(parts[3].trim()));
        
        String displayName = nameZh.isEmpty() ? nameEn : 
                             nameEn.isEmpty() ? nameZh : nameZh + "/" + nameEn;
        
        return new SimpleMsgTemplate(code, displayName, "", order, compiled);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=SimpleMsgTemplateTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/core/SimpleMsgTemplate.java
git add src/test/java/cn/itcraft/jmsg/core/SimpleMsgTemplateTest.java
git commit -m "feat: add SimpleMsgTemplate {} style template implementation"
```

---

## Task 11: NamedMsgTemplate

**Files:**
- Create: `src/main/java/cn/itcraft/jmsg/core/NamedMsgTemplate.java`
- Test: `src/test/java/cn/itcraft/jmsg/core/NamedMsgTemplateTest.java`

- [ ] **Step 1: Write the failing test**

```java
package cn.itcraft.jmsg.core;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

public class NamedMsgTemplateTest {
    
    @Before
    public void setUp() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
    }
    
    @After
    public void tearDown() {
        MsgTemplateConfig.reset();
        cn.itcraft.dyenums.core.EnumRegistry.clear();
    }
    
    @Test
    public void testFromValueStringBasic() {
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST_001", "测试|Test|中文:{name}|English:{name}|1");
        
        assertEquals("TEST_001", template.getCode());
        assertEquals("测试/Test", template.getName());
        assertEquals(MsgTemplate.Style.NAMED, template.getStyle());
        assertEquals(1, template.getOrder());
    }
    
    @Test
    public void testFromValueStringInvalidFormat() {
        try {
            NamedMsgTemplate.fromValueString("TEST", "too|short");
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid format"));
        }
    }
    
    @Test
    public void testRenderMapWithDefaultLocale() {
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST_001", "测试|Test|中文:{name}|English:{name}|1");
        
        Map<String, Object> args = new HashMap<>();
        args.put("name", "值");
        
        String result = template.render(args);
        
        assertEquals("中文:值", result);
    }
    
    @Test
    public void testRenderMapWithExplicitLocale() {
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST_001", "测试|Test|中文:{name}|English:{name}|1");
        
        Map<String, Object> args = new HashMap<>();
        args.put("name", "value");
        
        String result = template.render(Locale.US, args);
        
        assertEquals("English:value", result);
    }
    
    @Test
    public void testRenderBeanWithDefaultLocale() {
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST_002", "日志|Log|用户{userId}登录|User {userId} logged in|2");
        
        TestUserBean bean = new TestUserBean();
        bean.userId = "admin";
        
        String result = template.render(bean);
        
        assertEquals("用户admin登录", result);
    }
    
    @Test
    public void testRenderBeanWithExplicitLocale() {
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST_002", "日志|Log|用户{userId}登录|User {userId} logged in|2");
        
        TestUserBean bean = new TestUserBean();
        bean.userId = "admin";
        
        String result = template.render(Locale.US, bean);
        
        assertEquals("User admin logged in", result);
    }
    
    @Test
    public void testRenderMultipleParamsMap() {
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST_003", "日志|Log|用户{userId}于{time}登录|User {userId} logged in at {time}|3");
        
        Map<String, Object> args = new HashMap<>();
        args.put("userId", "admin");
        args.put("time", "2024-04-17");
        
        String result = template.render(args);
        
        assertEquals("用户admin于2024-04-17登录", result);
    }
    
    @Test
    public void testRenderMultipleParamsBean() {
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST_003", "日志|Log|用户{userId}于{time}登录|User {userId} logged in at {time}|3");
        
        TestLoginBean bean = new TestLoginBean();
        bean.userId = "admin";
        bean.time = "2024-04-17";
        
        String result = template.render(bean);
        
        assertEquals("用户admin于2024-04-17登录", result);
    }
    
    @Test
    public void testGetSupportedLocales() {
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST", "名|Name|模板:{x}|Template:{x}|1");
        
        Set<String> locales = template.getSupportedLocales();
        
        assertTrue(locales.contains("zh"));
        assertTrue(locales.contains("en"));
    }
    
    @Test
    public void testRenderNullArgs() {
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST", "名|Name|模板:{x}|Template:{x}|1");
        
        String result = template.render();
        
        assertEquals("模板:{x}", result);
    }
    
    @Test
    public void testLocaleFallback() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
        
        NamedMsgTemplate template = NamedMsgTemplate.fromValueString(
            "TEST", "名|Name|中文:{x}|English:{x}|1");
        
        Map<String, Object> args = new HashMap<>();
        args.put("x", "值");
        
        String result = template.render(Locale.JAPANESE, args);
        
        assertEquals("中文:值", result);
    }
    
    public static class TestUserBean {
        public String userId;
        
        public String getUserId() { return userId; }
    }
    
    public static class TestLoginBean {
        public String userId;
        public String time;
        
        public String getUserId() { return userId; }
        public String getTime() { return time; }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=NamedMsgTemplateTest -q`
Expected: FAIL (NamedMsgTemplate class not found)

- [ ] **Step 3: Write minimal implementation**

```java
package cn.itcraft.jmsg.core;

import cn.itcraft.jmsg.util.ReflectCache;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class NamedMsgTemplate implements MsgTemplate, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String code;
    private final String name;
    private final String description;
    private final int order;
    private final Map<String, CompiledTemplate> compiledTemplates;
    
    public NamedMsgTemplate(String code, String name, String description, int order,
                            Map<String, CompiledTemplate> compiledTemplates) {
        this.code = code;
        this.name = name;
        this.description = description != null ? description : "";
        this.order = order;
        this.compiledTemplates = compiledTemplates;
    }
    
    @Override
    public String getCode() { return code; }
    
    @Override
    public String getName() { return name; }
    
    @Override
    public String getDescription() { return description; }
    
    @Override
    public int getOrder() { return order; }
    
    @Override
    public Locale getLocale() {
        return MsgTemplateConfig.getDefaultLocale();
    }
    
    @Override
    public MsgTemplate.Style getStyle() { return MsgTemplate.Style.NAMED; }
    
    @Override
    public String render(Locale locale, Object... args) {
        if (args == null || args.length == 0) {
            CompiledTemplate compiled = getCompiled(locale);
            return compiled.getFragments()[0];
        }
        
        Object arg = args[0];
        if (arg instanceof Map) {
            return renderMap(locale, (Map<String, Object>) arg);
        } else {
            return renderBean(locale, arg);
        }
    }
    
    @Override
    public String render(Object... args) {
        return render(MsgTemplateConfig.getDefaultLocale(), args);
    }
    
    public String renderMap(Locale locale, Map<String, Object> namedArgs) {
        CompiledTemplate compiled = getCompiled(locale);
        return TemplateRenderer.renderNamedMap(compiled, namedArgs);
    }
    
    public String renderBean(Locale locale, Object bean) {
        CompiledTemplate compiled = getCompiled(locale);
        return TemplateRenderer.renderNamedBean(compiled, bean);
    }
    
    public CompiledTemplate getCompiled(Locale locale) {
        return compiledTemplates.getOrDefault(
            locale.getLanguage(),
            compiledTemplates.getOrDefault(MsgTemplateConfig.getDefaultLocale().getLanguage(), 
                compiledTemplates.getOrDefault("en", CompiledTemplate.EMPTY))
        );
    }
    
    public Set<String> getSupportedLocales() {
        return compiledTemplates.keySet();
    }
    
    public static NamedMsgTemplate fromValueString(String code, String valueString) {
        String[] parts = valueString.split("\\|", -1);
        if (parts.length < 4) {
            throw new IllegalArgumentException(
                "Invalid format. Expected: name_zh|name_en|template_zh|template_en|order");
        }
        
        String nameZh = parts[0].trim();
        String nameEn = parts[1].trim();
        int order = parts.length >= 5 ? Integer.parseInt(parts[4].trim()) : 0;
        
        Map<String, CompiledTemplate> compiled = new HashMap<>();
        compiled.put("zh", TemplateCompiler.compileNamed(parts[2].trim()));
        compiled.put("en", TemplateCompiler.compileNamed(parts[3].trim()));
        
        String displayName = nameZh.isEmpty() ? nameEn : 
                             nameEn.isEmpty() ? nameZh : nameZh + "/" + nameEn;
        
        return new NamedMsgTemplate(code, displayName, "", order, compiled);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=NamedMsgTemplateTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/core/NamedMsgTemplate.java
git add src/test/java/cn/itcraft/jmsg/core/NamedMsgTemplateTest.java
git commit -m "feat: add NamedMsgTemplate {name} style template with Map/Bean support"
```

---

## 完成检查

- [ ] Task 10-11 全部通过
- [ ] 运行全部core测试: `mvn test -Dtest=*MsgTemplate* -q`
- [ ] 推送到下个计划: 05-util.md