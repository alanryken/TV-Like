# tv-like-core YAML 重构记录

## 本次改造目标

本轮改造不再兼容旧版自定义 DSL，核心目标是把规则系统直接重构为更易读、更可维护的 YAML DSL。

## 关键变更

- 引入 `SnakeYAML`，由文本规则改为 YAML 解析
- `RuleParser` 重写为 `YAML -> RuleNode` 模型转换
- path 规则改为“更具体优先”匹配，而不是声明顺序优先
- 页面内嵌规则改为优先读取 `script[name="tv-like"]` 等 YAML script 容器
- 远程规则 Hub 改为尝试 `.yaml` / `.yml`
- 测试重写为 YAML 样例，并补充 path 优先级与 selector 兼容场景

## 现在的 YAML 规则结构

```yaml
version: 1
paths:
  - match: /detail/**
    sections:
      - name: hero
        selector: .hero-card
        limit: 1
        meta:
          badge: featured
        fields:
          text:
            selector: .title
            transforms: [trim, upper]
          link:
            selector: .title a
            attr: href
            transforms: [abs-url]
```

## 为什么放弃旧 DSL

- 旧语法和 CSS selector 的 `[]` 容易冲突
- option、selector、字段语义混在一行，不利于阅读
- 自定义文本语法难以做稳定校验和工具化
- 每扩展一种语法，解析器复杂度都会明显上涨

## 验证

在 `tv-like-core` 下执行：

```bash
mvn test
```

当前 YAML 测试已通过。
