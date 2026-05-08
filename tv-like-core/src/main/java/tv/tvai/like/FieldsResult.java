package tv.tvai.like;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class FieldsResult extends MetaResult {

    private FieldResult text;
    private FieldResult img;
    private FieldResult link;

    public boolean hasFieldContent() {
        return text != null || img != null || link != null;
    }
}
