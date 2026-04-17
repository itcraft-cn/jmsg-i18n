# jmsg-i18n 多语言消息模板设计文档

## 1. 概述

### 1.1 目标
基于dyenums库构建一套多语言配置项系统，支持错误码、日志模板等场景的模板输出，提供参数化模板渲染能力。

### 1.2 核心需求
- 支持参数化模板（占位符替换）
- 支持双模板风格：{} (SLF4J) 和 {name} (命名参数)
- 通用消息模板设计，可扩展错误码、日志、通知等
- 加载器多态设计，默认文件模式，易扩展数据库/API等
- 提供静态方法+Builder双API风格

### 1.3 选定方案
方案C：MsgTemplate继承MultiLangDyEnum + 内置可配置模板引擎 + 双模板存储

---

## 2. 架构设计

### 2.1 包结构

```
cn.itcraft.jmsg
├── core
│   ├── MsgTemplate.java          # 核心类，继承MultiLangDyEnum
│   ├── MsgTemplateRenderer.java  # 模板渲染引擎接口
│   ├── Slf4jStyleRenderer.java   # {} 风格渲染器实现
│   ├── NamedStyleRenderer.java   # {name} 风格渲染器实现
│   └── MsgTemplateConfig.java    # 全局配置类
├── loader
│   ├── MsgTemplateLoader.java    # 加载器接口（继承DyEnumsLoader）
│   ├── FileMsgTemplateLoader.java# 文件加载器（默认）
│   ├── PropMsgTemplateLoader.java# Properties加载器
│   └── DbMsgTemplateLoader.java  # 数据库加载器（扩展）
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
│   ├── messagesSlf4j: Map<Locale, String>                     │
│   ├── messagesNamed: Map<Locale, String>                     │
│   ├── render(), renderNamed()                                │
├─────────────────────────────────────────────────────────────┤
│   MsgTemplateRenderer (渲染引擎接口)                          │
│   ├── Slf4jStyleRenderer: {}占位符                           │
│   ├── NamedStyleRenderer: {name}占位符                        │
├─────────────────────────────────────────────────────────────┤
│   MsgTemplateLoader (加载器接口，继承DyEnumsLoader)            │
│   ├── FileMsgTemplateLoader (默认)                           │
│   ├── PropMsgTemplateLoader                                  │
│   ├── DbMsgTemplateLoader (扩展)                             │
├─────────────────────────────────────────────────────────────┤
│   dyenums-core (基础依赖)                                     │
│   ├── MultiLangDyEnum                                        │
│   ├── EnumRegistry                                           │
│   ├── DyEnumsLoader                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 核心类设计

### 3.1 MsgTemplate

继承MultiLangDyEnum，双模板存储，内置渲染能力。

```java
public class MsgTemplate extends MultiLangDyEnum {
    
    private static final long serialVersionUID = 1L;
    
    private final Map<String, String> messagesSlf4j;
    private final Map<String, String> messagesNamed;
    
    private static volatile MsgTemplateRenderer slf4jRenderer = new Slf4jStyleRenderer();
    private static volatile MsgTemplateRenderer namedRenderer = new NamedStyleRenderer();
    
    protected MsgTemplate(String code, String name, int order,
                          Map<String, String> messagesSlf4j,
                          Map<String, String> messagesNamed) {
        super(code, name, order, messagesNamed);
        this.messagesSlf4j = messagesSlf4j;
        this.messagesNamed = messagesNamed;
    }
    
    // ========== 静态API ==========
    
