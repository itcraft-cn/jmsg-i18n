# Changelog

All notable changes to this project will be documented in this file.

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