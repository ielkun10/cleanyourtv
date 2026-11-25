package sa.zaytoon;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Keeps the weather and the football data fresh while the home screen or the panel is on screen. One background
 * thread, no service and nothing running while another app is in front. Every result is cached, so the cards show
 * the last data straight away after a restart, and a failed fetch keeps the last good data.
 */
public final class Hub {
    private static Hub instance;

    public static synchronized Hub get(Context c) {
        if (instance == null) instance = new Hub(c.getApplicationContext());
        return instance;
    }

    private static final long MINUTE = 60_000L;
    private static final long FIXTURES_EVERY = 180 * MINUTE;
    private static final long LIVE_EVERY = MINUTE;
    private static final long TABLES_EVERY = 360 * MINUTE;

    private final Context app;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Runnable> listeners = new ArrayList<>();
    private final Set<String> running = new LinkedHashSet<>();

    public volatile Weather weather;
    public volatile long weatherAt;
    public volatile String weatherError;
    public final Map<String, List<Football.Match>> fixtures = new HashMap<>();
    public final Map<String, List<Football.Match>> results = new HashMap<>();
    public final Map<String, List<Football.Row>> tables = new HashMap<>();
    public volatile String footballError;
    public volatile long footballAt;

    private Hub(Context app) {
        this.app = app;
        String w = Store.read(app, "weather.json");
        if (w != null) weather = Weather.parse(w);
        weatherAt = Store.prefs(app).getLong("weather_at", 0);
        footballAt = Store.prefs(app).getLong("football_at", 0);
        for (Teams.Team t : Store.teams(app)) loadTeam(t);
    }

    private void loadTeam(Teams.Team t) {
        String f = Store.read(app, "fix_" + t.id + ".json");
        String r = Store.read(app, "res_" + t.id + ".json");
        String s = Store.read(app, "table_" + t.league + ".json");
        synchronized (this) {
            if (f != null) fixtures.put(t.id, Football.schedule(f));
            if (r != null) results.put(t.id, Football.schedule(r));
            if (s != null) tables.put(t.league, Football.standings(s));
        }
    }

    public void listen(Runnable r) {
        if (!listeners.contains(r)) listeners.add(r);
    }

    public void unlisten(Runnable r) {
        listeners.remove(r);
    }

    private void changed() {
        main.post(() -> {
            for (Runnable r : new ArrayList<>(listeners)) r.run();
        });
    }

    private void once(String key, Runnable job) {
        synchronized (running) {
            if (!running.add(key)) return;
        }
        worker.execute(() -> {
            try {
                job.run();
            } finally {
                synchronized (running) {
                    running.remove(key);
                }
            }
        });
    }

    /** Fetches whatever is due. force: everything now (the panel's Update button). */
    public void refresh(boolean force) {
        long now = System.currentTimeMillis();
        SharedPreferences p = Store.prefs(app);
        long weatherEvery = p.getInt(Store.WEATHER_MINUTES, 30) * MINUTE;
        if (force || now - weatherAt > weatherEvery) once("weather", this::fetchWeather);
        boolean live = liveWindow(now);
        long teamsEvery = live ? LIVE_EVERY : FIXTURES_EVERY;
        if (force || now - footballAt > teamsEvery) once("football", () -> fetchFootball(force || now - footballAt > FIXTURES_EVERY));
    }

    /** Whether a followed club is playing now (from ten minutes before kick-off to two and a half hours after). */
    public synchronized boolean liveWindow(long now) {
        for (List<Football.Match> list : fixtures.values()) {
            for (Football.Match m : list) {
                if (m.live()) return true;
                if (!m.finished() && now >= m.startMs - 10 * MINUTE && now <= m.startMs + 150 * MINUTE) return true;
            }
        }
        return false;
    }

    private void fetchWeather() {
        Places.City city = Store.city(app);
        try {
            String json = Net.text(Weather.url(city.lat, city.lng), 256 * 1024);
            Weather w = Weather.parse(json);
            if (w == null) throw new java.io.IOException("unreadable forecast");
            Store.write(app, "weather.json", json);
            weather = w;
            weatherAt = System.currentTimeMillis();
            weatherError = null;
            Store.prefs(app).edit().putLong("weather_at", weatherAt).putString("weather_city", city.id).apply();
        } catch (Exception e) {
            weatherError = "تعذّر تحديث الطقس، تُعرض آخر بيانات محفوظة";
        }
        changed();
    }

