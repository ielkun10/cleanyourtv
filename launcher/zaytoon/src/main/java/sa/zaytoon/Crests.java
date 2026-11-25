package sa.zaytoon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** Club crests from ESPN's image server, fetched once, kept on the TV and shown small. */
final class Crests {
    private static final Map<String, Bitmap> MEMORY = new HashMap<>();

    private Crests() {
    }

    static void show(Context c, String teamId, ImageView view) {
        if (teamId == null || teamId.isEmpty()) return;
        Bitmap b;
        synchronized (MEMORY) {
            b = MEMORY.get(teamId);
        }
        if (b != null) {
            view.setImageBitmap(b);
            return;
        }
        view.setTag(teamId);
        Context app = c.getApplicationContext();
        Hub.get(app).background("crest" + teamId, () -> {
            File f = new File(Store.cacheDir(app), "crest_" + teamId + ".png");
            try {
                if (!f.isFile()) Net.file("https://a.espncdn.com/i/teamlogos/soccer/500/" + teamId + ".png", f, 1024 * 1024);
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inSampleSize = 4;   // 500 px to 125 px: plenty for a 40 dp crest
                Bitmap bmp = BitmapFactory.decodeFile(f.getPath(), o);
                if (bmp == null) return;
                synchronized (MEMORY) {
                    MEMORY.put(teamId, bmp);
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (teamId.equals(view.getTag())) view.setImageBitmap(bmp);
                });
            } catch (Exception ignored) {
                // no crest, the name still shows
            }
        });
    }
}
