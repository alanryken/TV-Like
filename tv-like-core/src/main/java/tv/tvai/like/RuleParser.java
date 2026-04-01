package tv.tvai.like;

import tv.tvai.like.util.AntPathMatcher;
import tv.tvai.like.util.PathMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleParser {

    private static final Set<String> ALLOWED_FIELDS = new HashSet<>(Arrays.asList("text", "img", "link"));
    private final PathMatcher pathMatcher = new AntPathMatcher();
    private Map<String, List<RuleNode>> pathRuleMap;

    private boolean matchPath(String pattern, String path) {
        return pathMatcher.match(pattern, path);
    }

    public List<RuleNode> getPathRule(String path) {
        for (Map.Entry<String, List<RuleNode>> entry : pathRuleMap.entrySet()) {
            String pattern = entry.getKey();
            if (matchPath(pattern, path)) {
                return entry.getValue();
            }
        }
        return new ArrayList<>();
    }

    public void parse(String dsl) {
        pathRuleMap = new LinkedHashMap<>();

        Pattern pathPattern = Pattern.compile("path\\s*:\\s*([^\\{]+)\\{", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pathPattern.matcher(dsl);

        while (matcher.find()) {
            String pathPatternStr = matcher.group(1).trim();
            int blockStart = matcher.end() - 1;
            int blockEnd = findMatchingBrace(dsl, blockStart);
            if (blockEnd < 0) continue;

            String body = dsl.substring(blockStart + 1, blockEnd);
            List<RuleNode> nodes = parseSectionDsl(body);
            if (!nodes.isEmpty()) {
                pathRuleMap.put(pathPatternStr, nodes);
            }
        }

        if (pathRuleMap.isEmpty()) {
            List<RuleNode> nodes = parseSectionDsl(dsl);
            pathRuleMap.put("/**", nodes);
        }
    }

    private List<RuleNode> parseSectionDsl(String dsl) {
        List<RuleNode> sections = new ArrayList<>();
        if (dsl == null || dsl.trim().isEmpty()) {
            return sections;
        }

        Pattern sectionPattern = Pattern.compile("section\\s*:\\s*([\\w-]+)\\s+([^\\{]+)\\{", Pattern.CASE_INSENSITIVE);

        Matcher matcher = sectionPattern.matcher(dsl);
        while (matcher.find()) {
            String sectionName = matcher.group(1).trim();
            String selector = matcher.group(2).trim();
            int bodyStart = matcher.end() - 1;
            int bodyEnd = findMatchingBrace(dsl, bodyStart);
            if (bodyEnd < 0) continue;

            String body = dsl.substring(bodyStart + 1, bodyEnd);
            String afterBlock = bodyEnd + 1 < dsl.length() ? extractTrailingOptions(dsl, bodyEnd + 1) : "";

            RuleNode.Options sectionOptions = extractOptionsFromText(afterBlock);

            RuleNode node = new RuleNode(sectionName, selector);
            node.setSectionOptions(sectionOptions);
            parseSectionBody(body, node);
            sections.add(node);
        }
        return sections;
    }

    private static void parseSectionBody(String body, RuleNode node) {
        if (body == null) return;

        String remaining = body;
        remaining = parseItemsBlockIfPresent(remaining, node);
        parseFieldLines(remaining, node.getFieldSelectors(), node.getFieldOptions());
    }

    private static String parseItemsBlockIfPresent(String text, RuleNode parentNode) {
        Pattern itemsPattern = Pattern.compile("items\\s*:\\s*([^\\{\\n]+)\\{", Pattern.CASE_INSENSITIVE);
        Matcher matcher = itemsPattern.matcher(text);

        if (!matcher.find()) {
            return text;
        }

        String itemsSelector = matcher.group(1).trim();
        int blockStart = matcher.end() - 1;
        int blockEnd = findMatchingBrace(text, blockStart);
        if (blockEnd < 0) {
            return text;
        }

        String innerBody = text.substring(blockStart + 1, blockEnd);
        String afterBlock = blockEnd + 1 < text.length() ? extractTrailingOptions(text, blockEnd + 1) : "";

        RuleNode.Options itemOptions = extractOptionsFromText(afterBlock);

        RuleNode itemTemplate = new RuleNode();
        itemTemplate.setName("items");
        itemTemplate.setSelector(itemsSelector);
        itemTemplate.setSectionOptions(itemOptions);

        parseItemFields(innerBody, itemTemplate);
        parentNode.setItemTemplate(itemTemplate);

        String before = text.substring(0, matcher.start());
        return before + "\n" + afterBlock;
    }

    private static void parseItemFields(String inner, RuleNode node) {
        parseFieldLines(inner, node.getFieldSelectors(), node.getFieldOptions());
    }

    private static String extractTrailingOptions(String text, int start) {
        int i = start;

        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }

        int optionsStart = i;
        int bracketDepth = 0;
        boolean inOption = false;

        while (i < text.length()) {
            char c = text.charAt(i);

            if (c == '[') {
                bracketDepth++;
                inOption = true;
            } else if (c == ']') {
                bracketDepth--;
                if (bracketDepth == 0) {
                    inOption = false;
                }
            } else {
                if (!inOption && !Character.isWhitespace(c)) {
                    break;
                }
            }

            i++;
        }

        return text.substring(optionsStart, i).trim();
    }

    private static void parseFieldLines(String text,
                                        Map<String, String> selectorMap,
                                        Map<String, RuleNode.Options> optionsMap) {

        if (text == null) return;

        Scanner scanner = new Scanner(text);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            Matcher fieldMatcher = Pattern.compile(
                    "^(text|img|link)\\s*:\\s*(.+)$",
                    Pattern.CASE_INSENSITIVE).matcher(line);

            if (!fieldMatcher.find()) continue;

            String fieldName = fieldMatcher.group(1).toLowerCase();
            if (!ALLOWED_FIELDS.contains(fieldName)) continue;

            String rest = fieldMatcher.group(2).trim();
            List<String> optionTokens = extractAllOptionTokens(rest);

            String selector = rest.replaceAll("\\[.*?]", "").trim();
            if ("null".equalsIgnoreCase(selector)) {
                selector = null;
            }

            if (selector != null && !selectorMap.containsKey(fieldName)) {
                selectorMap.put(fieldName, selector);
            }

            RuleNode.Options fieldOptions = new RuleNode.Options();
            for (String token : optionTokens) {
                parseOptionToken(token, fieldOptions);
            }
            if (!optionsMap.containsKey(fieldName)) {
                optionsMap.put(fieldName, fieldOptions);
            }
        }
        scanner.close();
    }

    private static List<String> extractAllOptionTokens(String text) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\[(.*?)]").matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group(1).trim());
        }
        return tokens;
    }

    private static RuleNode.Options extractOptionsFromText(String text) {
        RuleNode.Options opts = new RuleNode.Options();
        Matcher matcher = Pattern.compile("\\[(.*?)]").matcher(text);
        while (matcher.find()) {
            parseOptionToken(matcher.group(1).trim(), opts);
        }
        return opts;
    }

    private static void parseOptionToken(String token, RuleNode.Options options) {
        if (token == null || token.isEmpty()) return;

        String[] parts = token.split(",");
        for (String part : parts) {
            String[] kv = part.split(":", 2);
            if (kv.length == 1) {
                options.putIfAbsent(kv[0].trim(), true);
            } else {
                options.putIfAbsent(kv[0].trim(), kv[1].trim());
            }
        }
    }

    private static int findMatchingBrace(String text, int openIndex) {
        int level = 0;
        for (int i = openIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                level++;
            } else if (c == '}') {
                level--;
                if (level == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
