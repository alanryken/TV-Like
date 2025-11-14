package tv.tvai.like;


import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class RuleParser {
    private static final Pattern SECTION_PATTERN = Pattern.compile("^section:\\s*([a-zA-Z0-9_-]+)\\s+([^ { ]+)\\s*\\{$");
    private static final Pattern FIELD_PATTERN = Pattern.compile("^([a-zA-Z0-9_-]+):\\s*([^\\[ ]+)\\s*(\\[([^\\]]+)\\])?\\s*$");
    private static final Pattern ITEMS_PATTERN = Pattern.compile("^items:\\s*([^ { ]+)\\s*\\{$");
    private static final Pattern GLOBALS_PATTERN = Pattern.compile("^globals\\s*\\{");
    private static final Pattern OPTION_PATTERN = Pattern.compile("([a-zA-Z-]+):\\s*([^,\\]]+)");
    private static final Pattern GLOBAL_OPTION = Pattern.compile("([a-zA-Z-]+):\\s*([^,\\]]+)");

    public List<RuleNode> parse(String rulesText) {
        rulesText = rulesText.replace("&#10;", "\n").replace("&#13;", "\r\n").replace("\r\n", "\n");

        String[] lines = rulesText.split("\n");
        List<RuleNode> sections = new ArrayList<>();
        Map<String, Object> globals = new HashMap<>();
        int braceLevel = 0;
        String currentName = null;
        String currentSelector = null;
        List<String> currentBlock = new ArrayList<>();
        boolean inItems = false;
        boolean inGlobals = false;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            int braces = countBraces(line);
            braceLevel += braces;

            if (line.startsWith("page-type: ")) {
                continue;
            }

            if (GLOBALS_PATTERN.matcher(line).find()) {
                inGlobals = true;
                braceLevel = 1;
                continue;
            }

            if (inGlobals) {
                if (braceLevel == 0) {
                    inGlobals = false;
                    continue;
                }
                Matcher globalMatcher = GLOBAL_OPTION.matcher(line);
                if (globalMatcher.find()) {
                    String key = globalMatcher.group(1).trim();
                    String value = globalMatcher.group(2).trim();
                    globals.put(key, value);
                }
                continue;
            }

            Matcher sectionMatcher = SECTION_PATTERN.matcher(line);
            if (sectionMatcher.find()) {
                if (currentBlock.size() > 0) {
                    processBlock(currentBlock, currentName, currentSelector, sections);
                }
                currentName = sectionMatcher.group(1).trim();
                currentSelector = sectionMatcher.group(2).trim();
                currentBlock = new ArrayList<>();
                inItems = false;
                braceLevel = 1;
                continue;
            }

            if (braceLevel > 0) {
                if (line.equals("}")) {
                    braceLevel = 0;
                    if (currentBlock.size() > 0) {
                        processBlock(currentBlock, currentName, currentSelector, sections);
                        currentName = null;
                        currentSelector = null;
                        currentBlock = new ArrayList<>();
                    }
                    continue;
                }

                Matcher itemsMatcher = ITEMS_PATTERN.matcher(line);
                if (itemsMatcher.find()) {
                    inItems = true;
                    currentBlock.add(line);
                    continue;
                }

                Matcher fieldMatcher = FIELD_PATTERN.matcher(line);
                if (fieldMatcher.matches()) {
                    String key = fieldMatcher.group(1);
                    String sel = fieldMatcher.group(2).trim();
                    String optStr = fieldMatcher.group(4);
                    currentBlock.add(key + ": " + sel + (optStr != null ? " [" + optStr + "]" : ""));
                    continue;
                }

                currentBlock.add(line);
            }
        }

        return sections;
    }

    private int countBraces(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == '{') count++;
            if (c == '}') count--;
        }
        return count;
    }

    private void processBlock(List<String> block, String name, String selector, List<RuleNode> sections) {
        Map<String, String> fieldSelectors = new HashMap<>();
        Map<String, RuleNode.Options> fieldOptions = new HashMap<>();
        RuleNode itemT = null;

        for (String blockLine : block) {
            Matcher fieldMatcher = FIELD_PATTERN.matcher(blockLine);
            if (fieldMatcher.matches()) {
                String key = fieldMatcher.group(1);
                String sel = fieldMatcher.group(2).trim();
                String optStr = fieldMatcher.group(4);
                RuleNode.Options opts = parseOptions(optStr);
                if ("items".equals(key)) {
                    itemT = buildItemTemplate(sel, block.subList(block.indexOf(blockLine) + 1, block.size()));
                } else {
                    fieldSelectors.put(key, sel);
                    fieldOptions.put(key, opts);
                }
            }
        }

        RuleNode.Options sectionOpts = new RuleNode.Options();
        RuleNode node;
        if (itemT != null) {
            Map<String, String> itemSels = itemT.getFieldSelectors();
            Map<String, RuleNode.Options> itemOpts = itemT.getFieldOptions();
            node = RuleNode.sectionWithItems(name, selector, fieldSelectors, fieldOptions, itemT.getSelector(), itemSels, itemOpts, sectionOpts);
        } else {
            node = RuleNode.section(name, selector, fieldSelectors, fieldOptions, sectionOpts);
        }
        sections.add(node);
    }

    private RuleNode buildItemTemplate(String itemSel, List<String> subBlock) {
        Map<String, String> itemSelectors = new HashMap<>();
        Map<String, RuleNode.Options> itemFieldOptions = new HashMap<>();
        RuleNode.Options itemSectionOpts = new RuleNode.Options();

        for (String subLine : subBlock) {
            Matcher fieldMatcher = FIELD_PATTERN.matcher(subLine);
            if (fieldMatcher.matches()) {
                String key = fieldMatcher.group(1);
                String sel = fieldMatcher.group(2).trim();
                String optStr = fieldMatcher.group(4);
                RuleNode.Options opts = parseOptions(optStr);
                itemSelectors.put(key, sel);
                itemFieldOptions.put(key, opts);
            }
        }

        return RuleNode.section("item", itemSel, itemSelectors, itemFieldOptions, itemSectionOpts);
    }

    private RuleNode.Options parseOptions(String optStr) {
        RuleNode.Options opts = new RuleNode.Options();
        if (optStr == null) return opts;
        Matcher optMatcher = OPTION_PATTERN.matcher(optStr);
        while (optMatcher.find()) {
            String key = optMatcher.group(1).trim();
            String value = optMatcher.group(2).trim();
            if ("limit".equals(key) || "index".equals(key)) {
                opts.put(key, Integer.valueOf(value));
            } else if ("transform".equals(key)) {
                opts.put(key, Arrays.asList(value.split("\\|")));
            } else {
                opts.put(key, value);
            }
        }
        return opts;
    }
}