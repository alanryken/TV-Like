# tv-like-core 优化记录

## 背景

本次工作聚焦 `tv-like-core`，目标是把核心提取链路梳理清楚，并在不改变整体使用方式的前提下，让规则解析、HTML 提取和运行时容错更稳。

## 本次改动范围

- `tv-like-core/src/main/java/tv/tvai/like/RuleParser.java`
- `tv-like-core/src/main/java/tv/tvai/like/Extractor.java`
- `tv-like-core/src/main/java/tv/tvai/like/TVLikeDSL.java`
- `tv-like-core/src/main/java/tv/tvai/like/TV.java`
- `tv-like-core/pom.xml`
- `tv-like-core/src/test/java/tv/tvai/like/TVCoreBehaviorTest.java`
- `README.md`

## 关键优化点

### 1. RuleParser

- 增加 DSL 预清洗，统一换行并去除块注释
- 支持一个 `path` 写多个路径模式，使用 `||` 分隔
- 修复 `items` 解析后尾部文本残留，避免污染后续字段扫描
- 优化 option 提取逻辑，减少重复正则创建
- 支持自动去掉带引号的 option 值外层引号
- 在未解析任何规则时安全返回空映射

### 2. Extractor

- 增加空文档、空规则、空 selector 的兜底
- 区块级 `limit` 开始生效，可限制 section 命中次数
- 列表级 `limit` 对非法值自动降级为不限量
- 列表项为空时不再写入结果
- 对非法 CSS Selector 使用安全查询，避免整个提取流程中断
- 新增 `transform` 执行能力：
  - `trim`
  - `upper`
  - `lower`
  - `digits`
  - `abs-url`

### 3. TVLikeDSL

- 获取页面内嵌 DSL 时增加空文档保护
- DSL Hub 请求增加 host 归一化和候选去重
- 网络请求增加连接关闭与统一响应读取
- 去掉直接打印堆栈的行为，失败时返回空字符串

### 4. TV

- 增加 `html` / `url` 空值保护
- 使用带 baseUri 的 HTML 解析，配合 `abs-url` 正确补全相对地址
- 无 DSL 或无规则命中时直接返回空结果

### 5. 测试补充

- 引入 JUnit 4
- 新增核心行为测试，覆盖：
  - 多路径规则匹配
  - `transform` 串联执行
  - 相对链接补全为绝对地址
  - `items limit` 截断
  - 非法 selector 容错

## 验证结果

在 `tv-like-core` 目录执行：

```bash
mvn test
```

结果：

- 2 个测试全部通过
- Maven 构建成功

## 当前收益

- 规则书写容错更高
- 解析器对脏输入更稳定
- 页面抽取更不容易被异常 selector 中断
- 结果结构更干净，空项更少
- README 与改动记录更加明确，便于后续维护

## 后续可继续推进的方向

- 支持更多 transform 能力
- 支持字段回退与默认值
- 补充真实网页样本回归集
- 增加 DSL 语法校验和调试输出工具
