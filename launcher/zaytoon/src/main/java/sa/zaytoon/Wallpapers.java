package sa.zaytoon;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Zaytoon's 4K wallpapers: freely licensed photos from Wikimedia Commons, listed with their photographer and licence
 * in assets/zaytoon/wallpapers.json and downloaded once into the theme's wallpapers/ folder, where the launcher's own
 * rotation shows them (memory-safe: it decodes each picture down to the screen's size). The photos are not in the APK.
 */
public final class Wallpapers {
    public static final class Item {
        public String set, title, artist, license, page, url;

        String file(int index) {
            return set + "-" + (index < 10 ? "0" : "") + index + ".jpg";
        }
    }

    public static final class Set_ {
        public String id, name;
    }

    public static final String USER_FOLDER = "Zaytoon";
    private static final long MAX_BYTES = 25L * 1024 * 1024;

    public volatile String status = "";
    public volatile boolean busy;

    private static Wallpapers instance;

    public static synchronized Wallpapers get() {
        if (instance == null) instance = new Wallpapers();
        return instance;
    }

    public static List<Set_> sets(Context c) {
        List<Set_> out = new ArrayList<>();
        JSONObject m = manifest(c);
        JSONArray a = m.optJSONArray("sets");
        if (a == null) return out;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            Set_ s = new Set_();
            s.id = o.optString("id");
            s.name = o.optString("name");
            out.add(s);
        }
        return out;
    }

    public static List<Item> items(Context c) {
        List<Item> out = new ArrayList<>();
        JSONArray a = manifest(c).optJSONArray("items");
        if (a == null) return out;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            Item it = new Item();
            it.set = o.optString("set");
            it.title = o.optString("title");
            it.artist = o.optString("artist");
            it.license = o.optString("license");
            it.page = o.optString("page");
            it.url = o.optString("url");
            out.add(it);
        }
        return out;
    }

    static JSONObject manifest(Context c) {
        try (InputStream in = c.getAssets().open("zaytoon/wallpapers.json")) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    /** The sets the owner picked; every set by default. */
    public static Set<String> chosen(Context c) {
        String v = Store.prefs(c).getString(Store.WALLPAPER_SETS, null);
        Set<String> out = new HashSet<>();
        if (v == null) {
            for (Set_ s : sets(c)) out.add(s.id);
        } else if (!v.isEmpty()) {
            out.addAll(Arrays.asList(v.split(",")));
        }
        return out;
    }

    public static void choose(Context c, Set<String> sets) {
        Store.prefs(c).edit().putString(Store.WALLPAPER_SETS, String.join(",", sets)).apply();
    }

    static File folder(Context c) {
        File dir = c.getExternalFilesDir(null);
        if (dir == null) return null;
        File f = new File(dir, "wallpapers");
        //noinspection ResultOfMethodCallIgnored
        f.mkdirs();
        return f;
    }

    /** Pictures in the folder now (downloaded and imported). */
    public static int count(Context c) {
        File f = folder(c);
        String[] names = f == null ? null : f.list((d, n) -> n.endsWith(".jpg") || n.endsWith(".png"));
        return names == null ? 0 : names.length;
    }

    /**
     * Brings the folder in line with the chosen sets: downloads what is missing, deletes the pictures of sets that
     * were switched off (they come back with one press), keeps the owner's own pictures. Runs on Zaytoon's thread.
     */
    public void sync(Context c, Runnable whenFirstArrives) {
        Context app = c.getApplicationContext();
        Hub.get(app).background("wallpapers", () -> {
            busy = true;
            try {
                doSync(app, whenFirstArrives);
            } finally {
                busy = false;
                Hub.get(app).notifyChanged();
            }
        });
    }

    private void doSync(Context c, Runnable whenFirstArrives) {
        File dir = folder(c);
        if (dir == null) {
            status = "ذاكرة التلفزيون غير متاحة الآن";
            return;
        }
        boolean hadAny = count(c) > 0;
        Set<String> chosen = chosen(c);
        List<Item> all = items(c);
        Map<String, Integer> perSet = new LinkedHashMap<>();
        Map<String, String> captions = readCaptions(dir);
        Set<String> wanted = new HashSet<>();
        int failed = 0, done = 0, total = 0;
        for (Item it : all) {
            int index = perSet.merge(it.set, 1, Integer::sum);
            String name = it.file(index);
            if (!chosen.contains(it.set)) continue;
            wanted.add(name);
            total++;
            captions.put(name, it.title + " · " + it.artist + " · " + it.license);
            File target = new File(dir, name);
            if (target.isFile() && target.length() > 0) {
                done++;
                continue;
            }
            status = "تنزيل الخلفيات: " + Ar.num(done) + " من " + Ar.num(total);
            Hub.get(c).notifyChanged();
            try {
                Net.file(it.url, target, MAX_BYTES);
                if (!isPicture(target)) {
                    //noinspection ResultOfMethodCallIgnored
                    target.delete();
                    failed++;
                    continue;
                }
                done++;
                if (!hadAny && whenFirstArrives != null) {
                    hadAny = true;
                    writeCaptions(dir, captions);
                    whenFirstArrives.run();
                }
            } catch (IOException e) {
                failed++;
            }
        }
        String[] present = dir.list();
        if (present != null) {
            for (String n : present) {
                boolean ours = n.matches("[a-z]+-\\d\\d\\.jpg");
                if (ours && !wanted.contains(n)) {
                    //noinspection ResultOfMethodCallIgnored
                    new File(dir, n).delete();
                    captions.remove(n);
                }
            }
        }
        writeCaptions(dir, captions);
        status = failed == 0 ? "الخلفيات جاهزة: " + Ar.num(count(c)) + " صورة"
                : "اكتمل " + Ar.num(done) + " وتعذّر " + Ar.num(failed) + "، يُعاد المحاولة لاحقًا";
    }

    /** A picture the launcher can show: decodes, landscape, at least Full HD. */
    static boolean isPicture(File f) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), o);
        return o.outWidth >= 1920 && o.outHeight >= 1000 && o.outWidth > o.outHeight;
    }

    static Map<String, String> readCaptions(File dir) {
        Map<String, String> m = new LinkedHashMap<>();
        File f = new File(dir, "captions.txt");
        if (!f.isFile()) return m;
        try {
            for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                int tab = line.indexOf('\t');
                if (tab > 0) m.put(line.substring(0, tab), line.substring(tab + 1));
            }
        } catch (IOException ignored) {
            // start a new file
        }
        return m;
    }

    static void writeCaptions(File dir, Map<String, String> captions) {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, String> e : captions.entrySet()) {
            if (new File(dir, e.getKey()).isFile()) b.append(e.getKey()).append('\t').append(e.getValue()).append('\n');
        }
        try {
            Files.write(new File(dir, "captions.txt").toPath(), b.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // captions are optional
        }
    }

    /**
     * Copies the owner's own pictures from any folder named Zaytoon (on the TV or a USB stick) into the rotation.
     * Needs the photos permission, which the panel asks for. Returns how many were added.
     */
    public static int importUser(Context c) {
        File dir = folder(c);
        if (dir == null) return 0;
        ContentResolver cr = c.getContentResolver();
        int added = 0;
        for (String volume : MediaStore.getExternalVolumeNames(c)) {
            Uri uri = MediaStore.Images.Media.getContentUri(volume);
            String[] cols = {MediaStore.Images.Media._ID, MediaStore.Images.Media.MIME_TYPE};
            String where = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + "=?";
            try (Cursor cur = cr.query(uri, cols, where, new String[] {USER_FOLDER}, null)) {
                if (cur == null) continue;
                while (cur.moveToNext() && added < 200) {
                    long id = cur.getLong(0);
                    String mime = cur.getString(1);
                    if (!"image/jpeg".equals(mime) && !"image/png".equals(mime)) continue;
                    File target = new File(dir, "user-" + volume.replaceAll("[^a-z0-9]", "") + "-" + id
                            + ("image/png".equals(mime) ? ".png" : ".jpg"));
                    if (target.isFile()) continue;
                    try (InputStream in = cr.openInputStream(ContentUris.withAppendedId(uri, id));
                         OutputStream out = new FileOutputStream(target)) {
                        if (in == null) continue;
                        byte[] buf = new byte[65536];
                        long total = 0;
                        int n;
                        while ((n = in.read(buf)) > 0) {
                            total += n;
                            if (total > MAX_BYTES * 2) throw new IOException("too large");
                            out.write(buf, 0, n);
                        }
                    } catch (IOException e) {
                        //noinspection ResultOfMethodCallIgnored
                        target.delete();
                        continue;
                    }
                    if (isPicture(target)) added++;
                    else //noinspection ResultOfMethodCallIgnored
                        target.delete();
                }
            } catch (RuntimeException ignored) {
                // a volume that went away mid-scan
            }
        }
        return added;
    }

    /** Removes the owner's imported pictures (the originals stay where they were). */
    public static int removeUser(Context c) {
        File dir = folder(c);
        String[] names = dir == null ? null : dir.list((d, n) -> n.startsWith("user-"));
        if (names == null) return 0;
        for (String n : names) //noinspection ResultOfMethodCallIgnored
            new File(dir, n).delete();
        return names.length;
    }
}
