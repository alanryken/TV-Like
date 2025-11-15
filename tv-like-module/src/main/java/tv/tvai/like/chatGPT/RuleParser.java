package tv.tvai.like.chatGPT;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleParser {

    private static final Set<String> ALLOWED_FIELDS =
            new HashSet<String>(Arrays.asList("text", "img", "link"));

    public List<RuleNode> parse(String dsl) {
        List<RuleNode> sections = new ArrayList<RuleNode>();
        if (dsl == null || dsl.trim().isEmpty()) return sections;

        Pattern sectionP = Pattern.compile(
                "section\\s*:\\s*(\\w+)\\s+([^\\{]+)\\{",
                Pattern.CASE_INSENSITIVE);

        Matcher m = sectionP.matcher(dsl);
        while (m.find()) {
            String name = m.group(1).trim();
            String selector = m.group(2).trim();

            int open = m.end() - 1;
            int close = findMatchingBrace(dsl, open);
            if (close < 0) continue;

            String body = dsl.substring(open + 1, close);

            RuleNode node = new RuleNode(name, selector);
            parseSectionBody(body, node);

            sections.add(node);
        }

        return sections;
    }

    private static void parseSectionBody(String body, RuleNode node) {
        if (body == null) return;

        String fullBody = body;

        // ---------- 解析 items ----------
        Pattern itemsP = Pattern.compile("items\\s*:\\s*([^\\{\\n]+)\\{", Pattern.CASE_INSENSITIVE);
        Matcher mi = itemsP.matcher(fullBody);
        if (mi.find()) {
            String itemsSelector = mi.group(1).trim();

            int open = mi.end() - 1;
            int close = findMatchingBrace(fullBody, open);

            if (close >= 0) {
                String inner = fullBody.substring(open + 1, close);

                // items options
                String after = "";
                if (close + 1 < fullBody.length()) {
                    after = fullBody.substring(close + 1);
                }

                RuleNode.Options opts = parseOptionsNear(after, 0);

                RuleNode itemNode = new RuleNode();
                itemNode.setName("items");
                itemNode.setSelector(itemsSelector);
                itemNode.setSectionOptions(opts);

                parseItemFields(inner, itemNode);

                node.setItemTemplate(itemNode);

                String before = fullBody.substring(0, mi.start());
                String aft = "";
                if (close + 1 < fullBody.length()) {
                    aft = fullBody.substring(close + 1);
                }
                fullBody = before + "\n" + aft;
            }
        }

        // ---------- 解析 text / img / link ----------
        Scanner sc = new Scanner(fullBody);
        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;

            Matcher fm = Pattern.compile(
                    "^(text|img|link)\\s*:\\s*(.+)$",
                    Pattern.CASE_INSENSITIVE).matcher(line);

            if (!fm.find()) continue;

            String field = fm.group(1).toLowerCase();
            if (!ALLOWED_FIELDS.contains(field)) continue;

            String rest = fm.group(2).trim();

            // 提取 [...] options
            List<String> optTokens = new ArrayList<String>();
            Matcher om = Pattern.compile("\\[(.*?)]").matcher(rest);
            while (om.find()) {
                optTokens.add(om.group(1).trim());
            }

            // 去掉 [..] 后就是 selector 部分
            String selectorPart = rest.replaceAll("\\[.*?]", "").trim();
            if ("null".equalsIgnoreCase(selectorPart)) selectorPart = null;

            // ★★★ putIfAbsent + null 不 put
            if (selectorPart != null) {
                if (!node.getFieldSelectors().containsKey(field)) {
                    node.getFieldSelectors().put(field, selectorPart);
                }
            }

            RuleNode.Options fo = new RuleNode.Options();
            for (String t : optTokens) {
                parseSingleOptionTokenInto(t, fo);
            }

            if (!node.getFieldOptions().containsKey(field)) {
                node.getFieldOptions().put(field, fo);
            }
        }
        sc.close();
    }

    private static void parseItemFields(String inner, RuleNode node) {
        Scanner sc = new Scanner(inner);
        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;

            Matcher fm = Pattern.compile(
                    "^(text|img|link)\\s*:\\s*(.+)$",
                    Pattern.CASE_INSENSITIVE).matcher(line);

            if (!fm.find()) continue;

            String field = fm.group(1).toLowerCase();
            if (!ALLOWED_FIELDS.contains(field)) continue;

            String rest = fm.group(2).trim();

            List<String> optTokens = new ArrayList<String>();
            Matcher om = Pattern.compile("\\[(.*?)]").matcher(rest);
            while (om.find()) {
                optTokens.add(om.group(1).trim());
            }

            String selectorPart = rest.replaceAll("\\[.*?]", "").trim();
            if ("null".equalsIgnoreCase(selectorPart)) selectorPart = null;

            if (selectorPart != null) {
                if (!node.getFieldSelectors().containsKey(field)) {
                    node.getFieldSelectors().put(field, selectorPart);
                }
            }

            RuleNode.Options fo = new RuleNode.Options();
            for (String t : optTokens) {
                parseSingleOptionTokenInto(t, fo);
            }

            if (!node.getFieldOptions().containsKey(field)) {
                node.getFieldOptions().put(field, fo);
            }
        }
        sc.close();
    }

    private static void parseSingleOptionTokenInto(String t, RuleNode.Options opts) {
        if (t == null || t.trim().isEmpty()) return;

        String[] arr = t.split(":", 2);
        if (arr.length == 1) {
            opts.putIfAbsent(arr[0].trim(), true);
        } else {
            opts.putIfAbsent(arr[0].trim(), arr[1].trim());
        }
    }

    public static RuleNode.Options parseOptionsNear(String str, int start) {
        RuleNode.Options opts = new RuleNode.Options();
        Matcher m = Pattern.compile("\\[(.*?)]").matcher(str);
        while (m.find()) {
            parseSingleOptionTokenInto(m.group(1).trim(), opts);
        }
        return opts;
    }

    public static int findMatchingBrace(String text, int openIndex) {
        int level = 0;
        for (int i = openIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') level++;
            else if (c == '}') {
                level--;
                if (level == 0) return i;
            }
        }
        return -1;
    }
}
