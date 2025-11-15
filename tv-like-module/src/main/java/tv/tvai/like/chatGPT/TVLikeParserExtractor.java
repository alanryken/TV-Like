package tv.tvai.like.chatGPT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import tv.tvai.like.grok.PageExtractor;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * TVLikeParserExtractor.java
 * <p>
 * Single-file implementation:
 * - RuleNode: rule object
 * - RuleParser: parse DSL from <script type="text/plain" name="tv-like"> (uses extractRulesFromScript)
 * - HtmlExtractor: extract data using Jsoup according to RuleNode
 * <p>
 * Constraints:
 * - supports only fields: text, img, link
 * - supports multiple options in brackets, e.g. [attr: href] [transform: trim] [limit: 6]
 * - supports a single-level items block
 */
public class TVLikeParserExtractor {

    // -------------------------
    // RuleNode
    // -------------------------
    public static class RuleNode {
        private String name;
        private String selector;
        private Map<String, String> fieldSelectors = new LinkedHashMap<>(); // text/img/link -> selector or null
        private Map<String, Options> fieldOptions = new LinkedHashMap<>(); // per-field options
        private RuleNode itemTemplate; // single-level items template
        private Options sectionOptions = new Options();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSelector() {
            return selector;
        }

        public void setSelector(String selector) {
            this.selector = selector;
        }

        public Map<String, String> getFieldSelectors() {
            return fieldSelectors;
        }

        public Map<String, Options> getFieldOptions() {
            return fieldOptions;
        }

        public RuleNode getItemTemplate() {
            return itemTemplate;
        }

        public void setItemTemplate(RuleNode itemTemplate) {
            this.itemTemplate = itemTemplate;
        }

        public Options getSectionOptions() {
            return sectionOptions;
        }

        public void setSectionOptions(Options sectionOptions) {
            this.sectionOptions = sectionOptions;
        }

        public static class Options {
            private final Map<String, Object> values = new LinkedHashMap<>();

            public void put(String k, Object v) {
                values.put(k, v);
            }

            public Object get(String k) {
                return values.get(k);
            }

            public boolean contains(String k) {
                return values.containsKey(k);
            }

            public Map<String, Object> asMap() {
                return Collections.unmodifiableMap(values);
            }

            @Override
            public String toString() {
                return values.toString();
            }
        }
    }

    // -------------------------
    // RuleParser
    // -------------------------
    public static class RuleParser {
        private static final Set<String> ALLOWED_FIELDS;

        static {
            Set<String> s = new HashSet<>();
            s.add("text");
            s.add("img");
            s.add("link");
            ALLOWED_FIELDS = Collections.unmodifiableSet(s);
        }

        /**
         * 从 Document 中提取 <script type=text/plain name=tv-like> 的 DSL 文本（使用你提供的方法）
         */
        public static String extractRulesFromScript(Document doc) {
            Element script = doc.selectFirst("script[type=text/plain][name=tv-like]");
            if (script == null) {
                return "";
            }
            return script.html()  // .html() 获取 <script> 内部原始文本
                    .replace("&#10;", "\n")
                    .replace("&#13;", "\r\n")
                    .replace("\r\n", "\n");
        }

        private final String raw; // the DSL text

        public RuleParser(String rawDsl) {
            this.raw = rawDsl == null ? "" : rawDsl;
        }

