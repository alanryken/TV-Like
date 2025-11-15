package tv.tvai.like.grok;
import java.util.*;

public class RuleNode {
    private final String name;
    private final String selector;
    private final Map<String, String> fieldSelectors;
    private final Map<String, RuleNode.Options> fieldOptions;
    private final RuleNode itemTemplate;
    private final Options sectionOptions;

    public RuleNode(String name, String selector, Map<String, String> fieldSelectors,
                    Map<String, RuleNode.Options> fieldOptions, RuleNode itemTemplate, Options sectionOptions) {
        this.name = name;
        this.selector = selector;
        this.fieldSelectors = fieldSelectors != null ? new HashMap<>(fieldSelectors) : new HashMap<>();
        this.fieldOptions = fieldOptions != null ? new HashMap<>(fieldOptions) : new HashMap<>();
        this.itemTemplate = itemTemplate;
        this.sectionOptions = sectionOptions != null ? sectionOptions : new Options();
    }

    public static RuleNode section(String name, String selector, Map<String, String> fieldSelectors,
                                   Map<String, RuleNode.Options> fieldOptions, Options sectionOptions) {
        return new RuleNode(name, selector, fieldSelectors, fieldOptions, null, sectionOptions);
    }

    public static RuleNode sectionWithItems(String name, String selector, Map<String, String> fieldSelectors,
                                            Map<String, RuleNode.Options> fieldOptions,
                                            String itemSel, Map<String, String> itemFieldSelectors,
                                            Map<String, RuleNode.Options> itemFieldOptions, Options sectionOptions) {
        RuleNode item = new RuleNode("item", itemSel, itemFieldSelectors, itemFieldOptions, null, new Options());
        return new RuleNode(name, selector, fieldSelectors, fieldOptions, item, sectionOptions);
    }

    public String getName() { return name; }
    public String getSelector() { return selector; }
    public Map<String, String> getFieldSelectors() { return new HashMap<>(fieldSelectors); }
    public Map<String, RuleNode.Options> getFieldOptions() { return new HashMap<>(fieldOptions); }
    public RuleNode getItemTemplate() { return itemTemplate; }
    public Options getSectionOptions() { return sectionOptions; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(": ").append(selector).append(" {");
        for (Map.Entry<String, String> e : fieldSelectors.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append(", ");
        }
        if (itemTemplate != null) {
            sb.append("items: ").append(itemTemplate.toString());
        }
        sb.append("}");
        return sb.toString();
    }

    public static class Options {
        private final Map<String, Object> values;

        public Options() { this.values = new HashMap<>(); }

        public Options put(String key, Object value) {
            values.put(key, value);
            return this;
        }

        public String getString(String key) {
            Object v = values.get(key);
            return v != null ? v.toString() : null;
        }

        public Integer getInt(String key) {
            Object v = values.get(key);
            try {
                return v != null ? Integer.parseInt(v.toString()) : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        public List<String> getList(String key) {
            Object v = values.get(key);
            if (v instanceof List) return (List<String>) v;
            if (v != null) return Arrays.asList(v.toString().split(",\\s*"));
            return new ArrayList<>();
        }
    }
}