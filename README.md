
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

**TV-Like** is an AI-driven experimental project, where most components are generated and directionally guided by AI. It provides a simple, readable extraction specification to pull key elements from web pages—such as images, text, videos, and hyperlinks—and outputs a unified structure optimized for TV interfaces. This structure is designed for easy adaptation into TV templates for re-rendering, powered by [QuickUI](https://github.com/quicktvui/quicktvui).

**TV-Like** 是一个由 AI 主导的实验性项目，大部分内容由 AI 生成并决定路线方向。它提供了一个简单、易读的提取规范，用于从网页中提取主要元素（如图片、文字、视频、超链接），并输出统一的 TV 端适配结构，便于适配到电视端模板中重新渲染，由 [QuickUI](https://github.com/quicktvui/quicktvui) 支持。

### Key Features / 主要特性
- **AI-Powered Extraction** / **AI 驱动提取**: Automatically identifies and extracts core web content using intelligent parsing.
- **TV-Optimized Output** / **TV 优化输出**: Generates a structured JSON/XML format tailored for large-screen TV rendering.
- **Cross-Platform Compatibility** / **跨平台兼容**: Works with modern web standards and integrates seamlessly with QuickUI for TV apps.
- **Lightweight & Readable** / **轻量且易读**: Minimal dependencies, with clear specs for easy extension.

| Feature / 特性 | Description / 描述 |
|---------------|-------------------|
| **Element Extraction** / **元素提取** | Supports images, text, videos, links / 支持图片、文字、视频、超链接 |
| **TV Adaptation** / **TV 适配** | Unified structure for QuickUI templates / 统一结构适配 QuickUI 模板 |

---

## Quick Start / 快速开始

### Prerequisites / 先决条件
- Node.js 14+ (for build tools) / Node.js 14+（用于构建工具）
- QuickUI TV framework / QuickUI TV 框架

### Installation / 安装
1. Clone the repo:  
   ```bash
   git clone https://github.com/yourusername/TV-Like.git
   cd TV-Like
   ```
2. Install dependencies:  
   ```bash
   npm install
   ```
3. Build the extractor:  
   ```bash
   npm run build
   ```

### Usage / 使用
Extract from a webpage:  
```javascript
 todo
```

For detailed examples, see the [English Docs](README.en.md#usage) or [Chinese Docs](README.zh.md#使用).

---

## Architecture / 架构
```
Web Page Input
    ↓
TV LIKE Parser (Extract Elements)
    ↓
TV Structure Generator
    ↓
QuickUI Template Output
```

---

## Contributing / 贡献指南
We welcome contributions! Please:
1. Fork the repo.
2. Create a feature branch (`git checkout -b feature/AmazingFeature`).
3. Commit changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

For more, check [CONTRIBUTING.md](CONTRIBUTING.md).

---

## License / 许可证
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<div align="center">
  <small>Built with ❤️ by AI & Humans | Last Updated: October 21, 2025</small>
</div>