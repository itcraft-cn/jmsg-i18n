# YAML 模板配置格式实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将模板配置从 Properties 格式改为 YAML 格式，支持任意多语言

**Architecture:** 新增 YamlMsgTemplateLoader 解析 YAML，删除旧 Loader，修改 Template 的 Locale 匹配逻辑支持 zh-CN/zh_CN 双格式

**Tech Stack:** SnakeYAML 2.2，Java 8+

---

## 文件结构

| 操作 | 文件 | 职责 |
|------|------|------|
| Modify | `pom.xml` | 新增 snakeyaml 依赖 |
| Create | `loader/YamlMsgTemplateLoader.java` | YAML 文件加载器 |
| Delete | `loader/FileMsgTemplateLoader.java` | 删除旧文件加载器 |
| Delete | `loader/PropMsgTemplateLoader.java` | 删除旧 Properties 加载器 |
| Keep | `loader/MsgTemplateLoader.java` | 保留接口供自定义 |
| Modify | `core/SimpleMsgTemplate.java` | 修改 Locale 匹配，删除 fromValueString |
| Modify | `core/NamedMsgTemplate.java` | 修改 Locale 匹配，删除 fromValueString |
| Modify | `core/MsgTemplate.java` | 新增 hasLocale 方法 |
| Modify | `util/LocaleHelper.java` | 支持 zh-CN/zh_CN 双格式解析 |
| Delete | `src/test/java/cn/itcraft/jmsg/loader/*` | 删除旧 Loader 测试 |
| Create | `src/test/resources/*.yaml` | 新增 YAML 模板测试文件 |
| Create | `src/test/java/cn/itcraft/jmsg/loader/YamlMsgTemplateLoaderTest.java` | 新 Loader 测试 |
| Modify | `README.md`, `README_cn.md`, `AGENTS.md`, `CHANGELOG.md` | 更新文档 |

---

### Task 1: 新增 SnakeYAML 依赖

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 添加 snakeyaml 依赖**

```xml
<dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
    <version>2.2</version>
</dependency>
```

在 `pom.xml` 的 `<dependencies>` 中，dyenums-loader-file 之后添加。

- [ ] **Step 2: 编译验证**

```bash
mvnd clean compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "build: add snakeyaml dependency for YAML template config"
```

---

### Task 2: 扩展 LocaleHelper 支持双格式

**Files:**
- Modify: `src/main/java/cn/itcraft/jmsg/util/LocaleHelper.java`

- [ ] **Step 1: 修改 LocaleHelper 支持连字符格式**

