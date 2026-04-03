# TV-Like 健壮性提升与多选择器后备机制 - 2026 年路线图

> 本文档基于与 OpenCLI 项目的对比分析，为 TV-Like 提供系统化的改进建议。
> 核心目标：通过多选择器后备机制，提升 DSL 对网站改版的容错能力。

---

## 📖 目录

1. [项目定位分析](#项目定位分析)
2. [为什么纯 DOM 选择器方案合理](#为什么纯-dom-选择器方案合理)
3. [当前面临的痛点](#当前面临的痛点)
4. [解决方案：多选择器后备机制](#解决方案多选择器后备机制)
5. [三阶段实施计划](#三阶段实施计划)
6. [实现细节与代码示例](#实现细节与代码示例)
7. [健壮性设计清单](#健壮性设计清单)

---

## 📊 项目定位分析

### TV-Like vs OpenCLI：两个不同的世界

#### OpenCLI 的假设环境
```
✅ Node.js 完整环境
✅ Chrome/Chromium 可用
✅ 内存充足 (1GB+)
✅ 网络通畅
✅ 可以调用 AI 模型
```

#### TV-Like 的约束环境
```
❌ TV 端内存有限 (MB 级)
❌ 无法运行 Chrome
❌ 处理能力弱
❌ 网络可能不稳定
✅ 需要完全离线能力
✅ 需要秒级响应
✅ 成本必须极低
```

### 两个项目的架构对比

**OpenCLI 数据流：**
```
真实 Chrome 浏览器
    ↓
API 优先 / Network Intercept
    ↓
实时生成 JSON
    ↓
CLI 输出
```

**TV-Like 数据流：**
```
网页 HTML (一次获取)
    ↓
DSL 规则 (一次生成，多次复用)
    ↓
TV 端秒级解析
    ↓
TV UI 焦点交互
```

---

## ✅ 为什么纯 DOM 选择器方案合理

### 1. 成本优势

| 对比项 | TV-Like | OpenCLI |
|--------|---------|---------|
| **内存占用** | MB 级 | 1GB+ |
| **依赖** | Java / 原生 | Node + Chrome |
| **离线能力** | ✅ 完全离线 | ❌ 需要网络 |
| **响应速度** | 毫秒级 | 秒级 |
| **运行成本** | 极低 | 高（Chrome + AI Token） |
| **TV 端支持** | ✅ 原生支持 | ❌ 无法运行 |

### 2. 一次解析多次使用

```
网页发布 → 生成 DSL (1 次投入)
TV 端 (每次使用) → DSL 解析 (毫秒级，成本接近 0)

vs

OpenCLI 方案：
每次使用 → 启动 Chrome → 注入 JS → 拦截网络 (每次都很重)
```

### 3. 不依赖 AI 的优势

```
OpenCLI 需要：
❌ Claude / GPT-4 API 账户
❌ 每次调用消耗 Token ($0.01 - $0.10 / 请求)
❌ 网络延迟 (往返 2-5 秒)

TV-Like 方案：
✅ 纯客户端解析
✅ 零运行成本
✅ 完全离线
✅ 毫秒响应
```

### 结论
**你的选择完全正确。** 不需要学 OpenCLI 的 API 优先策略，因为你面对的是完全不同的约束。

---

## ⚠️ 当前面临的痛点

### 问题 1：网站改版导致适配器失效

```
网站 A 改了 CSS class：
  原: <div class="video-card">
  新: <div class="movie-item">
  
结果: 
  你的 DSL 选择器失效 → TV 端无法解析 → 用户看不到内容 ❌
```

### 问题 2：缺乏版本管理机制

```
DSL 一旦生成，就是"一版永久"
网站改版后，没有办法平滑升级到新版本 DSL
```

### 问题 3：无法自动检测选择器是否还有效

```
网站改版后，TV 端继续调用旧 DSL
但不知道为什么没有解析出数据
错误信息不清晰 → 无法诊断问题
```

---

## 🎯 解决方案：多选择器后备机制

### 核心思想

**同一个字段支持多个选择器，按优先级尝试。如果第一个失效，自动尝试备选。**

### 新 DSL 语法示例

```yaml
path: /list/** {
    section:video .video-container {
        items: 
            li.card ||                  # 优先级 1：原始选择器
            .movie-item ||              # 优先级 2：备选 1
            div[data-video] ||          # 优先级 3：备选 2
            [role="listitem"]           # 优先级 4：备选 3
        {
            text: 
                .title ||               # 优先级 1
                h3 ||                   # 优先级 2
                .name ||                # 优先级 3
                [data-title]            # 优先级 4
            
            img: img [attr: src || data-src || data-lazy]
            
            link: a [attr: href || data-url] [transform: abs-url]
        } [limit: 20]
    }
}
```

### 实现效果

```
网站改版场景：
  1. 网站改了 li.card → .movie-item
  2. DSL 尝试 li.card → 失败
  3. 自动回退到 .movie-item → ���功 ✅
  4. TV 端继续正常工作，用户无感知
```

### 优势

| 优势 | 说明 |
|------|------|
| **平滑降级** | 网站小版本改动自动适应 |
| **用户无感知** | TV 端继续工作，不需要立即更新 DSL |
| **维护成本低** | 后向兼容，不需要立即修复所有 DSL |
| **错误诊断清晰** | 可以记录"用了备选选择器"以及具体原因 |
| **社区协作** | 用户遇到失效 DSL 时，可以贡献新的选择器 |

---

## 📅 三阶段实施计划

### 🔴 第 1 阶段（立即开始，2-4 周）：核心功能实现

#### 1.1 改进 DSL 解析器

**目标：** 支持 `||` 操作符，支持多个选择器和多个属性

**工作项：**

- [ ] 修改 DSL Lexer，支持 `||` 为操作符
- [ ] 修改 DSL Parser，将多选择器解析为列表而非单个字符串
- [ ] 修改选择器评估器，实现"尝试-回退"逻辑

**代码变更点：**

在 `tv-like-core` 中的选择器执行器：

```java
// 原逻辑
public String extractField(Element root, String selector) {
    Elements elements = root.select(selector);
    return elements.isEmpty() ? null : elements.get(0).text();
}

// 新逻辑：支持多选择器回退
public String extractField(Element root, List<String> selectors) {
    for (String selector : selectors) {
        try {
            Elements elements = root.select(selector);
            if (!elements.isEmpty()) {
                return elements.get(0).text();
            }
        } catch (Exception e) {
            // 选择器语法错误，继续尝试下一个
            continue;
        }
    }
    return null;  // 所有选择器都失效
}
```

**测试用例：**

```java
@Test
public void testMultipleSelectorFallback() {
    String html = "<div><h2 class='title'>Test</h2></div>";
    
    // 第一个选择器失效，第二个成功
    List<String> selectors = Arrays.asList(
        ".video-title",    // 失效
        "h2"               // 成功
    );
    
    String result = extractor.extractField(root, selectors);
    assertEquals("Test", result);
}
```

#### 1.2 增加详细的执行日志

**目标：** 记录选择器执行过程，便于诊断

```java
public class SelectorExecutionLog {
    String fieldName;           // 例如 "text"
    List<String> triedSelectors;
    String successfulSelector;  // 哪个选择器成功了
    String value;               // 提取的值
    List<String> failures;      // 各选择器失效原因
}
```

**输出示例：**

```json
{
  "field": "text",
  "tried_selectors": [".title", "h3", ".name", "[data-title]"],
  "successful_selector": "h3",
  "value": "视频标题",
  "fallback_count": 1,
  "failures": [
    ".title -> not found",
    "h3 -> success (used)"
  ]
}
```

#### 1.3 错误分类与提示

**目标：** 给用户清晰的反馈

```java
public enum SelectorErrorType {
    SYNTAX_ERROR("选择器语法错误"),
    NOT_FOUND("选择器未找到元素"),
    EMPTY_RESULT("所有备选选择器都失效"),
    PARTIAL_SUCCESS("部分字段失效，已降级");
    
    private final String message;
}
```

---

### 🟠 第 2 阶段（4-8 周）：工具与验证

#### 2.1 选择器验证工具

**目标：** 检测 DSL 是否还能工作

**命令行工具：**

```bash
# 检测 DSL 是否有效
tv-like validate --dsl ./rule.dsl --html ./page.html

# 输出报告
{
  "valid": true,
  "coverage": 0.95,
  "timestamp": "2026-04-03T10:00:00Z",
  "field_status": {
    "text": {
      "selector_used": "h3",
      "fallback_level": 2,
      "found_count": 18,
      "total_expected": 20
    },
    "img": {
      "selector_used": "img",
      "fallback_level": 1,
      "found_count": 20,
      "total_expected": 20
    }
  },
  "warnings": [
    "items 选择器 li.card 未找到，已回退到 .movie-item",
    "img 字段仅找到 80% 的项（可能是懒加载）"
  ]
}
```

#### 2.2 DSL Hub 版本管理

**目标：** 支持 DSL 版本控制

**存储结构：**

```
dsl-hub/
  example.com/
    list/
      ├── 1.0.dsl  (2024-01)
      ├── 1.1.dsl  (2024-06, 网站改版)
      ├── 1.2.dsl  (2024-12, 又改了)
      └── latest.dsl -> 1.2.dsl
    detail/
      ├── 1.0.dsl
      └── latest.dsl
```

**API 设计：**

```bash
# 获取最新版本
GET /dsl?url=https://example.com/list

# 获取特定版本
GET /dsl?url=https://example.com/list&version=1.1

# 获取支持的所有版本
GET /dsl/versions?url=https://example.com/list

# 返回
{
  "available_versions": ["1.0", "1.1", "1.2"],
  "latest": "1.2",
  "current": "1.2",
  "dsl_content": "..."
}
```

#### 2.3 Java API 增强

**目标：** 支持版本查询和降级

```java
// 获取最新 DSL
TV tv = new TV(html, url, dslHub);
Result result = tv.like();

// 获取特定版本 DSL
TV tv = new TV(html, url, dslHub);
tv.preferVersion("1.1");
Result result = tv.like();

// 获取执行日志（包含选择器回退信息）
ExecutionLog log = tv.getExecutionLog();
log.getSelectorFallbacks().forEach(f -> {
    System.out.println(f.getFieldName() + " 使用了备选选择器: " + f.getSuccessfulSelector());
});
```

---

### 🟡 第 3 阶段（2-3 个月）：社区与自动化

#### 3.1 社区 DSL 库

**目标：** 建立可共享的 DSL 仓库

**结构：**

```
tv-like-dsl-community/
  ├── sites/
  │   ├── bilibili.com/
  │   │   ├── list.yaml
  │   │   └── detail.yaml
  │   ├── youtube.com/
  │   └── ...
  ├── README.md
  └── CONTRIBUTING.md
```

**贡献流程：**

1. 社区成员贡献新的 DSL 或更新旧 DSL
2. CI 自动验证 DSL 的有效性
3. Code Review 和测试
4. 合并后自动发布到 DSL Hub
5. TV 端自动检查更新

#### 3.2 自动修复系统

**目标：** 检测到选择器失效时自动提示或修复

**工作流：**

```
1. TV 端解析失效 → 记录日志
   
2. 后端检测
   - 发现某个 DSL 的选择器失效率 > 20%
   - 自动触发 DSL 修复任务
   
3. 尝试自动修复
   - 用 AI/启发式算法尝试找新的选择器
   - 生成候选 DSL
   
4. 社区投票
   - 发布候选 DSL 给社区
   - 社区测试和投票
   
5. 发布
   - 最佳 DSL 版本发布
   - TV 端自动更新
```

#### 3.3 多语言选择器支持

**目标：** 同一字段支持 CSS + XPath + JavaScript

```yaml
path: /list/** {
    section:video .video-container {
        items: li.card {
            text: 
                [css] .title || h3 || .name
                [xpath] //div[@class='title']/text()
                [js] el.querySelector('.title')?.textContent
        }
    }
}
```

---

## 🔧 实现细节与代码示例

### 核心数据结构

#### DSL 解析结果

```java
public class FieldSelector {
    private String fieldName;           // "text", "img", "link"
    private List<String> selectors;     // ["css:.title", "css:h3", "xpath://..."]
    private String selectorLanguage;    // "css", "xpath", "js"
    private List<String> attributes;    // ["href", "data-url"]
    private List<String> transforms;    // ["trim", "abs-url"]
    
    // 执行时信息
    private String usedSelector;        // 实际用上的选择器
    private int fallbackLevel;          // 0 = 第一个, 1 = 第二个, ...
    private String value;               // 提取的值
    private ExecutionTime executionTime;
}

public class ItemsSelector extends FieldSelector {
    private int limit;                  // 最多返回多少项
    private List<FieldSelector> fields; // 列表项内的字段
    private List<Map<String, Object>> extractedItems;
}
```

#### 执行日志

```java
public class ExecutionLog {
    private String url;
    private long startTime;
    private long endTime;
    private List<SectionExecutionLog> sections;
    
    public static class SectionExecutionLog {
        private String sectionName;
        private String sectionSelector;
        private Element sectionElement;
        private List<FieldExecutionLog> fields;
    }
    
    public static class FieldExecutionLog {
        private String fieldName;
        private List<SelectorAttempt> selectorAttempts;
        
        public static class SelectorAttempt {
            private String selector;
            private boolean success;
            private String errorMessage;
            private long executionTime;
        }
    }
}
```

### 选择器执行器实现

```java
public class ResilientSelectorExecutor {
    
    /**
     * 从元素中提取字段值，支持多选择器后备
     */
    public FieldExtractionResult extractField(
        Element root, 
        FieldSelector fieldSelector,
        String baseUrl
    ) {
        FieldExtractionResult result = new FieldExtractionResult(fieldSelector.getFieldName());
        
        for (int i = 0; i < fieldSelector.getSelectors().size(); i++) {
            String selector = fieldSelector.getSelectors().get(i);
            
            try {
                // 尝试选择器
                Elements elements = root.select(selector);
                
                if (!elements.isEmpty()) {
                    // 成功！
                    Element element = elements.get(0);
                    String value = extractValue(element, fieldSelector, baseUrl);
                    
                    result.setSuccess(true);
                    result.setUsedSelector(selector);
                    result.setFallbackLevel(i);
                    result.setValue(value);
                    
                    // 记录日志
                    SelectorAttempt attempt = new SelectorAttempt(selector, true);
                    result.addAttempt(attempt);
                    
                    return result;
                }
                
                // 这个选择器没找到，尝试下一个
                SelectorAttempt attempt = new SelectorAttempt(selector, false, "no elements found");
                result.addAttempt(attempt);
                
            } catch (Exception e) {
                // 选择器语法错误，继续尝试下一个
                SelectorAttempt attempt = new SelectorAttempt(selector, false, e.getMessage());
                result.addAttempt(attempt);
            }
        }
        
        // 所有选择器都失效
        result.setSuccess(false);
        result.setErrorMessage("All selectors failed");
        return result;
    }
    
    private String extractValue(Element element, FieldSelector fieldSelector, String baseUrl) {
        String value;
        
        // 从属性还是文本中取值
        if (fieldSelector.getAttributes() != null && !fieldSelector.getAttributes().isEmpty()) {
            value = extractFromAttributes(element, fieldSelector.getAttributes());
        } else {
            value = element.text();
        }
        
        // 应用 transform
        if (fieldSelector.getTransforms() != null) {
            for (String transform : fieldSelector.getTransforms()) {
                value = applyTransform(value, transform, baseUrl);
            }
        }
        
        return value;
    }
    
    private String extractFromAttributes(Element element, List<String> attributes) {
        // 支持多个属性：[attr: href || data-url]
        for (String attr : attributes) {
            String value = element.attr(attr);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }
    
    private String applyTransform(String value, String transform, String baseUrl) {
        if (value == null) return null;
        
        switch (transform) {
            case "trim":
                return value.trim();
            case "upper":
                return value.toUpperCase();
            case "lower":
                return value.toLowerCase();
            case "digits":
                return value.replaceAll("[^0-9]", "");
            case "abs-url":
                return URLUtil.toAbsolute(value, baseUrl);
            default:
                return value;
        }
    }
}
```

### 选择器验证工具

```java
public class SelectorValidator {
    
    public ValidationReport validate(String html, String dsl, String url) {
        ValidationReport report = new ValidationReport();
        
        Document doc = Jsoup.parse(html);
        DSLRule rule = DSLParser.parse(dsl);
        
        for (SectionRule section : rule.getSections()) {
            validateSection(doc, section, report);
        }
        
        report.calculateCoverage();
        return report;
    }
    
    private void validateSection(Document doc, SectionRule section, ValidationReport report) {
        // 检查 section 选择器是否有效
        Elements sectionElements = doc.select(section.getSelector());
        
        if (sectionElements.isEmpty()) {
            report.addWarning("Section '" + section.getName() + 
                "' selector not found: " + section.getSelector());
            return;
        }
        
        // 检查每个字段选择器
        if (section.getItems() != null) {
            validateItems(sectionElements, section.getItems(), report, section.getName());
        } else {
            validateFields(sectionElements.get(0), section.getFields(), report, section.getName());
        }
    }
    
    private void validateItems(Elements parentElements, ItemsRule items, 
                               ValidationReport report, String sectionName) {
        Elements itemElements = parentElements.select(items.getSelector());
        
        if (itemElements.isEmpty()) {
            report.addWarning("Items selector not found in section '" + sectionName + 
                "': " + items.getSelector());
            return;
        }
        
        int foundFieldCount = 0;
        int totalFields = items.getFields().size();
        
        for (Element itemElement : itemElements) {
            for (FieldSelector field : items.getFields()) {
                if (canExtractField(itemElement, field)) {
                    foundFieldCount++;
                }
            }
        }
        
        double coverage = (double) foundFieldCount / (itemElements.size() * totalFields);
        report.addFieldCoverage(sectionName + ".items." + items.getSelector(), coverage);
    }
    
    private boolean canExtractField(Element element, FieldSelector field) {
        for (String selector : field.getSelectors()) {
            try {
                Elements elements = element.select(selector);
                if (!elements.isEmpty()) {
                    return true;
                }
            } catch (Exception e) {
                // 继续尝试下一个
            }
        }
        return false;
    }
}
```

---

## ✅ 健壮性设计清单

### 必做项（优先级：🔴 高）

- [ ] **多选择器后备机制**
  - DSL 支持 `||` 操作符
  - 支持多个属性 `[attr: href || data-url]`
  - 选择器执行器实现"尝试-回退"逻辑
  - 单元测试覆盖各种回退场景

- [ ] **详细的执行日志**
  - 记录每个选择器的尝试情况
  - 记录实际用上的选择器
  - 记录是否发生了回退

- [ ] **选择器验证工具**
  - 命令行工具检测 DSL 是否有效
  - 生成覆盖率报告

### 应做项（优先级：🟠 中）

- [ ] **版本管理**
  - DSL Hub 支持版本存储
  - API 支持版本查询和降级

- [ ] **社区 DSL 库**
  - 建立共享仓库
  - 自动化 CI 验证

- [ ] **错误分类**
  - 区分 SYNTAX_ERROR / NOT_FOUND / EMPTY_RESULT 等

### 可选项（优先级：🟡 低）

- [ ] **自动修复系统**
  - 检测失效 DSL
  - 尝试自动生成新选择器

- [ ] **多语言选择器**
  - 同时支持 CSS / XPath / JavaScript

---

## 📈 预期效果

### 实施前

```
网站改版 → DSL 选择器失效 → TV 端无法解析 → 用户看不到内容 ❌
            └─ 需要手动更新 DSL
```

### 实施后

```
网站小���本改动 → 自动尝试备选选择器 → 成功 ✅ (用户无感知)

网站大版本改动 → 所有备选都失效 → 清晰的错误信息 ℹ️
                  └─ 社区更新 DSL / 自动修复系统介入
```

---

## 📚 参考资源

- [OpenCLI 多层级降级策略](https://github.com/jackwener/opencli/blob/main/CLI-EXPLORER.md#step-2-选择认证策略)
- [Resilience4j 容错模式](https://resilience4j.readme.io/)
- [CSS 选择器参考](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors)

---

## 🤝 反馈与讨论

如果你对这份路线图有任何疑问或建议，欢迎在 GitHub Issues 中讨论。

---

**最后修改：** 2026-04-03  
**贡献者：** 基于 OpenCLI 项目的对比分析  
**状态：** 📋 建议阶段，等待社区反馈和优先级确认
