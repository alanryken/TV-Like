package tv.tvai.like;

import lombok.Data;

import java.util.HashMap;

@Data
public class Result extends HashMap<String, Object> {


    private static final String VALUE_KEY = "value";

    public Result(Object value) {
        super.put(VALUE_KEY, value);
    }

    @Override
    public Object put(String key, Object value) {
        // 禁止覆盖 value
        if (VALUE_KEY.equals(key) && super.containsKey(VALUE_KEY)) {
            throw new UnsupportedOperationException(
                    "'value' key is read-only and cannot be overwritten"
            );
        }
        return super.put(key, value);
    }

}
