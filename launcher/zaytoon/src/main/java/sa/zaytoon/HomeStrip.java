package sa.zaytoon;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * The three glass cards on the home screen, under the clock: the weather, the next prayer with its countdown, and the
 * followed clubs' next or live match (one club per minute). Display only, never focusable: the remote's path through
 * Home stays exactly as it was. Everything here is also in the panel, opened by the olive button in the top bar.
 */
final class HomeStrip extends LinearLayout {
    private final Typeface body;
    private final int accent;
    private final Card weather;
    private final Card prayer;
    private final Card sports;
    private int teamTurn;

    private final class Card extends LinearLayout {
        final TextView kicker;
        final TextView main;
        final TextView sub;

        Card(Context c) {
            super(c);
            setOrientation(VERTICAL);
            setBackground(Ui.glass(c));
            int p = Ui.dp(c, 18);
            setPadding(p, Ui.dp(c, 12), p, Ui.dp(c, 12));
            kicker = Ui.text(c, body, 13, accent);
            main = Ui.text(c, body, 24, Ui.TEXT);
            sub = Ui.text(c, body, 14, Ui.DIM);
            for (TextView t : new TextView[] {kicker, main, sub}) {
                t.setSingleLine(true);
                t.setEllipsize(TextUtils.TruncateAt.END);
                addView(t, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            }
            setFocusable(false);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    HomeStrip(Context c, Typeface body, int accent) {
        super(c);
        this.body = body;
        this.accent = accent;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.RIGHT);
        setFocusable(false);
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        weather = new Card(c);
        prayer = new Card(c);
        sports = new Card(c);
        int w = Ui.dp(c, 270);
        int gap = Ui.dp(c, 14);
        // Laid out right to left by hand: the launcher's layouts are left to right on purpose (no supportsRtl).
        Card[] order = {sports, prayer, weather};
        for (int i = 0; i < order.length; i++) {
            LayoutParams lp = new LayoutParams(w, LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.leftMargin = gap;
            addView(order[i], lp);
        }
    }

    void update() {
        Context c = getContext();
        Hub hub = Hub.get(c);
        Places.City city = Store.city(c);
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zone);

        Weather w = hub.weather;
        weather.kicker.setText("الطقس · " + city.name);
        if (w != null) {
            weather.main.setText(Weather.icon(w.code, w.day) + "  " + Ar.num(Math.round(w.temperature)) + "°");
            String range = "";
            if (!w.days.isEmpty()) {
                Weather.Day d = w.days.get(0);
                range = " · " + Ar.num(Math.round(d.max)) + "° / " + Ar.num(Math.round(d.min)) + "°";
            }
            weather.sub.setText(Weather.describe(w.code) + range);
        } else {
            weather.main.setText("…");
            weather.sub.setText(hub.weatherError != null ? "بانتظار الاتصال بالإنترنت" : "جارٍ التحديث");
        }

        Prayer.Next next = Prayer.next(c, now);
        prayer.kicker.setText("الصلاة القادمة");
        prayer.main.setText(Prayer.NAMES[next.index] + "  " + Ar.clock(next.minuteOfDay));
        prayer.sub.setText("بعد " + Ar.span(next.minutesLeft));

        List<Teams.Team> teams = Store.teams(c);
        sports.setVisibility(teams.isEmpty() ? View.GONE : View.VISIBLE);
        if (!teams.isEmpty()) {
            Teams.Team t = teams.get(Math.floorMod(teamTurn, teams.size()));
            Football.Match m = hub.next(t.id, System.currentTimeMillis());
            if (m == null) {
                List<Football.Match> last = hub.last(t.id, 1);
                m = last.isEmpty() ? null : last.get(0);
            }
            if (m == null) {
                sports.kicker.setText(t.name);
                sports.main.setText("لا مباريات قريبة");
                sports.sub.setText(hub.footballError != null ? "بانتظار الاتصال بالإنترنت" : "");
            } else {
                sports.kicker.setText(m.live() ? "● مباشر · " + m.competition : m.competition);
                sports.kicker.setTextColor(m.live() ? Ui.LIVE : accent);
                sports.main.setText(Matches.line(m));
                sports.sub.setText(Matches.when(m, zone, now.toLocalDate()));
            }
        }
    }

    /** Called once a minute: the next club gets its turn. */
    void tick() {
        teamTurn++;
        update();
    }

    static LocalDate today() {
        return LocalDate.now();
    }
}
