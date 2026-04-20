# jmsg-i18n

[中文文档](README_cn.md) | English

A high-performance multi-language message template system for Java, supporting both `{}` (Simple) and `{name}` (Named) placeholder styles with unlimited locale support.

## Features

- **Dual Template Styles**: Support positional `{}` and named `{name}` placeholders
- **Unlimited Locales**: YAML configuration supports any number of languages (zh-CN, en-US, en-GB, ja-JP, etc.)
- **High Performance**: Pre-compiled templates with StringBuilder pooling
- **ReflectCache**: 10x faster getter method caching for Bean rendering
- **Flexible Loading**: YAML loader with locale fallback chain
- **Builder Pattern**: Fluent API for easy template rendering

## Performance

JMH Benchmark Results (JDK 25, 4 threads):

| Method | Throughput (ops/s) | vs MessageFormat |
|--------|-------------------|------------------|
| jmsg_simple | 92M | **4.9x faster** |
| jmsg_namedMap | 58M | 3.1x faster |
| jmsg_namedBean | 49M | 2.6x faster |
| jdk_messageFormat | 19M | baseline |
| slf4j_style_manual | 61M | 3.2x faster |

**Key optimizations**:
- ReflectCache enables 10x speedup (258M vs 25M ops/s)
- StringBuilderPool achieves 297M ops/s
- TemplateCompiler pre-compiles patterns for fast rendering

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jmsg-i18n</artifactId>
    <version>1.2.0</version>
</dependency>
```

### Template YAML File

```yaml
# templates_simple.yaml ({} style)
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

# templates_named.yaml ({name} style)
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

### Usage Examples

```java
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.builder.MsgTemplateBuilder;
import cn.itcraft.jmsg.loader.YamlMsgTemplateLoader;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

// Initialize
MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
YamlMsgTemplateLoader.forSimple("templates_simple.yaml").load(MsgTemplate.class, null);
YamlMsgTemplateLoader.forNamed("templates_named.yaml").load(MsgTemplate.class, null);

// Simple style - {} placeholders
String msg = MsgTemplateBuilder.create()
    .code("ERR_001")
    .simple()
    .args("数据库超时")
    .render();
// Result: "内部错误:数据库超时"

// Named style - Map arguments
Map<String, Object> args = new HashMap<>();
args.put("userId", "admin");
args.put("time", "2024-04-17");

String msg = MsgTemplateBuilder.create()
    .code("LOG_001")
    .named()
    .args(args)
    .render();
// Result: "用户admin于2024-04-17登录"

// Named style - Bean arguments (with ReflectCache)
LoginEvent event = new LoginEvent();
event.userId = "admin";
event.time = "2024-04-17";

String msg = MsgTemplateBuilder.create()
    .code("LOG_001")
    .named()
    .bean(event)
    .render();

// Explicit Locale
String msg = MsgTemplateBuilder.create()
    .code("ERR_001")
    .locale(Locale.US)
    .simple()
    .args("timeout")
    .render();
// Result: "Internal error:timeout"

// Locale fallback: zh -> zh-CN, en -> en-US, no match -> default locale
String msg = MsgTemplateBuilder.create()
    .code("ERR_001")
    .locale(Locale.UK)  // Matches en-GB template
    .simple()
    .args("timeout")
    .render();
// Result: "Internal error:timeout"
```

## Architecture

```
jmsg-i18n/
├── core/
│   ├── MsgTemplate.java          # Interface (extends DyEnum)
│   ├── SimpleMsgTemplate.java    # {} style implementation
│   ├── NamedMsgTemplate.java     # {name} style implementation
│   ├── CompiledTemplate.java     # Pre-compiled template structure
│   ├── TemplateCompiler.java     # Pattern compiler
│   ├── TemplateRenderer.java     # High-performance renderer
│   └── MsgTemplateConfig.java    # Global configuration
├── util/
│   ├── StringBuilderPool.java    # ThreadLocal StringBuilder pooling
│   ├── ReflectCache.java         # Getter method caching
│   └ LocaleHelper.java           # Locale parsing (zh-CN/zh_CN)
├── loader/
│   ├── MsgTemplateLoader.java    # Interface (extends DyEnumsLoader)
│   └ YamlMsgTemplateLoader.java  # YAML template loader
└── builder/
    ├── MsgTemplateBuilder.java   # Entry point builder
    ├── SimpleBuilder.java        # Simple style builder
    └ NamedBuilder.java           # Named style builder
```

## Configuration

```java
// Set default locale
MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
MsgTemplateConfig.setDefaultLocale("zh-CN");  // String format (supports zh-CN and zh_CN)

// Configure ReflectCache
MsgTemplateConfig.setReflectCacheEnabled(true);  // Enable by default
MsgTemplateConfig.setReflectCacheMaxSize(1024);  // Cache size

// Reset to defaults
MsgTemplateConfig.reset();
```

## Template Format

YAML format with unlimited locale support:

```yaml
templates:
  CODE_001:
    order: 1                    # Display order (optional, default 0)
    default: zh-CN              # Default locale (required)
    messages:
      zh-CN: 模板{param}        # Locale → template mapping
      en-US: Template {param}
      en-GB: Template {param}
      ja-JP: テンプレート{param}
```

**Locale format**: Supports both `zh-CN` (hyphen) and `zh_CN` (underscore)

**Locale fallback**:
1. Exact match: `zh-CN` → `zh-CN` template
2. Language match: `zh` → `zh-CN` or `zh_CN` template
3. Default: No match → `default` locale template

## Benchmark

Run JMH benchmark:

```bash
# Compile
mvn clean compile test-compile

# Run benchmark
./run_bench.sh MsgTemplateBenchmark
./run_bench.sh TemplateCompareBenchmark
```

## Dependencies

- Built on [dyenums](https://github.com/itcraft-cn/dyenums) dynamic enum framework
- Uses [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml) 2.2 for YAML parsing

## License

Apache License 2.0