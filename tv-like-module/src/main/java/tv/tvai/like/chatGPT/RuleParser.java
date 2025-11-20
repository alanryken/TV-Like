package tv.tvai.like.chatGPT;

import tv.tvai.like.util.AntPathMatcher;
import tv.tvai.like.util.PathMatcher;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleParser {

    // 允许的字段名称白名单
    private static final Set<String> ALLOWED_FIELDS = new HashSet<>(Arrays.asList("text", "img", "link"));
    // 路径匹配器（Spring Ant 风格）
    private final PathMatcher pathMatcher = new AntPathMatcher();

    // 按定义顺序保存路径规则（LinkedHashMap 保证顺序）
    private Map<String, List<RuleNode>> pathRuleMap;

    // ===================================================================
    // 公共 API
    // ===================================================================

    /**
     * 根据实际路径查找最先匹配的规则列表（Ant 通配匹配）
     */
    private boolean matchPath(String pattern, String path) {
        return pathMatcher.match(pattern, path);
    }

    /**
     * 根据请求路径返回对应的 RuleNode 列表
     * @param path 请求路径
     * @return 匹配到的规则节点列表，未匹配返回空列表
     */
    public List<RuleNode> getPathRule(String path) {
        // 遍历所有路径规则，找到第一个匹配的即返回（保持原有顺序）
        for (Map.Entry<String, List<RuleNode>> entry : pathRuleMap.entrySet()) {
            String pattern = entry.getKey();
            if (matchPath(pattern, path)) {
                return entry.getValue();
            }
        }
        // 未匹配到任何路径规则时返回空列表（原逻辑）
        return new ArrayList<>();
    }

    /**
     * 解析完整的 DSL 字符串，构建 pathRuleMap
     * @param dsl DSL 内容
     */
    public void parse(String dsl) {
        pathRuleMap = new LinkedHashMap<>();

        // 正则：匹配 path: xxx { 形式的块
        Pattern pathPattern = Pattern.compile("path\\s*:\\s*([^\\{]+)\\{", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pathPattern.matcher(dsl);

        // 逐个处理显式的 path 块
        while (matcher.find()) {
            String pathPatternStr = matcher.group(1).trim();      // path 后的路径通配表达式
            int blockStart = matcher.end() - 1;                  // '{' 的位置
            int blockEnd = findMatchingBrace(dsl, blockStart);   // 对应的 '}' 位置
            if (blockEnd < 0) continue;                          // 括号不匹配直接跳过

            String body = dsl.substring(blockStart + 1, blockEnd); // 大括号内的内容
            List<RuleNode> nodes = parseSectionDsl(body);         // 解析 section
            if (!nodes.isEmpty()) {
                pathRuleMap.put(pathPatternStr, nodes);
            }
        }

        // 如果 DSL 中没有任何 path 定义，则整个 DSL 视为默认 /** 规则
        if (pathRuleMap.isEmpty()) {
            List<RuleNode> nodes = parseSectionDsl(dsl);
            pathRuleMap.put("/**", nodes);
        }
    }

    // ===================================================================
    // Section 解析（section: name selector { ... }）
    // ===================================================================

    /**
     * 解析一段 DSL（可能是整个文件或 path 块内部），提取所有 section
     */
    private List<RuleNode> parseSectionDsl(String dsl) {
        List<RuleNode> sections = new ArrayList<>();
        if (dsl == null || dsl.trim().isEmpty()) {
            return sections;
        }

        // 正则匹配 section: name selector {
        Pattern sectionPattern = Pattern.compile(
                "section\\s*:\\s*([\\w-]+)\\s+([^\\{]+)\\{",
                Pattern.CASE_INSENSITIVE);

        Matcher matcher = sectionPattern.matcher(dsl);
        while (matcher.find()) {
            String sectionName = matcher.group(1).trim();         // section 名称
            String selector = matcher.group(2).trim();         // section 的 CSS 选择器
            int bodyStart = matcher.end() - 1;                    // '{' 位置
            int bodyEnd = findMatchingBrace(dsl, bodyStart);   // 对应的 '}' 位置
            if (bodyEnd < 0) continue;

            String body = dsl.substring(bodyStart + 1, bodyEnd); // 大括号内的内容

            RuleNode node = new RuleNode(sectionName, selector);
            parseSectionBody(body, node);                        // 解析内部字段、items 等
            sections.add(node);
        }
        return sections;
    }

    // ===================================================================
    // Section 内部解析（items、text/img/link 字段）
    // ===================================================================

    /**
     * 解析 section { ... } 内的内容，包括 items 块和普通字段
     */
    private static void parseSectionBody(String body, RuleNode node) {
        if (body == null) return;

        String remaining = body;

        // ------------------ 解析 items 块 ------------------
        remaining = parseItemsBlockIfPresent(remaining, node);

        // ------------------ 解析普通字段（text/img/link） ------------------
        parseFieldLines(remaining, node.getFieldSelectors(), node.getFieldOptions());
    }

    /**
     * 解析 items: selector { ... } [options] 块
     * 返回去掉 items 块后的剩余文本（供后续字段解析使用）
     */
    private static String parseItemsBlockIfPresent(String text, RuleNode parentNode) {
        Pattern itemsPattern = Pattern.compile("items\\s*:\\s*([^\\{\\n]+)\\{", Pattern.CASE_INSENSITIVE);
        Matcher matcher = itemsPattern.matcher(text);

        if (!matcher.find()) {
            return text; // 没有 items 块，直接返回原文本
        }

        String itemsSelector = matcher.group(1).trim();
        int blockStart = matcher.end() - 1;
        int blockEnd = findMatchingBrace(text, blockStart);
        if (blockEnd < 0) {
            return text;
        }

        String innerBody = text.substring(blockStart + 1, blockEnd); // items 大括号内部

        // 提取 items 块后的 [options]
        String afterBlock = blockEnd + 1 < text.length() ? text.substring(blockEnd + 1) : "";
        RuleNode.Options itemOptions = extractOptionsFromText(afterBlock);

        // 构建 items 模板节点
        RuleNode itemTemplate = new RuleNode();
        itemTemplate.setName("items");
        itemTemplate.setSelector(itemsSelector);
        itemTemplate.setSectionOptions(itemOptions);

        // 递归解析 items 内部的字段（text/img/link）
        parseItemFields(innerBody, itemTemplate);
        parentNode.setItemTemplate(itemTemplate);

        // 移除整个 items 块（包括后面的 options），剩余部分继续解析普通字段
        String before = text.substring(0, matcher.start());
        return before + "\n" + afterBlock;
    }

    /**
     * 解析 items 块内部的字段（和普通字段解析逻辑完全相同）
     */
    private static void parseItemFields(String inner, RuleNode node) {
        parseFieldLines(inner,
                node.getFieldSelectors(),
                node.getFieldOptions());
    }

    // ===================================================================
    // 通用字段解析（text/img/link）——核心复用方法
    // ===================================================================

    /**
     * 逐行解析 text/img/link 字段，支持 [option1, key:value] 写法
     * @param text                待解析的文本（可能包含多行）
     * @param selectorMap         存储 field -> selector 的 Map
     * @param optionsMap          存储 field -> Options 的 Map
     */
    private static void parseFieldLines(String text,
                                        Map<String, String> selectorMap,
                                        Map<String, RuleNode.Options> optionsMap) {

        if (text == null) return;

        Scanner scanner = new Scanner(text);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            // 匹配 text: selector [options] 形式的行
            Matcher fieldMatcher = Pattern.compile(
                    "^(text|img|link)\\s*:\\s*(.+)$",
                    Pattern.CASE_INSENSITIVE).matcher(line);

            if (!fieldMatcher.find()) continue;

            String fieldName = fieldMatcher.group(1).toLowerCase();
            if (!ALLOWED_FIELDS.contains(fieldName)) continue;

            String rest = fieldMatcher.group(2).trim();

            // 提取所有 [...] 中的选项内容
            List<String> optionTokens = extractAllOptionTokens(rest);

            // 去除 [...] 后剩余的就是 selector（可能为 "null"）
            String selector = rest.replaceAll("\\[.*?]", "").trim();
            if ("null".equalsIgnoreCase(selector)) {
                selector = null;
            }

            // 只在第一次出现时写入（保持原有 putIfAbsent + null 不覆盖的语义）
            if (selector != null && !selectorMap.containsKey(fieldName)) {
                selectorMap.put(fieldName, selector);
            }

            // 解析选项并写入（同样只在第一次出现时写入）
            RuleNode.Options fieldOptions = new RuleNode.Options();
            for (String token : optionTokens) {
                parseOptionToken(token, fieldOptions);
            }
            if (!optionsMap.containsKey(fieldName)) {
                optionsMap.put(fieldName, fieldOptions);
            }
        }
        scanner.close();
    }

    /**
     * 从文本中提取所有 [xxx] 中的内容（不包含方括号）
     */
    private static List<String> extractAllOptionTokens(String text) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\[(.*?)]").matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group(1).trim());
        }
        return tokens;
    }

    /**
     * 从紧跟块后的文本中提取选项（用于 items 后的 [options]）
     */
    private static RuleNode.Options extractOptionsFromText(String text) {
        RuleNode.Options opts = new RuleNode.Options();
        Matcher matcher = Pattern.compile("\\[(.*?)]").matcher(text);
        while (matcher.find()) {
            parseOptionToken(matcher.group(1).trim(), opts);
        }
        return opts;
    }

    /**
     * 解析单个 [key1, key2:value, ...] 字符串到 Options 对象
     */
    private static void parseOptionToken(String token, RuleNode.Options options) {
        if (token == null || token.isEmpty()) return;

        String[] parts = token.split(",");
        for (String part : parts) {
            String[] kv = part.split(":", 2);
            if (kv.length == 1) {
                options.putIfAbsent(kv[0].trim(), true);
            } else {
                options.putIfAbsent(kv[0].trim(), kv[1].trim());
            }
        }
    }

    // ===================================================================
    // 工具方法
    // ===================================================================

    /**
     * 从开括号位置向后寻找匹配的闭括号位置（支持嵌套）
     * @param text 完整文本
     * @param openIndex 开括号 '{' 的索引
     * @return 匹配的闭括号 '}' 的索引，未找到返回 -1
     */
    private static int findMatchingBrace(String text, int openIndex) {
        int level = 0;
        for (int i = openIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                level++;
            } else if (c == '}') {
                level--;
                if (level == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}