    /** The city changed: drop the old forecast so another city's weather is never shown under the new name. */
    public void cityChanged() {
        weather = null;
        weatherAt = 0;
        changed();
        once("weather", this::fetchWeather);
    }

    private void fetchFootball(boolean full) {
        List<Teams.Team> teams = Store.teams(app);
        boolean ok = false;
        Set<String> leagues = new LinkedHashSet<>();
        for (Teams.Team t : teams) {
            leagues.add(t.league);
            try {
                String f = Net.text(Football.scheduleUrl("all", t.id, true), 2 * 1024 * 1024);
                List<Football.Match> list = Football.schedule(f);
                Store.write(app, "fix_" + t.id + ".json", f);
                synchronized (this) {
                    fixtures.put(t.id, list);
                }
                ok = true;
            } catch (Exception ignored) {
                // keep the cached fixtures
            }
            if (!full) continue;
            try {
                String r = Net.text(Football.scheduleUrl("all", t.id, false), 2 * 1024 * 1024);
                List<Football.Match> list = Football.schedule(r);
                if (list.isEmpty()) {
                    r = Net.text(Football.scheduleUrl(t.league, t.id, false), 2 * 1024 * 1024);
                    list = Football.schedule(r);
                }
                Store.write(app, "res_" + t.id + ".json", r);
                synchronized (this) {
                    results.put(t.id, list);
                }
            } catch (Exception ignored) {
                // keep the cached results
            }
        }
        long now = System.currentTimeMillis();
        if (full || now - Store.prefs(app).getLong("tables_at", 0) > TABLES_EVERY) {
            for (String league : leagues) {
                try {
                    String s = Net.text(Football.standingsUrl(league), 2 * 1024 * 1024);
                    List<Football.Row> rows = Football.standings(s);
                    if (!rows.isEmpty()) {
                        Store.write(app, "table_" + league + ".json", s);
                        synchronized (this) {
                            tables.put(league, rows);
                        }
                    }
                } catch (Exception ignored) {
                    // keep the cached table
                }
            }
            Store.prefs(app).edit().putLong("tables_at", now).apply();
        }
        if (ok) {
            footballAt = now;
            footballError = null;
            Store.prefs(app).edit().putLong("football_at", footballAt).apply();
        } else {
            footballError = "تعذّر الوصول إلى مصدر المباريات، تُعرض آخر بيانات محفوظة";
        }
        changed();
    }

    /** The followed clubs changed. */
    public void teamsChanged() {
        synchronized (this) {
            fixtures.clear();
            results.clear();
        }
        for (Teams.Team t : Store.teams(app)) loadTeam(t);
        footballAt = 0;
        changed();
        once("football", () -> fetchFootball(true));
    }

    /** The next or current match of a club: a live one first, else the next to start, else null. */
    public synchronized Football.Match next(String teamId, long now) {
        List<Football.Match> list = fixtures.get(teamId);
        if (list == null) return null;
        for (Football.Match m : list) if (m.live()) return m;
        for (Football.Match m : list) if (!m.finished() && m.startMs + 150 * MINUTE > now) return m;
        return null;
    }

    /** The club's last finished matches, newest first. */
    public synchronized List<Football.Match> last(String teamId, int n) {
        List<Football.Match> out = new ArrayList<>();
        List<Football.Match> list = results.get(teamId);
        if (list == null) return out;
        for (int i = list.size() - 1; i >= 0 && out.size() < n; i--) {
            if (list.get(i).finished()) out.add(list.get(i));
        }
        return out;
    }

    public synchronized List<Football.Row> table(String league) {
        List<Football.Row> t = tables.get(league);
        return t == null ? new ArrayList<>() : new ArrayList<>(t);
    }

    /** Runs a job on Zaytoon's background thread (wallpaper downloads). */
    void background(String key, Runnable job) {
        once(key, job);
    }

    void notifyChanged() {
        changed();
    }
}
