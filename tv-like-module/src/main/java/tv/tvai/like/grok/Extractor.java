package tv.tvai.like.grok;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;

public class Extractor {
    private List<RuleNode> sections;
    private String pageType = "generic";

    /**
     * 从 Document 中提取 <script type="text/plain" name="tv-like"> 内容作为规则
     * 若不存在则返回空字符串
     */
    private String extractRulesFromScript(Document doc) {
        Element script = doc.selectFirst("script[type=text/plain][name=tv-like]");
        if (script == null) {
            return "";
        }
        return script.html()  // .html() 获取 <script> 内部原始文本
                .replace("&#10;", "\n")
                .replace("&#13;", "\r\n")
                .replace("\r\n", "\n");
    }

    /**
     * 重载 parseRules：优先从 HTML 提取规则
     */
    public void parseRules(Document doc) {
        String rulesContent = extractRulesFromScript(doc);
        if (rulesContent.isEmpty()) {
            // 可选：fallback 到 meta 或 /extrules.txt
            // 目前保持简单，只用 script
            this.sections = new ArrayList<>();
            return;
        }
        parseRules(rulesContent);  // 复用原有字符串解析逻辑
    }

    public void parseRules(String rules) {
        rules = rules.replace("&#10;", "\n");
        RuleParser p = new RuleParser();
        this.sections = p.parse(rules);
        for (String l : rules.split("\n")) {
            if (l.trim().startsWith("page-type: ")) {
                pageType = l.trim().substring(11).trim();
                break;
            }
        }
    }

    public Map<String, Object> extractOrdered(Document doc) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("page-type", pageType);
        List<Map<String, Object>> secs = new ArrayList<>();

        Element body = doc.body();
        for (Node n : body.childNodes()) {
            if (!(n instanceof Element)) continue;
            Element el = (Element) n;
            RuleNode rule = match(el);
            if (rule == null) continue;

            Map<String, Object> data = extractSection(el, rule);
            if (!data.isEmpty()) {
                Map<String, Object> sec = new LinkedHashMap<>();
                sec.put("type", rule.getName());
                sec.putAll(data);
                secs.add(sec);
            }
        }
        res.put("sections", secs);
        return res;
    }

    private RuleNode match(Element el) {
        for (RuleNode r : sections)
            if (el.selectFirst(r.getSelector()) != null) return r;
        return null;
    }

    private Map<String, Object> extractSection(Element container, RuleNode rule) {
        Map<String, Object> data = new LinkedHashMap<>();
        for (String f : rule.getFieldSelectors().keySet()) {
            String sel = rule.getFieldSelectors().get(f);
            if ("null".equals(sel)) { data.put(f, null); continue; }
            RuleNode.Options opt = rule.getFieldOptions().get(f);
            Object v = extractField(container, f, sel, opt);
            data.put(f, v);
        }
        RuleNode it = rule.getItemTemplate();
        if (it != null) data.put("items", extractItems(container, it));
        return data;
    }

    private Object extractField(Element parent, String type, String sel, RuleNode.Options opt) {
        Elements els = parent.select(sel);
        if (els.isEmpty()) return null;

        Integer limit = opt != null ? opt.getInt("limit") : null;
        Integer idx   = opt != null ? opt.getInt("index") : null;
        if (idx != null && idx < els.size()) els = new Elements(els.get(idx));
        if (limit != null && els.size() > limit) els = new Elements(els.subList(0, limit));

        if (els.size() == 1) return process(els.first(), type, opt);
        List<Object> list = new ArrayList<>();
        for (Element e : els) { Object v = process(e, type, opt); if (v != null) list.add(v); }
        return list;
    }

    private Object process(Element el, String type, RuleNode.Options opt) {
        String raw = null;
        String attr = opt != null ? opt.getString("attr") : null;
        List<String> tr = opt != null ? opt.getList("transform") : new ArrayList<>();
        if ("text".equals(type)) raw = el.text();
        else if ("img".equals(type)) raw = attrFallback(el, attr, "src");
        else if ("link".equals(type)) {
            Element a = el.selectFirst("a");
            if (a != null) raw = attrFallback(a, "href", "href");
        }
        if (raw == null || raw.isEmpty()) return null;
        return transform(raw, tr);
    }

    private List<Map<String, Object>> extractItems(Element container, RuleNode itemRule) {
        Elements items = container.select(itemRule.getSelector());   // 相对容器
        RuleNode.Options secOpt = itemRule.getSectionOptions();
        Integer idx = secOpt.getInt("index");
        Integer limit = secOpt.getInt("limit");
        if (idx != null && idx < items.size()) items = new Elements(items.get(idx));
        if (limit != null && items.size() > limit) items = new Elements(items.subList(0, limit));

        List<Map<String, Object>> list = new ArrayList<>();
        for (Element it : items) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (String f : itemRule.getFieldSelectors().keySet()) {
                String sel = itemRule.getFieldSelectors().get(f);
                if ("null".equals(sel)) { map.put(f, null); continue; }
                RuleNode.Options opt = itemRule.getFieldOptions().get(f);
                Object v = extractField(it, f, sel, opt);
                map.put(f, v);
            }
            if (!map.isEmpty()) list.add(map);
        }
        return list;
    }

    private String attrFallback(Element el, String chain, String def) {
        String[] arr = (chain != null ? chain : def).split("\\|");
        for (String a : arr) { String v = el.attr(a.trim()); if (!v.isEmpty()) return v; }
        return null;
    }

    private String transform(String s, List<String> ops) {
        String r = s;
        for (String op : ops) {
            if ("trim".equals(op)) r = r.trim();
            else if ("url-join".equals(op)) {
                try { r = new URL(new URL("https://example.com"), r).toString(); }
                catch (MalformedURLException ignored) {}
            }
        }
        return r;
    }
}