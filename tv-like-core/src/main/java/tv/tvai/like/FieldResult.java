package tv.tvai.like;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FieldResult extends MetaResult {

    private String value;

    public FieldResult(String value) {
        this.value = value;
    }
}
