package tv.tvai.like;

import android.app.Application;

import eskit.sdk.support.core.EsProxy;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        EsProxy.get().registerModule();
    }
}
