# jmsg-i18n

A high-performance multi-language message template system for Java, supporting both `{}` (Simple) and `{name}` (Named) placeholder styles.

## Features

- **Dual Template Styles**: Support positional `{}` and named `{name}` placeholders
- **Multi-language Support**: Built-in Chinese/English template storage with Locale fallback
- **High Performance**: Pre-compiled templates with StringBuilder pooling
- **ReflectCache**: 10x faster getter method caching for Bean rendering
- **Flexible Loading**: File and Properties loaders, extensible for database sources
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
    <version>1.0.0</version>
</dependency>
```

### Template Properties File

```properties
# templates_simple.properties ({} style)
ERR_001=Error|Error|Internal error:{}|Internal error:{}|1
LOG_001=Login|Login|User {} logged in at {}|User {} logged in at {}|10

# templates_named.properties ({name} style)
ERR_001=Error|Error|Internal error:{reason}|Internal error:{reason}|1
LOG_001=Login|Login|User {userId} logged in at {time}|User {userId} logged in at {time}|10
```

### Usage Examples

```java
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.builder.MsgTemplateBuilder;
import cn.itcraft.jmsg.loader.FileMsgTemplateLoader;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

// Initialize
MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
FileMsgTemplateLoader.forSimple("templates_simple.properties").load(MsgTemplate.class, null);
FileMsgTemplateLoader.forNamed("templates_named.properties").load(MsgTemplate.class, null);

// Simple style - {} placeholders
String msg = MsgTemplateBuilder.create()
    .code("ERR_001")
    .simple()
    .args("Database timeout")
    .render();
// Result: "Internal error:Database timeout"

// Named style - Map arguments
Map<String, Object> args = new HashMap<>();
args.put("userId", "admin");
args.put("time", "2024-04-17");

String msg = MsgTemplateBuilder.create()
    .code("LOG_001")
    .named()
    .args(args)
    .render();
// Result: "User admin logged in at 2024-04-17"

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
│   └ LocaleHelper.java           # Locale parsing utility
├── loader/
│   ├── MsgTemplateLoader.java    # Interface (extends DyEnumsLoader)
│   ├── FileMsgTemplateLoader.java# File properties loader
│   └ PropMsgTemplateLoader.java  # In-memory Properties loader
└── builder/
    ├── MsgTemplateBuilder.java   # Entry point builder
    ├── SimpleBuilder.java        # Simple style builder
    └ NamedBuilder.java           # Named style builder
```

## Configuration

```java
// Set default locale
MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
MsgTemplateConfig.setDefaultLocale("en-US");  // String format

// Configure ReflectCache
MsgTemplateConfig.setReflectCacheEnabled(true);  // Enable by default
MsgTemplateConfig.setReflectCacheMaxSize(1024);  // Cache size

// Reset to defaults
MsgTemplateConfig.reset();
```

## Template Format

Properties file format: `name_zh|name_en|template_zh|template_en|order`

```properties
# Example
CODE_001=名称|Name|模板{param}|Template {param}|1
```

- `name_zh`: Chinese display name
- `name_en`: English display name  
- `template_zh`: Chinese template with placeholders
- `template_en`: English template with placeholders
- `order`: Display order (integer)

## Benchmark

Run JMH benchmark:

```bash
# Compile
mvn clean compile test-compile

# Run benchmark
./run_bench.sh MsgTemplateBenchmark
./run_bench.sh TemplateCompareBenchmark
```

## License

Apache License 2.0