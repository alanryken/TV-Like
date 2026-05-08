package tv.tvai.like;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ItemsResult extends MetaResult {

    private List<ItemResult> list = new ArrayList<ItemResult>();
}
