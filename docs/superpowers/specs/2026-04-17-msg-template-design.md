# jmsg-i18n 多语言消息模板设计文档

## 1. 概述

### 1.1 目标
基于dyenums库构建一套多语言配置项系统，支持错误码、日志模板等场景的模板输出，提供参数化模板渲染能力。

### 1.2 核心需求
- 支持参数化模板（占位符替换）
- 支持双模板风格：{} (SLF4J) 和 {name} (命名参数)
- 通用消息模板设计，可扩展错误码、日志、通知等
- 加载器多态设计，默认文件模式，其他数据源由用户自行扩展
- 提供静态方法+Builder双API风格
- 预编译模板，高性能渲染

### 1.3 选定方案
方案C：MsgTemplate继承MultiLangDyEnum + 预编译模板引擎 + 双模板存储

---

## 2. 架构设计

### 2.1 包结构

```
cn.itcraft.jmsg
├── core
│   ├── MsgTemplate.java          # 核心类，继承MultiLangDyEnum
│   ├── CompiledTemplate.java     # 预编译模板结构
│   ├── TemplateCompiler.java     # 模板预编译器
│   └── TemplateRenderer.java     # 渲染执行器
├── loader
│   ├── MsgTemplateLoader.java    # 加载器接口（继承DyEnumsLoader）
│   ├── FileMsgTemplateLoader.java# 文件加载器（默认）
│   └── PropMsgTemplateLoader.java# Properties加载器
├── builder
│   └── MsgTemplateBuilder.java   # Builder模式构建器
└── util
    └── LocaleHelper.java         # Locale辅助工具
```

### 2.2 依赖关系

```
┌─────────────────────────────────────────────────────────────┐
│                      应用层                                  │
│   MsgTemplate.get() / MsgTemplate.builder().render()         │
├─────────────────────────────────────────────────────────────┤
│   MsgTemplate (核心类，继承 MultiLangDyEnum)                  │
│   ├── compiledSlf4j: Map<Locale, CompiledTemplate>           │
│   ├── compiledNamed: Map<Locale, CompiledTemplate>           │
│   ├── render(), renderNamed()                                │
├─────────────────────────────────────────────────────────────┤
│   CompiledTemplate (预编译模板结构)                           │
│   ├── fragments: String[]        文本片段                    │
│   ├── paramIndices: int[]        {}位置索引                  │
│   ├── paramNames: String[]       {name}参数名                │
├─────────────────────────────────────────────────────────────┤
│   TemplateCompiler (预编译器)                                │
│   ├── compileSlf4j(template) -> CompiledTemplate             │
│   ├── compileNamed(template) -> CompiledTemplate             │
├─────────────────────────────────────────────────────────────┤
│   TemplateRenderer (渲染执行器)                              │
│   ├── render(compiled, args) -> String                       │
│   ├── renderNamed(compiled, namedArgs) -> String             │
├─────────────────────────────────────────────────────────────┤
│   MsgTemplateLoader (加载器接口)                              │
│   ├── FileMsgTemplateLoader (默认)                           │
│   ├── PropMsgTemplateLoader                                  │
├─────────────────────────────────────────────────────────────┤
│   dyenums-core (基础依赖)                                     │
│   ├── MultiLangDyEnum                                        │
│   ├── EnumRegistry                                           │
│   ├── DyEnumsLoader                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 核心类设计

### 3.1 CompiledTemplate - 预编译模板结构

核心数据结构，存储预编译后的模板信息。

```java
public class CompiledTemplate implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
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
    
    public String[] getFragments() { return fragments; }
    public int[] getParamIndices() { return paramIndices; }
    public String[] getParamNames() { return paramNames; }
    public int getParamCount() { return paramCount; }
    public String getOriginalTemplate() { return originalTemplate; }
    
    public boolean hasParams() { return paramCount > 0; }
}
```

预编译示例：

| 原始模板 | fragments | paramIndices | paramNames |
|---------|-----------|--------------|------------|
| `用户{}于{}登录成功` | `["用户", "于", "登录成功"]` | `[0, 1]` | `null` |
| `用户{userId}于{time}登录成功` | `["用户", "于", "登录成功"]` | `[0, 1]` | `["userId", "time"]` |
| `系统正常` | `["系统正常"]` | `[]` | `null` |

### 3.2 TemplateCompiler - 模板预编译器

加载时预编译模板，提取占位符位置。

```java
public final class TemplateCompiler {
    
