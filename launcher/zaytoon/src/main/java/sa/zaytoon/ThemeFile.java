package sa.zaytoon;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;

/**
 * The launcher's theme overlay (config.json in the app's external files folder, the file tools/theme.sh pushes).
 * Zaytoon ships its theme inside the APK and writes it there on first start, so the olive and gold look is there
 * from the first launch with nothing to push. The settings panel changes keys in the same file.
 */
public final class ThemeFile {
    static final String NAME = "config.json";
    static final String ASSET = "zaytoon/config.json";
    /** Raise when the bundled theme gains keys: an older file gets the new keys, and keeps the ones it has. */
    static final int VERSION = 1;
    private static final String VERSION_KEY = "bundled_theme_version";

    private ThemeFile() {
    }

    static File file(Context c) {
        File dir = c.getExternalFilesDir(null);
        return dir == null ? null : new File(dir, NAME);
    }

    static JSONObject bundled(Context c) {
        try (InputStream in = c.getAssets().open(ASSET)) {
            return new JSONObject(new String(readAll(in), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return out.toByteArray();
    }

    /** Puts the bundled theme in place when there is none, or adds keys a newer APK brought. Never throws. */
    public static void ensure(Context c) {
        try {
            File f = file(c);
            if (f == null) return;
            SharedPreferences p = Store.prefs(c);
            int seen = p.getInt(VERSION_KEY, 0);
            if (f.isFile() && seen >= VERSION) return;
            JSONObject theme = f.isFile() ? read(c) : new JSONObject();
            JSONObject b = bundled(c);
            for (Iterator<String> it = b.keys(); it.hasNext(); ) {
                String k = it.next();
                if (!theme.has(k)) theme.put(k, b.get(k));
            }
            write(c, theme);
            p.edit().putInt(VERSION_KEY, VERSION).apply();
        } catch (Exception ignored) {
            // without a theme the launcher still starts, in its stock look
        }
    }

    public static JSONObject read(Context c) {
        File f = file(c);
        if (f == null || !f.isFile()) return new JSONObject();
        try {
            return new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static void write(Context c, JSONObject theme) throws IOException {
        File f = file(c);
        if (f == null) throw new IOException("no external files folder");
        File tmp = new File(f.getPath() + ".tmp");
        try {
            Files.write(tmp.toPath(), theme.toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (org.json.JSONException e) {
            throw new IOException(e);
        }
        if (!tmp.renameTo(f)) throw new IOException("rename failed");
    }

    /** Sets one key (a String, Integer or Boolean) and saves. */
    public static void put(Context c, String key, Object value) {
        try {
            JSONObject t = read(c);
            t.put(key, value);
            write(c, t);
        } catch (Exception ignored) {
            // the panel shows the old value again on its next refresh
        }
    }

    /** The bundled theme again, as on the first start. */
    public static void reset(Context c) {
        try {
            write(c, bundled(c));
        } catch (IOException ignored) {
            // nothing to undo
        }
    }
}
