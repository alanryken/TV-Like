package tv.tvai.like;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.jsoup.Jsoup;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;
import java.net.HttpURLConnection;

public class Extractor {
    private List<RuleNode> sections;
    private String pageType = "generic";
    private Map<String, Object> globals = new HashMap<>();

    public void parseRules(String rulesContent) {
        rulesContent = rulesContent.replace("&#10;", "\n");
        RuleParser parser = new RuleParser();
        this.sections = parser.parse(rulesContent);
        String[] lines = rulesContent.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("page-type: ")) {
                this.pageType = line.substring(11).trim();
                break;
            }
        }
    }

    public String getRulesFromMetaOrFile(Document doc, String baseUrl) {
        // 优先 meta
        Element meta = doc.selectFirst("script[name=tv-like]");
        if (meta != null) {
            return meta.html();
        }

        // Fallback remote txt
        try {
            URL url = new URL(baseUrl + "/extrules.txt");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                return sb.toString();
            }
        } catch (IOException e) {
            // Ignore , fallback to empty
        }

        return "";
    }

    public List<RuleNode> getSections() {
        return new ArrayList<>(sections);
    }

    public Map<String, Object> getGlobals() {
        return new HashMap<>(globals);
    }

    public Map<String, Object> extractOrdered(Document doc, String baseUrl) {
        String rulesContent = getRulesFromMetaOrFile(doc, baseUrl);
        if (rulesContent.isEmpty()) {
            return new HashMap<>();
        }
        parseRules(rulesContent);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page-type", pageType);
        List<Map<String, Object>> orderedSections = new ArrayList<>();

        Element body = doc.body();
        for (Node node : body.childNodes()) {
            if (!(node instanceof Element)) continue;
            Element elem = (Element) node;

            RuleNode matched = findMatchingSection(elem);
            if (matched == null) continue;

            Map<String, Object> sectionData = extractSection(elem, matched);
            if (!sectionData.isEmpty()) {
                Map<String, Object> sec = new LinkedHashMap<>();
                sec.put("type", matched.getName());
                sec.putAll(sectionData);
                orderedSections.add(sec);
            }
        }

        result.put("sections", orderedSections);
        return result;
    }

    private RuleNode findMatchingSection(Element elem) {
        for (RuleNode rule : sections) {
            if (isMatch(elem, rule.getSelector())) {
                return rule;
            }
        }
        return null;
    }

    private boolean isMatch(Element elem, String selector) {
        return elem.selectFirst(selector) != null;
    }

    private Map<String, Object> extractSection(Element container, RuleNode rule) {
        Map<String, Object> data = new LinkedHashMap<>();

        for (String field : rule.getFieldSelectors().keySet()) {
            String sel = rule.getFieldSelectors().get(field);
            if ("null".equals(sel)) {
                data.put(field, null);
                continue;
            }
            RuleNode.Options fOpts = rule.getFieldOptions().get(field);
            Object value = extractField(container, field, sel, fOpts);
            data.put(field, value);
        }

        RuleNode itemT = rule.getItemTemplate();
        if (itemT != null) {
            List<Map<String, Object>> items = extractItems(container, itemT);
            data.put("items", items);
        }

        return data;
    }

    private Object extractField(Element parent, String fieldType, String sel, RuleNode.Options fOpts) {
        Elements matches = parent.select(sel);
        if (matches.isEmpty()) return null;

        Integer limit = fOpts != null ? fOpts.getInt("limit") : null;
        Integer index = fOpts != null ? fOpts.getInt("index") : null;
        if (index != null && index < matches.size()) {
            matches = new Elements(matches.get(index));
        }
        if (limit != null && matches.size() > limit) {
            matches = new Elements(matches.subList(0, limit));
        }

        if (matches.size() == 1) {
            return processField(matches.first(), fieldType, fOpts);
        } else {
            List<Object> values = new ArrayList<>();
            for (Element m : matches) {
                Object v = processField(m, fieldType, fOpts);
                if (v != null) values.add(v);
            }
            return values;
        }
    }

    private Object processField(Element elem, String fieldType, RuleNode.Options fOpts) {
        String raw = null;
        String attrChain = fOpts != null ? fOpts.getString("attr") : null;
        List<String> transforms = fOpts != null ? fOpts.getList("transform") : new ArrayList<>();
        String baseUrl = globals.get("base-url") != null ? globals.get("base-url").toString() : null;

        if ("text".equals(fieldType)) {
            raw = elem.text();
        } else if ("img".equals(fieldType)) {
            raw = getAttrFallback(elem, attrChain, "src");
        } else if ("link".equals(fieldType)) {
            Element link = elem.selectFirst("a");
            if (link != null) raw = getAttrFallback(link, "href", "href");
        }

        if (raw == null || raw.isEmpty()) return null;
        return applyTransforms(raw, transforms, baseUrl);
    }

    private List<Map<String, Object>> extractItems(Element container, RuleNode itemT) {
        Elements items = container.select(itemT.getSelector());
        RuleNode.Options itemSectionOpts = itemT.getSectionOptions();
        Integer index = itemSectionOpts.getInt("index");
        if (index != null && index < items.size()) {
            items = new Elements(items.get(index));
        }
        Integer limit = itemSectionOpts.getInt("limit");
        if (limit != null && items.size() > limit) {
            items = new Elements(items.subList(0, limit));
        }

        List<Map<String, Object>> itemList = new ArrayList<>();
        for (Element item : items) {
            Map<String, Object> itemData = new LinkedHashMap<>();
            for (String field : itemT.getFieldSelectors().keySet()) {
                String sel = itemT.getFieldSelectors().get(field);
                if ("null".equals(sel)) {
                    itemData.put(field, null);
                    continue;
                }
                RuleNode.Options fOpts = itemT.getFieldOptions().get(field);
                Object value = extractField(item, field, sel, fOpts);
                itemData.put(field, value);
            }
            if (!itemData.isEmpty()) itemList.add(itemData);
        }
        return itemList;
    }

    private String getAttrFallback(Element elem, String chain, String def) {
        String attrs = (chain != null) ? chain : def;
        for (String a : attrs.split("\\|")) {
            String val = elem.attr(a.trim());
            if (!val.isEmpty()) return val;
        }
        return null;
    }

    private String applyTransforms(String value, List<String> transforms, String baseUrl) {
        String result = value;
        for (String t : transforms) {
            if ("trim".equals(t)) result = result.trim();
            else if ("upper".equals(t)) result = result.toUpperCase();
            else if ("lower".equals(t)) result = result.toLowerCase();
            else if ("url-join".equals(t) && baseUrl != null) {
                try {
                    result = new URL(new URL(baseUrl), result).toString();
                } catch (MalformedURLException e) {
                    // ignore
                }
            }
        }
        return result;
    }
}