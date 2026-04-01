package tv.tvai.like;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import tv.tvai.like.enums.OptionKeyEnum;
import tv.tvai.like.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Extractor {

    public List<Map<String, Object>> extract(Document doc, List<RuleNode> rules) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (doc == null || rules == null || rules.isEmpty()) {
            return result;
        }
        Node root = doc.body() != null ? doc.body() : doc;
        this.traverse(root, rules, result, new LinkedHashMap<RuleNode, Integer>());
        return result;
    }

    private void traverse(Node node,
                          List<RuleNode> rules,
                          List<Map<String, Object>> result,
                          Map<RuleNode, Integer> matchedSectionCounts) {
        if (node instanceof Element) {
            Element el = (Element) node;
            for (RuleNode rule : rules) {
                if (this.hasReachedLimit(rule, matchedSectionCounts)) {
                    continue;
                }
                boolean matches = this.matches(el, rule.getSelector());
                if (matches) {
                    Map<String, Object> parsed = new LinkedHashMap<>();
                    parsed.put("section", rule.getName());
                    this.copyNonExecutableOptions(rule.getSectionOptions(), parsed);
                    Map<String, Object> map = this.extractSection(el, rule);
                    parsed.putAll(map);
                    result.add(parsed);
                    this.incrementMatchedCount(rule, matchedSectionCounts);
                }
            }
        }

        for (Node child : node.childNodes()) {
            this.traverse(child, rules, result, matchedSectionCounts);
        }
    }

    private boolean matches(Element el, String selector) {
        if (el == null || StringUtils.isBlank(selector)) {
            return false;
        }
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
            Elements elements = this.selectElements(el, itemTemplateRule.getSelector());
            List<Map<String, Object>> items = new ArrayList<>();

            long limit = this.resolveLimit(itemTemplateRule);
            int i = 0;
            for (Element itemEl : elements) {
                if (this.hasReachedLimit(limit, i)) break;
                Map<String, Object> itemMap = new LinkedHashMap<>();
                this.extractFields(itemEl, itemTemplateRule, itemMap);
                if (!itemMap.isEmpty()) {
                    items.add(itemMap);
                    i++;
                }
            }
            if (!items.isEmpty()) {
                Result itemsResult = new Result(items);
                result.put("items", itemsResult);
                this.copyNonExecutableOptions(itemTemplateRule.getSectionOptions(), itemsResult);
            }
        }

        return result;
    }

    private void extractFields(Element el, RuleNode rule, Map<String, Object> out) {
        if (el == null || rule == null || out == null || rule.getFieldSelectors() == null) {
            return;
        }
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
        try {
            return Long.parseLong(sectionOptions.getValues().get(OptionKeyEnum.LIMIT.getKey()).toString().trim());
        } catch (Exception e) {
            return -1;
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
        Element target = this.selectFirst(el, selector);
        if (target == null) return null;
        String attr = this.resolveAttr(rule, field);
        String value;
        switch (field) {
            case "text":
                if (attr != null) {
                    value = this.selectAttr(target, attr);
                } else {
                    value = this.selectText(target);
                }
                break;
            case "img":
                value = this.selectAttr(target, attr == null ? "src" : attr);
                break;
            case "link":
                value = this.selectAttr(target, attr == null ? "href" : attr);
                break;
            default:
                value = null;
        }
        value = this.applyTransforms(value, target, attr == null ? this.defaultAttr(field) : attr, rule, field);
        if (StringUtils.isBlank(value)) return null;

        Result fv = new Result(value);

        Map<String, Object> fieldOptions = this.getFieldOptions(rule, field);
        this.copyNonExecutableOptions(fieldOptions, fv);
        return fv;
    }

    private String selectText(Element element) {
        return element != null ? element.text().trim() : null;
    }

    private String selectAttr(Element element, String attr) {
        return element != null && StringUtils.isNotBlank(attr) ? element.attr(attr).trim() : null;
    }

    private Element selectFirst(Element element, String selector) {
        if (element == null || StringUtils.isBlank(selector)) {
            return null;
        }
        try {
            return element.selectFirst(selector);
        } catch (Exception e) {
            return null;
        }
    }

    private Elements selectElements(Element element, String selector) {
        if (element == null || StringUtils.isBlank(selector)) {
            return new Elements();
        }
        try {
            return element.select(selector);
        } catch (Exception e) {
            return new Elements();
        }
    }

    private void copyNonExecutableOptions(RuleNode.Options options, Map<String, Object> target) {
        if (options == null) {
            return;
        }
        this.copyNonExecutableOptions(options.getValues(), target);
    }

    private void copyNonExecutableOptions(Map<String, Object> options, Map<String, Object> target) {
        if (options == null || options.isEmpty() || target == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : options.entrySet()) {
            if (!OptionKeyEnum.executable(entry.getKey())) {
                target.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private boolean hasReachedLimit(RuleNode rule, Map<RuleNode, Integer> matchedSectionCounts) {
        return this.hasReachedLimit(this.resolveLimit(rule), matchedSectionCounts.get(rule) == null ? 0 : matchedSectionCounts.get(rule));
    }

    private boolean hasReachedLimit(long limit, int currentCount) {
        return limit >= 0 && currentCount >= limit;
    }

    private void incrementMatchedCount(RuleNode rule, Map<RuleNode, Integer> matchedSectionCounts) {
        Integer count = matchedSectionCounts.get(rule);
        matchedSectionCounts.put(rule, count == null ? 1 : count + 1);
    }

    private String defaultAttr(String field) {
        if ("img".equals(field)) {
            return "src";
        }
        if ("link".equals(field)) {
            return "href";
        }
        return null;
    }

    private String applyTransforms(String value, Element target, String attr, RuleNode rule, String field) {
        if (value == null) {
            return null;
        }
        String transformed = value;
        String transformValue = this.resolve(rule, field, OptionKeyEnum.TRANSFORM.getKey());
        if (StringUtils.isBlank(transformValue)) {
            return transformed.trim();
        }
        String[] transforms = transformValue.split("\\|");
        for (String transform : transforms) {
            transformed = this.applyTransform(transformed, transform == null ? "" : transform.trim(), target, attr);
            if (transformed == null) {
                return null;
            }
        }
        return transformed.trim();
    }

    private String applyTransform(String value, String transform, Element target, String attr) {
        if (StringUtils.isBlank(transform)) {
            return value;
        }
        if ("trim".equalsIgnoreCase(transform)) {
            return value.trim();
        }
        if ("lower".equalsIgnoreCase(transform) || "lower-case".equalsIgnoreCase(transform)) {
            return value.toLowerCase();
        }
        if ("upper".equalsIgnoreCase(transform) || "upper-case".equalsIgnoreCase(transform)) {
            return value.toUpperCase();
        }
        if ("digits".equalsIgnoreCase(transform)) {
            String digitsOnly = value.replaceAll("[^0-9]", "");
            return digitsOnly.isEmpty() ? null : digitsOnly;
        }
        if ("abs-url".equalsIgnoreCase(transform)) {
            if (target == null || StringUtils.isBlank(attr)) {
                return value;
            }
            String absUrl = target.absUrl(attr);
            return StringUtils.isNotBlank(absUrl) ? absUrl.trim() : value;
        }
        return value;
    }
}
