package sa.zaytoon;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The weather from Open-Meteo (free, no key; data CC BY 4.0, credited in the panel). */
public final class Weather {
    public static final class Day {
        public LocalDate date;
        public int code;
        public double max, min;
    }

    public double temperature;
    public int code;
    public boolean day = true;
    public int humidity;
    public double wind;
    public final List<Day> days = new ArrayList<>();

    static String url(double lat, double lng) {
        return String.format(Locale.ROOT, "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                + "&current=temperature_2m,weather_code,is_day,relative_humidity_2m,wind_speed_10m"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=Asia%%2FRiyadh&forecast_days=5", lat, lng);
    }

    /** null when the response is not a forecast. */
    public static Weather parse(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONObject cur = root.getJSONObject("current");
            Weather w = new Weather();
            w.temperature = cur.getDouble("temperature_2m");
            w.code = cur.optInt("weather_code", 0);
            w.day = cur.optInt("is_day", 1) == 1;
            w.humidity = cur.optInt("relative_humidity_2m", 0);
            w.wind = cur.optDouble("wind_speed_10m", 0);
            JSONObject daily = root.optJSONObject("daily");
            if (daily != null) {
                JSONArray t = daily.getJSONArray("time");
                JSONArray c = daily.getJSONArray("weather_code");
                JSONArray hi = daily.getJSONArray("temperature_2m_max");
                JSONArray lo = daily.getJSONArray("temperature_2m_min");
                for (int i = 0; i < t.length(); i++) {
                    Day d = new Day();
                    d.date = LocalDate.parse(t.getString(i));
                    d.code = c.optInt(i, 0);
                    d.max = hi.optDouble(i, 0);
                    d.min = lo.optDouble(i, 0);
                    w.days.add(d);
                }
            }
            return w;
        } catch (Exception e) {
            return null;
        }
    }

    /** WMO weather code in Arabic. */
    public static String describe(int code) {
        if (code == 0) return "صحو";
        if (code == 1) return "صحو غالبًا";
        if (code == 2) return "غائم جزئيًا";
        if (code == 3) return "غائم";
        if (code == 45 || code == 48) return "ضباب";
        if (code >= 51 && code <= 57) return "رذاذ";
        if (code >= 61 && code <= 67) return "مطر";
        if (code >= 71 && code <= 77) return "ثلج";
        if (code >= 80 && code <= 82) return "زخات مطر";
        if (code == 85 || code == 86) return "زخات ثلج";
        if (code >= 95) return "عواصف رعدية";
        return "—";
    }

    public static String icon(int code, boolean day) {
        if (code == 0) return day ? "☀️" : "🌙";
        if (code == 1 || code == 2) return day ? "⛅" : "🌙";
        if (code == 3) return "☁️";
        if (code == 45 || code == 48) return "🌫️";
        if (code >= 51 && code <= 67 || code >= 80 && code <= 82) return "🌧️";
        if (code >= 71 && code <= 77 || code == 85 || code == 86) return "❄️";
        if (code >= 95) return "⛈️";
        return "☀️";
    }
}
