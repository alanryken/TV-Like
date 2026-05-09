package tv.tvai.like;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import tv.tvai.like.enums.OptionKeyEnum;
import tv.tvai.like.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Extractor {

    public List<SectionResult> extract(Document doc, List<RuleNode> rules) {
        List<SectionResult> result = new ArrayList<SectionResult>();
        if (doc == null || rules == null || rules.isEmpty()) {
            return result;
        }
        Element root = doc.body() != null ? doc.body() : doc;
        List<Element> elementsInOrder = root.getAllElements();
        Map<Element, Integer> domPositions = this.indexElements(elementsInOrder);
        Map<RuleNode, Integer> matchedSectionCounts = new LinkedHashMap<RuleNode, Integer>();
        List<AnchoredSectionResult> anchoredResults = new ArrayList<AnchoredSectionResult>();
        int order = 0;
        for (Element element : elementsInOrder) {
            RuleNode matchedRule = this.findBestMatchedRule(element, rules, matchedSectionCounts);
            if (matchedRule == null) {
                continue;
            }
            List<AnchoredSectionResult> extractedResults = this.extractAnchoredSections(element, matchedRule, rules, order);
            anchoredResults.addAll(extractedResults);
            order += extractedResults.size();
            this.incrementMatchedCount(matchedRule, matchedSectionCounts);
        }
        Collections.sort(anchoredResults, new Comparator<AnchoredSectionResult>() {
            @Override
            public int compare(AnchoredSectionResult left, AnchoredSectionResult right) {
                int leftPosition = domPositions.get(left.anchor) == null ? Integer.MAX_VALUE : domPositions.get(left.anchor);
                int rightPosition = domPositions.get(right.anchor) == null ? Integer.MAX_VALUE : domPositions.get(right.anchor);
                if (leftPosition != rightPosition) {
                    return leftPosition - rightPosition;
                }
                return left.order - right.order;
            }
        });
        for (AnchoredSectionResult anchoredResult : anchoredResults) {
            result.add(anchoredResult.result);
        }
        return result;
    }

    private Map<Element, Integer> indexElements(List<Element> elementsInOrder) {
        Map<Element, Integer> domPositions = new LinkedHashMap<Element, Integer>();
        if (elementsInOrder == null) {
            return domPositions;
        }
        for (int i = 0; i < elementsInOrder.size(); i++) {
            domPositions.put(elementsInOrder.get(i), i);
        }
        return domPositions;
    }

    private List<AnchoredSectionResult> extractAnchoredSections(Element element,
                                                                RuleNode rule,
                                                                List<RuleNode> rules,
                                                                int startOrder) {
        List<AnchoredSectionResult> results = new ArrayList<AnchoredSectionResult>();
        if (this.shouldInlineItemsAsSections(element, rule, rules)) {
            RuleNode itemTemplateRule = rule.getItemTemplate();
            Elements itemElements = this.selectElements(element, itemTemplateRule.getSelector());
            long limit = this.resolveLimit(itemTemplateRule);
            int count = 0;
            int order = startOrder;
            for (Element itemElement : itemElements) {
                if (this.hasReachedLimit(limit, count)) {
                    break;
                }
                SectionResult parsed = new SectionResult(rule.getName());
                this.copyNonExecutableOptions(rule.getSectionOptions(), parsed.getMeta());
                this.copyNonExecutableOptions(itemTemplateRule.getSectionOptions(), parsed.getMeta());
                this.extractFields(itemElement, itemTemplateRule, parsed);
                if (parsed.hasFieldContent()) {
                    results.add(new AnchoredSectionResult(itemElement, parsed, order++));
                    count++;
                }
            }
            return results;
        }

        SectionResult parsed = new SectionResult(rule.getName());
        this.copyNonExecutableOptions(rule.getSectionOptions(), parsed.getMeta());
        this.extractSection(element, rule, parsed);
        results.add(new AnchoredSectionResult(element, parsed, startOrder));
        return results;
    }

    private boolean shouldInlineItemsAsSections(Element element, RuleNode rule, List<RuleNode> rules) {
        if (element == null || rule == null || rule.getItemTemplate() == null) {
            return false;
        }
        if (rule.getFieldSelectors() != null && !rule.getFieldSelectors().isEmpty()) {
            return false;
        }
        if (this.selectElements(element, rule.getItemTemplate().getSelector()).isEmpty()) {
            return false;
        }
        return this.containsNestedMatches(element, rules, rule);
    }

    private boolean containsNestedMatches(Element element, List<RuleNode> rules, RuleNode currentRule) {
        if (element == null || rules == null || rules.isEmpty()) {
            return false;
        }
        for (RuleNode rule : rules) {
            if (rule == null || rule == currentRule || StringUtils.isBlank(rule.getSelector())) {
                continue;
            }
            Elements matchedElements = this.selectElements(element, rule.getSelector());
            for (Element matchedElement : matchedElements) {
                if (matchedElement != null && matchedElement != element) {
                    return true;
                }
            }
        }
        return false;
    }

    private RuleNode findBestMatchedRule(Element element,
                                         List<RuleNode> rules,
                                         Map<RuleNode, Integer> matchedSectionCounts) {
        RuleNode bestRule = null;
        for (RuleNode rule : rules) {
            if (rule == null || this.hasReachedLimit(rule, matchedSectionCounts) || !this.matches(element, rule.getSelector())) {
                continue;
            }
            if (bestRule == null || this.comparePriority(rule, bestRule) < 0) {
                bestRule = rule;
            }
        }
        return bestRule;
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

    private void extractSection(Element el, RuleNode rule, SectionResult result) {
        this.extractFields(el, rule, result);

        RuleNode itemTemplateRule = rule.getItemTemplate();
        if (itemTemplateRule != null) {
            Elements elements = this.selectElements(el, itemTemplateRule.getSelector());
            ItemsResult itemsResult = new ItemsResult();

            long limit = this.resolveLimit(itemTemplateRule);
            int i = 0;
            for (Element itemEl : elements) {
                if (this.hasReachedLimit(limit, i)) break;
                ItemResult itemResult = new ItemResult();
                this.extractFields(itemEl, itemTemplateRule, itemResult);
                if (itemResult.hasFieldContent()) {
                    itemsResult.getList().add(itemResult);
                    i++;
                }
            }
            if (!itemsResult.getList().isEmpty()) {
                this.copyNonExecutableOptions(itemTemplateRule.getSectionOptions(), itemsResult.getMeta());
                result.setItems(itemsResult);
            }
        }
    }

    private void extractFields(Element el, RuleNode rule, FieldsResult out) {
        if (el == null || rule == null || out == null || rule.getFieldSelectors() == null) {
            return;
        }
        for (Map.Entry<String, String> en : rule.getFieldSelectors().entrySet()) {
            String field = en.getKey();
            String selector = en.getValue();
            if (selector == null) continue;

            FieldResult value = this.extractValue(el, field, selector, rule);
            if (value != null) {
                this.setFieldResult(out, field, value);
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

    private FieldResult extractValue(Element el, String field, String selector, RuleNode rule) {
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

        FieldResult fv = new FieldResult(value);

        Map<String, Object> fieldOptions = this.getFieldOptions(rule, field);
        this.copyNonExecutableOptions(fieldOptions, fv.getMeta());
        return fv;
    }

    private void setFieldResult(FieldsResult out, String field, FieldResult value) {
        if ("text".equals(field)) {
            if (out.getText() == null) {
                out.setText(value);
            }
            return;
        }
        if ("img".equals(field)) {
            if (out.getImg() == null) {
                out.setImg(value);
            }
            return;
        }
        if ("link".equals(field) && out.getLink() == null) {
            out.setLink(value);
        }
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

    private int comparePriority(RuleNode current, RuleNode existing) {
        int currentScore = this.selectorSpecificityScore(current == null ? null : current.getSelector());
        int existingScore = this.selectorSpecificityScore(existing == null ? null : existing.getSelector());
        if (currentScore != existingScore) {
            return existingScore - currentScore;
        }

        String currentSelector = current == null || current.getSelector() == null ? "" : current.getSelector();
        String existingSelector = existing == null || existing.getSelector() == null ? "" : existing.getSelector();
        int selectorCompare = existingSelector.length() - currentSelector.length();
        if (selectorCompare != 0) {
            return selectorCompare;
        }

        String currentName = current == null || current.getName() == null ? "" : current.getName();
        String existingName = existing == null || existing.getName() == null ? "" : existing.getName();
        return currentName.compareTo(existingName);
    }

    private int selectorSpecificityScore(String selector) {
        if (StringUtils.isBlank(selector)) {
            return Integer.MIN_VALUE;
        }
        int score = 0;
        for (int i = 0; i < selector.length(); i++) {
            char current = selector.charAt(i);
            if (current == '#') {
                score += 100;
            } else if (current == '.' || current == '[' || current == ':') {
                score += 10;
            } else if (current == '>' || current == '+' || current == '~') {
                score += 5;
            } else if (Character.isWhitespace(current)) {
                score += 1;
            }
        }
        return score;
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

    private static class AnchoredSectionResult {
        private final Element anchor;
        private final SectionResult result;
        private final int order;

        private AnchoredSectionResult(Element anchor, SectionResult result, int order) {
            this.anchor = anchor;
            this.result = result;
            this.order = order;
        }
    }
}
