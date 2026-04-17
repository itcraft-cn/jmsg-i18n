# jmsg-i18n 实现计划总览

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 基于dyenums构建多语言消息模板系统，支持{}和{name}双风格模板渲染

**Architecture:** MsgTemplate接口 + 双实现类 + 预编译模板引擎 + 反射缓存 + ThreadLocal StringBuilder池

**Tech Stack:** Java 8, dyenums-core, SLF4J, JUnit 4

---

## 文件结构总览

### 创建文件清单

```
src/main/java/cn.itcraft.jmsg
├── core
│   ├── MsgTemplate.java            # 接口定义（Task 01）
│   ├── MsgTemplateConfig.java      # 全局配置（Task 02）
│   ├── CompiledTemplate.java       # 预编译结构（Task 03）
│   ├── TemplateCompiler.java       # 预编译器（Task 04-05）
│   ├── StringBuilderPool.java      # ThreadLocal池（Task 06）
│   ├── TemplateRenderer.java       # 渲染执行器（Task 07-09）
│   ├── SimpleMsgTemplate.java      # {}风格实现（Task 10）
│   └── NamedMsgTemplate.java       # {name}风格实现（Task 11）
├── util
│   └── LocaleHelper.java           # Locale工具（Task 12）
├── loader
│   ├── MsgTemplateLoader.java      # 加载器接口（Task 13）
│   ├── FileMsgTemplateLoader.java  # 文件加载器（Task 14）
│   └── PropMsgTemplateLoader.java  # Properties加载器（Task 15）
└── builder
    ├── MsgTemplateBuilder.java     # Builder入口（Task 16）
    ├── SimpleBuilder.java          # {}风格Builder（Task 17）
    └── NamedBuilder.java           # {name}风格Builder（Task 18）

src/test/java/cn.itcraft.jmsg
├── core
│   ├── MsgTemplateConfigTest.java
│   ├── CompiledTemplateTest.java
│   ├── TemplateCompilerTest.java
│   ├── StringBuilderPoolTest.java
│   ├── TemplateRendererTest.java
│   ├── SimpleMsgTemplateTest.java
│   └── NamedMsgTemplateTest.java
├── util
│   └── LocaleHelperTest.java
├── loader
│   ├── FileMsgTemplateLoaderTest.java
│   └── PropMsgTemplateLoaderTest.java
└── builder
    ├── SimpleBuilderTest.java
    └── NamedBuilderTest.java

src/test/resources
├── msg_templates_simple.properties
├── msg_templates_named.properties
└── msg_templates_invalid.properties
```

---

## 计划拆分

| 计划文件 | 覆盖范围 |
|---------|---------|
| 01-core-config.md | Task 01-03: MsgTemplate接口、Config、CompiledTemplate |
| 02-core-compiler.md | Task 04-05: TemplateCompiler |
| 03-core-renderer.md | Task 06-09: StringBuilderPool、TemplateRenderer |
| 04-core-template.md | Task 10-11: SimpleMsgTemplate、NamedMsgTemplate |
| 05-util.md | Task 12: LocaleHelper |
| 06-loader.md | Task 13-15: 加载器接口和实现 |
| 07-builder.md | Task 16-18: Builder |
| 08-integration.md | Task 19-20: 集成测试、示例配置 |

---

## TDD原则

每个Task遵循：
1. Write failing test
2. Run test (verify fail)
3. Write minimal implementation
4. Run test (verify pass)
5. Commit

---

## 依赖关系

```
Task 01 (MsgTemplate接口) ─┐
Task 02 (MsgTemplateConfig) ─┼─> Task 10-11 (Simple/Named实现)
Task 03 (CompiledTemplate) ─┤
Task 04-05 (TemplateCompiler) ─┼─> Task 07-09 (TemplateRenderer)
Task 06 (StringBuilderPool) ─┤
Task 07-09 (TemplateRenderer) ─┼─> Task 10-11
Task 12 (LocaleHelper) ─┼─> Task 02, Task 16-18
Task 13-15 (Loader) ─┼─> Task 10-11
Task 16-18 (Builder) ─┴─> Task 19-20 (集成测试)
```

---

## 执行顺序

建议按计划文件顺序执行：
1. 01-core-config.md (基础接口和结构)
2. 02-core-compiler.md (预编译器)
3. 03-core-renderer.md (渲染器)
4. 04-core-template.md (模板实现类)
5. 05-util.md (辅助工具)
6. 06-loader.md (加载器)
7. 07-builder.md (Builder)
8. 08-integration.md (集成验证)