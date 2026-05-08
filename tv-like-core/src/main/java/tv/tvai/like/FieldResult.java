package tv.tvai.like;

import java.util.LinkedHashMap;
import java.util.Map;

public class FieldResult {

    private final String value;
    private final Map<String, Object> meta = new LinkedHashMap<String, Object>();

    public FieldResult(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }
}
