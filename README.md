![TV-Like Logo](images/1767871810.png)

# TV-Like

TV-Like 用一份易读的 YAML 规则，把网页中的核心内容提取成适合电视大屏消费的结构化数据。

TV-Like turns existing web pages into TV-ready structured content by using readable YAML extraction rules.

## 项目定位

- 输入：网页 HTML 与页面 URL
- 规则：页面内嵌 YAML 或远程 YAML Hub
- 输出：统一的区块化 JSON 结果
- 场景：电视端首页、详情页、列表页、推荐区、导航区内容抽取

## 仓库结构

- `tv-like-core`：YAML DSL 解析、路径匹配、HTML 抽取
- `tv-like-module`：样例页面与运行示例
- `tv-like-android-plugin`：Android / TV 集成示例
- `doc`：设计与改造记录
- `skills`：辅助生成规则的共享技能

## YAML DSL 示例

```yaml
version: 1
paths:
  - match: /list/**
    sections:
      - name: hero
        selector: .hero-card
        fields:
          text:
            selector: .title
            transforms: [trim]
          link:
            selector: .title a
            attr: href
            transforms: [abs-url]
      - name: recommend
        selector: .recommend-list
        items:
          selector: li.card
          limit: 12
          meta:
            img-ratio: 2/3
          fields:
            text:
              selector: .title
              transforms: [trim]
            link:
              selector: a
              attr: href
              transforms: [abs-url]
            img:
              selector: img[data-src]
              attr: data-src
              transforms: [abs-url]
```

## 快速开始

```java
String html = "...网页源码...";
String url = "https://example.com/list";

List<Map<String, Object>> result = new TV(html, url).like();
```

## 文档入口

- 中文说明：`README.zh.md`
- English overview: `README.en.md`
- 中文教程：`TUTORIAL.zh.md`
- English tutorial: `TUTORIAL.en.md`
- 官网介绍页：`index.html`
- 技能说明：`skills/tvlike-dsl-generator/SKILL.md`

## License

MIT License.
