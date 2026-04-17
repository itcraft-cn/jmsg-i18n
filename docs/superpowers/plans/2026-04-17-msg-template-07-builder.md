# jmsg-i18n 实现计划 - Part 7: Builder

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development

---

## Task 16: MsgTemplateBuilder

**Files:**
- Create: `src/main/java/cn/itcraft/jmsg/builder/MsgTemplateBuilder.java`

- [ ] **Step 1: Write minimal implementation**

```java
package cn.itcraft.jmsg.builder;

import cn.itcraft.jmsg.util.LocaleHelper;
import cn.itcraft.jmsg.builder.SimpleBuilder;
import cn.itcraft.jmsg.builder.NamedBuilder;
import java.util.Locale;

public class MsgTemplateBuilder {
    
    private String code;
    private Locale locale;
    
    public MsgTemplateBuilder code(String code) {
        this.code = code;
        return this;
    }
    
    public MsgTemplateBuilder locale(Locale locale) {
        this.locale = locale;
        return this;
    }
    
    public MsgTemplateBuilder locale(String localeCode) {
        this.locale = LocaleHelper.parse(localeCode);
        return this;
    }
    
    public SimpleBuilder simple() {
        return new SimpleBuilder(code, locale);
    }
    
    public NamedBuilder named() {
        return new NamedBuilder(code, locale);
    }
    
    public static MsgTemplateBuilder create() {
        return new MsgTemplateBuilder();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/builder/MsgTemplateBuilder.java
git commit -m "feat: add MsgTemplateBuilder entry point for Simple/Named builders"
```

---

## Task 17: SimpleBuilder

**Files:**
- Create: `src/main/java/cn/itcraft/jmsg/builder/SimpleBuilder.java`
- Test: `src/test/java/cn/itcraft/jmsg/builder/SimpleBuilderTest.java`

- [ ] **Step 1: Write the failing test**

```java
package cn.itcraft.jmsg.builder;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.core.SimpleMsgTemplate;
import cn.itcraft.jmsg.loader.FileMsgTemplateLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.Locale;
import static org.junit.Assert.*;

public class SimpleBuilderTest {
    
    @Before
    public void setUp() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
        FileMsgTemplateLoader.forSimple("msg_templates_simple.properties")
            .load(cn.itcraft.jmsg.core.MsgTemplate.class, null);
    }
    
    @After
    public void tearDown() {
        MsgTemplateConfig.reset();
        EnumRegistry.clear();
    }
    
    @Test
    public void testRenderWithDefaultLocale() {
        String result = MsgTemplateBuilder.create()
            .code("TEST_001")
            .simple()
            .args("值")
            .render();
        
        assertEquals("中文:值", result);
    }
    
    @Test
    public void testRenderWithExplicitLocale() {
        String result = MsgTemplateBuilder.create()
            .code("TEST_001")
            .locale(Locale.US)
            .simple()
            .args("value")
            .render();
        
        assertEquals("English:value", result);
    }
    
    @Test
    public void testRenderMultipleArgs() {
        String result = MsgTemplateBuilder.create()
            .code("TEST_002")
            .simple()
            .args("admin", "2024-04-17")
            .render();
        
        assertEquals("用户admin于2024-04-17登录", result);
    }
    
    @Test
    public void testRenderWithLocaleMethod() {
        String result = MsgTemplateBuilder.create()
            .code("TEST_001")
            .simple()
            .args("value")
            .render(Locale.US);
        
        assertEquals("English:value", result);
    }
    
    @Test
    public void testRenderCodeRequired() {
        try {
            MsgTemplateBuilder.create()
                .simple()
                .args("test")
                .render();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Code is required"));
        }
    }
    
    @Test
    public void testRenderTemplateNotFound() {
        try {
            MsgTemplateBuilder.create()
                .code("NONEXISTENT")
                .simple()
                .args("test")
                .render();
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("not found"));
        }
    }
    
    @Test
    public void testBuilderChaining() {
        String result = MsgTemplateBuilder.create()
            .code("TEST_001")
            .locale(Locale.CHINA)
            .simple()
            .args("测试值")
            .render();
        
        assertEquals("中文:测试值", result);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=SimpleBuilderTest -q`
