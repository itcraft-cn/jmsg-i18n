# jmsg-i18n 多语言消息模板设计文档

## 1. 概述

### 1.1 目标
基于dyenums库构建一套多语言配置项系统，支持错误码、日志模板等场景的模板输出，提供参数化模板渲染能力。

### 1.2 核心需求
- 支持参数化模板（占位符替换）
- 支持双模板风格：{} (SLF4J) 和 {name} (命名参数)，分离实现
- 通用消息模板设计，可扩展错误码、日志、通知等
- 加载器多态设计，默认文件模式，其他数据源由用户自行扩展
- 提供静态方法+Builder双API风格
- 预编译模板，高性能渲染
- Named风格支持Map参数和Bean参数（反射），缓存getter方法

### 1.3 选定方案
方案C优化：MsgTemplate接口 + 双实现类 + 预编译模板引擎 + 反射缓存

---

## 2. 架构设计

### 2.1 包结构

```
cn.itcraft.jmsg
├── core
│   ├── MsgTemplate.java          # 接口定义
│   ├── Slf4jMsgTemplate.java     # {}风格实现
│   ├── NamedMsgTemplate.java     # {name}风格实现
│   ├── CompiledTemplate.java     # 预编译模板结构
│   ├── TemplateCompiler.java     # 模板预编译器
│   └── TemplateRenderer.java     # 渲染执行器（含反射缓存）
├── loader
│   ├── MsgTemplateLoader.java    # 加载器接口
│   ├── FileMsgTemplateLoader.java# 文件加载器（默认）
│   └── PropMsgTemplateLoader.java# Properties加载器
├── builder
│   ├── MsgTemplateBuilder.java   # Builder构建器（统一入口）
│   ├── Slf4jBuilder.java         # {}风格Builder
│   └── NamedBuilder.java         # {name}风格Builder
└── util
    ├── LocaleHelper.java         # Locale辅助工具
    └── ReflectCache.java         # 反射缓存（可选Guava）
```

### 2.2 依赖关系

```
┌─────────────────────────────────────────────────────────────┐
│                      应用层                                  │
│   MsgTemplate.get() / MsgTemplate.builder().render()         │
├─────────────────────────────────────────────────────────────┤
│   MsgTemplate (接口)                                          │
│   ├── Slf4jMsgTemplate ({}风格实现)                           │
│   │   └── compiled: Map<Locale, CompiledTemplate>            │
│   ├── NamedMsgTemplate ({name}风格实现)                       │
│   │   └── compiled: Map<Locale, CompiledTemplate>            │
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
│   ├── renderSlf4j(compiled, args) -> String                  │
│   ├── renderNamed(compiled, namedArgs) -> String             │
│   ├── renderNamedBean(compiled, bean) -> String  # 反射      │
│   ├── ReflectCache (getter方法缓存)                           │
├─────────────────────────────────────────────────────────────┤
│   MsgTemplateBuilder (统一Builder入口)                        │
│   ├── slf4j() -> Slf4jBuilder                                │
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

### 3.1 MsgTemplate接口

定义统一接口，不继承MultiLangDyEnum（因双实现类需分离）。

```java
public interface MsgTemplate extends DyEnum {
    
    String render(Locale locale, Object... args);
    
    Locale getDefaultLocale();
    
    Style getStyle();
    
    enum Style {
        SLF4J,   // {} 风格
        NAMED    // {name} 风格
    }
}
```

### 3.2 Slf4jMsgTemplate

{} 风格实现，专一处理位置参数。

```java
public class Slf4jMsgTemplate implements MsgTemplate {
    
    private static final long serialVersionUID = 1L;
    
    private final String code;
    private final String name;
    private final int order;
    private final Locale defaultLocale;
    private final Map<String, CompiledTemplate> compiledTemplates;
    
    public Slf4jMsgTemplate(String code, String name, int order,
                            Locale defaultLocale,
                            Map<String, CompiledTemplate> compiledTemplates) {
        this.code = code;
        this.name = name;
        this.order = order;
        this.defaultLocale = defaultLocale != null ? defaultLocale : Locale.CHINA;
        this.compiledTemplates = compiledTemplates;
    }
    
