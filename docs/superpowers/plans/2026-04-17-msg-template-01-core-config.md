# jmsg-i18n 实现计划 - Part 1: Core Config

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development

---

## Task 01: MsgTemplate接口

**Files:**
- Create: `src/main/java/cn/itcraft/jmsg/core/MsgTemplate.java`
- Test: `src/test/java/cn/itcraft/jmsg/core/MsgTemplateTest.java`

- [ ] **Step 1: Write the failing test**

```java
package cn.itcraft.jmsg.core;

import org.junit.Test;
import java.util.Locale;
import static org.junit.Assert.*;

public class MsgTemplateTest {
    
    @Test
    public void testStyleEnumExists() {
        MsgTemplate.Style simple = MsgTemplate.Style.SIMPLE;
        MsgTemplate.Style named = MsgTemplate.Style.NAMED;
        
        assertEquals("SIMPLE", simple.name());
        assertEquals("NAMED", named.name());
    }
    
    @Test
    public void testInterfaceMethods() {
        // 验证接口定义存在
        MsgTemplate template = new MockMsgTemplate();
        
        assertEquals("TEST_CODE", template.getCode());
        assertEquals("Test Template", template.getName());
        assertNotNull(template.getStyle());
        assertNotNull(template.getLocale());
    }
    
    private static class MockMsgTemplate implements MsgTemplate {
        @Override
        public String getCode() { return "TEST_CODE"; }
        
        @Override
        public String getName() { return "Test Template"; }
        
        @Override
        public String getDescription() { return ""; }
        
        @Override
        public int getOrder() { return 0; }
        
        @Override
        public Locale getLocale() { return Locale.getDefault(); }
        
        @Override
        public MsgTemplate.Style getStyle() { return MsgTemplate.Style.SIMPLE; }
        
        @Override
        public String render(Locale locale, Object... args) { return ""; }
        
        @Override
        public String render(Object... args) { return ""; }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=MsgTemplateTest -q`
Expected: FAIL (MsgTemplate class not found)

- [ ] **Step 3: Write minimal implementation**

```java
package cn.itcraft.jmsg.core;

import cn.itcraft.dyenums.core.DyEnum;
import java.util.Locale;

public interface MsgTemplate extends DyEnum {
    
    Locale getLocale();
    
    MsgTemplate.Style getStyle();
    
    String render(Locale locale, Object... args);
    
    String render(Object... args);
    
    enum Style {
        SIMPLE,
        NAMED
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=MsgTemplateTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/core/MsgTemplate.java
git add src/test/java/cn/itcraft/jmsg/core/MsgTemplateTest.java
git commit -m "feat: add MsgTemplate interface with Style enum"
```

---

## Task 02: MsgTemplateConfig

**Files:**
- Create: `src/main/java/cn/itcraft/jmsg/core/MsgTemplateConfig.java`
- Test: `src/test/java/cn/itcraft/jmsg/core/MsgTemplateConfigTest.java`

- [ ] **Step 1: Write the failing test**

```java
package cn.itcraft.jmsg.core;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import java.util.Locale;
import static org.junit.Assert.*;

public class MsgTemplateConfigTest {
    
    @Before
    public void setUp() {
        MsgTemplateConfig.reset();
    }
    
    @After
    public void tearDown() {
        MsgTemplateConfig.reset();
    }
    
    @Test
    public void testDefaultLocale() {
        Locale initial = MsgTemplateConfig.getDefaultLocale();
        assertEquals(Locale.getDefault(), initial);
    }
    
    @Test
    public void testSetDefaultLocale() {
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
        assertEquals(Locale.CHINA, MsgTemplateConfig.getDefaultLocale());
    }
    
    @Test
    public void testSetDefaultLocaleString() {
        MsgTemplateConfig.setDefaultLocale("zh_CN");
        Locale locale = MsgTemplateConfig.getDefaultLocale();
        assertEquals("zh", locale.getLanguage());
        assertEquals("CN", locale.getCountry());
    }
    
    @Test
    public void testSetDefaultLocaleNull() {
        Locale before = MsgTemplateConfig.getDefaultLocale();
        MsgTemplateConfig.setDefaultLocale((Locale) null);
        assertEquals(before, MsgTemplateConfig.getDefaultLocale());
    }
    
    @Test
    public void testReflectCacheEnabled() {
        assertTrue(MsgTemplateConfig.isReflectCacheEnabled());
        
        MsgTemplateConfig.setReflectCacheEnabled(false);
        assertFalse(MsgTemplateConfig.isReflectCacheEnabled());
    }
    
    @Test
    public void testReflectCacheMaxSize() {
        assertEquals(1024, MsgTemplateConfig.getReflectCacheMaxSize());
        
        MsgTemplateConfig.setReflectCacheMaxSize(2048);
        assertEquals(2048, MsgTemplateConfig.getReflectCacheMaxSize());
    }
    
    @Test
    public void testReflectCacheMaxSizeInvalid() {
        MsgTemplateConfig.setReflectCacheMaxSize(-100);
        assertEquals(1024, MsgTemplateConfig.getReflectCacheMaxSize());
        
        MsgTemplateConfig.setReflectCacheMaxSize(0);
        assertEquals(1024, MsgTemplateConfig.getReflectCacheMaxSize());
    }
    
    @Test
    public void testReset() {
        MsgTemplateConfig.setDefaultLocale(Locale.US);
        MsgTemplateConfig.setReflectCacheMaxSize(5000);
        MsgTemplateConfig.setReflectCacheEnabled(false);
        
        MsgTemplateConfig.reset();
        
        assertEquals(Locale.getDefault(), MsgTemplateConfig.getDefaultLocale());
        assertEquals(1024, MsgTemplateConfig.getReflectCacheMaxSize());
        assertTrue(MsgTemplateConfig.isReflectCacheEnabled());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=MsgTemplateConfigTest -q`
