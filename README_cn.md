# jmsg-i18n

中文文档 | [English](README.md)

高性能Java多语言消息模板系统，支持 `{}` (Simple) 和 `{name}` (Named) 双占位符风格。

## 特性

- **双模板风格**: 支持位置占位符 `{}` 和命名占位符 `{name}`
- **多语言支持**: 内置中/英文模板存储，Locale回退链
- **高性能**: 预编译模板 + StringBuilder池化
- **ReflectCache**: Getter方法缓存，Bean渲染性能提升10倍
- **灵活加载**: 文件/Properties加载器，可扩展数据库源
- **Builder模式**: 流式API，简洁易用

## 性能测试

JMH基准测试结果 (JDK 25, 4线程):

| 方法 | 吞吐量 (ops/s) | vs MessageFormat |
|------|---------------|-----------------|
| jmsg_simple | 92M | **快4.9倍** |
| jmsg_namedMap | 58M | 快3.1倍 |
| jmsg_namedBean | 49M | 快2.6倍 |
| jdk_messageFormat | 19M | 基准 |
| slf4j_style_manual | 61M | 快3.2倍 |

**核心优化**:
- ReflectCache使反射性能提升10倍 (258M vs 25M ops/s)
- StringBuilderPool达到297M ops/s
- TemplateCompiler预编译模板加速渲染

## 快速开始

### Maven依赖

```xml
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jmsg-i18n</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 模板属性文件

```properties
# templates_simple.properties ({} 风格)
ERR_001=错误|Error|内部错误:{}|Internal error:{}|1
LOG_001=登录|Login|用户{}于{}登录|User {} logged in at {}|10

# templates_named.properties ({name} 风格)
ERR_001=错误|Error|内部错误:{reason}|Internal error:{reason}|1
LOG_001=登录|Login|用户{userId}于{time}登录|User {userId} logged in at {time}|10
```

### 使用示例

```java
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.builder.MsgTemplateBuilder;
import cn.itcraft.jmsg.loader.FileMsgTemplateLoader;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

// 初始化配置
MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
FileMsgTemplateLoader.forSimple("templates_simple.properties").load(MsgTemplate.class, null);
FileMsgTemplateLoader.forNamed("templates_named.properties").load(MsgTemplate.class, null);

// Simple风格 - {} 占位符
String msg = MsgTemplateBuilder.create()
    .code("ERR_001")
    .simple()
    .args("数据库超时")
    .render();
// 结果: "内部错误:数据库超时"

// Named风格 - Map参数
Map<String, Object> args = new HashMap<>();
args.put("userId", "admin");
args.put("time", "2024-04-17");

String msg = MsgTemplateBuilder.create()
    .code("LOG_001")
    .named()
    .args(args)
    .render();
// 结果: "用户admin于2024-04-17登录"

// Named风格 - Bean参数 (使用ReflectCache)
LoginEvent event = new LoginEvent();
event.userId = "admin";
event.time = "2024-04-17";

String msg = MsgTemplateBuilder.create()
    .code("LOG_001")
    .named()
    .bean(event)
    .render();

// 指定Locale
String msg = MsgTemplateBuilder.create()
    .code("ERR_001")
    .locale(Locale.US)
    .simple()
    .args("timeout")
    .render();
// 结果: "Internal error:timeout"
```

## 架构设计

```
jmsg-i18n/
├── core/                           # 核心模块
│   ├── MsgTemplate.java            # 接口 (继承DyEnum)
│   ├── SimpleMsgTemplate.java      # {} 风格实现
│   ├── NamedMsgTemplate.java       # {name} 风格实现
│   ├── CompiledTemplate.java       # 预编译模板结构
│   ├── TemplateCompiler.java       # 模式编译器
│   ├── TemplateRenderer.java       # 高性能渲染器
│   └── MsgTemplateConfig.java      # 全局配置
├── util/                            # 工具类
│   ├── StringBuilderPool.java      # ThreadLocal StringBuilder池
│   ├── ReflectCache.java           # Getter方法缓存
│   └ LocaleHelper.java             # Locale解析工具
├── loader/                          # 加载器
│   ├── MsgTemplateLoader.java      # 接口 (继承DyEnumsLoader)
│   ├── FileMsgTemplateLoader.java  # 文件Properties加载
│   └ PropMsgTemplateLoader.java    # 内存Properties加载
└── builder/                         # Builder模式
    ├── MsgTemplateBuilder.java     # 入口Builder
    ├── SimpleBuilder.java          # Simple风格Builder
    └ NamedBuilder.java             # Named风格Builder
```

## 配置说明

```java
// 设置默认Locale
MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
MsgTemplateConfig.setDefaultLocale("en-US");  // 字符串格式

// 配置ReflectCache
MsgTemplateConfig.setReflectCacheEnabled(true);  // 默认启用
MsgTemplateConfig.setReflectCacheMaxSize(1024);  // 缓存大小

// 重置为默认值
MsgTemplateConfig.reset();
```

## 模板格式

属性文件格式: `名称_zh|名称_en|模板_zh|模板_en|排序`

```properties
# 示例
CODE_001=名称|Name|模板{param}|Template {param}|1
```

- `名称_zh`: 中文显示名称
- `名称_en`: 英文显示名称
- `模板_zh`: 中文模板（含占位符）
- `模板_en`: 英文模板（含占位符）
- `排序`: 显示顺序（整数）

## 基准测试

运行JMH基准测试:

```bash
# 编译
mvn clean compile test-compile

# 运行测试
./run_bench.sh MsgTemplateBenchmark
./run_bench.sh TemplateCompareBenchmark
```

## 依赖项目

基于 [dyenums](https://github.com/itcraft-cn/dyenums) 动态枚举框架构建。

## License

Apache License 2.0