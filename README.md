
![TV-Like Logo](images/tvlike.png) 

## English / 英文

[📖 Full English Documentation](README.en.md)  
[📖 完整中文文档](README.zh.md)

---

## 中文 / Chinese

[📖 Full English Documentation](README.en.md)  
[📖 完整中文文档](README.zh.md)

---

## Project Overview / 项目介绍


-  TV Like : Convert web pages to TV style
-  TV Like : 将网页转换为电视样式

**TV Like** is an AI-driven experimental project, where most components are generated and directionally guided by AI. It provides a simple, readable extraction specification to pull key elements from web pages—such as images, text, videos, and hyperlinks—and outputs a unified structure optimized for TV interfaces. This structure is designed for easy adaptation into TV templates for re-rendering, powered by [QuickUI](https://github.com/quicktvui/quicktvui).

**TV Like** 是一个由 AI 主导的实验性项目，大部分内容由 AI 生成并决定路线方向。它提供了一个简单、易读的提取规范，用于从网页中提取主要元素（如图片、文字、视频、超链接），并输出统一的 TV 端适配结构，便于适配到电视端模板中重新渲染，由 [QuickUI](https://github.com/quicktvui/quicktvui) 支持。

---

## TV Like 

**TV Like** 是一个由 AI 主导的实验性开源项目，旨在将普通网页转换为适合电视大屏浏览的样式和结构。

它提供了一种简单易读的提取规范（DSL），通过自定义规则从网页中提取核心元素（如标题、图片、链接、视频等），并输出统一的标准化 JSON 数据结构。这些数据可交给电视端 UI 框架（如 [QuickUI](https://github.com/quicktvui/quicktvui)）进行渲染，实现完美的电视遥控器交互体验。

## Features

- **网页内容提取**：使用类 CSS 选择器的 DSL 语法精准提取网页主要内容
- **标准化输出**：统一 JSON 数据结构，便于电视端模板渲染
- **DSL 加载**：
  - 在目标网页中内嵌 DSL 脚本
  - 上传到远程 DSL Hub (默认仓库：https://hub.tvai.tv）,页面中不存在DSL时尝试读取DSL Hub
- **灵活配置**：
    - 支持页面级别规则配置,路径匹配、条件显示、图片比例、文本变换等
- **电视适配优化**：使用 QuickUI 框架 -- 专为大屏焦点导航设计
- **运行环境**：Java 8+


## Usage

### 1. DSL 编写方式

TV Like 使用一种类似于 CSS 选择器的领域特定语言（DSL）来描述提取规则。

#### 内嵌到网页中

在目标网页的 `<head>` 或 `<body>` 中添加以下脚本：

```html
<script type="text/plain" name="tv-like">
    # 页面类型
    page-type: video-detail

    # 全局配置
    globals {
        base-url: "https://example.com"   # 相对路径自动补全域名
        exclude: ".ads"                   # 排除广告类元素（可选）
    }

    # 规则适用于分类页及相关路径
    path: /category/** {

        # 顶部导航栏
        section:tab .top-navigation {
            items: ul.nav-list li.nav-item {
                text: a
                link: a [attr: href]
            } [limit: 8]
        }

        # 页面标题区块（单个元素）
        section:top-title .page-header h1 {
            text: span.title-text
            link: a.header-link
        }

        # 推荐列表 A（网格或横向列表）
        section:recommend .grid-container {
            items: .item-card.type-a {
                text: .item-title a
                img: .item-poster img [attr: data-src]
                link: .item-title a
            }
        }

        # 推荐列表 B（另一种布局样式）
        section:recommend .list-container {
            items: .item-card.type-b {
                text: .item-title a
                img: .item-poster img [attr: data-src]
                link: .item-title a
            }
        }

        # 演员/嘉宾展示区块（带固定图片比例）
        section:actor .cast-slider {
            items: li.cast-item {
                text: .cast-name a
                img: .cast-photo img [attr: data-src]
                link: .cast-name a
            } [img-ratio: 4/3]
        }
    }
</script>
```

#### 远程 DSL Hub

也可以将 DSL 文件上传至 https://hub.tvai.tv，页面中不存在 DSL 代码块时尝试读取DSL Hub。


### 2. DSL 语法详解

#### DSL 定义
- `page-type`: 声明页面类型（可选，用于模板匹配）
- `globals { ... }`: 全局配置，目前支持 `base-url`
- `path: <AntPath> { ... }`: 规则生效的 URL 路径匹配
- `section:<name> <selector> { ... }`: 定义一个内容区块
  - 直接包含 `text`、`img`、`link` 时为单个元素区块
  - 包含 `items` 时为列表区块
- `items: <selector> { ... }`: 定义列表项
  - 支持 `text`、`img`、`link`
  
- 选项（方括号内）：
  - `[attr: <attribute>]`: 提取属性值而非文本（如 `href`、`data-src`）
  - `[img-ratio: width/height]`: 指定图片推荐比例（如 `4/3`、`16/9`）
  - `[limit: N]`: 限制列表最多返回 N 条
  - `[自定义key: 自定义value]`: 自定义内容会返回到json中 和value同级


```html
<script type="text/plain" name="tv-like">
    # TVLike DSL 语法详解（本脚本仅作示例说明，可直接复制修改为实际规则）

    # page-type: 声明页面类型（可选，用于电视端模板自动匹配）
    # 可选值示例：home、detail、list、search、video-detail 等
    page-type: home

    # globals { ... }: 全局配置
    # base-url: 当提取的链接或图片为相对路径时，自动补全此域名
    globals {
        base-url: "https://example.com"
    }

    # path: <AntPath> { ... }: 规则生效的 URL 路径匹配（支持 Ant 风格通配符）
        # <AntPath> 请替换为实际路径模式，例如：
        #   /category/**                → 所有分类页
        #   /**/detail/*.html           → 所有详情页
        #   /category/** || /detail/**  → 多路径同时匹配（用 || 分隔）
    path: <AntPath> {

        # section:<name> <selector> { ... }[<key>: <value>]: 定义一个内容区块
            # <name> 为区块唯一标识，电视端模板会根据此名称渲染对应组件
            # - 如果区块内直接写 text / img / link → 输出单个元素对象
            # - 如果包含 items → 输出列表对象
            # - [<key>: <value>] → 选项（可选）

        # 示例1：顶部导航（列表区块）
        section:tab .top-navigation {
            # items: <selector> { ... } [<key>: <value>]: 定义一个列表项
                # 支持 text、img、link 三种元素提取
                # text: 提取文本内容
                # img: 提取图片 URL
                # link: 提取链接 URL
                # [<key>: <value>] → 选项（可选）
            items: ul.nav-list li.nav-item {
                text: a                            # 提取文字
                link: a [attr: href]               # 提取 href 属性作为链接
            } [limit: 10]                          # 限制最多返回 10 条（可选）
        }

        # 示例2：页面主标题（单个元素区块）
        section:top-title .page-header h1.title-box {
            text: span.main-title
            link: a.title-link [attr: href]
            img:  img.banner-img [attr: data-src]
        }  [img-ratio: 16/9]

        # 示例3：推荐内容网格（列表区块）
        section:recommend .content-grid {
            items: .video-card {
                text: .card-title a 
                img:  .card-poster img [attr: data-src] 
                link: .card-title a [attr: href]
            } [limit: 30] [img-ratio: 2/3]   # 限制30条 推荐海报比例 2:3
        }

        # 示例4：演员/嘉宾横向滑动列表（带固定图片比例）
        section:actor .cast-carousel {
            items: li.cast-item {
                text: .actor-name a 
                img:  .actor-photo img [attr: data-src]
                link: .actor-name a [attr: href]
            } [img-ratio: 4/3]
        }
    }



    # 支持的选项（放在选择器后，用方括号 [] 包裹，可组合使用）：
    # [attr: <attribute>]       → 提取指定属性值而非元素文本（如 href、data-src）
    # [img-ratio: width/height] → 推荐图片显示比例（如 16/9、4/3、2/3），用于电视端布局优化
    # [limit: N]                → 限制列表项最多返回 N 条数据
    # [自定义key: 自定义value]    → 自定义内容会返回到json中 和value同级
</script>

```
### 3. JSON 输出示例

```json
[
  {
    "section": "tab",
    "items": {
      "value": [
        {
          "link": { "value": "/" },
          "text": { "value": "首页" }
        },
        {
          "link": { "value": "/category/1.html" },
          "text": { "value": "分类A" }
        },
        {
          "link": { "value": "/category/2.html" },
          "text": { "value": "分类B" }
        },
        {
          "link": { "value": "/category/3.html" },
          "text": { "value": "分类C" }
        },
        {
          "link": { "value": "/category/4.html" },
          "text": { "value": "分类D" }
        },
        {
          "link": { "value": "/category/5.html" },
          "text": { "value": "分类E" }
        }
      ]
    }
  },
  {
    "section": "top-title",
    "text": { "value": "热门推荐" },
    "link": { "value": "/list/top.html" }
  },
  {
    "section": "episodes",
    "items": {
      "value": [
        {
          "img": { "value": "https://example.com/img/1.jpg" },
          "link": { "value": "/detail/1001.html" },
          "text": { "value": "示例视频1" }
        },
        {
          "img": { "value": "https://example.com/img/2.jpg" },
          "link": { "value": "/detail/1002.html" },
          "text": { "value": "示例视频2" }
        },
        {
          "img": { "value": "https://example.com/img/3.jpg" },
          "link": { "value": "/detail/1003.html" },
          "text": { "value": "示例视频3" }
        },
        {
          "img": { "value": "https://example.com/img/4.jpg" },
          "link": { "value": "/detail/1004.html" },
          "text": { "value": "示例视频4" }
        },
        {
          "img": { "value": "https://example.com/img/5.jpg" },
          "link": { "value": "/detail/1005.html" },
          "text": { "value": "示例视频5" }
        }
      ]
    }
  },
  {
    "section": "top-title",
    "text": { "value": "最新更新" },
    "link": { "value": "/list/new.html" }
  },
  {
    "section": "episodes",
    "items": {
      "value": [
        {
          "img": { "value": "https://example.com/img/6.jpg" },
          "link": { "value": "/detail/2001.html" },
          "text": { "value": "示例视频6" }
        },
        {
          "img": { "value": "https://example.com/img/7.jpg" },
          "link": { "value": "/detail/2002.html" },
          "text": { "value": "示例视频7" }
        },
        {
          "img": { "value": "https://example.com/img/8.jpg" },
          "link": { "value": "/detail/2003.html" },
          "text": { "value": "示例视频8" }
        }
      ]
    }
  },
  {
    "section": "actor",
    "items": {
      "img-ratio": "4/3",
      "value": [
        {
          "img": { "value": "https://example.com/actor/1.jpg" },
          "link": { "value": "/actor/1.html" },
          "text": { "value": "演员A" }
        },
        {
          "img": { "value": "https://example.com/actor/2.jpg" },
          "link": { "value": "/actor/2.html" },
          "text": { "value": "演员B" }
        },
        {
          "img": { "value": "https://example.com/actor/3.jpg" },
          "link": { "value": "/actor/3.html" },
          "text": { "value": "演员C" }
        }
      ]
    }
  }
]
```

### 4. 与 QuickUI 集成

解析得到的 JSON 数据可直接传入 QuickUI 的电视模板组件，实现焦点移动、遥控器操作等原生电视交互体验。

## Contributing

欢迎贡献！


我们特别欢迎：
- 新网站的 DSL 规则贡献
- DSL 语法增强建议
- 性能优化
- 文档改进

## License

本项目采用 MIT License，详见 [LICENSE](LICENSE) 文件。


---

<div align="center">
  <small>Built with ❤️ by AI & Humans | Last Updated: October 21, 2025</small>
</div>