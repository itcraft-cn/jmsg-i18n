# jmsg-i18n

## 项目简介

基于dyenums的高性能多语言消息模板系统，支持 `{}` (Simple) 和 `{name}` (Named) 双占位符风格，用于错误码、日志消息、业务提示等场景的国际化渲染。

## 核心特性

- **双模板风格**: Simple `{}` 位置占位符 + Named `{name}` 命名占位符
- **高性能**: 预编译模板、StringBuilderPool池化、ReflectCache反射缓存
- **多语言**: 中/英文模板内置，Locale回退链支持
- **灵活加载**: FileMsgTemplateLoader/PropMsgTemplateLoader，可扩展数据库源
- **Builder模式**: 流式API简洁易用

## 性能数据

JMH基准测试 (JDK 25, 4线程):

| 指标 | 吞吐量 | 说明 |
|------|--------|------|
| jmsg_simple | 92M ops/s | 比 MessageFormat 快 **4.9倍** |
| jmsg_namedMap | 58M ops/s | 比 MessageFormat 快 3.1倍 |
| ReflectCache | 258M ops/s | 比无缓存快 **10倍** |
| StringBuilderPool | 297M ops/s | ThreadLocal池化 |

## 模块结构

```
src/main/java/cn/itcraft/jmsg/
├── core/          # 核心: MsgTemplate接口、编译器、渲染器
├── util/          # 工具: StringBuilderPool、ReflectCache、LocaleHelper
├── loader/        # 加载器: File/Prop MsgTemplateLoader
├── builder/       # Builder: MsgTemplateBuilder、SimpleBuilder、NamedBuilder
└── benchmark/     # JMH基准测试
```

## API使用示例

```java
// 初始化
MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
FileMsgTemplateLoader.forSimple("templates.properties").load(MsgTemplate.class, null);

// Simple风格
MsgTemplateBuilder.create().code("ERR_001").simple().args("错误原因").render();

// Named风格 - Map
Map<String, Object> args = new HashMap<>();
args.put("userId", "admin");
MsgTemplateBuilder.create().code("LOG_001").named().args(args).render();

// Named风格 - Bean
MsgTemplateBuilder.create().code("LOG_001").named().bean(event).render();
```

## AI guide

### 角色定位

1. 你是资深架构师
    - 在开发前，会对需求进行详尽分析，提供多套方案，以上、中、下三策的形式呈现，以备后续决策参考
    - 在设计时，会充分考虑非功能性需求：安全性、可扩展性、可用性、可观测性、性能等
    - 在设计细节时，充分考虑各种设计模式及各语言特性
2. 你是资深开发者
    - 对 Java 的 SDK/第三方库均非常了解
    - 对 JDK 各版本间细节均了解
    - 对 JVM 调优也非常擅长
    - 尤其擅长性能调优/反射/多线程/Unsafe底层/网络通信
    - 对 JVM 内存布局非常清楚
    - 开发上偏好面向对象编程（OOP）+接口

### 环境信息

通过 skill `/java-env` 获取

### 项目信息

- **Java版本**: 8+ (当前测试 JDK 25)
- **构建工具**: Maven / mvnd
- **依赖**: dyenums-core 1.0.0, dyenums-loader-file 1.0.0
- **测试框架**: JUnit 4.13, JMH 1.37
- **编码规范**: 参考 `/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/spec.java.md`

### 交互规则

1. 所有交互均使用简体中文
2. 处于 AI Coding Plan 包月模式下，不需要担心 Token，专注于高效而完整地工作
3. 每次沟通产出文件后，均执行 git 提交
4. git 仅以当前 `user.name` 提交，不推送到远端
5. git 提交均遵循约定式提交规范（Conventional Commits）执行
6. 重要内容/TODO Plan，随时记录到 MEMORY.md，版本管理忽略该文件，写入 .gitignore，不提交到 Git

### 常用命令

```bash
# 编译
mvn clean compile -q
mvnd clean compile -q  # 推荐

# 测试
mvn test -q
mvn test -Dtest=MsgTemplateIntegrationTest -q

# 打包
mvn clean package -DskipTests -q

# JMH基准测试
./run_bench.sh MsgTemplateBenchmark
./run_bench.sh TemplateCompareBenchmark

# 快速验证测试
./run_bench.sh MsgTemplateBenchmark "-wi 3 -i 3 -t 1 -f 1"
```

### 编码规范

授权读取：`/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/spec.java.md`

### 构建工具

授权读取：`/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/ci.java.md`