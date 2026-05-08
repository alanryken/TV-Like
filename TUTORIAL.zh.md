# TV-Like YAML 教程

## 1. 设计目标

TV-Like 的 YAML DSL 只做一件事：把网页中的稳定内容区块提取成统一结构。

当前教程聚焦项目已实现的能力：

- `path` 匹配
- `section` 区块
- `items` 列表
- 字段类型 `text`、`img`、`link`
- `attr`、`limit`、`transforms`
- `meta` 透传

## 2. 顶层结构

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
```

可用顶层字段：

- `version`
- `paths`
- `sections`

如果你不需要按 URL path 分流，可以直接使用顶层 `sections`。

## 3. path 规则

### 单 path

```yaml
version: 1
paths:
  - match: /detail/**
    sections: []
```

### 多 path

```yaml
version: 1
paths:
  - matches:
      - /movie/**
      - /tv/**
    sections: []
```

规则优先级：更具体的 path 会优先命中。

## 4. section 结构

```yaml
- name: hero
  selector: .hero-card
  limit: 1
  meta:
    badge: featured
  fields:
    text:
      selector: .title
      transforms: [trim, upper]
```

字段说明：

- `name`：输出中的区块名
- `selector`：区块根节点
- `limit`：最多输出几个命中的 section
- `meta`：附加元数据，透传到结果里
- `fields`：字段定义
- `items`：列表模板

## 5. fields 写法

### text

```yaml
fields:
  text:
    selector: .title
    transforms: [trim]
```

### link

```yaml
fields:
  link:
    selector: a
    attr: href
    transforms: [abs-url]
```

### img

```yaml
fields:
  img:
    selector: img[data-src]
    attr: data-src
    transforms: [abs-url]
```

### 可用 transforms

- `trim`
- `upper`
- `lower`
- `digits`
- `abs-url`

## 6. items 列表

```yaml
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

`items` 会输出到结果里的 `items.list` 数组中，`items.meta` 用于承载列表级扩展参数。

## 7. 完整示例

```yaml
version: 1
paths:
  - match: /vodtype/**
    sections:
      - name: tab
        selector: .head-nav.ft4.roll.bold0.pc-show1.wap-show0
        items:
          selector: ul.swiper-wrapper li.swiper-slide
          limit: 6
          fields:
            text:
              selector: a
              transforms: [trim]
            link:
              selector: a
              attr: href
              transforms: [abs-url]
      - name: top-title
        selector: .title > h4.title-h.cor4
        fields:
          text:
            selector: a span:first-child
          link:
            selector: a
            attr: href
            transforms: [abs-url]
      - name: episodes
        selector: .flex.wrap.border-box.public-r
        items:
          selector: .public-list-box.public-pic-a
          meta:
            img-ratio: 2/3
          fields:
            text:
              selector: .public-list-button a.time-title
              transforms: [trim]
            img:
              selector: .public-list-div a.public-list-exp img[data-src]
              attr: data-src
              transforms: [abs-url]
            link:
              selector: .public-list-button a.time-title
              attr: href
              transforms: [abs-url]
```

## 8. 页面内嵌方式

```html
<script type="application/yaml" name="tv-like">
version: 1
sections:
  - name: hero
    selector: .hero
    fields:
      text:
        selector: h1
</script>
```

推荐使用：

- `script[name="tv-like"]`
- `script[id="tv-like"]`
- `script[id="tv-like-rules"]`

## 9. 远程 Hub 方式

如果页面没有内嵌规则，核心引擎会去规则 Hub 查询：

- `${host}.yaml`
- `${host}.yml`
- 域名倒序路径形式，如 `com/example/www.yaml`

## 10. 最佳实践

- selector 优先选稳定 class / id，不要过度依赖层级
- path 规则尽量按页面类型拆分
- `items` 只保留对电视端有价值的列表
- 对相对地址优先加 `abs-url`
- 元数据统一放在 `meta`，避免和执行选项混用
- 大站点按页面类型拆多个 YAML 文件维护

## 11. 规则校验

如果你希望在运行提取前先检查 YAML DSL 是否写对，可以直接调用：

```java
RuleParser parser = new RuleParser();
DslValidationResult result = parser.validate(dsl);

if (!result.isValid()) {
    for (String error : result.getErrors()) {
        System.out.println(error);
    }
}
```

当前校验会覆盖这些常见问题：

- 顶层缺少 `paths` 或 `sections`
- `section` 缺少 `name` 或 `selector`
- `items` 缺少 `selector`
- `fields` 为空或字段类型不受支持
- `limit` 不是合法整数
- `transforms` 包含未支持的变换器