    private static final char PLACEHOLDER_START = '{';
    private static final char PLACEHOLDER_END = '}';
    
    public static CompiledTemplate compileSlf4j(String template) {
        if (template == null || template.isEmpty()) {
            return new CompiledTemplate(new String[]{""}, new int[0], null, "");
        }
        
        List<String> fragments = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        StringBuilder currentFragment = new StringBuilder();
        int index = 0;
        
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            
            if (c == PLACEHOLDER_START && i + 1 < template.length() 
                && template.charAt(i + 1) == PLACEHOLDER_END) {
                fragments.add(currentFragment.toString());
                currentFragment.setLength(0);
                indices.add(index++);
                i++;
            } else {
                currentFragment.append(c);
            }
        }
        fragments.add(currentFragment.toString());
        
        return new CompiledTemplate(
            fragments.toArray(new String[0]),
            indices.stream().mapToInt(Integer::intValue).toArray(),
            null,
            template
        );
    }
    
    public static CompiledTemplate compileNamed(String template) {
        if (template == null || template.isEmpty()) {
            return new CompiledTemplate(new String[]{""}, new int[0], new String[0], "");
        }
        
        List<String> fragments = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<String> names = new ArrayList<>();
        
        StringBuilder currentFragment = new StringBuilder();
        int index = 0;
        
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            
            if (c == PLACEHOLDER_START) {
                int endPos = findPlaceholderEnd(template, i);
                if (endPos > i + 1) {
                    fragments.add(currentFragment.toString());
                    currentFragment.setLength(0);
                    
                    String paramName = template.substring(i + 1, endPos);
                    names.add(paramName);
                    indices.add(index++);
                    i = endPos;
                    continue;
                }
            }
            currentFragment.append(c);
        }
        fragments.add(currentFragment.toString());
        
        return new CompiledTemplate(
            fragments.toArray(new String[0]),
            indices.stream().mapToInt(Integer::intValue).toArray(),
            names.toArray(new String[0]),
            template
        );
    }
    
    private static int findPlaceholderEnd(String template, int start) {
        for (int i = start + 1; i < template.length(); i++) {
            char c = template.charAt(i);
            if (c == PLACEHOLDER_END) {
                return i;
            }
            if (!isValidParamChar(c)) {
                return -1;
            }
        }
        return -1;
    }
    
    private static boolean isValidParamChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }
}
```

### 3.3 TemplateRenderer - 渲染执行器

按预编译结构高效渲染，无需重新扫描模板。

```java
public final class TemplateRenderer {
    
    public static String render(CompiledTemplate compiled, Object[] args) {
        if (!compiled.hasParams()) {
            return compiled.getFragments()[0];
        }
        
        String[] fragments = compiled.getFragments();
        int[] indices = compiled.getParamIndices();
        int paramCount = compiled.getParamCount();
        
        int estimatedSize = estimateSize(fragments, args, paramCount);
        StringBuilder result = new StringBuilder(estimatedSize);
        
        for (int i = 0; i < fragments.length; i++) {
            result.append(fragments[i]);
            if (i < paramCount) {
                int argIndex = indices[i];
                if (argIndex < args.length && args[argIndex] != null) {
                    result.append(args[argIndex]);
                } else {
                    result.append("{}");
                }
            }
        }
        
        return result.toString();
    }
    
    public static String renderNamed(CompiledTemplate compiled, Map<String, Object> namedArgs) {
        if (!compiled.hasParams()) {
            return compiled.getFragments()[0];
        }
        
        String[] fragments = compiled.getFragments();
        String[] paramNames = compiled.getParamNames();
        int paramCount = compiled.getParamCount();
        
        int estimatedSize = estimateNamedSize(fragments, namedArgs, paramNames, paramCount);
        StringBuilder result = new StringBuilder(estimatedSize);
        
        for (int i = 0; i < fragments.length; i++) {
            result.append(fragments[i]);
            if (i < paramCount) {
                String paramName = paramNames[i];
                Object value = namedArgs != null ? namedArgs.get(paramName) : null;
                if (value != null) {
                    result.append(value);
                } else {
                    result.append('{').append(paramName).append('}');
                }
            }
        }
        
        return result.toString();
    }
    