        /**
         * Parse the DSL and return a map:
         * - "page-type": String (if present)
         * - "sections": List<RuleNode>
         */
        public Map<String, Object> parseAll() {
            String s = stripComments(raw);
            Map<String, Object> out = new LinkedHashMap<>();

            // page-type: capture single token/value after 'page-type:'
            Pattern pageP = Pattern.compile("page-type\\s*:\\s*(\\S+)", Pattern.CASE_INSENSITIVE);
            Matcher mp = pageP.matcher(s);
            if (mp.find()) {
                out.put("page-type", mp.group(1).trim());
            }

            // find all section blocks by searching for "section:" and matching braces
            List<RuleNode> sections = new ArrayList<>();
            Pattern secHeader = Pattern.compile("section\\s*:\\s*(\\w+)\\s+([^\\{]+)\\{", Pattern.CASE_INSENSITIVE);
            Matcher mh = secHeader.matcher(s);
            while (mh.find()) {
                String name = mh.group(1).trim();
                String selector = mh.group(2).trim();
                int braceStart = mh.end() - 1; // position of '{' in s
                int braceEnd = findMatchingBrace(s, braceStart);
                if (braceEnd < 0) {
                    // malformed block - skip
                    continue;
                }
                String blockContent = s.substring(braceStart + 1, braceEnd);
                // parse section-level options that may appear immediately after the block, e.g. "} [exclude: \".ads\"]"
                RuleNode.Options sectionOpts = parseOptionsNear(s, braceEnd + 1);

                RuleNode node = new RuleNode();
                node.setName(name);
                node.setSelector(selector);
                node.setSectionOptions(sectionOpts);

                // parse inner block: fields + optional items
                parseSectionBody(blockContent, node);

                sections.add(node);
            }

            out.put("sections", sections);
            return out;
        }

        // remove // comments and /* */ blocks
        private static String stripComments(String in) {
            if (in == null || in.isEmpty()) return "";
            // remove block comments
            String noBlock = in.replaceAll("/\\*([\\s\\S]*?)\\*/", " ");
            // remove line comments using Pattern with MULTILINE
            String noLine = Pattern.compile("//.*?$", Pattern.MULTILINE)
                    .matcher(noBlock)
                    .replaceAll("");
            return noLine;
        }

