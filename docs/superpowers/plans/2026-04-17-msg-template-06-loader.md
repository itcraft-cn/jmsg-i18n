# jmsg-i18n 实现计划 - Part 6: Loader

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development

---

## Task 13: MsgTemplateLoader接口

**Files:**
- Create: `src/main/java/cn/itcraft/jmsg/loader/MsgTemplateLoader.java`

- [ ] **Step 1: Write minimal interface**

```java
package cn.itcraft.jmsg.loader;

import cn.itcraft.dyenums.loader.DyEnumsLoader;
import cn.itcraft.jmsg.core.MsgTemplate;
import java.util.function.BiFunction;

public interface MsgTemplateLoader extends DyEnumsLoader<MsgTemplate> {
    
    @Override
    int load(Class<MsgTemplate> enumClass, BiFunction<String, String, MsgTemplate> factory);
    
    @Override
    boolean validateSource();
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/loader/MsgTemplateLoader.java
git commit -m "feat: add MsgTemplateLoader interface extending DyEnumsLoader"
```

---

## Task 14: FileMsgTemplateLoader

**Files:**
- Create: `src/main/java/cn/itcraft/jmsg/loader/FileMsgTemplateLoader.java`
- Test: `src/test/java/cn/itcraft/jmsg/loader/FileMsgTemplateLoaderTest.java`
- Resource: `src/test/resources/msg_templates_simple.properties`
- Resource: `src/test/resources/msg_templates_named.properties`

- [ ] **Step 1: Create test resources**

```properties
# msg_templates_simple.properties
TEST_001=测试|Test|中文:{}|English:{}|1
TEST_002=日志|Log|用户{}于{}登录|User {} logged in at {}|2
```

```properties
# msg_templates_named.properties
TEST_001=测试|Test|中文:{name}|English:{name}|1
TEST_002=日志|Log|用户{userId}于{time}登录|User {userId} logged in at {time}|2
```

- [ ] **Step 2: Write the failing test**

```java
package cn.itcraft.jmsg.loader;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.core.SimpleMsgTemplate;
import cn.itcraft.jmsg.core.NamedMsgTemplate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.Locale;
import static org.junit.Assert.*;

public class FileMsgTemplateLoaderTest {
    
    @Before
    public void setUp() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
    }
    
    @After
    public void tearDown() {
        MsgTemplateConfig.reset();
        EnumRegistry.clear();
    }
    
    @Test
    public void testLoadSimple() {
        FileMsgTemplateLoader loader = FileMsgTemplateLoader.forSimple(
            "msg_templates_simple.properties");
        
        int count = loader.load(MsgTemplate.class, null);
        
        assertEquals(2, count);
    }
    
    @Test
    public void testLoadSimpleAndRegistry() {
        FileMsgTemplateLoader loader = FileMsgTemplateLoader.forSimple(
            "msg_templates_simple.properties");
        loader.load(MsgTemplate.class, null);
        
        SimpleMsgTemplate template = EnumRegistry.valueOf(SimpleMsgTemplate.class, "TEST_001")
            .orElse(null);
        
        assertNotNull(template);
        assertEquals("中文:值", template.render("值"));
    }
    
    @Test
    public void testLoadNamed() {
        FileMsgTemplateLoader loader = FileMsgTemplateLoader.forNamed(
            "msg_templates_named.properties");
        
        int count = loader.load(MsgTemplate.class, null);
        
        assertEquals(2, count);
    }
    
    @Test
    public void testLoadNamedAndRegistry() {
        FileMsgTemplateLoader loader = FileMsgTemplateLoader.forNamed(
            "msg_templates_named.properties");
        loader.load(MsgTemplate.class, null);
        
        NamedMsgTemplate template = EnumRegistry.valueOf(NamedMsgTemplate.class, "TEST_001")
            .orElse(null);
        
        assertNotNull(template);
        java.util.Map<String, Object> args = new java.util.HashMap<>();
        args.put("name", "值");
        assertEquals("中文:值", template.render(args));
    }
    
    @Test
    public void testValidateSourceExists() {
        FileMsgTemplateLoader loader = FileMsgTemplateLoader.forSimple(
            "msg_templates_simple.properties");
        
        assertTrue(loader.validateSource());
    }
    
    @Test
    public void testValidateSourceNotExists() {
        FileMsgTemplateLoader loader = FileMsgTemplateLoader.forSimple(
            "nonexistent.properties");
        
        assertFalse(loader.validateSource());
    }
    
    @Test
    public void testLoadFileNotFound() {
        FileMsgTemplateLoader loader = FileMsgTemplateLoader.forSimple(
            "nonexistent.properties");
        
        try {
            loader.load(MsgTemplate.class, null);
            fail("Should throw RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("not found"));
        }
    }
    
    @Test
    public void testDefaultConstructor() {
        FileMsgTemplateLoader loader = new FileMsgTemplateLoader("msg_templates_simple.properties");
        
        assertEquals(MsgTemplate.Style.SIMPLE, loader.getStyle());
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn test -Dtest=FileMsgTemplateLoaderTest -q`
Expected: FAIL (FileMsgTemplateLoader class not found)