    public static String get(String code, Locale locale, Object... args) {
        MsgTemplate template = EnumRegistry.valueOf(MsgTemplate.class, code)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + code));
        return template.renderSlf4j(locale, args);
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
    
    public String renderSlf4j(Locale locale, Object... args) {
        String template = getSlf4jTemplate(locale);
        return slf4jRenderer.render(template, args);
    }
    
    public String renderNamed(Locale locale, Map<String, Object> namedArgs) {
        String template = getNamedTemplate(locale);
        return namedRenderer.renderNamed(template, namedArgs);
    }
    
    public String getSlf4jTemplate(Locale locale) {
        return messagesSlf4j.getOrDefault(locale.getLanguage(), messagesSlf4j.get("en"));
    }
    
    public String getNamedTemplate(Locale locale) {
        return messagesNamed.getOrDefault(locale.getLanguage(), messagesNamed.get("en"));
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
        
        Map<String, String> messagesSlf4j = new HashMap<>();
        messagesSlf4j.put("zh", parts[2].trim());
        messagesSlf4j.put("en", parts[3].trim());
        
        Map<String, String> messagesNamed = new HashMap<>();
        messagesNamed.put("zh", parts[4].trim());
        messagesNamed.put("en", parts[5].trim());
        
        return new MsgTemplate(code, nameZh + "/" + nameEn, order, messagesSlf4j, messagesNamed);
    }
    
    // ========== 渲染器配置 ==========
    
    public static void setSlf4jRenderer(MsgTemplateRenderer renderer) {
        slf4jRenderer = renderer;
    }
    
    public static void setNamedRenderer(MsgTemplateRenderer renderer) {
        namedRenderer = renderer;
    }
}
```

### 3.2 MsgTemplateRenderer

渲染引擎接口。

```java
public interface MsgTemplateRenderer {
    
    String render(String template, Object[] args);
    
    String renderNamed(String template, Map<String, Object> namedArgs);
}
```

### 3.3 Slf4jStyleRenderer

{} 占位符风格渲染器。

```java
public class Slf4jStyleRenderer implements MsgTemplateRenderer {
    
    @Override
    public String render(String template, Object[] args) {
        if (template == null || args == null || args.length == 0) {
            return template;
        }
        
        StringBuilder result = new StringBuilder();
        int argIndex = 0;
        int i = 0;
        
        while (i < template.length()) {
            if (i + 1 < template.length() && template.charAt(i) == '{' && template.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    result.append(args[argIndex]);
                    argIndex++;
                } else {
                    result.append("{}");
                }
                i += 2;
            } else {
                result.append(template.charAt(i));
                i++;
            }
        }
        
        return result.toString();
    }
    
    @Override
    public String renderNamed(String template, Map<String, Object> namedArgs) {
        throw new UnsupportedOperationException("Slf4jStyleRenderer does not support named args");
    }
}
```

### 3.4 NamedStyleRenderer

{name} 占位符风格渲染器。

```java
public class NamedStyleRenderer implements MsgTemplateRenderer {
    
    @Override
    public String render(String template, Object[] args) {
        throw new UnsupportedOperationException("NamedStyleRenderer does not support positional args");
    }
    