        // find matching '}' for '{' at openIndex
        private static int findMatchingBrace(String s, int openIndex) {
            if (openIndex < 0 || openIndex >= s.length() || s.charAt(openIndex) != '{') return -1;
            int depth = 0;
            for (int i = openIndex; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
            return -1;
        }

        private static void parseSectionBody(String body, RuleNode node) {
            if (body == null) return;

            // 不对 body 做 trim()，以保证索引一致
            // 但后面逐行处理时会对每行做 trim()
            String fullBody = body;

            // first, try to extract items block if exists
            // items:\s*<selector> { ... } [options]
            Pattern itemsP = Pattern.compile("items\\s*:\\s*([^\\{\\n]+)\\{", Pattern.CASE_INSENSITIVE);
            Matcher mi = itemsP.matcher(fullBody);
            if (mi.find()) {
                String itemsSelector = mi.group(1).trim(); // selector text itself can be trimmed safely

                int itemsBlockOpenInBody = mi.end() - 1; // position of '{' inside fullBody
                int itemsBlockCloseInBody = findMatchingBrace(fullBody, itemsBlockOpenInBody);
                if (itemsBlockCloseInBody >= 0) {
                    // extract inner content of items { ... }
                    String itemsInner = fullBody.substring(itemsBlockOpenInBody + 1, itemsBlockCloseInBody);

                    // capture options that may appear immediately after the items block within the same section body
                    String afterItems = "";
                    if (itemsBlockCloseInBody + 1 < fullBody.length()) {
                        afterItems = fullBody.substring(itemsBlockCloseInBody + 1);
                    }
                    RuleNode.Options itemsOpts = parseOptionsNear(afterItems, 0);

                    // build item template
                    RuleNode itemNode = new RuleNode();
                    itemNode.setName("items");
                    itemNode.setSelector(itemsSelector);
                    itemNode.setSectionOptions(itemsOpts);
                    parseItemFields(itemsInner, itemNode);

                    node.setItemTemplate(itemNode);

                    // remove items block from the fullBody to avoid re-parsing it when processing top-level fields
                    // Use substring positions that are consistent with fullBody
                    String before = fullBody.substring(0, mi.start());
                    String after = "";
                    if (itemsBlockCloseInBody + 1 < fullBody.length()) {
                        after = fullBody.substring(itemsBlockCloseInBody + 1);
                    }
                    // new working content (safe to trim later per-line)
                    fullBody = before + "\n" + after;
                }
            }

            // now parse top-level fields (text/img/link) line-by-line from fullBody
            Scanner sc = new Scanner(fullBody);
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;
                // match: (text|img|link): <selector-or-null> [opt] [opt] ...
                Pattern fieldP = Pattern.compile("^(text|img|link)\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
                Matcher fm = fieldP.matcher(line);
                if (!fm.find()) continue;
                String field = fm.group(1).toLowerCase();
                String rest = fm.group(2).trim();

                // Extract all [...] tokens
                List<String> optTokens = new ArrayList<>();
                Pattern optTokenP = Pattern.compile("\\[(.*?)]");
                Matcher om = optTokenP.matcher(rest);
                while (om.find()) {
                    optTokens.add(om.group(1).trim());
                }

                // remove all [..] from rest to get selector part
                String selectorPart = rest.replaceAll("\\[.*?]", "").trim();
                if ("null".equalsIgnoreCase(selectorPart)) selectorPart = null;
                if (selectorPart != null) {
                    node.getFieldSelectors().putIfAbsent(field, selectorPart);
                }

                // parse options tokens into Options object
                RuleNode.Options fo = new RuleNode.Options();
                for (String t : optTokens) {
                    parseSingleOptionTokenInto(t, fo);
                }
                if (fo != null) {
                    node.getFieldOptions().putIfAbsent(field, fo);
                }
            }
            sc.close();
        }

        // parse fields inside items { ... } (only allowed text/img/link)
        private static void parseItemFields(String inner, RuleNode itemNode) {
            if (inner == null) return;
            Scanner sc = new Scanner(inner);
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;
                Pattern fieldP = Pattern.compile("^(text|img|link)\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
                Matcher fm = fieldP.matcher(line);
                if (!fm.find()) continue;
                String field = fm.group(1).toLowerCase();
                String rest = fm.group(2).trim();
                List<String> optTokens = new ArrayList<>();
                Pattern optTokenP = Pattern.compile("\\[(.*?)]");
                Matcher om = optTokenP.matcher(rest);
                while (om.find()) {
                    optTokens.add(om.group(1).trim());
                }
                String selectorPart = rest.replaceAll("\\[.*?]", "").trim();
                if ("null".equalsIgnoreCase(selectorPart)) selectorPart = null;

                if (!ALLOWED_FIELDS.contains(field)) continue;
                if (selectorPart != null) {
                    itemNode.getFieldSelectors().putIfAbsent(field, selectorPart);
                }
                RuleNode.Options fo = new RuleNode.Options();
                for (String t : optTokens) parseSingleOptionTokenInto(t, fo);
                if (fo != null) {
                    itemNode.getFieldOptions().putIfAbsent(field, fo);
                }
            }
            sc.close();
        }

        // parse tokens like "attr: href", "transform: trim", "limit: 6", or flags like "lazy"
        private static void parseSingleOptionTokenInto(String token, RuleNode.Options opts) {
            if (token == null || token.isEmpty()) return;
            // token may be "attr: href" or "transform: trim" or "limit: 6" or "flag"
            int colon = token.indexOf(':');
            if (colon >= 0) {
                String k = token.substring(0, colon).trim();
                String v = token.substring(colon + 1).trim();
                // unquote if present
                if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                    v = v.substring(1, v.length() - 1);
                }
                // parse integers for known keys
                if ("limit".equalsIgnoreCase(k)) {
                    try {
                        opts.put(k, Integer.parseInt(v));
                    } catch (NumberFormatException e) {
                        opts.put(k, v);
                    }
                } else {
                    opts.put(k, v);
                }
            } else {
                opts.put(token, Boolean.TRUE);
            }
        }

        // parse options that may appear near some offset: scans a leading sequence of [..][..] and parse them
        private static RuleNode.Options parseOptionsNear(String text, int startOffset) {
            RuleNode.Options opts = new RuleNode.Options();
            if (text == null || startOffset >= text.length()) return opts;
            String tail = text.substring(startOffset);
            Pattern p = Pattern.compile("^(\\s*\\[[^]]+])+", Pattern.DOTALL);
            Matcher m = p.matcher(tail);
            if (m.find()) {
                String group = m.group(0);
                Pattern inner = Pattern.compile("\\[(.*?)]");
                Matcher im = inner.matcher(group);
                while (im.find()) {
                    parseSingleOptionTokenInto(im.group(1).trim(), opts);
                }
            }
            return opts;
        }
    }

