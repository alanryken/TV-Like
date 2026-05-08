# TV-Like 解析输出格式

本文档描述 `TV.like()` 当前实际返回的结构，供渲染程序直接对接。

## 顶层结构

`TV.like()` 返回：

```java
List<SectionResult>
```

每个元素代表一个命中的区块。

## SectionResult

```json
{
  "section": "hero",
  "meta": {
    "badge": "featured"
  },
  "text": {
    "value": "HELLO WORLD",
    "meta": {}
  },
  "link": {
    "value": "https://example.com/detail/1",
    "meta": {}
  },
  "img": {
    "value": "https://example.com/img/1.jpg",
    "meta": {}
  },
  "items": {
    "meta": {
      "img-ratio": "2/3"
    },
    "list": [
      {
        "text": {
          "value": "片名A",
          "meta": {}
        },
        "link": {
          "value": "https://example.com/a",
          "meta": {}
        },
        "img": {
          "value": "https://example.com/a.jpg",
          "meta": {}
        }
      }
    ]
  }
}
```

字段说明：

- `section`：区块名，对应 DSL 中的 `name`
- `meta`：section 级透传参数，对应 DSL 中 section 的 `meta`
- `text` / `img` / `link`：固定字段对象，不存在时为 `null`
- `items`：列表结果，不存在列表时为 `null`

## FieldResult

字段对象结构：

```json
{
  "value": "HELLO WORLD",
  "meta": {
    "badge": "featured-text"
  }
}
```

字段说明：

- `value`：字段实际值
- `meta`：字段级透传参数，对应 DSL 中 field 的 `meta`

说明：

- `text.value` 一般是文本内容
- `img.value` 一般是图片地址
- `link.value` 一般是跳转地址
- `attr`、`transforms` 属于执行参数，不会出现在输出里

## ItemsResult

列表对象结构：

```json
{
  "meta": {
    "img-ratio": "2/3"
  },
  "list": [
    {
      "text": {
        "value": "片名A",
        "meta": {}
      },
      "link": {
        "value": "https://example.com/a",
        "meta": {}
      }
    }
  ]
}
```

字段说明：

- `meta`：items 级透传参数，对应 DSL 中 `items.meta`
- `list`：列表项数组

## ItemResult

每个列表项是一个字段容器，当前支持：

- `text`
- `img`
- `link`

结构与 section 中的同名字段一致。

## 当前固定字段

当前引擎只解析三类固定字段：

- `text`
- `img`
- `link`

这几个字段之所以保持固定，是为了让渲染层对接简单稳定。

如果后续需要扩展展示参数，不建议直接把字段改成字符串，而是继续往这些对象的 `meta` 中扩展。

## 可空规则

- 某个 section 没有命中 `text` / `img` / `link`，对应字段为 `null`
- 某个 section 没有定义 `items` 或没有提取出有效列表时，`items` 为 `null`
- `meta` 始终存在，未配置时为空对象
- `list` 始终存在于 `items` 内，未提取到数据时不会输出整个 `items`

## 渲染侧建议

- 优先按 `section` 区分区块类型
- 对 `text` / `img` / `link` 先判空，再读取 `value`
- 需要布局参数时，从 `section.meta` 或 `items.meta` 中读取
- 不要依赖执行参数如 `attr`、`transforms` 出现在结果里

## Java 对接示例

```java
List<SectionResult> sections = new TV(html, url).like();

for (SectionResult section : sections) {
    String sectionName = section.getSection();

    if (section.getText() != null) {
        String text = section.getText().getValue();
    }

    if (section.getItems() != null) {
        for (ItemResult item : section.getItems().getList()) {
            if (item.getLink() != null) {
                String link = item.getLink().getValue();
            }
        }
    }
}
```
