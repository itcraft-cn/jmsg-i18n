# jmsg-i18n

高性能多语言消息模板系统，基于 dyenums，支持 `{}` 和 `{name}` 双占位符风格，YAML 配置支持无限语言。

## 项目信息

- **Java**: 8+ (测试 JDK 25)
- **构建**: Maven / mvnd (首选 mvnd)
- **依赖**: dyenums-core, dyenums-loader-file, snakeyaml 2.2
- **测试**: JUnit 4 + Mockito + PowerMock + JMH 1.37

## 常用命令

```bash
# 编译
mvnd clean compile -q

# 测试
mvn test -q
mvn test -Dtest=YamlMsgTemplateLoaderTest -q

# 打包
mvn clean package -DskipTests -q

# JMH基准测试
./run_bench.sh MsgTemplateBenchmark
./run_bench.sh TemplateCompareBenchmark "-wi 3 -i 3 -t 1 -f 1"  # 快速验证
```

## 模块结构

```
src/main/java/cn/itcraft/jmsg/
├── core/          # MsgTemplate接口、编译器、渲染器
├── util/          # StringBuilderPool、ReflectCache、LocaleHelper
├── loader/        # YamlMsgTemplateLoader
└── builder/       # MsgTemplateBuilder入口
```

## 模板格式

YAML 格式，支持无限语言：

```yaml
templates:
  ERR_001:
    order: 1
    default: zh-CN
    messages:
      zh-CN: 内部错误:{}
      en-US: Internal error:{}
      en-GB: Internal error:{}
```

**Locale 格式**: 支持 `zh-CN` 和 `zh_CN`

**Locale 回退**: 精确匹配 → 语言匹配 → 默认

## 性能要点

- ReflectCache: getter缓存，10x加速
- StringBuilderPool: ThreadLocal池化
- TemplateCompiler: 预编译模板
- 主分支代码禁止包含 `main` 方法或 `test` 代码

## 外部规范

- 编码规范: `/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/spec.java.md`
- 构建工具: `/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/ci.java.md`

## 环境信息

通过 skill `/java-env` 获取

## 交互规则

1. 所有交互使用简体中文
2. 每次产出文件后执行 git 提交
3. git 仅以当前 user.name 提交，不推送到远端
4. git 提交遵循 Conventional Commits 规范
5. 重要内容记录到 MEMORY.md (不提交到 Git)