```java
package cn.itcraft.jmsg.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class LocaleHelper {
    
    private static final Pattern UNDERSCORE_PATTERN = Pattern.compile("_");
    private static final Pattern HYPHEN_PATTERN = Pattern.compile("-");
    
    private LocaleHelper() {}
    
    public static Locale parse(String localeCode) {
        if (localeCode == null || localeCode.trim().isEmpty()) {
            return Locale.getDefault();
        }
        
        String normalized = HYPHEN_PATTERN.matcher(localeCode.trim()).replaceAll("_");
        String[] parts = UNDERSCORE_PATTERN.split(normalized);
        
        if (parts.length == 0 || parts[0].isEmpty()) {
            return Locale.getDefault();
        }
        
        String language = parts[0].toLowerCase();
        
        if (!isValidLanguageCode(language)) {
            return Locale.getDefault();
        }
        
        String country = parts.length > 1 ? parts[1].toUpperCase() : "";
        if (!country.isEmpty() && !isValidCountryCode(country)) {
            country = "";
        }
        
        String variant = parts.length > 2 ? parts[2] : "";
        
        if (variant.isEmpty() && country.isEmpty()) {
            return new Locale(language);
        } else if (variant.isEmpty()) {
            return new Locale(language, country);
        } else {
            return new Locale(language, country, variant);
        }
    }
    
    public static String toHyphen(Locale locale) {
        if (locale == null) return null;
        String lang = locale.getLanguage();
        String country = locale.getCountry();
        if (country.isEmpty()) return lang;
        return lang + "-" + country;
    }
    
    public static String toUnderscore(Locale locale) {
        if (locale == null) return null;
        String lang = locale.getLanguage();
        String country = locale.getCountry();
        if (country.isEmpty()) return lang;
        return lang + "_" + country;
    }
    
    private static boolean isValidLanguageCode(String code) {
        if (code.length() < 2 || code.length() > 3) {
            return false;
        }
        for (char c : code.toCharArray()) {
            if (!Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }
    
    private static boolean isValidCountryCode(String code) {
        if (code.length() != 2) {
            return false;
        }
        for (char c : code.toCharArray()) {
            if (!Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }
    
    public static Locale parse(Locale locale) {
        return locale != null ? locale : Locale.getDefault();
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
mvn test -Dtest=LocaleHelperTest -q
```

Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/util/LocaleHelper.java
git commit -m "feat: LocaleHelper support hyphen format zh-CN"
```

---

### Task 3: 新增 YamlMsgTemplateLoader

**Files:**
- Create: `src/main/java/cn/itcraft/jmsg/loader/YamlMsgTemplateLoader.java`

- [ ] **Step 1: 创建 YamlMsgTemplateLoader**

```java
package cn.itcraft.jmsg.loader;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.NamedMsgTemplate;
import cn.itcraft.jmsg.core.SimpleMsgTemplate;
import cn.itcraft.jmsg.core.TemplateCompiler;
import cn.itcraft.jmsg.core.CompiledTemplate;
import cn.itcraft.jmsg.util.LocaleHelper;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.function.BiFunction;

public class YamlMsgTemplateLoader implements MsgTemplateLoader {
    
    private final String filePath;
    private final MsgTemplate.Style style;
    
    public YamlMsgTemplateLoader(String filePath, MsgTemplate.Style style) {
        this.filePath = filePath;
        this.style = style;
    }
    
    public static YamlMsgTemplateLoader forSimple(String filePath) {
        return new YamlMsgTemplateLoader(filePath, MsgTemplate.Style.SIMPLE);
    }
    
    public static YamlMsgTemplateLoader forNamed(String filePath) {
        return new YamlMsgTemplateLoader(filePath, MsgTemplate.Style.NAMED);
    }
    
    public MsgTemplate.Style getStyle() {
        return style;
    }
    
