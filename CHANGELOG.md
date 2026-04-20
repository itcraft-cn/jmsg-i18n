# Changelog

All notable changes to this project will be documented in this file.

## [1.2.0] - 2026-04-20

### Added

- YAML template configuration format supporting unlimited locales
- YamlMsgTemplateLoader for YAML file parsing
- LocaleHelper support for both zh-CN and zh_CN formats
- MsgTemplate.hasLocale() and getDefaultLocale() methods
- Locale fallback chain: exact match → language match → default locale

### Changed

- Template format changed from Properties to YAML
- Locale matching improved with language-level fallback

### Removed

- FileMsgTemplateLoader (use YamlMsgTemplateLoader)
- PropMsgTemplateLoader (use YamlMsgTemplateLoader)
- Properties template format support
- fromValueString methods in SimpleMsgTemplate/NamedMsgTemplate

### Dependencies

- Added snakeyaml 2.2

## [1.1.0] - 2026-04-20

### Fixed

- ReflectCache now prioritizes `getDeclaredMethod` + `setAccessible` for private inner class reflection support

### Changed

- Removed `dyenums-loader-db` dependency (not used in this project)

### Added

- JMessageSampleTest: integration test demonstrating private inner class bean rendering

## [1.0.0] - 2026-04-17

### Added

- Initial release
- Dual template styles: `{}` (Simple) and `{name}` (Named) placeholders
- Multi-language support: Chinese/English templates with Locale fallback
- High performance optimizations:
  - ReflectCache: 10x getter method caching (258M vs 25M ops/s)
  - StringBuilderPool: ThreadLocal pooling (297M ops/s)
  - TemplateCompiler: Pre-compiled templates
- Flexible loading: FileMsgTemplateLoader, PropMsgTemplateLoader
- Builder pattern: Fluent API for easy template rendering
- JMH benchmarks: MsgTemplateBenchmark, TemplateCompareBenchmark