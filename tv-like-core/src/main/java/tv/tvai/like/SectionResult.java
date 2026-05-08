package tv.tvai.like;

import java.util.LinkedHashMap;
import java.util.Map;

public class SectionResult extends FieldsResult {

    private final String section;
    private final Map<String, Object> meta = new LinkedHashMap<String, Object>();
    private ItemsResult items;

    public SectionResult(String section) {
        this.section = section;
    }

    public String getSection() {
        return section;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public ItemsResult getItems() {
        return items;
    }

    public void setItems(ItemsResult items) {
        this.items = items;
    }
}