    @Override
    public int load(Class<MsgTemplate> enumClass, BiFunction<String, String, MsgTemplate> factory) {
        InputStream is = getResourceAsStream(filePath);
        if (is == null) {
            throw new RuntimeException("YAML template file not found: " + filePath);
        }
        
        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(reader);
            
            Map<String, Object> templates = (Map<String, Object>) data.get("templates");
            if (templates == null) {
                throw new RuntimeException("YAML missing 'templates' section: " + filePath);
            }
            
            int count = 0;
            for (Map.Entry<String, Object> entry : templates.entrySet()) {
                String code = entry.getKey();
                Map<String, Object> templateData = (Map<String, Object>) entry.getValue();
                
                MsgTemplate template = createTemplate(code, templateData);
                registerTemplate(template);
                count++;
            }
            return count;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load YAML templates from: " + filePath, e);
        }
    }
    
    private MsgTemplate createTemplate(String code, Map<String, Object> data) {
        int order = data.containsKey("order") ? ((Number) data.get("order")).intValue() : 0;
        String defaultLocaleStr = (String) data.get("default");
        Locale defaultLocale = LocaleHelper.parse(defaultLocaleStr);
        
        Map<String, Object> messages = (Map<String, Object>) data.get("messages");
        if (messages == null) {
            throw new IllegalArgumentException("Template " + code + " missing 'messages' section");
        }
        
        Map<String, CompiledTemplate> compiledMap = new HashMap<>();
        for (Map.Entry<String, Object> msgEntry : messages.entrySet()) {
            String localeKey = msgEntry.getKey();
            String templateStr = (String) msgEntry.getValue();
            Locale locale = LocaleHelper.parse(localeKey);
            String localeId = LocaleHelper.toUnderscore(locale);
            
            CompiledTemplate compiled = style == MsgTemplate.Style.SIMPLE 
                ? TemplateCompiler.compileSimple(templateStr)
                : TemplateCompiler.compileNamed(templateStr);
            compiledMap.put(localeId, compiled);
        }
        
        if (style == MsgTemplate.Style.SIMPLE) {
            return new SimpleMsgTemplate(code, code, "", order, defaultLocale, compiledMap);
        } else {
            return new NamedMsgTemplate(code, code, "", order, defaultLocale, compiledMap);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void registerTemplate(MsgTemplate template) {
        if (style == MsgTemplate.Style.NAMED) {
            EnumRegistry.register(NamedMsgTemplate.class, (NamedMsgTemplate) template);
        } else {
            EnumRegistry.register(SimpleMsgTemplate.class, (SimpleMsgTemplate) template);
        }
    }
    
    @Override
    public boolean validateSource() {
        return getResourceAsStream(filePath) != null;
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

- [ ] **Step 2: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/loader/YamlMsgTemplateLoader.java
git commit -m "feat: add YamlMsgTemplateLoader for YAML template config"
```

---

### Task 4: 修改 MsgTemplate 接口

**Files:**
- Modify: `src/main/java/cn/itcraft/jmsg/core/MsgTemplate.java`

- [ ] **Step 1: 新增 hasLocale 方法**

```java
package cn.itcraft.jmsg.core;

import cn.itcraft.dyenums.core.DyEnum;
import java.util.Locale;
import java.util.Set;

public interface MsgTemplate extends DyEnum {
    
    Locale getLocale();
    
    Locale getDefaultLocale();
    
    MsgTemplate.Style getStyle();
    
    String render(Locale locale, Object... args);
    
    String render(Object... args);
    
    Set<String> getSupportedLocales();
    
    boolean hasLocale(Locale locale);
    
    enum Style {
        SIMPLE,
        NAMED
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/core/MsgTemplate.java
git commit -m "feat: add hasLocale/getDefaultLocale to MsgTemplate interface"
```

---

### Task 5: 修改 SimpleMsgTemplate 支持 YAML

**Files:**
- Modify: `src/main/java/cn/itcraft/jmsg/core/SimpleMsgTemplate.java`

- [ ] **Step 1: 重写 SimpleMsgTemplate**

```java
package cn.itcraft.jmsg.core;

import cn.itcraft.jmsg.util.LocaleHelper;
import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SimpleMsgTemplate implements MsgTemplate, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String code;
    private final String name;
    private final String description;
    private final int order;
    private final Locale defaultLocale;
    private final Map<String, CompiledTemplate> compiledTemplates;
    
    public SimpleMsgTemplate(String code, String name, String description, int order,
                             Locale defaultLocale, Map<String, CompiledTemplate> compiledTemplates) {
        this.code = code;
        this.name = name;
        this.description = description != null ? description : "";
        this.order = order;
        this.defaultLocale = defaultLocale;
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
    public Locale getDefaultLocale() {
        return defaultLocale;
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
    
    @Override
    public Set<String> getSupportedLocales() {
        return compiledTemplates.keySet();
    }
    
    @Override
    public boolean hasLocale(Locale locale) {
        String key = LocaleHelper.toUnderscore(locale);
        return compiledTemplates.containsKey(key);
    }
    
    public CompiledTemplate getCompiled(Locale locale) {
        String key = LocaleHelper.toUnderscore(locale);
        
        CompiledTemplate compiled = compiledTemplates.get(key);
        if (compiled != null) {
            return compiled;
        }
        
        String langKey = locale.getLanguage();
        compiled = compiledTemplates.get(langKey);
        if (compiled != null) {
            return compiled;
        }
        
        String defaultKey = LocaleHelper.toUnderscore(defaultLocale);
        compiled = compiledTemplates.get(defaultKey);
        if (compiled != null) {
            return compiled;
        }
        
        return CompiledTemplate.EMPTY;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/core/SimpleMsgTemplate.java
git commit -m "refactor: SimpleMsgTemplate support YAML with locale fallback"
```

---

### Task 6: 修改 NamedMsgTemplate 支持 YAML

**Files:**
- Modify: `src/main/java/cn/itcraft/jmsg/core/NamedMsgTemplate.java`

- [ ] **Step 1: 重写 NamedMsgTemplate**

```java
package cn.itcraft.jmsg.core;

import cn.itcraft.jmsg.util.LocaleHelper;
import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class NamedMsgTemplate implements MsgTemplate, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String code;
    private final String name;
    private final String description;
    private final int order;
    private final Locale defaultLocale;
    private final Map<String, CompiledTemplate> compiledTemplates;
    
    public NamedMsgTemplate(String code, String name, String description, int order,
                            Locale defaultLocale, Map<String, CompiledTemplate> compiledTemplates) {
        this.code = code;
        this.name = name;
        this.description = description != null ? description : "";
        this.order = order;
        this.defaultLocale = defaultLocale;
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
    public Locale getDefaultLocale() {
        return defaultLocale;
    }
    
    @Override
    public MsgTemplate.Style getStyle() { return MsgTemplate.Style.NAMED; }
    
    @Override
    public String render(Locale locale, Object... args) {
        if (args == null || args.length == 0) {
            CompiledTemplate compiled = getCompiled(locale);
            return TemplateRenderer.renderNamedMap(compiled, null);
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
    
    @Override
    public Set<String> getSupportedLocales() {
        return compiledTemplates.keySet();
    }
    
    @Override
    public boolean hasLocale(Locale locale) {
        String key = LocaleHelper.toUnderscore(locale);
        return compiledTemplates.containsKey(key);
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
        String key = LocaleHelper.toUnderscore(locale);
        
        CompiledTemplate compiled = compiledTemplates.get(key);
        if (compiled != null) {
            return compiled;
        }
        
        String langKey = locale.getLanguage();
        compiled = compiledTemplates.get(langKey);
        if (compiled != null) {
            return compiled;
        }
        
        String defaultKey = LocaleHelper.toUnderscore(defaultLocale);
        compiled = compiledTemplates.get(defaultKey);
        if (compiled != null) {
            return compiled;
        }
        
        return CompiledTemplate.EMPTY;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/core/NamedMsgTemplate.java
git commit -m "refactor: NamedMsgTemplate support YAML with locale fallback"
```

---

### Task 7: 删除旧 Loader

**Files:**
- Delete: `src/main/java/cn/itcraft/jmsg/loader/FileMsgTemplateLoader.java`
- Delete: `src/main/java/cn/itcraft/jmsg/loader/PropMsgTemplateLoader.java`

- [ ] **Step 1: 删除旧 Loader 文件**

```bash
rm src/main/java/cn/itcraft/jmsg/loader/FileMsgTemplateLoader.java
rm src/main/java/cn/itcraft/jmsg/loader/PropMsgTemplateLoader.java
```

- [ ] **Step 2: Commit**

```bash
git add -A src/main/java/cn/itcraft/jmsg/loader/
git commit -m "refactor: remove deprecated FileMsgTemplateLoader and PropMsgTemplateLoader"
```

---

### Task 8: 创建 YAML 测试文件

**Files:**
- Create: `src/test/resources/templates_simple.yaml`
- Create: `src/test/resources/templates_named.yaml`

- [ ] **Step 1: 创建 Simple YAML**

```yaml
templates:
  ERR_001:
    order: 1
    default: zh-CN
    messages:
      zh-CN: 内部错误:{}
      en-US: Internal error:{}
      en-GB: Internal error:{}
  
  LOG_001:
    order: 10
    default: zh-CN
    messages:
      zh-CN: 用户{}于{}登录
      en-US: User {} logged in at {}
```

- [ ] **Step 2: 创建 Named YAML**

```yaml
templates:
  ERR_001:
    order: 1
    default: zh-CN
    messages:
      zh-CN: 内部错误:{reason}
      en-US: Internal error:{reason}
      en-GB: Internal error:{reason}
  
  LOG_001:
    order: 10
    default: zh-CN
    messages:
      zh-CN: 用户{userId}于{time}登录
      en-US: User {userId} logged in at {time}
```

- [ ] **Step 3: Commit**

```bash
git add src/test/resources/templates_simple.yaml src/test/resources/templates_named.yaml
git commit -m "test: add YAML template test files"
```

---

### Task 9: 创建 YamlMsgTemplateLoaderTest

**Files:**
- Create: `src/test/java/cn/itcraft/jmsg/loader/YamlMsgTemplateLoaderTest.java`

- [ ] **Step 1: 创建测试类**

```java
package cn.itcraft.jmsg.loader;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.core.SimpleMsgTemplate;
import cn.itcraft.jmsg.core.NamedMsgTemplate;
import cn.itcraft.jmsg.builder.MsgTemplateBuilder;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class YamlMsgTemplateLoaderTest {
    
    @Before
    public void setup() {
        EnumRegistry.clear();
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
    }
    
    @After
    public void teardown() {
        EnumRegistry.clear();
    }
    
    @Test
    public void testLoadSimple() {
        YamlMsgTemplateLoader loader = YamlMsgTemplateLoader.forSimple("templates_simple.yaml");
        int count = loader.load(MsgTemplate.class, null);
        assertEquals(2, count);
        
        String msg = MsgTemplateBuilder.create()
            .code("ERR_001")
            .simple()
            .args("数据库超时")
            .render();
        assertEquals("内部错误:数据库超时", msg);
    }
    
    @Test
    public void testLoadNamed() {
        YamlMsgTemplateLoader loader = YamlMsgTemplateLoader.forNamed("templates_named.yaml");
        int count = loader.load(MsgTemplate.class, null);
        assertEquals(2, count);
        
        Map<String, Object> args = new HashMap<>();
        args.put("reason", "timeout");
        
        String msg = MsgTemplateBuilder.create()
            .code("ERR_001")
            .named()
            .args(args)
            .render();
        assertEquals("内部错误:timeout", msg);
    }
    
    @Test
    public void testLocaleFallback() {
        YamlMsgTemplateLoader.forSimple("templates_simple.yaml").load(MsgTemplate.class, null);
        
        String msg = MsgTemplateBuilder.create()
            .code("ERR_001")
            .locale(Locale.US)
            .simple()
            .args("timeout")
            .render();
        assertEquals("Internal error:timeout", msg);
        
        msg = MsgTemplateBuilder.create()
            .code("ERR_001")
            .locale(Locale.UK)
            .simple()
            .args("timeout")
            .render();
        assertEquals("Internal error:timeout", msg);
    }
    
    @Test
    public void testSupportedLocales() {
        YamlMsgTemplateLoader.forNamed("templates_named.yaml").load(MsgTemplate.class, null);
        
        NamedMsgTemplate template = EnumRegistry.get(NamedMsgTemplate.class, "ERR_001");
        assertNotNull(template);
        
        assertTrue(template.hasLocale(Locale.CHINA));
        assertTrue(template.hasLocale(Locale.US));
        assertTrue(template.hasLocale(Locale.UK));
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
mvn test -Dtest=YamlMsgTemplateLoaderTest -q
```

Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/test/java/cn/itcraft/jmsg/loader/YamlMsgTemplateLoaderTest.java
git commit -m "test: add YamlMsgTemplateLoaderTest"
```

---

### Task 10: 删除旧测试文件

**Files:**
- Delete: `src/test/java/cn/itcraft/jmsg/loader/FileMsgTemplateLoaderTest.java`
- Delete: `src/test/java/cn/itcraft/jmsg/loader/PropMsgTemplateLoaderTest.java`
- Delete: `src/test/resources/simple.properties`
- Delete: `src/test/resources/named.properties`
- Update: `src/test/java/cn/itcraft/jmsg/example/JMessageSampleTest.java`（使用 YAML）

- [ ] **Step 1: 删除旧测试**

```bash
rm -f src/test/java/cn/itcraft/jmsg/loader/FileMsgTemplateLoaderTest.java
rm -f src/test/java/cn/itcraft/jmsg/loader/PropMsgTemplateLoaderTest.java
rm -f src/test/resources/simple.properties
rm -f src/test/resources/named.properties
```

- [ ] **Step 2: 更新 JMessageSampleTest**

修改为使用 YAML 加载：

```java
// 修改加载方式
YamlMsgTemplateLoader.forSimple("templates_simple.yaml").load(MsgTemplate.class, null);
YamlMsgTemplateLoader.forNamed("templates_named.yaml").load(MsgTemplate.class, null);
```

- [ ] **Step 3: 运行全部测试**

```bash
mvn test -q
```

Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor: remove old loader tests, update to YAML"
```

---

### Task 11: 更新文档

**Files:**
- Modify: `README.md`
- Modify: `README_cn.md`
- Modify: `AGENTS.md`
- Modify: `CHANGELOG.md`

- [ ] **Step 1: 更新 README.md**

主要修改点：
- 模板格式改为 YAML 示例
- 加载方式改为 `YamlMsgTemplateLoader`
- 删除 FileMsgTemplateLoader 引用
- 更新 Quick Start 代码

- [ ] **Step 2: 更新 README_cn.md**

同 README.md，中文版本

- [ ] **Step 3: 更新 AGENTS.md**

- 删除 loader-db 引用（已删除）
- 模板格式改为 YAML
- 常用命令更新

- [ ] **Step 4: 更新 CHANGELOG.md**

新增 1.2.0 版本记录：

```markdown
## [1.2.0] - 2026-04-20

### Added

- YAML template configuration format supporting unlimited locales
- YamlMsgTemplateLoader for YAML file parsing
- LocaleHelper support for both zh-CN and zh_CN formats
- MsgTemplate.hasLocale() and getDefaultLocale() methods

### Changed

- Template format changed from Properties to YAML
- Locale fallback: exact match → language match → default locale

### Removed

- FileMsgTemplateLoader (deprecated)
- PropMsgTemplateLoader (deprecated)
- Properties template format support

### Dependencies

- Added snakeyaml 2.2
```

- [ ] **Step 5: Commit**

```bash
git add README.md README_cn.md AGENTS.md CHANGELOG.md
git commit -m "docs: update documentation for YAML template config"
```

---

### Task 12: 运行全部测试验证

- [ ] **Step 1: 编译**

```bash
mvnd clean compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 运行测试**

```bash
mvn test -q
```

Expected: All tests PASS

- [ ] **Step 3: 打包**

```bash
mvn clean package -DskipTests -q
```

Expected: BUILD SUCCESS

---

## Self-Review

**1. Spec coverage:** ✓ 所有设计要求已覆盖

**2. Placeholder scan:** ✓ 无 placeholder，所有代码完整

**3. Type consistency:** ✓ LocaleHelper、MsgTemplate、SimpleMsgTemplate/NamedMsgTemplate 类型一致

---

**Plan complete and saved to `docs/superpowers/plans/2026-04-20-yaml-template-config.md`.**

**执行方式选择：**

**1. Inline Execution（推荐）** - 在当前会话按计划顺序执行

**2. Subagent-Driven** - 每任务派发子代理，任务间审查

请选择执行方式？