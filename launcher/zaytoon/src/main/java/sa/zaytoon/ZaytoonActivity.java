package sa.zaytoon;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Zaytoon's panel: weather, prayer times, football, wallpapers and the look, all with the remote's arrows, OK and
 * Back. Tabs on the right; focusing a tab shows it. Settings are lists: OK on a row opens its choices, OK picks one.
 */
public final class ZaytoonActivity extends Activity {
    public static final String EXTRA_TAB = "tab";

    private static final String[] TABS = {"الطقس", "الصلاة", "الرياضة", "الخلفيات", "المظهر", "حول زيتون"};
    private static final int WEATHER = 0, PRAYER = 1, SPORTS = 2, WALLPAPERS = 3, LOOK = 4, ABOUT = 5;
    private static final int PHOTOS_PERMISSION = 7;

    private Typeface body;
    private Typeface display;
    private int accent;
    private final List<TextView> tabViews = new ArrayList<>();
    private LinearLayout content;
    private ScrollView scroll;
    private TextView title;
    private int tab = -1;
    /** A chooser is open over the tab: Back returns to the tab, not Home. */
    private boolean choosing;
    private boolean themeChanged;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable onData = () -> {
        if (!choosing && tab >= 0 && !content.hasFocus()) render(tab);
    };
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!choosing && (tab == PRAYER || tab == SPORTS || tab == WALLPAPERS) && !content.hasFocus()) render(tab);
            main.postDelayed(this, 30_000);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        int bodyId = getResources().getIdentifier("noto_kufi_arabic", "font", getPackageName());
        Typeface b = null;
        try {
            if (bodyId != 0) b = getResources().getFont(bodyId);
        } catch (RuntimeException ignored) {
            // the system font then
        }
        body = b != null ? b : Typeface.DEFAULT;
        display = getResources().getFont(R.font.katibeh);
        accent = accentFromTheme();

        FrameLayout root = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {0xFF1F2919, 0xFF141A12, 0xFF0D110B});
        root.setBackground(bg);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int pad = Ui.dp(this, 48);
        row.setPadding(pad, Ui.dp(this, 36), pad, Ui.dp(this, 28));

        // Content on the left, tabs on the right: added in that order because the launcher lays out left to right.
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        title = Ui.text(this, display, 40, accent);
        left.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        int sp = Ui.dp(this, 8);
        scroll.setPadding(sp, sp, sp, Ui.dp(this, 40));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        left.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.VERTICAL);
        TextView brand = Ui.text(this, display, 46, Ui.TEXT);
        brand.setText("زيتون");
        tabs.addView(brand, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView hint = Ui.text(this, body, 13, Ui.FAINT);
        hint.setText("الأسهم للتنقل · OK للاختيار · رجوع للخروج");
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.bottomMargin = Ui.dp(this, 18);
        tabs.addView(hint, hp);
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            TextView t = Ui.text(this, body, 20, Ui.TEXT);
            t.setText(TABS[i]);
            t.setFocusable(true);
            t.setBackground(Ui.focusable(this, 18, accent));
            int p = Ui.dp(this, 20);
            t.setPadding(p, Ui.dp(this, 12), p, Ui.dp(this, 12));
            t.setOnFocusChangeListener((v, focused) -> {
                if (focused && (index != tab || choosing)) {
                    choosing = false;
                    render(index);
                }
                v.animate().scaleX(focused ? 1.04f : 1f).scaleY(focused ? 1.04f : 1f).setDuration(140).start();
            });
            t.setOnClickListener(v -> focusFirstContent());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = Ui.dp(this, 10);
            tabs.addView(t, lp);
            tabViews.add(t);
        }
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(Ui.dp(this, 250), ViewGroup.LayoutParams.MATCH_PARENT);
        tp.leftMargin = Ui.dp(this, 36);
        row.addView(tabs, tp);
        root.addView(row);
        setContentView(root);

        int start = Math.max(0, Math.min(TABS.length - 1, getIntent().getIntExtra(EXTRA_TAB, 0)));
        render(start);
        tabViews.get(start).requestFocus();
    }

    private int accentFromTheme() {
        try {
            String hex = ThemeFile.read(this).optString("accent_text_color", "");
            if (hex.matches("#[0-9A-Fa-f]{6}")) return 0xFF000000 | Integer.parseInt(hex.substring(1), 16);
        } catch (RuntimeException ignored) {
            // the default gold
        }
        return Ui.GOLD;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Hub hub = Hub.get(this);
        hub.listen(onData);
        hub.refresh(false);
        main.postDelayed(tick, 30_000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Hub.get(this).unlisten(onData);
        main.removeCallbacks(tick);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && choosing) {
            choosing = false;
            render(tab);
            focusFirstContent();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void finish() {
        setResult(themeChanged ? RESULT_OK : RESULT_CANCELED);
        super.finish();
    }

    private void focusFirstContent() {
        for (int i = 0; i < content.getChildCount(); i++) {
            View v = firstFocusable(content.getChildAt(i));
            if (v != null) {
                v.requestFocus();
                return;
            }
        }
    }

    private static View firstFocusable(View v) {
        if (v.isFocusable() && v.getVisibility() == View.VISIBLE) return v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View f = firstFocusable(g.getChildAt(i));
                if (f != null) return f;
            }
        }
        return null;
    }

    // ---- building blocks -------------------------------------------------------------------------------------

    private LinearLayout card(boolean focusable) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 22);
        c.setPadding(p, Ui.dp(this, 16), p, Ui.dp(this, 16));
        if (focusable) {
            c.setFocusable(true);
            c.setBackground(Ui.focusable(this, 22, accent));
        } else {
            c.setBackground(Ui.glass(this));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(this, 14);
        content.addView(c, lp);
        return c;
    }

    private TextView line(ViewGroup parent, String text, float sp, int color, Typeface face) {
        TextView t = Ui.text(this, face, sp, color);
        t.setText(text);
        parent.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return t;
    }

    private void heading(String text) {
        TextView t = Ui.text(this, body, 15, accent);
        t.setText(text);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 10);
        lp.bottomMargin = Ui.dp(this, 8);
        content.addView(t, lp);
    }

    private void note(String text) {
        TextView t = Ui.text(this, body, 13, Ui.FAINT);
        t.setText(text);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 6);
        lp.bottomMargin = Ui.dp(this, 6);
        content.addView(t, lp);
    }

    /** A settings row: the label on the right, its value in gold on the left. OK runs action. */
    private View row(String label, String value, Runnable action) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setFocusable(true);
        r.setClickable(true);
        r.setBackground(Ui.focusable(this, 16, accent));
        int p = Ui.dp(this, 20);
        r.setPadding(p, Ui.dp(this, 13), p, Ui.dp(this, 13));
        TextView v = Ui.text(this, body, 17, accent);
        v.setText(value == null ? "" : value + "  ‹");
        v.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        TextView l = Ui.text(this, body, 18, Ui.TEXT);
        l.setText(label);
        r.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        r.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        r.setOnClickListener(x -> action.run());
        r.setOnFocusChangeListener((x, f) -> x.animate().scaleX(f ? 1.015f : 1f).scaleY(f ? 1.015f : 1f).setDuration(120).start());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(this, 10);
        content.addView(r, lp);
        return r;
    }

    interface Pick {
        void on(int index);
    }

    /** A list of choices in place of the tab; OK picks, Back goes back. */
    private void choose(String heading, String[] options, int selected, Pick pick) {
        choosing = true;
        content.removeAllViews();
        title.setText(heading);
        View first = null;
        for (int i = 0; i < options.length; i++) {
            final int index = i;
            View r = row(options[i], i == selected ? "✓" : "", () -> {
                choosing = false;
                pick.on(index);
                render(tab);
                focusFirstContent();
            });
            if (i == selected || first == null) first = r;
        }
        scroll.scrollTo(0, 0);
        if (first != null) first.requestFocus();
    }

    /** Several choices that can each be on or off; OK toggles, Back saves. */
    private void chooseMany(String heading, String[] options, boolean[] on, Runnable save) {
        choosing = true;
        content.removeAllViews();
        title.setText(heading);
        note("OK يُفعّل أو يلغي · رجوع للحفظ");
        View first = null;
        for (int i = 0; i < options.length; i++) {
            final int index = i;
            final View[] holder = new View[1];
            holder[0] = row(options[i], on[i] ? "✓" : "—", () -> {
                on[index] = !on[index];
                save.run();
                TextView value = (TextView) ((LinearLayout) holder[0]).getChildAt(0);
                value.setText((on[index] ? "✓" : "—") + "  ‹");
            });
            if (first == null) first = holder[0];
        }
        scroll.scrollTo(0, 0);
        if (first != null) first.requestFocus();
    }

    private void render(int index) {
        tab = index;
        for (int i = 0; i < tabViews.size(); i++) tabViews.get(i).setActivated(i == index);
        content.removeAllViews();
        title.setText(TABS[index]);
        try {
            switch (index) {
                case WEATHER: weatherTab(); break;
                case PRAYER: prayerTab(); break;
                case SPORTS: sportsTab(); break;
                case WALLPAPERS: wallpapersTab(); break;
                case LOOK: lookTab(); break;
                default: aboutTab(); break;
            }
        } catch (RuntimeException e) {
            android.util.Log.w("Zaytoon", "tab " + index, e);
            note("تعذّر عرض هذه الصفحة الآن.");
        }
    }

    // ---- tabs ----------------------------------------------------------------------------------------------------

    private String cityRowValue() {
        return Store.city(this).name;
    }

    private void chooseCity(Runnable after) {
        String[] names = new String[Places.CITIES.length];
        int sel = 0;
        String current = Store.city(this).id;
        for (int i = 0; i < names.length; i++) {
            names[i] = Places.CITIES[i].name;
            if (Places.CITIES[i].id.equals(current)) sel = i;
        }
        choose("اختر المدينة", names, sel, i -> {
            Store.prefs(this).edit().putString(Store.CITY, Places.CITIES[i].id).apply();
            Hub.get(this).cityChanged();
            if (after != null) after.run();
        });
    }

    private void weatherTab() {
        Hub hub = Hub.get(this);
        Weather w = hub.weather;
        Places.City city = Store.city(this);
        LinearLayout now = card(true);
        line(now, "الآن في " + city.name, 15, accent, body);
        if (w == null) {
            line(now, hub.weatherError != null ? hub.weatherError : "جارٍ جلب الطقس…", 22, Ui.TEXT, body);
        } else {
            line(now, Weather.icon(w.code, w.day) + "  " + Ar.num(Math.round(w.temperature)) + "°", 56, Ui.TEXT, display);
            line(now, Weather.describe(w.code) + " · الرطوبة " + Ar.num(w.humidity) + "٪ · الرياح " + Ar.num(Math.round(w.wind)) + " كم/س",
                    17, Ui.DIM, body);
            if (!w.days.isEmpty()) {
                LinearLayout days = new LinearLayout(this);
                days.setOrientation(LinearLayout.HORIZONTAL);
                List<Weather.Day> list = w.days;
                // Right to left: today on the right.
                for (int i = Math.min(list.size(), 5) - 1; i >= 0; i--) {
                    Weather.Day d = list.get(i);
                    LinearLayout col = new LinearLayout(this);
                    col.setOrientation(LinearLayout.VERTICAL);
                    col.setGravity(Gravity.CENTER_HORIZONTAL);
                    col.setBackground(Ui.glass(this, 16, 0x12FFFFFF, 0x1FFFFFFF));
                    int p = Ui.dp(this, 10);
                    col.setPadding(p, p, p, p);
                    String name = i == 0 ? "اليوم" : Ar.shortDay(d.date.atStartOfDay(ZoneId.systemDefault()));
                    for (String s : new String[] {name, Weather.icon(d.code, true),
                            Ar.num(Math.round(d.max)) + "° / " + Ar.num(Math.round(d.min)) + "°"}) {
                        TextView t = Ui.text(this, body, 15, Ui.TEXT);
                        t.setGravity(Gravity.CENTER);
                        t.setText(s);
                        col.addView(t);
                    }
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                    lp.leftMargin = Ui.dp(this, 5);
                    lp.rightMargin = Ui.dp(this, 5);
                    days.addView(col, lp);
                }
                LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dl.topMargin = Ui.dp(this, 14);
                now.addView(days, dl);
            }
        }
        heading("الإعدادات");
        row("المدينة", cityRowValue(), () -> chooseCity(null));
        int[] every = {15, 30, 60};
        int cur = Store.prefs(this).getInt(Store.WEATHER_MINUTES, 30);
        row("التحديث التلقائي", "كل " + Ar.num(cur) + " دقيقة", () -> choose("التحديث التلقائي كل",
                new String[] {"١٥ دقيقة", "٣٠ دقيقة", "ساعة"}, cur == 15 ? 0 : cur == 60 ? 2 : 1,
                i -> Store.prefs(this).edit().putInt(Store.WEATHER_MINUTES, every[i]).apply()));
        row("تحديث الآن", hub.weatherAt == 0 ? "" : "آخر تحديث " + Ar.time(hub.weatherAt, ZoneId.systemDefault()),
                () -> hub.refresh(true));
        note("بيانات الطقس من Open-Meteo.com (مجانية، رخصة CC BY 4.0). تُحدَّث فقط والشاشة الرئيسية أو هذه اللوحة مفتوحة.");
    }

    private void prayerTab() {
        ZonedDateTime now = ZonedDateTime.now();
        Prayer.Next next = Prayer.next(this, now);
        LinearLayout top = card(true);
        line(top, Prayer.hijri() + "  ·  " + Ar.day(now), 15, accent, body);
        line(top, Prayer.NAMES[next.index] + "  " + Ar.clock(next.minuteOfDay), 48, Ui.TEXT, display);
        line(top, "بعد " + Ar.span(next.minutesLeft), 18, Ui.DIM, body);

        int[] t = Prayer.today(this, now);
        int offsetMin = now.getOffset().getTotalSeconds() / 60 - (int) (Places.UTC_OFFSET * 60);
        LinearLayout table = card(true);
        line(table, "مواقيت اليوم في " + Store.city(this).name, 15, accent, body);
        for (int i = 0; i < t.length; i++) {
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);
            boolean isNext = i == next.index && next.minuteOfDay == t[i] + offsetMin;
            int color = isNext ? accent : (i == PrayerTimes.SUNRISE ? Ui.DIM : Ui.TEXT);
            TextView time = Ui.text(this, body, 20, color);
            time.setGravity(Gravity.LEFT);
            time.setText(Ar.clock(t[i] + offsetMin));
            TextView name = Ui.text(this, body, 20, color);
            name.setText((isNext ? "◂ " : "") + Prayer.NAMES[i]);
            r.addView(time, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            r.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = Ui.dp(this, 6);
            table.addView(r, lp);
        }
        heading("الإعدادات");
        row("المدينة", cityRowValue(), () -> chooseCity(null));
        PrayerTimes.Method[] methods = PrayerTimes.Method.values();
        String[] methodNames = {"أم القرى (المملكة)", "رابطة العالم الإسلامي", "الهيئة المصرية العامة للمساحة", "جامعة العلوم الإسلامية بكراتشي", "الجمعية الإسلامية لأمريكا الشمالية"};
        int m = Store.method(this).ordinal();
        row("طريقة الحساب", methodNames[m], () -> choose("طريقة الحساب", methodNames, m,
                i -> Store.prefs(this).edit().putString(Store.METHOD, methods[i].name()).apply()));
        boolean hanafi = Store.prefs(this).getBoolean(Store.HANAFI, false);
        String[] asr = {"الجمهور (الظل مثله)", "الحنفي (الظل مثلاه)"};
        row("وقت العصر", asr[hanafi ? 1 : 0], () -> choose("وقت العصر", asr, hanafi ? 1 : 0,
                i -> Store.prefs(this).edit().putBoolean(Store.HANAFI, i == 1).apply()));
        note("تُحسب المواقيت على التلفزيون نفسه دون إنترنت، بتوقيت المملكة. في رمضان يصبح العشاء بعد المغرب بساعتين وفق أم القرى.");
    }

    private void sportsTab() {
        Hub hub = Hub.get(this);
        List<Teams.Team> teams = Store.teams(this);
        long nowMs = System.currentTimeMillis();
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        if (teams.isEmpty()) note("لا تتابع أي نادٍ الآن. اختر أنديتك من «الأندية المتابعة» أدناه.");
        for (Teams.Team t : teams) {
            LinearLayout c = card(true);
            LinearLayout head = new LinearLayout(this);
            head.setOrientation(LinearLayout.HORIZONTAL);
            head.setGravity(Gravity.CENTER_VERTICAL);
            TextView name = Ui.text(this, display, 30, Ui.TEXT);
            name.setText(t.name);
            TextView league = Ui.text(this, body, 14, Ui.DIM);
            league.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            league.setText(Teams.league(t.league));
            ImageView crest = new ImageView(this);
            crest.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Crests.show(this, t.id, crest);
            head.addView(league, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            head.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            int cs = Ui.dp(this, 44);
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(cs, cs);
            cl.leftMargin = Ui.dp(this, 12);
            head.addView(crest, cl);
            c.addView(head);

            Football.Match m = hub.next(t.id, nowMs);
            if (m != null) {
                TextView k = line(c, (m.live() ? "● مباشر الآن · " : "المباراة القادمة · ") + m.competition, 14, m.live() ? Ui.LIVE : accent, body);
                k.setSingleLine(true);
                line(c, Matches.line(m), 22, Ui.TEXT, body);
                line(c, Matches.when(m, zone, today), 15, Ui.DIM, body);
            } else {
                line(c, "لا مباريات قادمة في الجدول المتاح", 16, Ui.DIM, body);
            }
            List<Football.Match> last = hub.last(t.id, 3);
            if (!last.isEmpty()) {
                TextView k = line(c, "آخر النتائج", 14, accent, body);
                k.setPadding(0, Ui.dp(this, 10), 0, 0);
                for (Football.Match r : last) {
                    String when = Ar.shortDay(Instant.ofEpochMilli(r.startMs).atZone(zone));
                    TextView l = line(c, Matches.outcome(r, t.id) + " · " + Matches.line(r) + " · " + when, 16, Ui.TEXT, body);
                    l.setSingleLine(true);
                    l.setEllipsize(TextUtils.TruncateAt.END);
                }
            }
        }
        Set<String> leagues = new LinkedHashSet<>();
        Set<String> followed = new LinkedHashSet<>();
        for (Teams.Team t : teams) {
            leagues.add(t.league);
            followed.add(t.id);
        }
        for (String league : leagues) {
            List<Football.Row> rows = hub.table(league);
            if (rows.isEmpty()) continue;
            LinearLayout c = card(true);
            line(c, "ترتيب " + Teams.league(league), 15, accent, body);
            tableRow(c, "#", "النادي", "لعب", "الفارق", "النقاط", Ui.DIM);
            for (Football.Row r : rows) {
                boolean mine = followed.contains(r.teamId);
                if (r.rank > 6 && !mine) continue;
                tableRow(c, Ar.num(r.rank), r.team, Ar.num(r.played), (r.goalDiff > 0 ? "+" : "") + Ar.num(r.goalDiff),
                        Ar.num(r.points), mine ? accent : Ui.TEXT);
            }
        }
        heading("الإعدادات");
        StringBuilder names = new StringBuilder();
        for (Teams.Team t : teams) names.append(names.length() > 0 ? "، " : "").append(t.name);
        row("الأندية المتابعة", names.length() == 0 ? "لا شيء" : names.toString(), this::chooseTeams);
        row("تحديث الآن", hub.footballAt == 0 ? "" : "آخر تحديث " + Ar.time(hub.footballAt, zone), () -> hub.refresh(true));
        if (hub.footballError != null) note(hub.footballError);
        note("المصدر: خدمة ESPN العامة المجانية، بلا مفتاح ولا اشتراك، للعرض الشخصي على تلفزيونك. النتائج المباشرة تُحدَّث كل دقيقة أثناء مباريات أنديتك فقط. الشعارات ملك أنديتها.");
    }

    private void tableRow(LinearLayout parent, String rank, String team, String played, String diff, String points, int color) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        String[] left = {points, diff, played};
        int[] widths = {70, 80, 60};
        for (int i = 0; i < left.length; i++) {
            TextView v = Ui.text(this, body, 16, color);
            v.setGravity(Gravity.CENTER);
            v.setText(left[i]);
            r.addView(v, new LinearLayout.LayoutParams(Ui.dp(this, widths[i]), ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        TextView name = Ui.text(this, body, 16, color);
        name.setText(team);
        name.setSingleLine(true);
        r.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView rk = Ui.text(this, body, 16, color);
        rk.setGravity(Gravity.CENTER);
        rk.setText(rank);
        r.addView(rk, new LinearLayout.LayoutParams(Ui.dp(this, 44), ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 4);
        parent.addView(r, lp);
    }

    private void chooseTeams() {
        Teams.Team[] all = Teams.ALL;
        String[] names = new String[all.length];
        boolean[] on = new boolean[all.length];
        Set<String> current = new LinkedHashSet<>();
        for (Teams.Team t : Store.teams(this)) current.add(t.id);
        for (int i = 0; i < all.length; i++) {
            names[i] = all[i].name + " · " + Teams.league(all[i].league);
            on[i] = current.contains(all[i].id);
        }
        chooseMany("الأندية المتابعة", names, on, () -> {
            List<Teams.Team> chosen = new ArrayList<>();
            // Keep the order they were followed in, then the new ones.
            for (String id : current) {
                for (int i = 0; i < all.length; i++) if (all[i].id.equals(id) && on[i]) chosen.add(all[i]);
            }
            for (int i = 0; i < all.length; i++) if (on[i] && !chosen.contains(all[i])) chosen.add(all[i]);
            Store.setTeams(this, chosen);
            current.clear();
            for (Teams.Team t : chosen) current.add(t.id);
            Hub.get(this).teamsChanged();
        });
    }

    private void wallpapersTab() {
        Wallpapers wp = Wallpapers.get();
        JSONObject theme = ThemeFile.read(this);
        LinearLayout c = card(true);
        line(c, "في التلفزيون الآن", 15, accent, body);
        line(c, Ar.num(Wallpapers.count(this)) + " صورة", 34, Ui.TEXT, display);
        if (!wp.status.isEmpty()) line(c, wp.status, 16, Ui.DIM, body);

        heading("الإعدادات");
        List<Wallpapers.Set_> sets = Wallpapers.sets(this);
        Set<String> chosen = Wallpapers.chosen(this);
        StringBuilder sv = new StringBuilder();
        for (Wallpapers.Set_ s : sets) if (chosen.contains(s.id)) sv.append(sv.length() > 0 ? "، " : "").append(s.name);
        row("المجموعات", sv.length() == 0 ? "لا شيء" : sv.toString(), () -> {
            String[] names = new String[sets.size()];
            boolean[] on = new boolean[sets.size()];
            for (int i = 0; i < sets.size(); i++) {
                names[i] = sets.get(i).name;
                on[i] = chosen.contains(sets.get(i).id);
            }
            chooseMany("مجموعات الخلفيات", names, on, () -> {
                Set<String> picked = new LinkedHashSet<>();
                for (int i = 0; i < on.length; i++) if (on[i]) picked.add(sets.get(i).id);
                Wallpapers.choose(this, picked);
                Wallpapers.get().sync(this, null);
                themeChanged = true;
            });
        });
        int[] minutes = {0, 5, 10, 20, 30, 60};
        String[] minuteNames = {"بلا تبديل", "كل ٥ دقائق", "كل ١٠ دقائق", "كل ٢٠ دقيقة", "كل ٣٠ دقيقة", "كل ساعة"};
        int rot = theme.optInt("wallpaper_rotation_minutes", 20);
        int ri = indexOf(minutes, rot, 3);
        row("تبديل الخلفية", minuteNames[ri], () -> choose("تبديل الخلفية", minuteNames, ri, i -> setTheme("wallpaper_rotation_minutes", minutes[i])));
        int[] dims = {0, 15, 25, 40, 55};
        String[] dimNames = {"بلا تعتيم", "خفيف", "متوسط", "واضح", "قوي"};
        int di = indexOf(dims, theme.optInt("wallpaper_dim", 25), 2);
        row("تعتيم الخلفية", dimNames[di], () -> choose("تعتيم الخلفية خلف الأيقونات", dimNames, di, i -> setTheme("wallpaper_dim", dims[i])));
        boolean captions = theme.optBoolean("wallpaper_captions", true);
        row("اسم الصورة والمصوّر", captions ? "يظهر" : "مخفي", () -> choose("اسم الصورة والمصوّر", new String[] {"يظهر", "مخفي"},
                captions ? 0 : 1, i -> setTheme("wallpaper_captions", i == 0)));
        row("تنزيل الخلفيات الآن", wp.busy ? "جارٍ…" : "", () -> {
            Wallpapers.get().sync(this, null);
            themeChanged = true;
            render(tab);
        });
        row("استيراد صوري", "من مجلد Zaytoon", this::importPhotos);
        row("إزالة صوري المستوردة", "", () -> {
            int n = Wallpapers.removeUser(this);
            Wallpapers.get().status = "أُزيلت " + Ar.num(n) + " صورة من التدوير (الأصل باقٍ في مكانه)";
            themeChanged = true;
            render(tab);
        });
        note("الصور من ويكيميديا كومنز بتراخيص حرة (CC0 أو CC BY أو CC BY-SA)، باسم كل مصوّر ورخصته تحت الأزرار. "
                + "لإضافة صورك: ضعها في مجلد اسمه Zaytoon على فلاشة USB أو في ذاكرة التلفزيون ثم اختر «استيراد صوري». "
                + "تُعرض الصور بحجم الشاشة دون استهلاك زائد للذاكرة.");
    }

    private static int indexOf(int[] values, int v, int fallback) {
        for (int i = 0; i < values.length; i++) if (values[i] == v) return i;
        return fallback;
    }

    private void setTheme(String key, Object value) {
        ThemeFile.put(this, key, value);
        themeChanged = true;
    }

    private void importPhotos() {
        String perm = Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {perm}, PHOTOS_PERMISSION);
            return;
        }
        Wallpapers.get().status = "جارٍ البحث عن صورك…";
        render(tab);
        Hub.get(this).background("import", () -> {
            int n = Wallpapers.importUser(getApplicationContext());
            Wallpapers.get().status = n == 0 ? "لم أجد صورًا جديدة في مجلد اسمه Zaytoon" : "أُضيفت " + Ar.num(n) + " صورة إلى التدوير";
            themeChanged = themeChanged || n > 0;
            Hub.get(this).notifyChanged();
            main.post(() -> {
                if (tab == WALLPAPERS && !choosing) render(tab);
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != PHOTOS_PERMISSION) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) importPhotos();
        else {
            Wallpapers.get().status = "بدون إذن الصور لا يمكن استيراد صورك";
            render(tab);
        }
    }

    private void lookTab() {
        JSONObject theme = ThemeFile.read(this);
        String[] accents = {"#C9A961", "#D4AF37", "#B8C17A", "#E8DFC8"};
        String[] accentNames = {"ذهبي هادئ", "ذهبي لامع", "زيتوني فاتح", "عاجي"};
        int ai = indexOf(accents, theme.optString("accent_text_color", "#C9A961"));
        row("لون اللمسات", accentNames[ai], () -> choose("لون اللمسات", accentNames, ai, i -> {
            setTheme("accent_text_color", accents[i]);
            accent = 0xFF000000 | Integer.parseInt(accents[i].substring(1), 16);
        }));
        String[] backgrounds = {"#141A12", "#0F140D", "#1C2318", "#1A1A14"};
        String[] bgNames = {"زيتوني داكن", "زيتوني عميق", "زيتوني دافئ", "زيتي رمادي"};
        int bi = indexOf(backgrounds, theme.optString("background_color", "#141A12"));
        row("لون الخلفية", bgNames[bi], () -> choose("لون الخلفية (خلف الصور وعند غيابها)", bgNames, bi, i -> setTheme("background_color", backgrounds[i])));
        int[] opacities = {100, 70, 50, 30};
        String[] opNames = {"معتم", "زجاج خفيف", "زجاج شفاف", "شفاف جدًا"};
        int oi = indexOf(opacities, theme.optInt("dock_opacity", 70), 1);
        row("شفافية الـ Dock", opNames[oi], () -> choose("شفافية الـ Dock", opNames, oi, i -> setTheme("dock_opacity", opacities[i])));
        int[] columns = {4, 5, 6, 7};
        String[] colNames = {"كبيرة (٤ في الصف)", "متوسطة (٥ في الصف)", "صغيرة (٦ في الصف)", "أصغر (٧ في الصف)"};
        int ci = indexOf(columns, theme.optInt("columns", 5), 1);
        row("حجم الأيقونات", colNames[ci], () -> choose("حجم الأيقونات", colNames, ci, i -> setTheme("columns", columns[i])));
        String[] sizes = {"متوسطة", "كبيرة", "كبيرة جدًا"};
        int si = Math.max(0, Math.min(2, Store.prefs(this).getInt(Store.CLOCK_SIZE, 1)));
        row("حجم الساعة", sizes[si], () -> choose("حجم الساعة", sizes, si, i -> {
            Store.prefs(this).edit().putInt(Store.CLOCK_SIZE, i).apply();
            themeChanged = true;
        }));
        String[] fonts = {"كتيبة (مستوحى من الثلث)", "كوفي (نوتو كوفي)"};
        boolean kufi = "kufi".equals(Store.prefs(this).getString(Store.CLOCK_FONT, "katibeh"));
        row("خط الساعة", fonts[kufi ? 1 : 0], () -> choose("خط الساعة", fonts, kufi ? 1 : 0, i -> {
            Store.prefs(this).edit().putString(Store.CLOCK_FONT, i == 1 ? "kufi" : "katibeh").apply();
            themeChanged = true;
        }));
        boolean strip = Store.prefs(this).getBoolean(Store.STRIP, true);
        row("بطاقات الطقس والصلاة والرياضة", strip ? "تظهر" : "مخفية", () -> choose("البطاقات تحت الساعة",
                new String[] {"تظهر", "مخفية"}, strip ? 0 : 1, i -> {
                    Store.prefs(this).edit().putBoolean(Store.STRIP, i == 0).apply();
                    themeChanged = true;
                }));
        int[] idle = {0, 10, 30, 60};
        String[] idleNames = {"لا", "بعد ١٠ دقائق", "بعد ٣٠ دقيقة", "بعد ساعة"};
        int ii = indexOf(idle, theme.optInt("home_idle_clock_minutes", 30), 2);
        row("الساعة الكبيرة عند الخمول", idleNames[ii], () -> choose("الساعة الكبيرة على الخلفية عند ترك الريموت",
                idleNames, ii, i -> setTheme("home_idle_clock_minutes", idle[i])));
        row("استعادة المظهر الافتراضي", "", () -> choose("استعادة مظهر زيتون الأصلي؟", new String[] {"نعم، استعد", "لا"}, 1, i -> {
            if (i != 0) return;
            ThemeFile.reset(this);
            Store.prefs(this).edit().remove(Store.CLOCK_SIZE).remove(Store.CLOCK_FONT).remove(Store.STRIP).apply();
            themeChanged = true;
        }));
        note("ترتيب تطبيقات الـ Dock: في الشاشة الرئيسية اضغط مطوّلًا على OK فوق التطبيق واختر «نقل»، أو استخدم زر الترتيب في الشريط العلوي. "
                + "التغييرات تظهر عند الرجوع إلى الشاشة الرئيسية.");
    }

    private static int indexOf(String[] values, String v) {
        for (int i = 0; i < values.length; i++) if (values[i].equalsIgnoreCase(v)) return i;
        return 0;
    }

    private void aboutTab() {
        LinearLayout c = card(true);
        line(c, "زيتون", 40, Ui.TEXT, display);
        String version = "";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            // shown without
        }
        line(c, "الإصدار " + version + " · مبني على CleanYourTV (رخصة MIT)", 16, Ui.DIM, body);
        line(c, "تطبيق عادي يُثبَّت بجانب واجهة TCL وGoogle TV ولا يعطّل شيئًا منهما.", 16, Ui.TEXT, body);
        heading("العودة إلى واجهة TCL / Google TV");
        row("اختيار الشاشة الرئيسية", "", () -> {
            try {
                startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
            } catch (RuntimeException e) {
                try {
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                } catch (RuntimeException ignored) {
                    // no settings app
                }
            }
        });
        note("إن جعلت زيتون شاشتك الرئيسية، فاختر Google TV من هذه الصفحة للعودة. ولحذف زيتون نهائيًا: الإعدادات ← التطبيقات ← زيتون ← إلغاء التثبيت.");
        heading("المصادر والحقوق");
        note("الطقس: Open-Meteo.com، رخصة CC BY 4.0.\n"
                + "مواقيت الصلاة: تُحسب على الجهاز بطريقة أم القرى، دون إنترنت.\n"
                + "الرياضة: خدمة ESPN العامة المجانية، للعرض الشخصي؛ الشعارات ملك أنديتها.\n"
                + "الخلفيات: ويكيميديا كومنز، بأسماء المصورين وتراخيصهم.\n"
                + "الخطوط: Katibeh وNoto Kufi Arabic، رخصة SIL OFL 1.1.");
    }
}
