package sa.zaytoon;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Zaytoon's own choices (city, prayer method, clubs, clock) and its cache of downloaded feeds. */
public final class Store {
    static final String PREFS = "zaytoon";
    public static final String CITY = "city";
    public static final String METHOD = "prayer_method";
    public static final String HANAFI = "asr_hanafi";
    public static final String TEAMS = "teams";
    public static final String WEATHER_MINUTES = "weather_minutes";
    public static final String CLOCK_SIZE = "clock_size";
    public static final String CLOCK_FONT = "clock_font";
    public static final String STRIP = "show_strip";
    public static final String WALLPAPER_SETS = "wallpaper_sets";

    public static final int[] CLOCK_SIZES_SP = {64, 88, 112};

    private Store() {
    }

    public static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static Places.City city(Context c) {
        return Places.city(prefs(c).getString(CITY, "khamis"));
    }

    public static PrayerTimes.Method method(Context c) {
        try {
            return PrayerTimes.Method.valueOf(prefs(c).getString(METHOD, PrayerTimes.Method.UMM_AL_QURA.name()));
        } catch (IllegalArgumentException e) {
            return PrayerTimes.Method.UMM_AL_QURA;
        }
    }

    public static List<Teams.Team> teams(Context c) {
        List<Teams.Team> out = new ArrayList<>();
        for (String id : prefs(c).getString(TEAMS, Teams.DEFAULT).split(",")) {
            Teams.Team t = Teams.team(id.trim());
            if (t != null) out.add(t);
        }
        return out;
    }

    public static void setTeams(Context c, List<Teams.Team> teams) {
        StringBuilder b = new StringBuilder();
        for (Teams.Team t : teams) {
            if (b.length() > 0) b.append(',');
            b.append(t.id);
        }
        prefs(c).edit().putString(TEAMS, b.toString()).apply();
    }

    public static int clockSizeSp(Context c) {
        int i = prefs(c).getInt(CLOCK_SIZE, 1);
        return CLOCK_SIZES_SP[Math.max(0, Math.min(CLOCK_SIZES_SP.length - 1, i))];
    }

    static File cacheDir(Context c) {
        File d = new File(c.getFilesDir(), "zaytoon");
        //noinspection ResultOfMethodCallIgnored
        d.mkdirs();
        return d;
    }

    static String read(Context c, String name) {
        File f = new File(cacheDir(c), name);
        if (!f.isFile()) return null;
        try {
            return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    static void write(Context c, String name, String text) {
        File f = new File(cacheDir(c), name);
        File tmp = new File(cacheDir(c), name + ".tmp");
        try {
            Files.write(tmp.toPath(), text.getBytes(StandardCharsets.UTF_8));
            if (!tmp.renameTo(f)) //noinspection ResultOfMethodCallIgnored
                tmp.delete();
        } catch (IOException ignored) {
            // the cache is only a speed-up
        }
    }
}
