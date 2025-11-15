package tv.tvai.like.chatGPT;

import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

import java.util.*;

public class Extractor {

    public List<Map<String, Object>> extract(Document doc, List<RuleNode> rules) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        Element body = doc.body();
        traverse(body, rules, result);

        return result;
    }

    private void traverse(Node node, List<RuleNode> rules, List<Map<String, Object>> result) {
        if (node instanceof Element) {
            Element el = (Element) node;

            // 按 DOM 顺序匹配所有 section
            for (RuleNode rule : rules) {
                if (matches(el, rule.getSelector())) {
                    Map<String, Object> parsed = extractSection(el, rule);
                    parsed.put("type", rule.getName());
                    result.add(parsed);
                }
            }
        }

        for (Node child : node.childNodes()) {
            traverse(child, rules, result);
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
        Map<String, Object> map = new LinkedHashMap<String, Object>();

        // text / img / link
        for (Map.Entry<String, String> en : rule.getFieldSelectors().entrySet()) {
            String field = en.getKey();
            String sel = en.getValue();

            String value = null;
            if (sel != null) {
                if ("text".equals(field)) {
                    value = selectText(el, sel);
                } else if ("img".equals(field)) {
                    value = selectAttr(el, sel, "src");
                } else if ("link".equals(field)) {
                    value = selectAttr(el, sel, "href");
                }
            }

            // ★★★ null 不 put
            if (value != null && !map.containsKey(field)) {
                map.put(field, value);
            }
        }

        // items
        RuleNode item = rule.getItemTemplate();
        if (item != null) {
            Elements items = el.select(item.getSelector());
            List<Map<String, Object>> arr = new ArrayList<Map<String, Object>>();

            for (Element ie : items) {
                Map<String, Object> mm = new LinkedHashMap<String, Object>();

                for (Map.Entry<String, String> en : item.getFieldSelectors().entrySet()) {
                    String field = en.getKey();
                    String sel = en.getValue();

                    String value = null;
                    if ("text".equals(field)) {
                        value = selectText(ie, sel);
                    } else if ("img".equals(field)) {
                        value = selectAttr(ie, sel, "src");
                    } else if ("link".equals(field)) {
                        value = selectAttr(ie, sel, "href");
                    }

                    if (value != null && !mm.containsKey(field)) mm.put(field, value);
                }

                arr.add(mm);
            }

            map.put("items", arr);
        }

        return map;
    }

    private String selectText(Element el, String selector) {
        Element e = el.selectFirst(selector);
        return e != null ? e.text().trim() : null;
    }

    private String selectAttr(Element el, String selector, String attr) {
        Element e = el.selectFirst(selector);
        return e != null ? e.attr(attr).trim() : null;
    }


    /** 去掉 selector 中的 [attr: xxx] 部分 */
    private static String stripAttr(String selector) {
        int idx = selector.indexOf("[attr:");
        if (idx >= 0) {
            return selector.substring(0, idx).trim();
        }
        return selector;
    }

    /** 提取 [attr: xxx] 中的属性名 */
    private static String extractAttrName(String selector) {
        int start = selector.indexOf("[attr:");
        if (start >= 0) {
            int end = selector.indexOf("]", start);
            if (end > start) {
                return selector.substring(start + 6, end).trim();
            }
        }
        return null;
    }
}