- [ ] **Step 4: Write minimal implementation**

```java
package cn.itcraft.jmsg.loader;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.NamedMsgTemplate;
import cn.itcraft.jmsg.core.SimpleMsgTemplate;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.BiFunction;

public class FileMsgTemplateLoader implements MsgTemplateLoader {
    
    private final String filePath;
    private final MsgTemplate.Style style;
    
    public FileMsgTemplateLoader(String filePath, MsgTemplate.Style style) {
        this.filePath = filePath;
        this.style = style;
    }
    
    public FileMsgTemplateLoader(String filePath) {
        this(filePath, MsgTemplate.Style.SIMPLE);
    }
    
    public static FileMsgTemplateLoader forSimple(String filePath) {
        return new FileMsgTemplateLoader(filePath, MsgTemplate.Style.SIMPLE);
    }
    
    public static FileMsgTemplateLoader forNamed(String filePath) {
        return new FileMsgTemplateLoader(filePath, MsgTemplate.Style.NAMED);
    }
    
    public MsgTemplate.Style getStyle() {
        return style;
    }
    
    @Override
    public int load(Class<MsgTemplate> enumClass, BiFunction<String, String, MsgTemplate> factory) {
        Properties props = new Properties();
        try (InputStream is = getResourceAsStream(filePath)) {
            if (is == null) {
                throw new IOException("Template file not found: " + filePath);
            }
            props.load(is);
            
            BiFunction<String, String, MsgTemplate> actualFactory = getFactory();
            
            int count = 0;
            for (String code : props.stringPropertyNames()) {
                String valueString = props.getProperty(code);
                MsgTemplate template = actualFactory.apply(code, valueString);
                Class<?> registerClass = getRegisterClass();
                EnumRegistry.register(registerClass, template);
                count++;
            }
            return count;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load templates from: " + filePath, e);
        }
    }
    
    @Override
    public boolean validateSource() {
        return getResourceAsStream(filePath) != null;
    }
    
    private BiFunction<String, String, MsgTemplate> getFactory() {
        return style == MsgTemplate.Style.NAMED 
            ? NamedMsgTemplate::fromValueString 
            : SimpleMsgTemplate::fromValueString;
    }
    
    private Class<?> getRegisterClass() {
        return style == MsgTemplate.Style.NAMED ? NamedMsgTemplate.class : SimpleMsgTemplate.class;
    }
    
    private InputStream getResourceAsStream(String path) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (is == null) {
            is = getClass().getClassLoader().getResourceAsStream(path);
        }
        return is;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn test -Dtest=FileMsgTemplateLoaderTest -q`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/loader/FileMsgTemplateLoader.java
git add src/test/java/cn/itcraft/jmsg/loader/FileMsgTemplateLoaderTest.java
git add src/test/resources/msg_templates_simple.properties
git add src/test/resources/msg_templates_named.properties
git commit -m "feat: add FileMsgTemplateLoader with Simple/Named style support"
```

---

## Task 15: PropMsgTemplateLoader

**Files:**
- Create: `src/main/java/cn/itcraft/jmsg/loader/PropMsgTemplateLoader.java`
- Test: `src/test/java/cn/itcraft/jmsg/loader/PropMsgTemplateLoaderTest.java`

- [ ] **Step 1: Write the failing test**

```java
package cn.itcraft.jmsg.loader;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.core.SimpleMsgTemplate;
import cn.itcraft.jmsg.core.NamedMsgTemplate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.Locale;
import java.util.Properties;
import static org.junit.Assert.*;

public class PropMsgTemplateLoaderTest {
    