    private static int estimateSize(String[] fragments, Object[] args, int paramCount) {
        int size = 0;
        for (String f : fragments) {
            size += f.length();
        }
        for (Object arg : args) {
            size += arg != null ? String.valueOf(arg).length() : 2;
        }
        return size;
    }
    
    private static int estimateNamedSize(String[] fragments, Map<String, Object> namedArgs,
                                          String[] paramNames, int paramCount) {
        int size = 0;
        for (String f : fragments) {
            size += f.length();
        }
        if (namedArgs != null) {
            for (String name : paramNames) {
                Object val = namedArgs.get(name);
                size += val != null ? String.valueOf(val).length() : name.length() + 2;
            }
        }
        return size;
    }
}
```

### 3.4 MsgTemplate - 核心类

继承MultiLangDyEnum，存储预编译模板，提供静态API和Builder入口。

```java
public class MsgTemplate extends MultiLangDyEnum {
    
    private static final long serialVersionUID = 1L;
    
    private final Map<String, CompiledTemplate> compiledSlf4j;
    private final Map<String, CompiledTemplate> compiledNamed;
    
    protected MsgTemplate(String code, String name, int order,
                          Map<String, CompiledTemplate> compiledSlf4j,
                          Map<String, CompiledTemplate> compiledNamed) {
        super(code, name, order, Collections.emptyMap());
        this.compiledSlf4j = compiledSlf4j;
        this.compiledNamed = compiledNamed;
    }
    
    // ========== 静态API ==========
    
    public static String get(String code, Locale locale, Object... args) {
        MsgTemplate template = EnumRegistry.valueOf(MsgTemplate.class, code)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + code));
        return template.render(locale, args);
    }
    
    public static String getNamed(String code, Locale locale, Map<String, Object> namedArgs) {
        MsgTemplate template = EnumRegistry.valueOf(MsgTemplate.class, code)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + code));
        return template.renderNamed(locale, namedArgs);
    }
    
    public static String getZh(String code, Object... args) {
        return get(code, Locale.CHINA, args);
    }
    
    public static String getEn(String code, Object... args) {
        return get(code, Locale.US, args);
    }
    
    public static String getNamedZh(String code, Map<String, Object> namedArgs) {
        return getNamed(code, Locale.CHINA, namedArgs);
    }
    
    public static String getNamedEn(String code, Map<String, Object> namedArgs) {
        return getNamed(code, Locale.US, namedArgs);
    }
    
    // ========== Builder入口 ==========
    
    public static MsgTemplateBuilder builder() {
        return new MsgTemplateBuilder();
    }
    
    // ========== 实例方法 ==========
    
    public String render(Locale locale, Object... args) {
        CompiledTemplate compiled = getCompiledSlf4j(locale);
        return TemplateRenderer.render(compiled, args);
    }
    
    public String renderNamed(Locale locale, Map<String, Object> namedArgs) {
        CompiledTemplate compiled = getCompiledNamed(locale);
        return TemplateRenderer.renderNamed(compiled, namedArgs);
    }
    
    public CompiledTemplate getCompiledSlf4j(Locale locale) {
        return compiledSlf4j.getOrDefault(locale.getLanguage(), 
            compiledSlf4j.getOrDefault("en", CompiledTemplate.EMPTY));
    }
    
    public CompiledTemplate getCompiledNamed(Locale locale) {
        return compiledNamed.getOrDefault(locale.getLanguage(), 
            compiledNamed.getOrDefault("en", CompiledTemplate.EMPTY));
    }
    
    public Set<String> getSupportedLocales() {
        Set<String> locales = new HashSet<>();
        locales.addAll(compiledSlf4j.keySet());
        locales.addAll(compiledNamed.keySet());
        return locales;
    }
    
    // ========== 工厂方法 ==========
    
    public static MsgTemplate fromValueString(String code, String valueString) {
        String[] parts = valueString.split("\\|", -1);
        if (parts.length < 7) {
            throw new IllegalArgumentException(
                "Invalid format. Expected: name_zh|name_en|tpl_slf4j_zh|tpl_slf4j_en|tpl_named_zh|tpl_named_en|order");
        }
        
        String nameZh = parts[0].trim();
        String nameEn = parts[1].trim();
        int order = Integer.parseInt(parts[6].trim());
        
        Map<String, CompiledTemplate> compiledSlf4j = new HashMap<>();
        compiledSlf4j.put("zh", TemplateCompiler.compileSlf4j(parts[2].trim()));
        compiledSlf4j.put("en", TemplateCompiler.compileSlf4j(parts[3].trim()));
        
        Map<String, CompiledTemplate> compiledNamed = new HashMap<>();
        compiledNamed.put("zh", TemplateCompiler.compileNamed(parts[4].trim()));
        compiledNamed.put("en", TemplateCompiler.compileNamed(parts[5].trim()));
        
        String displayName = nameZh.isEmpty() ? nameEn : 
                             nameEn.isEmpty() ? nameZh : nameZh + "/" + nameEn;
        
        return new MsgTemplate(code, displayName, order, compiledSlf4j, compiledNamed);
    }
}
```

### 3.5 MsgTemplateBuilder

Builder模式构建器。

```java
public class MsgTemplateBuilder {
    
