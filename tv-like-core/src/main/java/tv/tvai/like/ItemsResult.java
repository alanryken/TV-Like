package tv.tvai.like;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ItemsResult {

    private final Map<String, Object> meta = new LinkedHashMap<String, Object>();
    private final List<ItemResult> list = new ArrayList<ItemResult>();

    public Map<String, Object> getMeta() {
        return meta;
    }

    public List<ItemResult> getList() {
        return list;
    }
}