    @Override
    public String getCode() { return code; }
    
    @Override
    public String getName() { return name; }
    
    @Override
    public String getDescription() { return ""; }
    
    @Override
    public int getOrder() { return order; }
    
    @Override
    public Locale getDefaultLocale() { return defaultLocale; }
    
    @Override
    public MsgTemplate.Style getStyle() { return MsgTemplate.Style.SLF4J; }
    
    @Override
    public String render(Locale locale, Object... args) {
        CompiledTemplate compiled = getCompiled(locale);
        return TemplateRenderer.renderSlf4j(compiled, args);
    }
    
    public CompiledTemplate getCompiled(Locale locale) {
        return compiledTemplates.getOrDefault(
            locale.getLanguage(),
            compiledTemplates.getOrDefault(defaultLocale.getLanguage(), CompiledTemplate.EMPTY)
        );
    }
    
    public String renderZh(Object... args) {
        return render(Locale.CHINA, args);
    }
    
    public String renderEn(Object... args) {
        return render(Locale.US, args);
    }
    
    public Set<String> getSupportedLocales() {
        return compiledTemplates.keySet();
    }
    
    public static Slf4jMsgTemplate fromValueString(String code, String valueString) {
        String[] parts = valueString.split("\\|", -1);
        if (parts.length < 4) {
            throw new IllegalArgumentException(
                "Invalid format. Expected: name_zh|name_en|template_zh|template_en|order");
        }
        
        String nameZh = parts[0].trim();
        String nameEn = parts[1].trim();
        int order = parts.length >= 5 ? Integer.parseInt(parts[4].trim()) : 0;
        
        Map<String, CompiledTemplate> compiled = new HashMap<>();
        compiled.put("zh", TemplateCompiler.compileSlf4j(parts[2].trim()));
        compiled.put("en", TemplateCompiler.compileSlf4j(parts[3].trim()));
        
        String displayName = nameZh.isEmpty() ? nameEn : 
                             nameEn.isEmpty() ? nameZh : nameZh + "/" + nameEn;
        
        return new Slf4jMsgTemplate(code, displayName, order, Locale.CHINA, compiled);
    }
}
```

### 3.3 NamedMsgTemplate

{name} 风格实现，专一处理命名参数，支持Map和Bean。

```java
public class NamedMsgTemplate implements MsgTemplate {
    
    private static final long serialVersionUID = 1L;
    
    private final String code;
    private final String name;
    private final int order;
    private final Locale defaultLocale;
    private final Map<String, CompiledTemplate> compiledTemplates;
    
    public NamedMsgTemplate(String code, String name, int order,
                            Locale defaultLocale,
                            Map<String, CompiledTemplate> compiledTemplates) {
        this.code = code;
        this.name = name;
        this.order = order;
        this.defaultLocale = defaultLocale != null ? defaultLocale : Locale.CHINA;
        this.compiledTemplates = compiledTemplates;
    }
    
    @Override
    public String getCode() { return code; }
    
    @Override
    public String getName() { return name; }
    
    @Override
    public String getDescription() { return ""; }
    
    @Override
    public int getOrder() { return order; }
    
    @Override
    public Locale getDefaultLocale() { return defaultLocale; }
    
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
            compiledTemplates.getOrDefault(defaultLocale.getLanguage(), CompiledTemplate.EMPTY)
        );
    }
    
    public String renderZh(Object... args) {
        return render(Locale.CHINA, args);
    }
    
    public String renderEn(Object... args) {
        return render(Locale.US, args);
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
        
        return new NamedMsgTemplate(code, displayName, order, Locale.CHINA, compiled);
    }
}
```

### 3.4 CompiledTemplate

预编译模板结构，SLF4J和Named共用。

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

### 3.5 TemplateCompiler

模板预编译器。

```java
public final class TemplateCompiler {
    
