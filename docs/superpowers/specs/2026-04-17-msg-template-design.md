# jmsg-i18n 多语言消息模板设计文档

## 1. 概述

### 1.1 目标
基于dyenums库构建一套多语言配置项系统，支持错误码、日志模板等场景的模板输出，提供参数化模板渲染能力。

### 1.2 核心需求
- 支持参数化模板（占位符替换）
- 支持双模板风格：{} (Simple) 和 {name} (Named)，分离实现
- 通用消息模板设计，可扩展错误码、日志、通知等
- 加载器多态设计，默认文件模式，其他数据源由用户自行扩展
- 提供静态方法+Builder双API风格
- 预编译模板，高性能渲染
- Named风格支持Map参数和Bean参数（反射），缓存getter方法
- 默认Locale可配置（全局配置或API设置）
- ThreadLocal StringBuilder池化，避免频繁创建/扩容

### 1.3 选定方案
方案C优化：MsgTemplate接口 + 双实现类 + 预编译模板引擎 + 反射缓存 + 全局配置

---

## 2. 架构设计

### 2.1 包结构

```
cn.itcraft.jmsg
├── core
│   ├── MsgTemplate.java          # 接口定义
│   ├── SimpleMsgTemplate.java    # {}风格实现（原名Slf4jMsgTemplate）
│   ├── NamedMsgTemplate.java     # {name}风格实现
│   ├── CompiledTemplate.java     # 预编译模板结构
│   ├── TemplateCompiler.java     # 模板预编译器
│   ├── TemplateRenderer.java     # 渲染执行器（含反射缓存）
│   └── MsgTemplateConfig.java    # 全局配置（默认Locale等）
├── loader
│   ├── MsgTemplateLoader.java    # 加载器接口
│   ├── FileMsgTemplateLoader.java# 文件加载器（默认）
│   └── PropMsgTemplateLoader.java# Properties加载器
├── builder
│   ├── MsgTemplateBuilder.java   # Builder构建器（统一入口）
│   ├── SimpleBuilder.java        # {}风格Builder（原名Slf4jBuilder）
│   └── NamedBuilder.java         # {name}风格Builder
└── util
    ├── LocaleHelper.java         # Locale辅助工具
    ├── ReflectCache.java         # 反射缓存（可选Guava）
    └── StringBuilderPool.java    # ThreadLocal StringBuilder池化
```

### 2.2 依赖关系

```
┌─────────────────────────────────────────────────────────────┐
│                      应用层                                  │
│   MsgTemplate.render(args) / Builder.render()                 │
├─────────────────────────────────────────────────────────────┤
│   MsgTemplateConfig (全局配置)                                │
│   ├── defaultLocale: Locale        默认语言                  │
│   ├── reflectCacheMaxSize: int     反射缓存容量              │
├─────────────────────────────────────────────────────────────┤
│   MsgTemplate (接口)                                          │
│   ├── render(locale, args) -> String                         │
│   ├── render(args) -> String       使用默认Locale            │
│   ├── SimpleMsgTemplate ({}风格)                              │
│   │   └── compiled: Map<Locale, CompiledTemplate>            │
│   ├── NamedMsgTemplate ({name}风格)                           │
│   │   └── compiled: Map<Locale, CompiledTemplate>            │
├─────────────────────────────────────────────────────────────┤
│   CompiledTemplate (预编译模板结构)                           │
│   ├── fragments: String[]        文本片段                    │
│   ├── paramIndices: int[]        {}位置索引                  │
│   ├── paramNames: String[]       {name}参数名                │
├─────────────────────────────────────────────────────────────┤
│   TemplateCompiler (预编译器)                                │
│   ├── compileSimple(template) -> CompiledTemplate             │
│   ├── compileNamed(template) -> CompiledTemplate             │
├─────────────────────────────────────────────────────────────┤
│   TemplateRenderer (渲染执行器)                              │
│   ├── renderSimple(compiled, args) -> String                 │
│   ├── renderNamedMap(compiled, namedArgs) -> String          │
│   ├── renderNamedBean(compiled, bean) -> String  # 反射      │
│   ├── ReflectCache (getter方法缓存)                           │
├─────────────────────────────────────────────────────────────┤
│   MsgTemplateBuilder (统一Builder入口)                        │
│   ├── simple() -> SimpleBuilder                              │
│   ├── named() -> NamedBuilder                                │
├─────────────────────────────────────────────────────────────┤
│   MsgTemplateLoader (加载器接口)                              │
│   ├── FileMsgTemplateLoader (默认)                           │
│   ├── PropMsgTemplateLoader                                  │
├─────────────────────────────────────────────────────────────┤
│   dyenums-core (基础依赖)                                     │
│   ├── DyEnum接口                                             │
│   ├── EnumRegistry                                           │
│   ├── DyEnumsLoader                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 核心类设计

### 3.1 MsgTemplateConfig - 全局配置类

全局配置，可设置默认Locale、反射缓存容量等。

```java
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
            defaultLocale = LocaleHelper.parse(localeCode);
        }
    }
    
    public static int getReflectCacheMaxSize() {
        return reflectCacheMaxSize;
    }
    
    public static void setReflectCacheMaxSize(int maxSize) {
        reflectCacheMaxSize = maxSize > 0 ? maxSize : 1024;
        ReflectCache.setMaxSize(reflectCacheMaxSize);
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
        ReflectCache.clear();
    }
}
```

### 3.2 MsgTemplate接口

定义统一接口，render方法支持指定Locale和使用默认Locale。

```java
public interface MsgTemplate extends DyEnum {
    