    @Before
    public void setUp() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
    }
    
    @After
    public void tearDown() {
        MsgTemplateConfig.reset();
        EnumRegistry.clear();
    }
    
    @Test
    public void testLoadSimpleFromProperties() {
        Properties props = new Properties();
        props.setProperty("TEST_001", "测试|Test|中文:{}|English:{}|1");
        
        PropMsgTemplateLoader loader = new PropMsgTemplateLoader(props, MsgTemplate.Style.SIMPLE);
        
        int count = loader.load(MsgTemplate.class, null);
        
        assertEquals(1, count);
    }
    
    @Test
    public void testLoadSimpleAndRegistry() {
        Properties props = new Properties();
        props.setProperty("TEST_001", "测试|Test|中文:{}|English:{}|1");
        
        PropMsgTemplateLoader loader = new PropMsgTemplateLoader(props, MsgTemplate.Style.SIMPLE);
        loader.load(MsgTemplate.class, null);
        
        SimpleMsgTemplate template = EnumRegistry.valueOf(SimpleMsgTemplate.class, "TEST_001")
            .orElse(null);
        
        assertNotNull(template);
        assertEquals("中文:值", template.render("值"));
    }
    
    @Test
    public void testLoadNamedFromProperties() {
        Properties props = new Properties();
        props.setProperty("TEST_001", "测试|Test|中文:{name}|English:{name}|1");
        
        PropMsgTemplateLoader loader = new PropMsgTemplateLoader(props, MsgTemplate.Style.NAMED);
        
        int count = loader.load(MsgTemplate.class, null);
        
        assertEquals(1, count);
    }
    
    @Test
    public void testLoadNamedAndRegistry() {
        Properties props = new Properties();
        props.setProperty("TEST_001", "测试|Test|中文:{name}|English:{name}|1");
        
        PropMsgTemplateLoader loader = new PropMsgTemplateLoader(props, MsgTemplate.Style.NAMED);
        loader.load(MsgTemplate.class, null);
        
        NamedMsgTemplate template = EnumRegistry.valueOf(NamedMsgTemplate.class, "TEST_001")
            .orElse(null);
        
        assertNotNull(template);
        java.util.Map<String, Object> args = new java.util.HashMap<>();
        args.put("name", "值");
        assertEquals("中文:值", template.render(args));
    }
    
    @Test
    public void testValidateSourceEmpty() {
        Properties props = new Properties();
        
        PropMsgTemplateLoader loader = new PropMsgTemplateLoader(props, MsgTemplate.Style.SIMPLE);
        
        assertFalse(loader.validateSource());
    }
    
    @Test
    public void testValidateSourceNull() {
        PropMsgTemplateLoader loader = new PropMsgTemplateLoader(null, MsgTemplate.Style.SIMPLE);
        
        assertFalse(loader.validateSource());
    }
    
    @Test
    public void testValidateSourceNotEmpty() {
        Properties props = new Properties();
        props.setProperty("TEST", "value");
        
        PropMsgTemplateLoader loader = new PropMsgTemplateLoader(props, MsgTemplate.Style.SIMPLE);
        
        assertTrue(loader.validateSource());
    }
    
    @Test
    public void testLoadMultipleTemplates() {
        Properties props = new Properties();
        props.setProperty("TEST_001", "测试1|Test1|中文1:{}|English1:{}|1");
        props.setProperty("TEST_002", "测试2|Test2|中文2:{}|English2:{}|2");
        props.setProperty("TEST_003", "测试3|Test3|中文3:{}|English3:{}|3");
        
        PropMsgTemplateLoader loader = new PropMsgTemplateLoader(props, MsgTemplate.Style.SIMPLE);
        
        int count = loader.load(MsgTemplate.class, null);
        
        assertEquals(3, count);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=PropMsgTemplateLoaderTest -q`
Expected: FAIL (PropMsgTemplateLoader class not found)

- [ ] **Step 3: Write minimal implementation**

```java
package cn.itcraft.jmsg.loader;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.NamedMsgTemplate;
import cn.itcraft.jmsg.core.SimpleMsgTemplate;
import java.util.Properties;
import java.util.function.BiFunction;

public class PropMsgTemplateLoader implements MsgTemplateLoader {
    
    private final Properties properties;
    private final MsgTemplate.Style style;
    
    public PropMsgTemplateLoader(Properties properties, MsgTemplate.Style style) {
        this.properties = properties;
        this.style = style;
    }
    
    @Override
    public int load(Class<MsgTemplate> enumClass, BiFunction<String, String, MsgTemplate> factory) {
        BiFunction<String, String, MsgTemplate> actualFactory = getFactory();
        
        int count = 0;
        for (String code : properties.stringPropertyNames()) {
            String valueString = properties.getProperty(code);
            MsgTemplate template = actualFactory.apply(code, valueString);
            Class<?> registerClass = getRegisterClass();
            EnumRegistry.register(registerClass, template);
            count++;
        }
        return count;
    }
    
    @Override
    public boolean validateSource() {
        return properties != null && !properties.isEmpty();
    }
    
    private BiFunction<String, String, MsgTemplate> getFactory() {
        return style == MsgTemplate.Style.NAMED 
            ? NamedMsgTemplate::fromValueString 
            : SimpleMsgTemplate::fromValueString;
    }
    
    private Class<?> getRegisterClass() {
        return style == MsgTemplate.Style.NAMED ? NamedMsgTemplate.class : SimpleMsgTemplate.class;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=PropMsgTemplateLoaderTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/loader/PropMsgTemplateLoader.java
git add src/test/java/cn/itcraft/jmsg/loader/PropMsgTemplateLoaderTest.java
git commit -m "feat: add PropMsgTemplateLoader for in-memory Properties loading"
```

---

## 完成检查

- [ ] Task 13-15 全部通过
- [ ] 运行全部loader测试: `mvn test -Dtest=*Loader* -q`
- [ ] 推送到下个计划: 07-builder.md