package tv.tvai.like;

import android.content.Context;


import java.util.List;

import eskit.sdk.support.EsPromise;
import eskit.sdk.support.args.EsArray;
import eskit.sdk.support.args.EsMap;
import eskit.sdk.support.module.IEsModule;

public class TVModule implements IEsModule {

    @Override
    public void init(Context context) {

    }

    @Override
    public void destroy() {

    }

    public void like(EsMap map, EsPromise promise) {
        String html = map.getString("html");
        String url = map.getString("url");
        String dslHub = map.getString("dslHub");
        TV tv = new TV(html, url, dslHub);
        List<SectionResult> like = tv.like();
        EsArray array = getArray(like);
        promise.resolve(array);
    }

    private EsArray getArray(List<SectionResult> like) {
        EsArray array = new EsArray();
        for (SectionResult section : like) {
            array.pushObject(section);
        }
        return array;
    }
}