    // -------------------------
    // HtmlExtractor
    // -------------------------
    public static class HtmlExtractor {

        /**
         * Extract a section according to rule from Document
         */
        public Map<String, Object> extractSection(RuleNode rule, Document doc) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("type", rule.getName());

            Element root = doc.selectFirst(rule.getSelector());
            if (root == null) {
                // not found -> still return basic info with fields null
                for (String f : Arrays.asList("text", "img", "link")) out.put(f, null);
                out.put("items", Collections.emptyList());
                return out;
            }

            // fields
            for (String f : Arrays.asList("text", "img", "link")) {
                String sel = rule.getFieldSelectors().get(f);
                RuleNode.Options opts = rule.getFieldOptions().getOrDefault(f, new RuleNode.Options());
                Object val = extractField(root, sel, opts);
                out.put(f, val);
            }

            // items
            if (rule.getItemTemplate() != null) {
                List<Map<String, Object>> items = extractItems(root, rule.getItemTemplate());
                out.put("items", items);
            } else {
                out.put("items", Collections.emptyList());
            }

            return out;
        }

        private Object extractField(Element context, String selector, RuleNode.Options opts) {
            if (selector == null) return null;
            selector = selector.trim();
            // handle case where selector might be like "a" or "a" with opts specifying attr
            Element el = context.selectFirst(selector);
            if (el == null) return null;

            String result;
            if (opts != null && opts.contains("attr")) {
                String attrName = String.valueOf(opts.get("attr"));
                result = el.attr(attrName);
            } else {
                result = el.text();
            }

            if (opts != null && opts.contains("transform")) {
                String t = String.valueOf(opts.get("transform"));
                if ("trim".equalsIgnoreCase(t)) result = result == null ? null : result.trim();
                // more transforms can be added later
            }

            if (result != null && result.isEmpty()) return null;
            return result;
        }

        private List<Map<String, Object>> extractItems(Element root, RuleNode itemRule) {
            List<Map<String, Object>> out = new ArrayList<>();
            Elements els = root.select(itemRule.getSelector());
            int limit = Integer.MAX_VALUE;
            if (itemRule.getSectionOptions() != null && itemRule.getSectionOptions().contains("limit")) {
                Object o = itemRule.getSectionOptions().get("limit");
                if (o instanceof Number) limit = ((Number) o).intValue();
                else {
                    try {
                        limit = Integer.parseInt(String.valueOf(o));
                    } catch (Exception ignored) {
                    }
                }
            }
            int take = Math.min(limit, els.size());
            for (int i = 0; i < take; i++) {
                Element el = els.get(i);
                Map<String, Object> item = new LinkedHashMap<>();
                for (String f : Arrays.asList("text", "img", "link")) {
                    String sel = itemRule.getFieldSelectors().get(f);
                    RuleNode.Options opts = itemRule.getFieldOptions().getOrDefault(f, new RuleNode.Options());
                    Object v = extractField(el, sel, opts);
                    item.put(f, v);
                }
                out.add(item);
            }
            return out;
        }
    }

    // -------------------------
    // Demo main
    // -------------------------
    public static void main(String[] args) throws IOException {

        try (InputStream inputStream = PageExtractor.class.getClassLoader().getResourceAsStream("mtyy1_com_index.html")) {
            // === 2. 解析 HTML ===
            Document doc = Jsoup.parse(inputStream, "UTF-8", "");

            // 1) extract DSL text from script node (your method)
            String dsl = RuleParser.extractRulesFromScript(doc);

            // 2) parse DSL into RuleNode objects
            RuleParser parser = new RuleParser(dsl);
            Map<String, Object> parsed = parser.parseAll();
            String pageType = (String) parsed.getOrDefault("page-type", null);
            List<RuleNode> sections = (List<RuleNode>) parsed.getOrDefault("sections", Collections.emptyList());

            // 3) extract data using HtmlExtractor
            HtmlExtractor extractor = new HtmlExtractor();
            List<Object> sectionOut = sections.stream()
                    .map(r -> extractor.extractSection(r, doc))
                    .collect(Collectors.toList());

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("page-type", pageType);
            out.put("sections", sectionOut);

            ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            System.out.println(om.writeValueAsString(out));
        }
    }
}
