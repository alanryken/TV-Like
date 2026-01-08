package tv.tvai.like;

import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import tv.tvai.like.enums.OptionKeyEnum;

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
                    Map<String, Object> parsed = new HashMap<>();
                    parsed.put("section", rule.getName());
                    //填充可读数据
                    if (rule.getSectionOptions() != null) {
                        for (Map.Entry<String, Object> entry : rule.getSectionOptions().getValues().entrySet()) {
                            String key = entry.getKey();
                            boolean executable = OptionKeyEnum.executable(key);
                            if (!executable) {
                                parsed.put(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                    Map<String, Object> map = this.extractSection(el, rule);
                    parsed.putAll(map);
                    result.add(parsed);
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

        RuleNode itemTemplateRule = rule.getItemTemplate();                     // 获取 items 模板
        if (itemTemplateRule != null) {                                         // 如果存在 items 配置
            Elements elements = el.select(itemTemplateRule.getSelector());      // 根据 selector 查询所有 item 节点
            List<Map<String, Object>> items = new ArrayList<>();            // 用于存放 items 数组

            long limit = this.resolveLimit(itemTemplateRule);
            int i = 0;
            for (Element itemEl : elements) {                               // 遍历每个 item DOM 节点
                if (limit > 0 && i >= limit) break;
                Map<String, Object> itemMap = new LinkedHashMap<>();        // 单个 item 的解析结果
                this.extractFields(itemEl, itemTemplateRule, itemMap);               // 解析每个 item 的字段
                items.add(itemMap);                                         // 加入 items 数组
                i++;
            }
            if (!items.isEmpty()) {
                Result itemsResult = new Result(items);
                result.put("items", itemsResult);                                     // 将 items 放入最终结果
                //填充可读数据
                this.putItemResultOptions(itemTemplateRule, itemsResult);
            }
        }

        return result;                                                      // 返回最终解析结果
    }


    // 统一解析字段(text/img/link)
    private void extractFields(Element el, RuleNode rule, Map<String, Object> out) {
        for (Map.Entry<String, String> en : rule.getFieldSelectors().entrySet()) {
            String field = en.getKey();                                     // 字段名：text / img / link
            String selector = en.getValue();                                // CSS selector
            if (selector == null) continue;                                 // 如果无 selector 直接跳过

            Result value = this.extractValue(el, field, selector, rule);         // 根据字段类型提取具体值
            if (value != null && !value.isEmpty() && !out.containsKey(field)) {
                out.put(field, value);                                      // 仅在 value 非空 且不存在时才 put
            }
        }
    }

    private long resolveLimit(RuleNode rule) {
        RuleNode.Options sectionOptions = rule.getSectionOptions();
        if (sectionOptions == null || sectionOptions.getValues() == null || sectionOptions.getValues().isEmpty() || !sectionOptions.getValues().containsKey(OptionKeyEnum.LIMIT.getKey())) {
            return -1;
        }
        return Long.parseLong(sectionOptions.getValues().get(OptionKeyEnum.LIMIT.getKey()).toString());
    }

    private void putItemResultOptions(RuleNode rule, Result itemsResult) {
        RuleNode.Options sectionOptions = rule.getSectionOptions();
        if (sectionOptions == null || sectionOptions.getValues() == null || sectionOptions.getValues().isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : sectionOptions.getValues().entrySet()) {
            String key = entry.getKey();
            boolean executable = OptionKeyEnum.executable(key);
            if (!executable) {
                itemsResult.put(entry.getKey(), entry.getValue());
            }
        }
    }

    // 从 RuleNode 读取字段的 attr 配置（可复用）
    private String resolveAttr(RuleNode rule, String field) {
        return resolve(rule, field, OptionKeyEnum.ATTR.getKey()); // 读取 attr 配置
    }

    private String resolveImageRatio(RuleNode rule, String field) {
        return resolve(rule, field, OptionKeyEnum.IMG_RATIO.getKey()); // 读取 ratio 配置
    }


    private String resolve(RuleNode rule, String field, String key) {
        Map<String, RuleNode.Options> fieldOptions = rule.getFieldOptions(); // 获取字段配置
        if (fieldOptions == null) return null;

        RuleNode.Options opt = fieldOptions.get(field);                      // 获取当前字段的配置
        if (opt == null) return null;

        Map<String, Object> values = getFieldOptions(rule, field);            // 取出 values
        if (values == null) return null;

        Object value = values.get(key);                                    // 读取 配置
        return value == null ? null : value.toString();
    }

    private Map<String, Object> getFieldOptions(RuleNode rule, String field) {
        Map<String, RuleNode.Options> fieldOptions = rule.getFieldOptions(); // 获取字段配置
        if (fieldOptions == null) return null;

        RuleNode.Options opt = fieldOptions.get(field);                      // 获取当前字段的配置
        if (opt == null) return null;

        return opt.getValues();                        // 取出 values
    }


    // 根据 field 类型统一提取值
    private Result extractValue(Element el, String field, String selector, RuleNode rule) {
        Element target = el.selectFirst(selector);                           // 找到第一个匹配元素
        if (target == null) return null;
        String attr = this.resolveAttr(rule, field);    // 获取字段自定义 attr（如 src/href）
        Object value;
        switch (field) {
            case "text":
                if (attr != null) {                                          // 若配置了 attr，则优先取 attr
                    value = this.selectAttr(el, selector, attr);
                } else {
                    value = this.selectText(el, selector);                        // 否则取文本
                }
                break;
            case "img":
                value = this.selectAttr(el, selector, attr == null ? "src" : attr); // 默认 src
                break;
            case "link":
                value = this.selectAttr(el, selector, attr == null ? "href" : attr); // 默认 href
                break;
            default:
                value = null;                                                 // 未知字段类型
        }
        if (value == null) return null;

        Result fv = new Result(value);

        Map<String, Object> fieldOptions = this.getFieldOptions(rule, field);  //当前字段的规则选项
        if (fieldOptions != null) {
            for (Map.Entry<String, Object> entry : fieldOptions.entrySet()) {
                boolean executable = OptionKeyEnum.executable(entry.getKey());
                if (!executable) {
                    fv.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return fv;
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