    Locale getLocale();
    
    MsgTemplate.Style getStyle();
    
    String render(Locale locale, Object... args);
    
    String render(Object... args);
    
    enum Style {
        SIMPLE,   // {} 风格（原名SLF4J）
        NAMED     // {name} 风格
    }
}
```

### 3.3 SimpleMsgTemplate

{} 风格实现，专一处理位置参数。

```java
public class SimpleMsgTemplate implements MsgTemplate {
    
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
                CompiledTemplate.EMPTY)
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

### 3.4 NamedMsgTemplate

{name} 风格实现，专一处理命名参数，支持Map和Bean。

```java
public class NamedMsgTemplate implements MsgTemplate {
    
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
                CompiledTemplate.EMPTY)
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

### 3.5 CompiledTemplate

预编译模板结构，Simple和Named共用。

```java
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
    
    public String[] getFragments() { return fragments; }
    public int[] getParamIndices() { return paramIndices; }
    public String[] getParamNames() { return paramNames; }
    public int getParamCount() { return paramCount; }
    public String getOriginalTemplate() { return originalTemplate; }
    
    public boolean hasParams() { return paramCount > 0; }
}
```

### 3.6 TemplateCompiler

模板预编译器，方法名从compileSlf4j改为compileSimple。

```java
public final class TemplateCompiler {
    
    private static final char PLACEHOLDER_START = '{';
    private static final char PLACEHOLDER_END = '}';
    