    private String code;
    private Locale locale = Locale.CHINA;
    private Object[] args;
    private Map<String, Object> namedArgs;
    private boolean useNamed = false;
    
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
    
    public MsgTemplateBuilder args(Object... args) {
        this.args = args;
        this.useNamed = false;
        return this;
    }
    
    public MsgTemplateBuilder namedArgs(Map<String, Object> namedArgs) {
        this.namedArgs = namedArgs;
        this.useNamed = true;
        return this;
    }
    
    public MsgTemplateBuilder namedArg(String name, Object value) {
        if (this.namedArgs == null) {
            this.namedArgs = new HashMap<>();
        }
        this.namedArgs.put(name, value);
        this.useNamed = true;
        return this;
    }
    
    public String render() {
        if (code == null) {
            throw new IllegalStateException("Code is required");
        }
        
        MsgTemplate template = EnumRegistry.valueOf(MsgTemplate.class, code)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + code));
        
        if (useNamed) {
            return template.renderNamed(locale, namedArgs);
        } else {
            return template.render(locale, args);
        }
    }
    
    public String renderZh() {
        return locale(Locale.CHINA).render();
    }
    
    public String renderEn() {
        return locale(Locale.US).render();
    }
}
```

---

## 4. 加载器设计

### 4.1 MsgTemplateLoader接口

继承dyenums的DyEnumsLoader接口。

```java
public interface MsgTemplateLoader extends DyEnumsLoader<MsgTemplate> {
    
    @Override
    int load(Class<MsgTemplate> enumClass, BiFunction<String, String, MsgTemplate> factory);
    
    @Override
    boolean validateSource();
}
```

### 4.2 FileMsgTemplateLoader

文件加载器（默认实现）。

```java
public class FileMsgTemplateLoader implements MsgTemplateLoader {
    
    private final String filePath;
    
    public FileMsgTemplateLoader(String filePath) {
        this.filePath = filePath;
    }
    
    public FileMsgTemplateLoader() {
        this("msg_templates.properties");
    }
    