Expected: FAIL (SimpleBuilder class not found)

- [ ] **Step 3: Write minimal implementation**

```java
package cn.itcraft.jmsg.builder;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.core.SimpleMsgTemplate;
import java.util.Locale;

public class SimpleBuilder {
    
    private final String code;
    private final Locale locale;
    private Object[] args;
    
    SimpleBuilder(String code, Locale locale) {
        this.code = code;
        this.locale = locale;
    }
    
    public SimpleBuilder args(Object... args) {
        this.args = args;
        return this;
    }
    
    public String render() {
        if (code == null) {
            throw new IllegalStateException("Code is required");
        }
        
        MsgTemplate template = findTemplate(code);
        if (template.getStyle() != MsgTemplate.Style.SIMPLE) {
            throw new IllegalArgumentException("Template is not SIMPLE style: " + code);
        }
        
        Locale renderLocale = locale != null ? locale : MsgTemplateConfig.getDefaultLocale();
        return template.render(renderLocale, args);
    }
    
    public String render(Locale locale) {
        if (code == null) {
            throw new IllegalStateException("Code is required");
        }
        
        MsgTemplate template = findTemplate(code);
        return template.render(locale, args);
    }
    
    private MsgTemplate findTemplate(String code) {
        MsgTemplate template = EnumRegistry.valueOf(SimpleMsgTemplate.class, code)
            .orElse(null);
        if (template == null) {
            template = EnumRegistry.valueOf(MsgTemplate.class, code)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + code));
        }
        return template;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=SimpleBuilderTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/builder/SimpleBuilder.java
git add src/test/java/cn/itcraft/jmsg/builder/SimpleBuilderTest.java
git commit -m "feat: add SimpleBuilder for {} style template rendering"
```

---

## Task 18: NamedBuilder

**Files:**
- Create: `src/main/java/cn/itcraft/jmsg/builder/NamedBuilder.java`
- Test: `src/test/java/cn/itcraft/jmsg/builder/NamedBuilderTest.java`

- [ ] **Step 1: Write the failing test**

```java
package cn.itcraft.jmsg.builder;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.core.NamedMsgTemplate;
import cn.itcraft.jmsg.loader.FileMsgTemplateLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import static org.junit.Assert.*;

public class NamedBuilderTest {
    
    @Before
    public void setUp() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
        FileMsgTemplateLoader.forNamed("msg_templates_named.properties")
            .load(cn.itcraft.jmsg.core.MsgTemplate.class, null);
    }
    
    @After
    public void tearDown() {
        MsgTemplateConfig.reset();
        EnumRegistry.clear();
    }
    
    @Test
    public void testRenderMapWithDefaultLocale() {
        String result = MsgTemplateBuilder.create()
            .code("TEST_001")
            .named()
            .arg("name", "值")
            .render();
        
        assertEquals("中文:值", result);
    }
    
    @Test
    public void testRenderMapWithExplicitLocale() {
        String result = MsgTemplateBuilder.create()
            .code("TEST_001")
            .locale(Locale.US)
            .named()
            .arg("name", "value")
            .render();
        
        assertEquals("English:value", result);
    }
    
    @Test
    public void testRenderMapMultipleArgs() {
        Map<String, Object> args = new HashMap<>();
        args.put("userId", "admin");
        args.put("time", "2024-04-17");
        
        String result = MsgTemplateBuilder.create()
            .code("TEST_002")
            .named()
            .args(args)
            .render();
        
        assertEquals("用户admin于2024-04-17登录", result);
    }
    
    @Test
    public void testRenderBeanWithDefaultLocale() {
        LoginEventBean bean = new LoginEventBean();
        bean.userId = "admin";
        bean.time = "2024-04-17";
        
        String result = MsgTemplateBuilder.create()
            .code("TEST_002")
            .named()
            .bean(bean)
            .render();
        
        assertEquals("用户admin于2024-04-17登录", result);
    }
    
    @Test
    public void testRenderBeanWithExplicitLocale() {
        LoginEventBean bean = new LoginEventBean();
        bean.userId = "admin";
        bean.time = "2024-04-17";
        
        String result = MsgTemplateBuilder.create()
            .code("TEST_002")
            .locale(Locale.US)
            .named()
            .bean(bean)
            .render();
        
        assertEquals("User admin logged in at 2024-04-17", result);
    }
    
    @Test
    public void testRenderWithLocaleMethod() {
        String result = MsgTemplateBuilder.create()
            .code("TEST_001")
            .named()
            .arg("name", "value")
            .render(Locale.US);
        
        assertEquals("English:value", result);
    }
    
    @Test
    public void testRenderCodeRequired() {
        try {
            MsgTemplateBuilder.create()
                .named()
                .arg("name", "test")
                .render();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Code is required"));
        }
    }
    
    @Test
    public void testRenderTemplateNotFound() {
        try {
            MsgTemplateBuilder.create()
                .code("NONEXISTENT")
                .named()
                .arg("name", "test")
                .render();
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("not found"));
        }
    }
    
    @Test
    public void testBuilderChaining() {
        String result = MsgTemplateBuilder.create()
            .code("TEST_001")
            .locale(Locale.CHINA)
            .named()
            .arg("name", "测试值")
            .render();
        
        assertEquals("中文:测试值", result);
    }
    
    public static class LoginEventBean {
        public String userId;
        public String time;
        
        public String getUserId() { return userId; }
        public String getTime() { return time; }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=NamedBuilderTest -q`
