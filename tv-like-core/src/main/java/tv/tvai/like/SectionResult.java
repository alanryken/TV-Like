package tv.tvai.like;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SectionResult extends FieldsResult {

    private String section;
    private ItemsResult items;

    public SectionResult(String section) {
        this.section = section;
    }
}