    @Override
    public int load(Class<MsgTemplate> enumClass, BiFunction<String, String, MsgTemplate> factory) {
        Properties props = new Properties();
        try (InputStream is = getResourceAsStream(filePath)) {
            if (is == null) {
                throw new IOException("Template file not found: " + filePath);
            }
            props.load(is);
            
            int count = 0;
            for (String code : props.stringPropertyNames()) {
                String valueString = props.getProperty(code);
                MsgTemplate template = factory.apply(code, valueString);
                EnumRegistry.register(enumClass, template);
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
    
    private InputStream getResourceAsStream(String path) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (is == null) {
            is = getClass().getClassLoader().getResourceAsStream(path);
        }
        return is;
    }
}
```

### 4.3 PropMsgTemplateLoader

Properties对象加载器（内存加载）。

```java
public class PropMsgTemplateLoader implements MsgTemplateLoader {
    
    private final Properties properties;
    
    public PropMsgTemplateLoader(Properties properties) {
        this.properties = properties;
    }
    
    @Override
    public int load(Class<MsgTemplate> enumClass, BiFunction<String, String, MsgTemplate> factory) {
        int count = 0;
        for (String code : properties.stringPropertyNames()) {
            String valueString = properties.getProperty(code);
            MsgTemplate template = factory.apply(code, valueString);
            EnumRegistry.register(enumClass, template);
            count++;
        }
        return count;
    }
    
    @Override
    public boolean validateSource() {
        return properties != null && !properties.isEmpty();
    }
}
```

---

## 5. 配置文件格式

### 5.1 标准格式

```properties
# msg_templates.properties
# 格式：name_zh|name_en|template_slf4j_zh|template_slf4j_en|template_named_zh|template_named_en|order

# 系统错误类
SYS_ERR_001=系统错误|System error|系统内部错误:{}|Internal system error:{}|系统内部错误:{reason}|Internal system error:{reason}|1
SYS_ERR_002=参数错误|Parameter error|参数{}验证失败|Parameter {} validation failed|参数{paramName}验证失败|Parameter {paramName} validation failed|2
SYS_ERR_003=权限错误|Permission error|用户{}无权限访问资源{}|User {} has no permission to access {}|用户{userId}无权限访问资源{resource}|User {userId} has no permission to access {resource}|3

# 业务错误类
BIZ_ERR_001=订单错误|Order error|订单{}创建失败|Order {} creation failed|订单{orderId}创建失败|Order {orderId} creation failed|10
BIZ_ERR_002=库存错误|Inventory error|商品{}库存不足|Product {} inventory insufficient|商品{productId}库存不足|Product {productId} inventory insufficient|11

# 日志模板类
LOG_001=登录日志|Login log|用户{}于{}登录成功|User {} logged in at {}|用户{userId}于{time}登录成功|User {userId} logged in at {time}|20
LOG_002=操作日志|Operation log|用户{}执行了{}操作|User {} performed {} operation|用户{userId}执行了{action}操作|User {userId} performed {action} operation|21
```

### 5.2 多语言扩展

支持扩展更多语言：

```properties
# 扩展葡萄牙语、俄语（可选段）
SYS_ERR_001=系统错误|System error|系统内部错误:{}|Internal system error:{}|系统内部错误:{reason}|Internal system error:{reason}|pt:Erro interno:{}|pt:Erro interno:{reason}|ru:Внутренняя ошибка:{}|ru:Внутренняя ошибка:{reason}|1
```

扩展格式解析规则：
- 基础7段后，可选添加语言扩展：`langCode:slf4j_tpl|langCode:named_tpl` 成对格式
- 解析时检测扩展语言并编译加入对应Map

---

## 6. 使用示例

### 6.1 初始化

```java
public class AppInitializer {
    public void init() {
        FileMsgTemplateLoader loader = new FileMsgTemplateLoader("msg_templates.properties");
        loader.load(MsgTemplate.class, MsgTemplate::fromValueString);
    }
}
```

### 6.2 静态方法调用

```java
String msg = MsgTemplate.get("SYS_ERR_001", Locale.CHINA, "数据库连接超时");
// 输出：系统内部错误：数据库连接超时

String msgEn = MsgTemplate.getEn("SYS_ERR_002", "email");
// 输出：Parameter email validation failed

Map<String, Object> args = new HashMap<>();
args.put("userId", "admin");
args.put("resource", "/admin/dashboard");
String msg = MsgTemplate.getNamed("SYS_ERR_003", Locale.CHINA, args);
// 输出：用户admin无权限访问资源/admin/dashboard
```

### 6.3 Builder模式调用

```java
String msg = MsgTemplate.builder()
    .code("LOG_001")
    .locale(Locale.CHINA)
    .args("admin", "2024-04-17 10:30:00")
    .render();
// 输出：用户admin于2024-04-17 10:30:00登录成功

String msg = MsgTemplate.builder()
    .code("LOG_002")
    .locale(Locale.US)
    .namedArg("userId", "john")
    .namedArg("action", "delete")
    .render();
// 输出：User john performed delete operation

String msgZh = MsgTemplate.builder()
    .code("BIZ_ERR_001")
    .namedArg("orderId", "ORD-12345")
    .renderZh();
```

---

## 7. 测试策略

### 7.1 单元测试

| 测试类 | 测试范围 |
|--------|----------|
| CompiledTemplateTest | 预编译结构：数据存储、序列化 |
| TemplateCompilerTest | 预编译器：{}解析、{name}解析、边界情况 |
| TemplateRendererTest | 渲染器：按位置填充、按名称填充、空参数 |
| MsgTemplateTest | 核心类：静态方法、实例方法、工厂方法 |
| MsgTemplateBuilderTest | Builder模式：链式调用、参数校验 |
| FileMsgTemplateLoaderTest | 文件加载：正常加载、格式错误、文件缺失 |
| PropMsgTemplateLoaderTest | Properties加载：内存加载、空Properties |

### 7.2 性能测试

```java
@Test
public void testRenderPerformance() {
    CompiledTemplate compiled = TemplateCompiler.compileNamed(
        "用户{userId}于{time}在{location}执行了{action}操作，结果：{result}");
    
    Map<String, Object> args = new HashMap<>();
    args.put("userId", "admin");
    args.put("time", "2024-04-17");
    args.put("location", "server1");
    args.put("action", "login");
    args.put("result", "success");
    
    long start = System.nanoTime();
    for (int i = 0; i < 100000; i++) {
        TemplateRenderer.renderNamed(compiled, args);
    }
    long elapsed = System.nanoTime() - start;
    
    System.out.println("100K renders: " + elapsed / 1_000_000 + "ms");
}
```

---

## 8. 性能分析

### 8.1 预编译优势

| 操作 | 传统方式 | 预编译方式 |
|------|---------|-----------|
| 编译时机 | 每次渲染时扫描 | 加载时一次性编译 |
| 渲染扫描 | O(template.length) | O(0) - 无扫描 |
| 参数填充 | 动态查找位置 | 直接按索引填充 |
| 内存占用 | 存原始字符串 | 存fragments数组 |

### 8.2 渲染性能

- **SLF4J风格**：O(fragments.length + args.length)，约O(n+m)
- **Named风格**：O(fragments.length + paramNames.length)，约O(n+m)
- **无正则表达式**：避免Pattern.compile开销
- **StringBuilder预分配**：estimateSize避免多次扩容

### 8.3 内存结构

- CompiledTemplate：1个模板 ≈ fragments数组 + indices数组 + names数组
- 典型场景：100模板 * 4语言 * 2风格 = 800个CompiledTemplate
- 内存预估：每个CompiledTemplate ≈ 200字节，总计 ≈ 160KB

### 8.4 线程安全

- CompiledTemplate不可变（final字段）
- MsgTemplate不可变（final字段）
- EnumRegistry基于ConcurrentHashMap
- 无锁竞争，适合高并发场景

---

## 9. 扩展点

### 9.1 自定义加载器

实现MsgTemplateLoader接口，支持其他数据源：
- YAML文件
- Redis缓存
- 远程配置中心（Apollo、Nacos）
- 数据库（用户自行实现DbMsgTemplateLoader）

### 9.2 预定义模板子类

继承MsgTemplate创建领域专用模板：
- ErrorCode（错误码）- 添加HTTP状态码、错误级别
- LogTemplate（日志模板）- 添加日志级别、日志分类
- NotificationTemplate（通知模板）- 添加通知渠道、推送策略

---

## 10. 与dyenums的关系

| dyenums提供 | jmsg-i18n扩展 |
|-------------|---------------|
| MultiLangDyEnum基类 | MsgTemplate继承，替换messages为预编译模板 |
| EnumRegistry注册表 | 直接使用，无修改 |
| DyEnumsLoader接口 | MsgTemplateLoader继承，实现模板专用加载 |
| BaseDyEnum功能 | 通过继承链间接复用 |

jmsg-i18n作为dyenums的扩展库，遵循dyenums的设计原则：
- 类型安全
- 线程安全
- 不可变实例
- 可扩展加载器
- 预编译高性能

---

## 11. 实现计划

详见后续实现计划文档（由writing-plans skill生成）。