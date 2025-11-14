package tv.tvai.like;

// RuleParser.java
import java.util.*;
import java.util.regex.*;

public class RuleParser {
    private static final Pattern SECTION = Pattern.compile("^section:\\s*([a-zA-Z0-9_-]+)\\s+(.+?)\\s*\\{$");
    private static final Pattern FIELD   = Pattern.compile("^\\s*([a-zA-Z0-9_-]+):\\s*([^\\[\\s]+)\\s*(\\[([^\\]]+)\\])?\\s*$");
    private static final Pattern ITEMS_START = Pattern.compile("^\\s*items:\\s*([^\\{\\s]+)\\s*\\{$");
    private static final Pattern BLOCK_END   = Pattern.compile("^\\s*\\}\\s*(\\[([^\\]]+)\\])?\\s*$");
    private static final Pattern OPT         = Pattern.compile("([a-zA-Z-]+):\\s*([^,\\]]+)");

    public List<RuleNode> parse(String rulesText) {
        rulesText = rulesText.replace("&#10;", "\n").replace("&#13;", "\r\n").replace("\r\n", "\n");
        String[] lines = rulesText.split("\n");
        List<RuleNode> sections = new ArrayList<>();

        String curName = null, curSel = null;
        List<String> blockLines = new ArrayList<>();
        int brace = 0;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("page-type:") || line.startsWith("globals")) continue;

            Matcher sec = SECTION.matcher(line);
            if (sec.find()) {
                if (brace > 0) process(blockLines, curName, curSel, sections);
                curName = sec.group(1).trim();
                curSel  = sec.group(2).trim();
                blockLines.clear();
                brace = 1;
                continue;
            }

            if (brace > 0) {
                blockLines.add(raw);  // 保留原始缩进行
                brace += countBraces(raw);
                if (brace == 0) {
                    process(blockLines, curName, curSel, sections);
                    blockLines.clear();
                }
            }
        }
        if (brace > 0) process(blockLines, curName, curSel, sections);
        return sections;
    }

    private int countBraces(String s) {
        int c = 0;
        for (char ch : s.toCharArray()) { if (ch == '{') c++; else if (ch == '}') c--; }
        return c;
    }

    private void process(List<String> lines, String name, String selector, List<RuleNode> out) {
        if (lines.isEmpty() || name == null) return;

        Map<String, String> fSel = new HashMap<>();
        Map<String, RuleNode.Options> fOpt = new HashMap<>();
        RuleNode itemT = null;
        RuleNode.Options itemSecOpt = new RuleNode.Options();

        List<String> itemLines = null;
        int i = 0;
        while (i < lines.size()) {
            String raw = lines.get(i);
            String trimmed = raw.trim();

            // 检测 items: 开头
            Matcher itemsStart = ITEMS_START.matcher(trimmed);
            if (itemsStart.find()) {
                String itemSel = itemsStart.group(1).trim();
                itemLines = new ArrayList<>();
                i++;
                int subBrace = 1;
                while (i < lines.size() && subBrace > 0) {
                    String subRaw = lines.get(i);
                    String subTrim = subRaw.trim();
                    itemLines.add(subRaw);
                    subBrace += countBraces(subRaw);
                    if (BLOCK_END.matcher(subTrim).find()) {
                        // 提取 [limit: 6]
                        Matcher end = BLOCK_END.matcher(subTrim);
                        if (end.find() && end.group(2) != null) {
                            itemSecOpt = parseOpt(end.group(2));
                        }
                        break;
                    }
                    i++;
                }
                itemT = buildItem(itemSel, itemLines);
                continue; // 跳过已处理的 items 行
            }

            // 普通字段
            Matcher field = FIELD.matcher(trimmed);
            if (field.matches()) {
                String k = field.group(1);
                String s = field.group(2).trim();
                String o = field.group(4);
                fSel.put(k, s);
                fOpt.put(k, parseOpt(o));
            }
            i++;
        }

        RuleNode node = itemT != null
                ? RuleNode.sectionWithItems(name, selector, fSel, fOpt,
                itemT.getSelector(), itemT.getFieldSelectors(), itemT.getFieldOptions(), itemSecOpt)
                : RuleNode.section(name, selector, fSel, fOpt, new RuleNode.Options());
        out.add(node);
    }

    private RuleNode buildItem(String sel, List<String> lines) {
        Map<String, String> iSel = new HashMap<>();
        Map<String, RuleNode.Options> iOpt = new HashMap<>();
        int brace = 0;
        for (String raw : lines) {
            String trim = raw.trim();
            brace += countBraces(raw);
            if (brace <= 0) break; // 跳过结束 }
            if (trim.isEmpty() || trim.startsWith("}")) continue;
            Matcher m = FIELD.matcher(trim);
            if (m.matches()) {
                iSel.put(m.group(1), m.group(2).trim());
                iOpt.put(m.group(1), parseOpt(m.group(4)));
            }
        }
        return RuleNode.section("item", sel, iSel, iOpt, new RuleNode.Options());
    }

    private RuleNode.Options parseOpt(String s) {
        RuleNode.Options o = new RuleNode.Options();
        if (s == null) return o;
        Matcher m = OPT.matcher(s);
        while (m.find()) {
            String k = m.group(1).trim();
            String v = m.group(2).trim();
            if ("limit".equals(k) || "index".equals(k)) o.put(k, Integer.valueOf(v));
            else if ("transform".equals(k)) o.put(k, Arrays.asList(v.split("\\|")));
            else o.put(k, v);
        }
        return o;
    }
}