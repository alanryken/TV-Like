package tv.tvai.like.chatGPT;

import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

import java.util.*;

public class Extractor {

    public List<Map<String, Object>> extract(Document doc, List<RuleNode> rules) {
        List<Map<String, Object>> result = new ArrayList<>();

        Element body = doc.body();
        this.traverse(body, rules, result);

        return result;
    }

    private void traverse(Node node, List<RuleNode> rules, List<Map<String, Object>> result) {
        if (node instanceof Element) {
            Element el = (Element) node;

            // 按 DOM 顺序匹配所有 section
            for (RuleNode rule : rules) {
                boolean matches = this.matches(el, rule.getSelector());
                if (matches) {
                    Map<String, Object> parsed = this.extractSection(el, rule);
                    if (parsed.isEmpty()) continue;
                    parsed.put("type", rule.getName());
                    result.add(parsed);
                    break;
                }
            }
        }

        for (Node child : node.childNodes()) {
            this.traverse(child, rules, result);
        }
    }

    private boolean matches(Element el, String selector) {
        try {
            return el.is(selector);
        } catch (Exception e) {
            return false;
        }
    }

    // 统一负责解析一个 Section（含字段 + items）
    private Map<String, Object> extractSection(Element el, RuleNode rule) {
        Map<String, Object> result = new LinkedHashMap<>();                 // 存放当前 section 的解析结果

        this.extractFields(el, rule, result);                                    // 解析 section 自己的字段(text/img/link)

        RuleNode itemTemplate = rule.getItemTemplate();                     // 获取 items 模板
        if (itemTemplate != null) {                                         // 如果存在 items 配置
            Elements elements = el.select(itemTemplate.getSelector());      // 根据 selector 查询所有 item 节点
            List<Map<String, Object>> items = new ArrayList<>();            // 用于存放 items 数组

            for (Element itemEl : elements) {                               // 遍历每个 item DOM 节点
                Map<String, Object> itemMap = new LinkedHashMap<>();        // 单个 item 的解析结果
                this.extractFields(itemEl, itemTemplate, itemMap);               // 解析每个 item 的字段
                items.add(itemMap);                                         // 加入 items 数组
            }
            if (!items.isEmpty()) {
                result.put("items", items);                                     // 将 items 放入最终结果
            }
        }

        return result;                                                      // 返回最终解析结果
    }


    // 统一解析字段(text/img/link)，避免重复代码
    private void extractFields(Element el, RuleNode rule, Map<String, Object> out) {
        for (Map.Entry<String, String> en : rule.getFieldSelectors().entrySet()) {
            String field = en.getKey();                                     // 字段名：text / img / link
            String selector = en.getValue();                                // CSS selector
            String attr = this.resolveAttr(rule, field);                         // 获取字段自定义 attr（如 src/href）

            if (selector == null) continue;                                 // 如果无 selector 直接跳过

            String value = this.extractValue(el, field, selector, attr);         // 根据字段类型提取具体值

            if (value != null && !value.isEmpty() && !out.containsKey(field)) {
                out.put(field, value);                                      // 仅在 value 非空 且不存在时才 put
            }
        }
    }


    // 从 RuleNode 读取字段的 attr 配置（可复用）
    private String resolveAttr(RuleNode rule, String field) {
        Map<String, RuleNode.Options> fieldOptions = rule.getFieldOptions(); // 获取字段配置
        if (fieldOptions == null) return null;

        RuleNode.Options opt = fieldOptions.get(field);                      // 获取当前字段的配置
        if (opt == null) return null;

        Map<String, Object> values = opt.getValues();                        // 取出 values
        if (values == null) return null;

        Object attr = values.get("attr");                                    // 读取 attr 配置
        return attr == null ? null : attr.toString();
    }


    // 根据 field 类型统一提取值
    private String extractValue(Element el, String field, String selector, String attr) {
        Element target = el.selectFirst(selector);                           // 找到第一个匹配元素
        if (target == null) return null;

        switch (field) {
            case "text":
                if (attr != null) {                                          // 若配置了 attr，则优先取 attr
                    String v = this.selectAttr(el, selector, attr);
                    if (v != null && !v.isEmpty()) return v;                 // attr 非空则返回
                }
                return this.selectText(el, selector);                             // 否则取文本

            case "img":
                return this.selectAttr(el, selector, attr == null ? "src" : attr);// 默认 src

            case "link":
                return this.selectAttr(el, selector, attr == null ? "href" : attr);// 默认 href

            default:
                return null;                                                 // 未知字段类型
        }
    }


    private String selectText(Element el, String selector) {
        Element e = el.selectFirst(selector);
        return e != null ? e.text().trim() : null;
    }

    private String selectAttr(Element el, String selector, String attr) {
        Element e = el.selectFirst(selector);
        return e != null ? e.attr(attr).trim() : null;
    }
}
