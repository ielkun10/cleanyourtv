package sa.zaytoon;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Fixtures, results, live scores and league tables for the followed clubs, from ESPN's public JSON feed
 * (site.api.espn.com): free, no key, no account. It is not an official, documented API, so the parsers here
 * skip anything they do not recognise and the panel says when the feed could not be read.
 */
public final class Football {
    public static final class Match {
        public String id = "";
        public long startMs;
        public String competition = "";
        public String homeId = "", awayId = "";
        public String home = "", away = "";
        public String homeScore = "", awayScore = "";
        /** pre, in or post. */
        public String state = "pre";
        /** ESPN's short status: "FT", "45'", "HT". */
        public String detail = "";

        public boolean live() {
            return "in".equals(state);
        }

        public boolean finished() {
            return "post".equals(state);
        }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id).put("start", startMs).put("comp", competition).put("hid", homeId).put("aid", awayId)
                        .put("home", home).put("away", away).put("hs", homeScore).put("as", awayScore)
                        .put("state", state).put("detail", detail);
            } catch (Exception ignored) {
                // put only throws for NaN numbers
            }
            return o;
        }

        static Match fromJson(JSONObject o) {
            Match m = new Match();
            m.id = o.optString("id");
            m.startMs = o.optLong("start");
            m.competition = o.optString("comp");
            m.homeId = o.optString("hid");
            m.awayId = o.optString("aid");
            m.home = o.optString("home");
            m.away = o.optString("away");
            m.homeScore = o.optString("hs");
            m.awayScore = o.optString("as");
            m.state = o.optString("state", "pre");
            m.detail = o.optString("detail");
            return m;
        }
    }

    public static final class Row {
        public int rank;
        public String teamId = "";
        public String team = "";
        public int played, won, drawn, lost, goalDiff, points;
    }

    private static final DateTimeFormatter ESPN_TIME = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm")
            .optionalStart().appendPattern(":ss").optionalEnd()
            .appendPattern("X")
            .toFormatter(Locale.ROOT);

    private Football() {
    }

    static String scheduleUrl(String league, String teamId, boolean fixtures) {
        return "https://site.api.espn.com/apis/site/v2/sports/soccer/" + league + "/teams/" + teamId + "/schedule"
                + (fixtures ? "?fixture=true" : "");
    }

    static String standingsUrl(String league) {
        return "https://site.api.espn.com/apis/v2/sports/soccer/" + league + "/standings";
    }

    static long parseTime(String s) {
        try {
            return OffsetDateTime.parse(s, ESPN_TIME).toInstant().toEpochMilli();
        } catch (Exception e) {
            try {
                return Instant.parse(s).toEpochMilli();
            } catch (Exception e2) {
                return 0;
            }
        }
    }

    private static String score(JSONObject competitor) {
        Object s = competitor.opt("score");
        if (s instanceof JSONObject) {
            JSONObject o = (JSONObject) s;
            String d = o.optString("displayValue", "");
            if (!d.isEmpty()) return d;
            double v = o.optDouble("value", Double.NaN);
            return Double.isNaN(v) ? "" : Long.toString(Math.round(v));
        }
        return s == null || s == JSONObject.NULL ? "" : s.toString();
    }

    /** The matches in a team schedule (results or fixtures), oldest first. */
    public static List<Match> schedule(String json) {
        List<Match> out = new ArrayList<>();
        JSONObject root;
        try {
            root = new JSONObject(json);
        } catch (Exception e) {
            return out;
        }
        JSONArray events = root.optJSONArray("events");
        if (events == null) return out;
        for (int i = 0; i < events.length(); i++) {
            try {
                Match m = event(events.getJSONObject(i));
                if (m != null) out.add(m);
            } catch (Exception ignored) {
                // one odd event never hides the others
            }
        }
        Collections.sort(out, (a, b) -> Long.compare(a.startMs, b.startMs));
        return out;
    }

    static Match event(JSONObject e) throws Exception {
        JSONArray comps = e.optJSONArray("competitions");
        if (comps == null || comps.length() == 0) return null;
        JSONObject c = comps.getJSONObject(0);
        JSONArray teams = c.optJSONArray("competitors");
        if (teams == null || teams.length() < 2) return null;
        Match m = new Match();
        m.id = e.optString("id");
        m.startMs = parseTime(c.optString("date", e.optString("date")));
        JSONObject league = e.optJSONObject("league");
        String comp = league != null ? league.optString("name", league.optString("abbreviation")) : "";
        if (comp.isEmpty()) {
            JSONObject st = e.optJSONObject("seasonType");
            comp = st != null ? st.optString("name") : "";
        }
        m.competition = Teams.competition(comp);
        for (int i = 0; i < teams.length(); i++) {
            JSONObject t = teams.getJSONObject(i);
            JSONObject team = t.optJSONObject("team");
            String id = t.optString("id", team != null ? team.optString("id") : "");
            String name = team != null ? team.optString("displayName", team.optString("name")) : "";
            if ("home".equals(t.optString("homeAway"))) {
                m.homeId = id;
                m.home = Teams.name(name);
                m.homeScore = score(t);
            } else {
                m.awayId = id;
                m.away = Teams.name(name);
                m.awayScore = score(t);
            }
        }
        JSONObject status = c.optJSONObject("status");
        if (status == null) status = e.optJSONObject("status");
        if (status != null) {
            JSONObject type = status.optJSONObject("type");
            if (type != null) {
                m.state = type.optString("state", "pre");
                m.detail = type.optString("shortDetail", "");
            }
            if (m.live()) {
                String clock = status.optString("displayClock", "");
                if (!clock.isEmpty() && !clock.startsWith("0")) m.detail = clock;
            }
        }
        return m;
    }

    /** A league table, best first. Group stages (several tables) are flattened in ESPN's order. */
    public static List<Row> standings(String json) {
        List<Row> out = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(json);
            JSONArray children = root.optJSONArray("children");
            if (children == null) return out;
            for (int g = 0; g < children.length(); g++) {
                JSONObject s = children.getJSONObject(g).optJSONObject("standings");
                JSONArray entries = s == null ? null : s.optJSONArray("entries");
                if (entries == null) continue;
                for (int i = 0; i < entries.length(); i++) {
                    JSONObject en = entries.getJSONObject(i);
                    JSONObject team = en.optJSONObject("team");
                    if (team == null) continue;
                    Row r = new Row();
                    r.teamId = team.optString("id");
                    r.team = Teams.name(team.optString("displayName"));
                    JSONArray stats = en.optJSONArray("stats");
                    if (stats != null) {
                        for (int k = 0; k < stats.length(); k++) {
                            JSONObject st = stats.getJSONObject(k);
                            int v = (int) Math.round(st.optDouble("value", 0));
                            switch (st.optString("name")) {
                                case "rank": r.rank = v; break;
                                case "gamesPlayed": r.played = v; break;
                                case "wins": r.won = v; break;
                                case "ties": r.drawn = v; break;
                                case "losses": r.lost = v; break;
                                case "pointDifferential": r.goalDiff = v; break;
                                case "points": r.points = v; break;
                                default: break;
                            }
                        }
                    }
                    out.add(r);
                }
            }
        } catch (Exception ignored) {
            // an unreadable table shows as none
        }
        for (int i = 0; i < out.size(); i++) if (out.get(i).rank == 0) out.get(i).rank = i + 1;
        return out;
    }

    static JSONArray toJson(List<Match> matches) {
        JSONArray a = new JSONArray();
        for (Match m : matches) a.put(m.toJson());
        return a;
    }

    static List<Match> fromJson(JSONArray a) {
        List<Match> out = new ArrayList<>();
        if (a == null) return out;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null) out.add(Match.fromJson(o));
        }
        return out;
    }
}