Expected: FAIL (MsgTemplateConfig class not found)

- [ ] **Step 3: Write minimal implementation**

注意：LocaleHelper.parse()尚未实现，先使用简化版本。

```java
package cn.itcraft.jmsg.core;

import java.util.Locale;

public final class MsgTemplateConfig {
    
    private static volatile Locale defaultLocale = Locale.getDefault();
    
    private static volatile int reflectCacheMaxSize = 1024;
    
    private static volatile boolean reflectCacheEnabled = true;
    
    private MsgTemplateConfig() {}
    
    public static Locale getDefaultLocale() {
        return defaultLocale;
    }
    
    public static void setDefaultLocale(Locale locale) {
        if (locale != null) {
            defaultLocale = locale;
        }
    }
    
    public static void setDefaultLocale(String localeCode) {
        if (localeCode != null) {
            defaultLocale = parseLocale(localeCode);
        }
    }
    
    private static Locale parseLocale(String localeCode) {
        String[] parts = localeCode.replace("-", "_").split("_");
        if (parts.length == 1) {
            return new Locale(parts[0]);
        } else if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        } else {
            return new Locale(parts[0], parts[1], parts[2]);
        }
    }
    
    public static int getReflectCacheMaxSize() {
        return reflectCacheMaxSize;
    }
    
    public static void setReflectCacheMaxSize(int maxSize) {
        reflectCacheMaxSize = maxSize > 0 ? maxSize : 1024;
    }
    
    public static boolean isReflectCacheEnabled() {
        return reflectCacheEnabled;
    }
    
    public static void setReflectCacheEnabled(boolean enabled) {
        reflectCacheEnabled = enabled;
    }
    
    public static void reset() {
        defaultLocale = Locale.getDefault();
        reflectCacheMaxSize = 1024;
        reflectCacheEnabled = true;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=MsgTemplateConfigTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/core/MsgTemplateConfig.java
git add src/test/java/cn/itcraft/jmsg/core/MsgTemplateConfigTest.java
git commit -m "feat: add MsgTemplateConfig with default locale and reflect cache settings"
```

---

## Task 03: CompiledTemplate

**Files:**
- Create: `src/main/java/cn/itcraft/jmsg/core/CompiledTemplate.java`
- Test: `src/test/java/cn/itcraft/jmsg/core/CompiledTemplateTest.java`

- [ ] **Step 1: Write the failing test**

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=CompiledTemplateTest -q`
Expected: FAIL (CompiledTemplate class not found)

- [ ] **Step 3: Write minimal implementation**

```java
package cn.itcraft.jmsg.core;

import java.io.Serializable;

public class CompiledTemplate implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public static final CompiledTemplate EMPTY = 
        new CompiledTemplate(new String[]{""}, new int[0], null, "");
    
    private final String[] fragments;
    private final int[] paramIndices;
    private final String[] paramNames;
    private final int paramCount;
    private final String originalTemplate;
    
    CompiledTemplate(String[] fragments, int[] paramIndices, 
                     String[] paramNames, String originalTemplate) {
        this.fragments = fragments;
        this.paramIndices = paramIndices;
        this.paramNames = paramNames;
        this.paramCount = paramIndices.length;
        this.originalTemplate = originalTemplate;
    }
    
    public String[] getFragments() {
        return fragments;
    }
    
    public int[] getParamIndices() {
        return paramIndices;
    }
    
    public String[] getParamNames() {
        return paramNames;
    }
    
    public int getParamCount() {
        return paramCount;
    }
    
    public String getOriginalTemplate() {
        return originalTemplate;
    }
    
    public boolean hasParams() {
        return paramCount > 0;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=CompiledTemplateTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/core/CompiledTemplate.java
git add src/test/java/cn/itcraft/jmsg/core/CompiledTemplateTest.java
git commit -m "feat: add CompiledTemplate for precompiled template structure"
```

---

## 完成检查

- [ ] Task 01-03 全部通过
- [ ] 运行全部测试: `mvn test -q`
- [ ] 推送到下个计划: 02-core-compiler.md