    @Override
    public String renderNamed(String template, Map<String, Object> namedArgs) {
        if (template == null || namedArgs == null || namedArgs.isEmpty()) {
            return template;
        }
        
        String result = template;
        for (Map.Entry<String, Object> entry : namedArgs.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = String.valueOf(entry.getValue());
            result = result.replace(placeholder, value);
        }
        
        return result;
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
            return template.renderSlf4j(locale, args);
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

### 4.4 DbMsgTemplateLoader

数据库加载器（扩展点）。

```java
public class DbMsgTemplateLoader implements MsgTemplateLoader {
    
    private final DataSource dataSource;
    private final String tableName;
    
    public DbMsgTemplateLoader(DataSource dataSource) {
        this(dataSource, "msg_template");
    }
    
    public DbMsgTemplateLoader(DataSource dataSource, String tableName) {
        this.dataSource = dataSource;
        this.tableName = tableName;
    }
    
    @Override
    public int load(Class<MsgTemplate> enumClass, BiFunction<String, String, MsgTemplate> factory) {
        String sql = "SELECT code, name_zh, name_en, tpl_slf4j_zh, tpl_slf4j_en, " +
                     "tpl_named_zh, tpl_named_en, order FROM " + tableName;
        
        int count = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String code = rs.getString("code");
                String valueString = buildValueString(rs);
                MsgTemplate template = factory.apply(code, valueString);
                EnumRegistry.register(enumClass, template);
                count++;
            }
            return count;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load templates from database", e);
        }
    }
    
    private String buildValueString(ResultSet rs) throws SQLException {
        return rs.getString("name_zh") + "|" +
               rs.getString("name_en") + "|" +
               rs.getString("tpl_slf4j_zh") + "|" +
               rs.getString("tpl_slf4j_en") + "|" +
               rs.getString("tpl_named_zh") + "|" +
               rs.getString("tpl_named_en") + "|" +
               rs.getInt("order");
    }
    
    @Override
    public boolean validateSource() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
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
# 扩展葡萄牙语、俄语
SYS_ERR_001=系统错误|System error|系统内部错误:{}|Internal system error:{}|系统内部错误:{reason}|Internal system error:{reason}|pt:Erro interno:{}|pt:Erro interno:{reason}|ru:Внутренняя ошибка:{}|ru:Внутренняя ошибка:{reason}|1
```

扩展格式解析规则：
- 基础6段后，可选添加语言扩展：`langCode:template` 格式
- 解析时检测扩展语言并加入对应Map

---

## 6. 使用示例

### 6.1 初始化

```java
// 应用启动时加载模板
public class AppInitializer {
    public void init() {
        FileMsgTemplateLoader loader = new FileMsgTemplateLoader("msg_templates.properties");
        loader.load(MsgTemplate.class, MsgTemplate::fromValueString);
    }
}
```

### 6.2 静态方法调用

```java
// {}风格参数
String msg = MsgTemplate.get("SYS_ERR_001", Locale.CHINA, "数据库连接超时");
// 输出：系统内部错误：数据库连接超时

String msgEn = MsgTemplate.getEn("SYS_ERR_002", "email");
// 输出：Parameter email validation failed

// {name}风格参数
Map<String, Object> args = new HashMap<>();
args.put("userId", "admin");
args.put("resource", "/admin/dashboard");
String msg = MsgTemplate.getNamed("SYS_ERR_003", Locale.CHINA, args);
// 输出：用户admin无权限访问资源/admin/dashboard
```

### 6.3 Builder模式调用

```java
// {}风格
String msg = MsgTemplate.builder()
    .code("LOG_001")
    .locale(Locale.CHINA)
    .args("admin", "2024-04-17 10:30:00")
    .render();
// 输出：用户admin于2024-04-17 10:30:00登录成功

// {name}风格
String msg = MsgTemplate.builder()
    .code("LOG_002")
    .locale(Locale.US)
    .namedArg("userId", "john")
    .namedArg("action", "delete")
    .render();
// 输出：User john performed delete operation

// 快捷方法
String msgZh = MsgTemplate.builder()
    .code("BIZ_ERR_001")
    .namedArg("orderId", "ORD-12345")
    .renderZh();
```

### 6.4 自定义渲染器

```java
// 替换默认渲染器
MsgTemplate.setSlf4jRenderer(new CustomSlf4jRenderer());
MsgTemplate.setNamedRenderer(new CustomNamedRenderer());

// 自定义渲染器实现
public class CustomNamedRenderer implements MsgTemplateRenderer {
    @Override
    public String renderNamed(String template, Map<String, Object> namedArgs) {
        // 支持嵌套占位符、条件渲染等高级特性
        return processTemplate(template, namedArgs);
    }
}
```

---

## 7. 数据库表设计（可选扩展）

```sql
CREATE TABLE msg_template (
    code VARCHAR(50) PRIMARY KEY,
    name_zh VARCHAR(100),
    name_en VARCHAR(100),
    tpl_slf4j_zh VARCHAR(500),
    tpl_slf4j_en VARCHAR(500),
    tpl_named_zh VARCHAR(500),
    tpl_named_en VARCHAR(500),
    order INT DEFAULT 0,
    category VARCHAR(20),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 扩展多语言表
CREATE TABLE msg_template_locale (
    template_code VARCHAR(50),
    locale VARCHAR(10),
    tpl_slf4j VARCHAR(500),
    tpl_named VARCHAR(500),
    PRIMARY KEY (template_code, locale)
);
```

---

## 8. 测试策略

### 8.1 单元测试

| 测试类 | 测试范围 |
|--------|----------|
| MsgTemplateTest | 核心类功能：静态方法、实例方法、工厂方法 |
| Slf4jStyleRendererTest | {}风格渲染：空参数、多参数、占位符位置 |
| NamedStyleRendererTest | {name}风格渲染：单参数、多参数、缺失参数 |
| MsgTemplateBuilderTest | Builder模式：完整链式调用、缺失参数校验 |
| FileMsgTemplateLoaderTest | 文件加载：正常加载、格式错误、文件缺失 |
| DbMsgTemplateLoaderTest | 数据库加载：模拟数据源、SQL执行 |

### 8.2 集成测试

```java
@Test
public void testFullWorkflow() {
    // 加载
    FileMsgTemplateLoader loader = new FileMsgTemplateLoader("test_templates.properties");
    loader.load(MsgTemplate.class, MsgTemplate::fromValueString);
    
    // 验证注册
    assertTrue(EnumRegistry.contains(MsgTemplate.class, "TEST_001"));
    
    // 验证渲染
    MsgTemplate template = EnumRegistry.valueOf(MsgTemplate.class, "TEST_001").get();
    assertEquals("测试消息:test", template.renderSlf4j(Locale.CHINA, "test"));
}
```

---

## 9. 性能考虑

### 9.1 内存结构

- 每个MsgTemplate实例存储两个Map（messagesSlf4j、messagesNamed）
- 典型场景：100个模板 * 4种语言 * 2种风格 ≈ 800条消息文本
- 内存占用预估：100KB以内

### 9.2 渲染性能

- SLF4J风格：线性扫描模板，O(n)复杂度
- Named风格：Map遍历替换，O(m)复杂度（m=参数数量）
- 无正则表达式，性能优于MessageFormat

### 9.3 线程安全

- MsgTemplate实例不可变（final字段）
- EnumRegistry基于ConcurrentHashMap
- 渲染器可替换（volatile变量），无锁竞争

---

## 10. 扩展点

### 10.1 自定义加载器

实现MsgTemplateLoader接口，支持任意数据源：
- YAML文件
- Redis缓存
- 远程配置中心（Apollo、Nacos）

### 10.2 自定义渲染器

实现MsgTemplateRenderer接口，支持高级特性：
- 条件渲染：`{if:condition}content{endif}`
- 循环渲染：`{for:item in list}item.name{endfor}`
- 国际化格式化：数字、日期、货币格式

### 10.3 预定义模板子类

继承MsgTemplate创建领域专用模板：
- ErrorCode（错误码）- 添加HTTP状态码、错误级别
- LogTemplate（日志模板）- 添加日志级别、日志分类
- NotificationTemplate（通知模板）- 添加通知渠道、推送策略

---

## 11. 与dyenums的关系

| dyenums提供 | jmsg-i18n扩展 |
|-------------|---------------|
| MultiLangDyEnum基类 | MsgTemplate继承并扩展双模板存储 |
| EnumRegistry注册表 | 直接使用，无修改 |
| DyEnumsLoader接口 | MsgTemplateLoader继承，实现模板专用加载 |
| BaseDyEnum功能 | 通过继承链间接复用 |

jmsg-i18n作为dyenums的扩展库，遵循dyenums的设计原则：
- 类型安全
- 线程安全
- 不可变实例
- 可扩展加载器

---

## 12. 实现计划

详见后续实现计划文档（由writing-plans skill生成）。