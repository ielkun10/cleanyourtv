package sa.zaytoon;

import android.app.Application;

/** Puts the bundled Zaytoon theme in place before the first activity reads its settings. */
public final class ZaytoonApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeFile.ensure(this);
    }
}
