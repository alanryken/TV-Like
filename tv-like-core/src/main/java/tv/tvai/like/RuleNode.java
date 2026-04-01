package tv.tvai.like;

import java.util.HashMap;
import java.util.Map;

public class RuleNode {
    private String name;
    private String selector;
    private Map<String, String> fieldSelectors = new HashMap<String, String>();
    private Map<String, Options> fieldOptions = new HashMap<String, Options>();
    private RuleNode itemTemplate;
    private Options sectionOptions;

    public RuleNode() {}

    public RuleNode(String name, String selector) {
        this.name = name;
        this.selector = selector;
    }

    public static class Options {
        private Map<String, Object> values = new HashMap<String, Object>();

        public void putIfAbsent(String key, Object value) {
            if (key != null && value != null && !values.containsKey(key)) {
                values.put(key, value);
            }
        }

        public Map<String, Object> getValues() {
            return values;
        }
    }

    public void setName(String name) {
        if (this.name == null) this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setSelector(String selector) {
        if (selector != null && this.selector == null) {
            this.selector = selector;
        }
    }

    public String getSelector() {
        return selector;
    }

    public Map<String, String> getFieldSelectors() {
        return fieldSelectors;
    }

    public Map<String, Options> getFieldOptions() {
        return fieldOptions;
    }

    public void setItemTemplate(RuleNode t) {
        if (this.itemTemplate == null) {
            this.itemTemplate = t;
        }
    }

    public RuleNode getItemTemplate() {
        return itemTemplate;
    }

    public void setSectionOptions(Options options) {
        if (this.sectionOptions == null) {
            this.sectionOptions = options;
        }
    }

    public Options getSectionOptions() {
        return sectionOptions;
    }
}
