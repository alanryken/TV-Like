package tv.tvai.like;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public abstract class MetaResult {

    private Map<String, Object> meta = new LinkedHashMap<String, Object>();
}