Expected: FAIL (NamedBuilder class not found)

- [ ] **Step 3: Write minimal implementation**

```java
package cn.itcraft.jmsg.builder;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.core.NamedMsgTemplate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NamedBuilder {
    
    private final String code;
    private final Locale locale;
    private Map<String, Object> namedArgs;
    private Object bean;
    
    NamedBuilder(String code, Locale locale) {
        this.code = code;
        this.locale = locale;
    }
    
    public NamedBuilder args(Map<String, Object> namedArgs) {
        this.namedArgs = namedArgs;
        this.bean = null;
        return this;
    }
    
    public NamedBuilder arg(String name, Object value) {
        if (this.namedArgs == null) {
            this.namedArgs = new HashMap<>();
        }
        this.namedArgs.put(name, value);
        this.bean = null;
        return this;
    }
    
    public NamedBuilder bean(Object bean) {
        this.bean = bean;
        this.namedArgs = null;
        return this;
    }
    
    public String render() {
        if (code == null) {
            throw new IllegalStateException("Code is required");
        }
        
        MsgTemplate template = findTemplate(code);
        if (template.getStyle() != MsgTemplate.Style.NAMED) {
            throw new IllegalArgumentException("Template is not NAMED style: " + code);
        }
        
        Locale renderLocale = locale != null ? locale : MsgTemplateConfig.getDefaultLocale();
        
        if (bean != null) {
            return template.render(renderLocale, bean);
        } else {
            return template.render(renderLocale, namedArgs);
        }
    }
    
    public String render(Locale locale) {
        if (code == null) {
            throw new IllegalStateException("Code is required");
        }
        
        MsgTemplate template = findTemplate(code);
        
        if (bean != null) {
            return template.render(locale, bean);
        } else {
            return template.render(locale, namedArgs);
        }
    }
    
    private MsgTemplate findTemplate(String code) {
        MsgTemplate template = EnumRegistry.valueOf(NamedMsgTemplate.class, code)
            .orElse(null);
        if (template == null) {
            template = EnumRegistry.valueOf(MsgTemplate.class, code)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + code));
        }
        return template;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=NamedBuilderTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/builder/NamedBuilder.java
git add src/test/java/cn/itcraft/jmsg/builder/NamedBuilderTest.java
git commit -m "feat: add NamedBuilder for {name} style template with Map/Bean support"
```

---

## 完成检查

- [ ] Task 16-18 全部通过
- [ ] 运行全部builder测试: `mvn test -Dtest=*Builder* -q`
- [ ] 推送到下个计划: 08-integration.md