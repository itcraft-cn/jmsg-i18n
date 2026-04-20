# YAML 模板配置格式设计

## 背景

当前 Properties 配置格式：
```properties
ERR_001=错误|Error|内部错误:{}|Internal error:{}|1
```

局限性：
- 固定 2 种语言（中英文），无法扩展
- 字段顺序固定，不易阅读
- name 字段实际使用较少

## 目标

设计支持任意多语言的 YAML 配置格式。

## 配置格式

### Simple 风格（{} 占位符）

```yaml
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
```

### Named 风格（{name} 占位符）

```yaml
templates:
  ERR_001:
    order: 1
    default: zh-CN
    messages:
      zh-CN: 内部错误:{reason}
      en-US: Internal error:{reason}
  
  LOG_001:
    order: 10
    default: zh-CN
    messages:
      zh-CN: 用户{userId}于{time}登录
      en-US: User {userId} logged in at {time}
```

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `order` | int | 否 | 显示顺序，默认 0 |
| `default` | string | 是 | 默认 Locale，如 `zh-CN` |
| `messages` | map | 是 | Locale → 模板映射 |

## Locale 格式

支持以下格式：
- `zh-CN`：连字符格式（推荐，HTTP 标准）
- `zh_CN`：下划线格式（Java Locale 标准）
- `zh`：仅语言代码

LocaleHelper 自动解析并统一转换为 `Locale` 对象。

## Locale 匹配逻辑

1. **精确匹配**：请求 `zh-CN`，返回 `zh-CN` 模板
2. **语言匹配**：请求 `zh`，匹配 `zh-CN` 或 `zh_CN`
3. **默认回退**：无匹配时，返回 `default` 指定的模板

## 依赖变更

新增 SnakeYAML 依赖：

```xml
<dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
    <version>2.2</version>
</dependency>
```

## API 变更

### 新增 YamlMsgTemplateLoader

```java
// 加载 YAML 文件
YamlMsgTemplateLoader.forSimple("templates_simple.yaml")
    .load(MsgTemplate.class, null);

YamlMsgTemplateLoader.forNamed("templates_named.yaml")
    .load(MsgTemplate.class, null);
```

### 删除旧 Loader

- **删除** `FileMsgTemplateLoader`
- **删除** `PropMsgTemplateLoader`
- **保留** `MsgTemplateLoader` 接口，供用户自定义实现

### 自定义 Loader 示例

```java
public class MyDbMsgTemplateLoader implements MsgTemplateLoader {
    @Override
    public int load(Class<MsgTemplate> enumClass, BiFunction<String, String, MsgTemplate> factory) {
        // 从数据库加载模板
        // ...
    }
}
```

## 数据结构变更

### CompiledTemplate

新增 `Map<String, CompiledTemplate> messages` 字段，按 Locale 存储预编译模板。

### MsgTemplate

新增方法：
- `getSupportedLocales()`：返回支持的 Locale 列表
- `hasLocale(Locale)`：检查是否支持某 Locale

## 测试策略

1. **单元测试**：YAML 解析、Locale 匹配、模板编译
2. **集成测试**：完整加载流程、多 Locale 渲染
3. **兼容性测试**：SnakeYAML 在 Java 8+ 的运行

## 迁移影响

- **不向后兼容**：旧 Properties 格式不再支持
- **迁移方式**：手动转换配置文件，或提供转换脚本

## 文件命名规范

- Simple 风格：`*_simple.yaml`
- Named 风格：`*_named.yaml`

## 实现范围

1. 新增 `snakeyaml` 依赖
2. 新增 `YamlMsgTemplateLoader`
3. 删除 `FileMsgTemplateLoader`、`PropMsgTemplateLoader`
4. 修改 `CompiledTemplate` 支持 Locale 映射
5. 修改 `MsgTemplate` 接口新增 Locale 相关方法
6. 修改 `SimpleMsgTemplate`/`NamedMsgTemplate` 实现
7. 清理废弃代码和测试
8. 更新 README、README_cn、AGENTS.md、CHANGELOG
9. 更新示例和模板文件