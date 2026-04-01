package tv.tvai.like;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import tv.tvai.like.enums.OptionKeyEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            for (RuleNode rule : rules) {
                boolean matches = this.matches(el, rule.getSelector());
                if (matches) {
                    Map<String, Object> parsed = new HashMap<>();
                    parsed.put("section", rule.getName());
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

    private Map<String, Object> extractSection(Element el, RuleNode rule) {
        Map<String, Object> result = new LinkedHashMap<>();

        this.extractFields(el, rule, result);

        RuleNode itemTemplateRule = rule.getItemTemplate();
        if (itemTemplateRule != null) {
            Elements elements = el.select(itemTemplateRule.getSelector());
            List<Map<String, Object>> items = new ArrayList<>();

            long limit = this.resolveLimit(itemTemplateRule);
            int i = 0;
            for (Element itemEl : elements) {
                if (limit > 0 && i >= limit) break;
                Map<String, Object> itemMap = new LinkedHashMap<>();
                this.extractFields(itemEl, itemTemplateRule, itemMap);
                items.add(itemMap);
                i++;
            }
            if (!items.isEmpty()) {
                Result itemsResult = new Result(items);
                result.put("items", itemsResult);
                this.putItemResultOptions(itemTemplateRule, itemsResult);
            }
        }

        return result;
    }

    private void extractFields(Element el, RuleNode rule, Map<String, Object> out) {
        for (Map.Entry<String, String> en : rule.getFieldSelectors().entrySet()) {
            String field = en.getKey();
            String selector = en.getValue();
            if (selector == null) continue;

            Result value = this.extractValue(el, field, selector, rule);
            if (value != null && !value.isEmpty() && !out.containsKey(field)) {
                out.put(field, value);
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

    private String resolveAttr(RuleNode rule, String field) {
        return resolve(rule, field, OptionKeyEnum.ATTR.getKey());
    }

    private String resolve(RuleNode rule, String field, String key) {
        Map<String, RuleNode.Options> fieldOptions = rule.getFieldOptions();
        if (fieldOptions == null) return null;

        RuleNode.Options opt = fieldOptions.get(field);
        if (opt == null) return null;

        Map<String, Object> values = getFieldOptions(rule, field);
        if (values == null) return null;

        Object value = values.get(key);
        return value == null ? null : value.toString();
    }

    private Map<String, Object> getFieldOptions(RuleNode rule, String field) {
        Map<String, RuleNode.Options> fieldOptions = rule.getFieldOptions();
        if (fieldOptions == null) return null;

        RuleNode.Options opt = fieldOptions.get(field);
        if (opt == null) return null;

        return opt.getValues();
    }

    private Result extractValue(Element el, String field, String selector, RuleNode rule) {
        Element target = el.selectFirst(selector);
        if (target == null) return null;
        String attr = this.resolveAttr(rule, field);
        Object value;
        switch (field) {
            case "text":
                if (attr != null) {
                    value = this.selectAttr(el, selector, attr);
                } else {
                    value = this.selectText(el, selector);
                }
                break;
            case "img":
                value = this.selectAttr(el, selector, attr == null ? "src" : attr);
                break;
            case "link":
                value = this.selectAttr(el, selector, attr == null ? "href" : attr);
                break;
            default:
                value = null;
        }
        if (value == null) return null;

        Result fv = new Result(value);

        Map<String, Object> fieldOptions = this.getFieldOptions(rule, field);
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