    public static CompiledTemplate compileSimple(String template) {
        if (template == null || template.isEmpty()) {
            return CompiledTemplate.EMPTY;
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
            return CompiledTemplate.EMPTY;
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

### 3.7 TemplateRenderer + ReflectCache

渲染执行器，包含反射缓存支持，使用ThreadLocal StringBuilder池化提升性能。

```java
public final class TemplateRenderer {
    
    public static String renderSimple(CompiledTemplate compiled, Object[] args) {
        if (!compiled.hasParams()) {
            return compiled.getFragments()[0];
        }
        
        String[] fragments = compiled.getFragments();
        int[] indices = compiled.getParamIndices();
        int paramCount = compiled.getParamCount();
        
        int estimatedSize = estimateSize(fragments, args);
        StringBuilder result = StringBuilderPool.acquire(estimatedSize);
        
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
        
        return StringBuilderPool.releaseAndToString(result);
    }
    
    public static String renderNamedMap(CompiledTemplate compiled, Map<String, Object> namedArgs) {
        if (!compiled.hasParams()) {
            return compiled.getFragments()[0];
        }
        
        String[] fragments = compiled.getFragments();
        String[] paramNames = compiled.getParamNames();
        int paramCount = compiled.getParamCount();
        
        int estimatedSize = estimateNamedSize(fragments, paramNames, namedArgs);
        StringBuilder result = StringBuilderPool.acquire(estimatedSize);
        
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
        
        return StringBuilderPool.releaseAndToString(result);
    }
    
    public static String renderNamedBean(CompiledTemplate compiled, Object bean) {
        if (!compiled.hasParams()) {
            return compiled.getFragments()[0];
        }
        
        if (bean == null) {
            return renderOriginal(compiled);
        }
        
        if (!MsgTemplateConfig.isReflectCacheEnabled()) {
            return renderNamedBeanNoCache(compiled, bean);
        }
        
        String[] fragments = compiled.getFragments();
        String[] paramNames = compiled.getParamNames();
        int paramCount = compiled.getParamCount();
        
        int estimatedSize = estimateBeanSize(fragments, paramNames, bean);
        StringBuilder result = StringBuilderPool.acquire(estimatedSize);
        
        for (int i = 0; i < fragments.length; i++) {
            result.append(fragments[i]);
            if (i < paramCount) {
                String paramName = paramNames[i];
                Object value = ReflectCache.getProperty(bean, paramName);
                if (value != null) {
                    result.append(value);
                } else {
                    result.append('{').append(paramName).append('}');
                }
            }
        }
        
        return StringBuilderPool.releaseAndToString(result);
    }
    
    private static String renderNamedBeanNoCache(CompiledTemplate compiled, Object bean) {
        String[] fragments = compiled.getFragments();
        String[] paramNames = compiled.getParamNames();
        int paramCount = compiled.getParamCount();
        
        StringBuilder result = StringBuilderPool.acquire();
        
        for (int i = 0; i < fragments.length; i++) {
            result.append(fragments[i]);
            if (i < paramCount) {
                String paramName = paramNames[i];
                Object value = ReflectCache.getPropertyNoCache(bean, paramName);
                if (value != null) {
                    result.append(value);
                } else {
                    result.append('{').append(paramName).append('}');
                }
            }
        }
        
        return StringBuilderPool.releaseAndToString(result);
    }
    
    private static String renderOriginal(CompiledTemplate compiled) {
        StringBuilder result = StringBuilderPool.acquire();
        for (String fragment : compiled.getFragments()) {
            result.append(fragment);
        }
        String[] paramNames = compiled.getParamNames();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length && i < compiled.getParamCount(); i++) {
                result.append('{').append(paramNames[i]).append('}');
            }
        }
        return StringBuilderPool.releaseAndToString(result);
    }
    
    private static int estimateSize(String[] fragments, Object[] args) {
        int size = 0;
        for (String f : fragments) size += f.length();
        if (args != null) {
            for (Object arg : args) size += arg != null ? 32 : 2;
        }
        return size;
    }
    
    private static int estimateNamedSize(String[] fragments, String[] paramNames, Map<String, Object> namedArgs) {
        int size = 0;
        for (String f : fragments) size += f.length();
        if (namedArgs != null && paramNames != null) {
            for (String name : paramNames) {
                Object val = namedArgs.get(name);
                size += val != null ? String.valueOf(val).length() : name.length() + 2;
            }
        }
        return size;
    }
    
    private static int estimateBeanSize(String[] fragments, String[] paramNames, Object bean) {
        int size = 0;
        for (String f : fragments) size += f.length();
        if (paramNames != null) {
            for (String name : paramNames) size += 32;
        }
        return size;
    }
}
```

### 3.7.1 StringBuilderPool

ThreadLocal StringBuilder池化，避免频繁创建和扩容，提升渲染性能。

```java
public final class StringBuilderPool {
    
    private static final int DEFAULT_INITIAL_CAPACITY = 256;
    
    private static final ThreadLocal<StringBuilder> pool = ThreadLocal.withInitial(
        () -> new StringBuilder(DEFAULT_INITIAL_CAPACITY)
    );
    
    public static StringBuilder acquire(int estimatedSize) {
        StringBuilder sb = pool.get();
        sb.setLength(0);
        if (estimatedSize > sb.capacity()) {
            sb.ensureCapacity(estimatedSize);
        }
        return sb;
    }
    
    public static StringBuilder acquire() {
        StringBuilder sb = pool.get();
        sb.setLength(0);
        return sb;
    }
    
    public static String releaseAndToString(StringBuilder sb) {
        return sb.toString();
    }
    
    public static int getCurrentCapacity() {
        return pool.get().capacity();
    }
    
    public static void clear() {
        pool.remove();
    }
}
```

**使用方式**：

```java
StringBuilder sb = StringBuilderPool.acquire(estimatedSize);
sb.append(fragment).append(value);
return StringBuilderPool.releaseAndToString(sb);
```

**性能优势**：

| 场景 | 传统方式 | ThreadLocal池化 |
|------|---------|-----------------|
| 对象创建 | 每次new StringBuilder | ThreadLocal复用 |
| 内存分配 | 每次分配新内存 | 同一线程复用 |
| 扩容开销 | 频繁扩容 | ensureCapacity一次 |
| GC压力 | 高 | 低 |

### 3.8 ReflectCache

反射缓存，支持缓存开关和容量控制。

```java
public final class ReflectCache {
    
    private static final int DEFAULT_MAX_SIZE = 1024;
    
    private static final ConcurrentMap<Class<?>, ConcurrentMap<String, Method>> cache = 
        new ConcurrentHashMap<>();
    
    private static volatile int maxSize = DEFAULT_MAX_SIZE;
    
    public static void setMaxSize(int size) {
        maxSize = size > 0 ? size : DEFAULT_MAX_SIZE;
    }
    
    public static int getMaxSize() {
        return maxSize;
    }
    
    public static Object getProperty(Object bean, String propertyName) {
        if (bean == null || propertyName == null || propertyName.isEmpty()) {
            return null;
        }
        
        Class<?> clazz = bean.getClass();
        Method getter = getGetterMethod(clazz, propertyName);
        
        if (getter == null) {
            return null;
        }
        
        try {
            return getter.invoke(bean);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static Object getPropertyNoCache(Object bean, String propertyName) {
        if (bean == null || propertyName == null || propertyName.isEmpty()) {
            return null;
        }
        
        Method getter = findGetterMethod(bean.getClass(), propertyName);
        
        if (getter == null) {
            return null;
        }
        
        try {
            return getter.invoke(bean);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static Method getGetterMethod(Class<?> clazz, String propertyName) {
        ConcurrentMap<String, Method> classCache = cache.computeIfAbsent(clazz, k -> {
            if (cache.size() >= maxSize) {
                evictOldest();
            }
            return new ConcurrentHashMap<>();
        });
        
        return classCache.computeIfAbsent(propertyName, name -> findGetterMethod(clazz, name));
    }
    
    private static Method findGetterMethod(Class<?> clazz, String propertyName) {
        String capitalizedName = capitalize(propertyName);
        
        Method getter = tryGetMethod(clazz, "get" + capitalizedName);
        if (getter != null && isAccessibleGetter(getter)) {
            return getter;
        }
        
        getter = tryGetMethod(clazz, "is" + capitalizedName);
        if (getter != null && isBooleanGetter(getter)) {
            return getter;
        }
        
        getter = tryGetMethod(clazz, propertyName);
        if (getter != null && isAccessibleGetter(getter)) {
            return getter;
        }
        
        return null;
    }
    
    private static Method tryGetMethod(Class<?> clazz, String methodName) {
        try {
            return clazz.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            try {
                Method method = clazz.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ex) {
                Class<?> superClass = clazz.getSuperclass();
                if (superClass != null && superClass != Object.class) {
                    return tryGetMethod(superClass, methodName);
                }
                return null;
            }
        }
    }
    
    private static boolean isAccessibleGetter(Method method) {
        return method.getParameterCount() == 0 && method.getReturnType() != void.class;
    }
    
    private static boolean isBooleanGetter(Method method) {
        Class<?> returnType = method.getReturnType();
        return method.getParameterCount() == 0 && 
               (returnType == Boolean.class || returnType == boolean.class);
    }
    
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
    
    private static void evictOldest() {
        Iterator<Class<?>> it = cache.keySet().iterator();
        if (it.hasNext()) {
            it.next();
            it.remove();
        }
    }
    
    public static void clear() {
        cache.clear();
    }
    
    public static int size() {
        return cache.size();
    }
    
    public static boolean contains(Class<?> clazz, String propertyName) {
        ConcurrentMap<String, Method> classCache = cache.get(clazz);
        return classCache != null && classCache.containsKey(propertyName);
    }
}
```

---

## 4. Builder设计

### 4.1 MsgTemplateBuilder（统一入口）

```java
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

### 4.2 SimpleBuilder

{} 风格Builder，render方法支持指定Locale和使用默认Locale。

```java
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

### 4.3 NamedBuilder

{name} 风格Builder，支持Map参数和Bean参数。

```java
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

---

## 5. 加载器设计

### 5.1 MsgTemplateLoader接口

```java
public interface MsgTemplateLoader extends DyEnumsLoader<MsgTemplate> {
    
    @Override
    int load(Class<MsgTemplate> enumClass, BiFunction<String, String, MsgTemplate> factory);
    
    @Override
    boolean validateSource();
}
```

### 5.2 FileMsgTemplateLoader

文件加载器，支持Simple和Named分离加载。

```java
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

### 5.3 PropMsgTemplateLoader

```java
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

---

## 6. 配置文件格式

### 6.1 Simple风格配置

```properties
# msg_templates_simple.properties
# 格式：name_zh|name_en|template_zh|template_en|order

SYS_ERR_001=系统错误|System error|系统内部错误:{}|Internal system error:{}|1
SYS_ERR_002=参数错误|Parameter error|参数{}验证失败|Parameter {} validation failed|2
LOG_001=登录日志|Login log|用户{}于{}登录成功|User {} logged in at {}|20
```

### 6.2 Named风格配置

```properties
# msg_templates_named.properties
# 格式：name_zh|name_en|template_zh|template_en|order

SYS_ERR_001=系统错误|System error|系统内部错误:{reason}|Internal system error:{reason}|1
SYS_ERR_002=参数错误|Parameter error|参数{paramName}验证失败|Parameter {paramName} validation failed|2
LOG_001=登录日志|Login log|用户{userId}于{time}登录成功|User {userId} logged in at {time}|20
```

---

## 7. 使用示例

### 7.1 全局配置

```java
// 设置默认Locale（全局）
MsgTemplateConfig.setDefaultLocale(Locale.CHINA);

// 或通过字符串设置
MsgTemplateConfig.setDefaultLocale("zh_CN");

// 设置反射缓存容量
MsgTemplateConfig.setReflectCacheMaxSize(2048);

// 禁用反射缓存
MsgTemplateConfig.setReflectCacheEnabled(false);

// 重置为默认值
MsgTemplateConfig.reset();
```

### 7.2 初始化加载

```java
public class AppInitializer {
    public void init() {
        // 设置默认Locale
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
        
        // 加载Simple风格模板
        FileMsgTemplateLoader.forSimple("msg_templates_simple.properties")
            .load(MsgTemplate.class, null);
        
        // 加载Named风格模板
        FileMsgTemplateLoader.forNamed("msg_templates_named.properties")
            .load(MsgTemplate.class, null);
    }
}
```

### 7.3 Simple风格使用

```java
// 使用默认Locale
MsgTemplate template = EnumRegistry.valueOf(SimpleMsgTemplate.class, "SYS_ERR_001")
    .orElseThrow();
String msg = template.render("数据库连接超时");
// 输出：系统内部错误：数据库连接超时（使用默认Locale=CHINA）

// 指定Locale
String msg = template.render(Locale.US, "Database timeout");
// 输出：Internal system error: Database timeout

// Builder方式
String msg = MsgTemplateBuilder.create()
    .code("LOG_001")
    .simple()
    .args("admin", "2024-04-17 10:30:00")
    .render();
// 使用默认Locale

String msg = MsgTemplateBuilder.create()
    .code("LOG_001")
    .locale(Locale.US)
    .simple()
    .args("admin", "2024-04-17 10:30:00")
    .render();
// 使用指定的Locale.US
```

### 7.4 Named风格使用（Map参数）

```java
// 使用默认Locale
Map<String, Object> args = new HashMap<>();
args.put("userId", "admin");
args.put("time", "2024-04-17 10:30:00");

MsgTemplate template = EnumRegistry.valueOf(NamedMsgTemplate.class, "LOG_001")
    .orElseThrow();
String msg = template.render(args);
// 输出：用户admin于2024-04-17 10:30:00登录成功

// 指定Locale
String msg = template.render(Locale.US, args);
// 输出：User admin logged in at 2024-04-17 10:30:00

// Builder方式
String msg = MsgTemplateBuilder.create()
    .code("SYS_ERR_002")
    .named()
    .arg("paramName", "email")
    .render();
// 使用默认Locale

String msg = MsgTemplateBuilder.create()
    .code("SYS_ERR_002")
    .locale(Locale.US)
    .named()
    .arg("paramName", "email")
    .render();
// 输出：Parameter email validation failed
```

### 7.5 Named风格使用（Bean参数）

```java
public class LoginEvent {
    private String userId;
    private String time;
    
    public String getUserId() { return userId; }
    public String getTime() { return time; }
}

LoginEvent event = new LoginEvent();
event.userId = "admin";
event.time = "2024-04-17 10:30:00";

// 使用默认Locale
MsgTemplate template = EnumRegistry.valueOf(NamedMsgTemplate.class, "LOG_001")
    .orElseThrow();
String msg = template.render(event);
// 输出：用户admin于2024-04-17 10:30:00登录成功

// 指定Locale
String msg = template.render(Locale.US, event);
// 输出：User admin logged in at 2024-04-17 10:30:00

// Builder方式
String msg = MsgTemplateBuilder.create()
    .code("LOG_001")
    .named()
    .bean(event)
    .render();
// 使用默认Locale

String msg = MsgTemplateBuilder.create()
    .code("LOG_001")
    .locale(Locale.US)
    .named()
    .bean(event)
    .render();
// 使用Locale.US
```

---

## 8. Locale配置说明

### 8.1 默认Locale来源

默认Locale取值顺序：
1. `MsgTemplateConfig.getDefaultLocale()` - 全局配置（用户设置）
2. `Locale.getDefault()` - JVM默认Locale

### 8.2 设置方式

```java
// 方式1：全局配置（推荐）
MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
MsgTemplateConfig.setDefaultLocale("zh_CN");
MsgTemplateConfig.setDefaultLocale("en");

// 方式2：Builder指定（单次调用）
MsgTemplateBuilder.create()
    .locale(Locale.US)
    .simple()
    .args(...)
    .render();

// 方式3：render方法指定（单次调用）
template.render(Locale.US, args);
template.render(args);  // 使用默认Locale
```

### 8.3 Locale回退机制

当指定的Locale模板不存在时，回退顺序：
1. 指定Locale（如 Locale.CHINA -> "zh"）
2. 默认Locale（如 MsgTemplateConfig.getDefaultLocale() -> "zh"）
3. 英文Locale（"en"）作为兜底

```java
CompiledTemplate getCompiled(Locale locale) {
    return compiledTemplates.getOrDefault(
        locale.getLanguage(),
        compiledTemplates.getOrDefault(
            MsgTemplateConfig.getDefaultLocale().getLanguage(), 
            compiledTemplates.getOrDefault("en", CompiledTemplate.EMPTY)
        )
    );
}
```

---

## 9. 测试策略

### 9.1 单元测试

| 测试类 | 测试范围 |
|--------|----------|
| MsgTemplateConfigTest | 全局配置：默认Locale、反射缓存设置 |
| CompiledTemplateTest | 预编译结构 |
| TemplateCompilerTest | {}解析、{name}解析 |
| TemplateRendererTest | 位置填充、Map填充、Bean填充 |
| ReflectCacheTest | getter查找、缓存命中、容量控制 |
| SimpleMsgTemplateTest | {}风格模板、render方法 |
| NamedMsgTemplateTest | {name}风格模板、Map/Bean参数 |
| SimpleBuilderTest | {}风格Builder |
| NamedBuilderTest | {name}风格Builder |
| FileMsgTemplateLoaderTest | 文件加载、风格分离 |

### 9.2 性能测试

```java
@Test
public void testReflectCachePerformance() {
    MsgTemplateConfig.setReflectCacheEnabled(true);
    MsgTemplateConfig.setReflectCacheMaxSize(1024);
    
    LoginEvent event = new LoginEvent();
    event.userId = "admin";
    event.time = "2024-04-17";
    
    CompiledTemplate compiled = TemplateCompiler.compileNamed(
        "用户{userId}于{time}登录成功");
    
    ReflectCache.clear();
    long startNoCache = System.nanoTime();
    for (int i = 0; i < 10000; i++) {
        TemplateRenderer.renderNamedBean(compiled, event);
    }
    long elapsedNoCache = System.nanoTime() - startNoCache;
    
    long startWithCache = System.nanoTime();
    for (int i = 0; i < 100000; i++) {
        TemplateRenderer.renderNamedBean(compiled, event);
    }
    long elapsedWithCache = System.nanoTime() - startWithCache;
    
    System.out.println("No cache (10K): " + elapsedNoCache / 1_000_000 + "ms");
    System.out.println("With cache (100K): " + elapsedWithCache / 1_000_000 + "ms");
}

@Test
public void testDefaultLocale() {
    MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
    
    SimpleMsgTemplate template = SimpleMsgTemplate.fromValueString(
        "TEST", "测试|Test|默认中文:{}|Default English:{}|1");
    
    String msgDefault = template.render("value");
    assertEquals("默认中文:value", msgDefault);
    
    String msgEn = template.render(Locale.US, "value");
    assertEquals("Default English:value", msgEn);
}
```

---

## 10. 性能分析

### 10.1 预编译优势

| 操作 | 传统方式 | 预编译方式 |
|------|---------|-----------|
| 编译时机 | 每次渲染扫描 | 加载时一次性 |
| 渲染扫描 | O(template.length) | O(0) |
| 参数填充 | 动态查找位置 | 按索引直接填充 |

### 10.2 反射缓存优势

| 操作 | 无缓存 | 有缓存 |
|------|--------|--------|
| getter查找 | 每次反射查找 | 首次查找后缓存 |
| Bean属性读取 | O(methods.length) | O(1) |

### 10.3 默认Locale优化

- 全局配置避免每次调用都传入Locale参数
- Builder可选指定Locale，灵活兼顾便捷

### 10.4 线程安全

- CompiledTemplate不可变
- MsgTemplate实现类不可变
- MsgTemplateConfig使用volatile
- ReflectCache基于ConcurrentHashMap
- EnumRegistry基于ConcurrentHashMap
- 无锁竞争

---

## 11. 扩展点

### 11.1 自定义加载器

实现MsgTemplateLoader接口，支持：
- YAML文件
- Redis缓存
- 配置中心（Apollo、Nacos）
- 数据库（用户自行实现）

### 11.2 自定义反射缓存

替换ReflectCache实现：
- 使用Guava Cache（需引入依赖）
- 使用Caffeine Cache（高性能）
- 使用Spring Cache

### 11.3 预定义模板子类

继承SimpleMsgTemplate或NamedMsgTemplate：
- ErrorCode（错误码）
- LogTemplate（日志模板）
- NotificationTemplate（通知模板）

---

## 12. 与dyenums的关系

| dyenums提供 | jmsg-i18n扩展 |
|-------------|---------------|
| DyEnum接口 | MsgTemplate接口继承 |
| EnumRegistry注册表 | 直接使用 |
| DyEnumsLoader接口 | MsgTemplateLoader继承 |

---

## 13. 实现计划

详见后续实现计划文档。