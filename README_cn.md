# jmsg-i18n

中文文档 | [English](README.md)

高性能Java多语言消息模板系统，支持 `{}` (Simple) 和 `{name}` (Named) 双占位符风格，支持无限语言。

## 特性

- **双模板风格**: 支持位置占位符 `{}` 和命名占位符 `{name}`
- **无限语言**: YAML 配置支持任意多语言 (zh-CN, en-US, en-GB, ja-JP 等)
- **高性能**: 预编译模板 + StringBuilder池化
- **ReflectCache**: Getter方法缓存，Bean渲染性能提升10倍
- **灵活加载**: YAML 加载器，支持 Locale 回退链
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
    <version>1.2.0</version>
</dependency>
```

### 模板 YAML 文件

```yaml
# templates_simple.yaml ({} 风格)
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

# templates_named.yaml ({name} 风格)
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

### 使用示例

```java
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.builder.MsgTemplateBuilder;
import cn.itcraft.jmsg.loader.YamlMsgTemplateLoader;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

// 初始化配置
MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
YamlMsgTemplateLoader.forSimple("templates_simple.yaml").load(MsgTemplate.class, null);
YamlMsgTemplateLoader.forNamed("templates_named.yaml").load(MsgTemplate.class, null);

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

// Locale回退: zh -> zh-CN, en -> en-US, 无匹配 -> default locale
String msg = MsgTemplateBuilder.create()
    .code("ERR_001")
    .locale(Locale.UK)  // 匹配 en-GB 模板
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
│   └ LocaleHelper.java             # Locale解析 (zh-CN/zh_CN)
├── loader/                          # 加载器
│   ├── MsgTemplateLoader.java      # 接口 (继承DyEnumsLoader)
│   └ YamlMsgTemplateLoader.java    # YAML模板加载器
└── builder/                         # Builder模式
    ├── MsgTemplateBuilder.java     # 入口Builder
    ├── SimpleBuilder.java          # Simple风格Builder
    └ NamedBuilder.java             # Named风格Builder
```

## 配置说明

```java
// 设置默认Locale
MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
MsgTemplateConfig.setDefaultLocale("zh-CN");  // 字符串格式 (支持 zh-CN 和 zh_CN)

// 配置ReflectCache
MsgTemplateConfig.setReflectCacheEnabled(true);  // 默认启用
MsgTemplateConfig.setReflectCacheMaxSize(1024);  // 缓存大小

// 重置为默认值
MsgTemplateConfig.reset();
```

## 模板格式

YAML 格式，支持无限语言：

```yaml
templates:
  CODE_001:
    order: 1                    # 显示顺序 (可选，默认0)
    default: zh-CN              # 默认Locale (必填)
    messages:
      zh-CN: 模板{param}        # Locale → 模板映射
      en-US: Template {param}
      en-GB: Template {param}
      ja-JP: テンプレート{param}
```

**Locale 格式**: 同时支持 `zh-CN` (连字符) 和 `zh_CN` (下划线)

**Locale 回退**:
1. 精确匹配: `zh-CN` → `zh-CN` 模板
2. 语言匹配: `zh` → `zh-CN` 或 `zh_CN` 模板
3. 默认: 无匹配 → `default` Locale 模板

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

- 基于 [dyenums](https://github.com/itcraft-cn/dyenums) 动态枚举框架构建
- 使用 [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml) 2.2 解析 YAML

## License

Apache License 2.0