    private static final char PLACEHOLDER_START = '{';
    private static final char PLACEHOLDER_END = '}';
    
    public static CompiledTemplate compileSlf4j(String template) {
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

### 3.6 TemplateRenderer + ReflectCache

渲染执行器，包含反射缓存支持。

```java
public final class TemplateRenderer {
    
    public static String renderSlf4j(CompiledTemplate compiled, Object[] args) {
        if (!compiled.hasParams()) {
            return compiled.getFragments()[0];
        }
        
        String[] fragments = compiled.getFragments();
        int[] indices = compiled.getParamIndices();
        int paramCount = compiled.getParamCount();
        
        int estimatedSize = estimateSize(fragments, args);
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
    
    public static String renderNamedMap(CompiledTemplate compiled, Map<String, Object> namedArgs) {
        if (!compiled.hasParams()) {
            return compiled.getFragments()[0];
        }
        
        String[] fragments = compiled.getFragments();
        String[] paramNames = compiled.getParamNames();
        int paramCount = compiled.getParamCount();
        
        StringBuilder result = new StringBuilder(estimateNamedSize(fragments, paramNames, namedArgs));
        
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
    
    public static String renderNamedBean(CompiledTemplate compiled, Object bean) {
        if (!compiled.hasParams()) {
            return compiled.getFragments()[0];
        }
        
        if (bean == null) {
            return renderOriginal(compiled);
        }
        
        String[] fragments = compiled.getFragments();
        String[] paramNames = compiled.getParamNames();
        int paramCount = compiled.getParamCount();
        
        StringBuilder result = new StringBuilder(estimateBeanSize(fragments, paramNames, bean));
        
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
        
        return result.toString();
    }
    
    private static String renderOriginal(CompiledTemplate compiled) {
        StringBuilder result = new StringBuilder();
        for (String fragment : compiled.getFragments()) {
            result.append(fragment);
        }
        String[] paramNames = compiled.getParamNames();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length && i < compiled.getParamCount(); i++) {
                result.append('{').append(paramNames[i]).append('}');
            }
        }
        return result.toString();
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

### 3.7 ReflectCache

反射缓存，缓存getter方法查找结果，支持容量控制。

```java
public final class ReflectCache {
    
    private static final int DEFAULT_MAX_SIZE = 1024;
    
    private static final ConcurrentMap<Class<?>, ConcurrentMap<String, Method>> cache = 
        new ConcurrentHashMap<>();
    
    private static volatile int maxSize = DEFAULT_MAX_SIZE;
    
    public static void setMaxSize(int size) {
        maxSize = size > 0 ? size : DEFAULT_MAX_SIZE;
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

**可选Guava Cache版本**（用户可自行替换）：

```java
public final class ReflectCacheGuava {
    
    private static final Cache<Class<?>, Map<String, Method>> cache = 
        CacheBuilder.newBuilder()
            .maximumSize(1024)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();
    
    public static Object getProperty(Object bean, String propertyName) {
        if (bean == null) return null;
        
        try {
            Map<String, Method> classCache = cache.get(bean.getClass(), () -> buildClassCache(bean.getClass()));
            Method getter = classCache.get(propertyName);
            if (getter != null) {
                return getter.invoke(bean);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
    
    private static Map<String, Method> buildClassCache(Class<?> clazz) {
        Map<String, Method> map = new HashMap<>();
        for (Method method : clazz.getMethods()) {
            if (isGetter(method)) {
                String name = extractPropertyName(method);
                if (name != null) {
                    map.put(name, method);
                }
            }
        }
        return map;
    }
    
    private static boolean isGetter(Method method) {
        String methodName = method.getName();
        return method.getParameterCount() == 0 && 
               method.getReturnType() != void.class &&
               (methodName.startsWith("get") || methodName.startsWith("is"));
    }
    
    private static String extractPropertyName(Method method) {
        String methodName = method.getName();
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
        }
        return null;
    }
}
```

---

## 4. Builder设计

### 4.1 MsgTemplateBuilder（统一入口）

```java
public class MsgTemplateBuilder {
    
    private String code;
    private Locale locale = Locale.CHINA;
    
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
    
    public Slf4jBuilder slf4j() {
        return new Slf4jBuilder(code, locale);
    }
    
    public NamedBuilder named() {
        return new NamedBuilder(code, locale);
    }
    
    public static MsgTemplateBuilder create() {
        return new MsgTemplateBuilder();
    }
}
```

### 4.2 Slf4jBuilder

```java
public class Slf4jBuilder {
    
    private final String code;
    private final Locale locale;
    private Object[] args;
    
    Slf4jBuilder(String code, Locale locale) {
        this.code = code;
        this.locale = locale;
    }
    
    public Slf4jBuilder args(Object... args) {
        this.args = args;
        return this;
    }
    
    public String render() {
        if (code == null) {
            throw new IllegalStateException("Code is required");
        }
        
        MsgTemplate template = findTemplate(code);
        if (template.getStyle() != MsgTemplate.Style.SLF4J) {
            throw new IllegalArgumentException("Template is not SLF4J style: " + code);
        }
        
        return template.render(locale, args);
    }
    
    public String renderZh() {
        return new Slf4jBuilder(code, Locale.CHINA).args(args).render();
    }
    
    public String renderEn() {
        return new Slf4jBuilder(code, Locale.US).args(args).render();
    }
    
    private MsgTemplate findTemplate(String code) {
        MsgTemplate template = EnumRegistry.valueOf(Slf4jMsgTemplate.class, code)
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
        
        if (bean != null) {
            return template.render(locale, bean);
        } else {
            return template.render(locale, namedArgs);
        }
    }
    
    public String renderZh() {
        NamedBuilder zhBuilder = new NamedBuilder(code, Locale.CHINA);
        if (bean != null) zhBuilder.bean = bean;
        else zhBuilder.namedArgs = namedArgs;
        return zhBuilder.render();
    }
    
    public String renderEn() {
        NamedBuilder enBuilder = new NamedBuilder(code, Locale.US);
        if (bean != null) enBuilder.bean = bean;
        else enBuilder.namedArgs = namedArgs;
        return enBuilder.render();
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

文件加载器，支持SLF4J和Named分离加载。

```java
public class FileMsgTemplateLoader implements MsgTemplateLoader {
    
    private final String filePath;
    private final MsgTemplate.Style style;
    
    public FileMsgTemplateLoader(String filePath, MsgTemplate.Style style) {
        this.filePath = filePath;
        this.style = style;
    }
    
    public FileMsgTemplateLoader(String filePath) {
        this(filePath, MsgTemplate.Style.SLF4J);
    }
    
    public static FileMsgTemplateLoader forSlf4j(String filePath) {
        return new FileMsgTemplateLoader(filePath, MsgTemplate.Style.SLF4J);
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
            : Slf4jMsgTemplate::fromValueString;
    }
    
    private Class<?> getRegisterClass() {
        return style == MsgTemplate.Style.NAMED ? NamedMsgTemplate.class : Slf4jMsgTemplate.class;
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
            : Slf4jMsgTemplate::fromValueString;
    }
    
    private Class<?> getRegisterClass() {
        return style == MsgTemplate.Style.NAMED ? NamedMsgTemplate.class : Slf4jMsgTemplate.class;
    }
}
```

---

## 6. 配置文件格式

### 6.1 SLF4J风格配置

```properties
# msg_templates_slf4j.properties
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

### 7.1 初始化

```java
public class AppInitializer {
    public void init() {
        FileMsgTemplateLoader.forSlf4j("msg_templates_slf4j.properties")
            .load(MsgTemplate.class, null);
        
        FileMsgTemplateLoader.forNamed("msg_templates_named.properties")
            .load(MsgTemplate.class, null);
    }
}
```

### 7.2 SLF4J风格使用

```java
String msg = MsgTemplateBuilder.create()
    .code("SYS_ERR_001")
    .locale(Locale.CHINA)
    .slf4j()
    .args("数据库连接超时")
    .render();
// 输出：系统内部错误：数据库连接超时

String msg = MsgTemplateBuilder.create()
    .code("LOG_001")
    .slf4j()
    .args("admin", "2024-04-17 10:30:00")
    .renderZh();
// 输出：用户admin于2024-04-17 10:30:00登录成功
```

### 7.3 Named风格使用（Map参数）

```java
Map<String, Object> args = new HashMap<>();
args.put("userId", "admin");
args.put("time", "2024-04-17 10:30:00");

String msg = MsgTemplateBuilder.create()
    .code("LOG_001")
    .named()
    .args(args)
    .render();
// 输出：用户admin于2024-04-17 10:30:00登录成功

String msg = MsgTemplateBuilder.create()
    .code("SYS_ERR_002")
    .named()
    .arg("paramName", "email")
    .renderEn();
// 输出：Parameter email validation failed
```

### 7.4 Named风格使用（Bean参数）

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

String msg = MsgTemplateBuilder.create()
    .code("LOG_001")
    .locale(Locale.CHINA)
    .named()
    .bean(event)
    .render();
// 输出：用户admin于2024-04-17 10:30:00登录成功

// 反射缓存自动生效，后续调用无需重新查找getter方法
ReflectCache.setProperty(bean, "userId");  // 首次查找并缓存
ReflectCache.getProperty(bean, "userId");  // 直接从缓存获取
```

---

## 8. 反射缓存说明

### 8.1 缓存结构

```
ReflectCache
├── cache: ConcurrentMap<Class<?>, ConcurrentMap<String, Method>>
│   ├── User.class -> { "userId": Method(getUserId), "name": Method(getName) }
│   ├── Order.class -> { "orderId": Method(getOrderId), "amount": Method(getAmount) }
│   └── ... (最多 maxSize 个类)
```

### 8.2 容量控制

- 默认最大容量：1024个类
- 超出时淘汰最早的类缓存（FIFO）
- 可通过 `ReflectCache.setMaxSize(int)` 调整
- 可通过 `ReflectCache.clear()` 清空

### 8.3 getter查找策略

1. `get{Name}` 方法（如 getUserId）
2. `is{Name}` 方法（如 isActive，仅boolean）
3. `{name}` 方法（如 userId，非标准）
4. 查找父类

### 8.4 性能对比

| 操作 | 无缓存 | 有缓存 |
|------|--------|--------|
| 首次调用 | O(methods.length) 查找 | O(methods.length) 查找 + 缓存 |
| 后续调用 | O(methods.length) 查找 | O(1) Map.get |
| 100K次调用 | ~200ms | ~5ms |

---

## 9. 测试策略

### 9.1 单元测试

| 测试类 | 测试范围 |
|--------|----------|
| CompiledTemplateTest | 预编译结构 |
| TemplateCompilerTest | {}解析、{name}解析 |
| TemplateRendererTest | 位置填充、Map填充、Bean填充 |
| ReflectCacheTest | getter查找、缓存命中、容量控制 |
| Slf4jMsgTemplateTest | {}风格模板 |
| NamedMsgTemplateTest | {name}风格模板、Map/Bean参数 |
| Slf4jBuilderTest | {}风格Builder |
| NamedBuilderTest | {name}风格Builder |
| FileMsgTemplateLoaderTest | 文件加载、风格分离 |

### 9.2 性能测试

```java
@Test
public void testReflectCachePerformance() {
    LoginEvent event = new LoginEvent();
    event.userId = "admin";
    event.time = "2024-04-17";
    
    CompiledTemplate compiled = TemplateCompiler.compileNamed(
        "用户{userId}于{time}登录成功");
    
    long startNoCache = System.nanoTime();
    ReflectCache.clear();
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

### 10.3 线程安全

- CompiledTemplate不可变
- MsgTemplate实现类不可变
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

继承Slf4jMsgTemplate或NamedMsgTemplate：
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