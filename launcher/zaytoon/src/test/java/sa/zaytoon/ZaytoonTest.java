package sa.zaytoon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

public class ZaytoonTest {
    private static String resource(String name) throws Exception {
        try (InputStream in = ZaytoonTest.class.getClassLoader().getResourceAsStream(name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void near(int expectedMinutes, int actual, String what) {
        assertTrue(what + ": expected " + expectedMinutes + " got " + actual, Math.abs(expectedMinutes - actual) <= 1);
    }

    private static int hm(String s) {
        return Integer.parseInt(s.substring(0, 2)) * 60 + Integer.parseInt(s.substring(3, 5));
    }

    /** Against Aladhan's Umm al-Qura times (method 4), read for these dates in November 2026. */
    @Test
    public void prayerTimesMatchUmmAlQura() {
        String[][] cases = {
                // lat, lng, date, Fajr, sunrise, Dhuhr, Asr, Maghrib, Isha
                {"18.3", "42.73", "2026-11-25", "05:02", "06:21", "11:56", "15:08", "17:31", "19:01"},
                {"21.4225", "39.8262", "2026-11-25", "05:18", "06:38", "12:08", "15:16", "17:37", "19:07"},
                {"24.7136", "46.6753", "2026-06-01", "03:34", "05:04", "11:51", "15:13", "18:38", "20:08"},
        };
        for (String[] c : cases) {
            int[] t = PrayerTimes.minutes(LocalDate.parse(c[2]), Double.parseDouble(c[0]), Double.parseDouble(c[1]), 3,
                    PrayerTimes.Method.UMM_AL_QURA, false, false);
            for (int i = 0; i < 6; i++) near(hm(c[3 + i]), t[i], c[2] + " " + c[0] + " #" + i);
        }
    }

    @Test
    public void ramadanIshaIsTwoHoursAfterMaghrib() {
        int[] t = PrayerTimes.minutes(LocalDate.of(2027, 2, 20), 18.3, 42.73, 3, PrayerTimes.Method.UMM_AL_QURA, false, true);
        assertEquals(120, t[PrayerTimes.ISHA] - t[PrayerTimes.MAGHRIB]);
    }

    @Test
    public void hanafiAsrIsLater() {
        LocalDate d = LocalDate.of(2026, 11, 25);
        int[] a = PrayerTimes.minutes(d, 18.3, 42.73, 3, PrayerTimes.Method.UMM_AL_QURA, false, false);
        int[] b = PrayerTimes.minutes(d, 18.3, 42.73, 3, PrayerTimes.Method.UMM_AL_QURA, true, false);
        assertTrue(b[PrayerTimes.ASR] > a[PrayerTimes.ASR] + 30);
    }

    @Test
    public void easternArabicDigits() {
        assertEquals("٠١٢٣٤٥٦٧٨٩", Ar.digits("0123456789"));
        assertEquals("٥:٠٢ ص", Ar.clock(5 * 60 + 2));
        assertEquals("١٢:٠٠ م", Ar.clock(12 * 60));
        assertEquals("١٢:٣٠ ص", Ar.clock(30));
        assertEquals("٧:٠١ م", Ar.clock(19 * 60 + 1));
        assertEquals("٤٥ د", Ar.span(45));
        assertEquals("٢ س ٥ د", Ar.span(125));
    }

    @Test
    public void fixturesParse() throws Exception {
        List<Football.Match> list = Football.schedule(resource("espn_fixtures.json"));
        assertEquals(4, list.size());
        Football.Match m = list.get(0);
        assertEquals("pre", m.state);
        assertEquals("آرسنال", m.home);
        assertEquals("مانشستر سيتي", m.away);
        assertEquals("الدوري الإنجليزي", m.competition);
        assertEquals(java.time.Instant.parse("2026-11-29T16:30:00Z").toEpochMilli(), m.startMs);
        assertEquals("دوري أبطال أوروبا", list.get(3).competition);
    }

    @Test
    public void resultsParseWithScores() throws Exception {
        List<Football.Match> list = Football.schedule(resource("espn_results.json"));
        assertEquals(3, list.size());
        Football.Match newest = list.get(list.size() - 1);
        assertTrue(newest.finished());
        assertFalse(newest.homeScore.isEmpty());
        assertFalse(newest.awayScore.isEmpty());
        assertTrue(list.get(0).startMs < newest.startMs);
        String outcome = Matches.outcome(newest, "382");
        assertTrue(outcome, outcome.equals("فوز") || outcome.equals("تعادل") || outcome.equals("خسارة"));
    }

    @Test
    public void standingsParse() throws Exception {
        List<Football.Row> rows = Football.standings(resource("espn_standings.json"));
        assertEquals(4, rows.size());
        assertEquals(1, rows.get(0).rank);
        assertEquals("آرسنال", rows.get(0).team);
        assertEquals(28, rows.get(0).points);
        assertEquals(11, rows.get(0).played);
    }

    @Test
    public void brokenFeedsGiveNothingNotACrash() {
        assertTrue(Football.schedule("not json").isEmpty());
        assertTrue(Football.schedule("{\"events\":[{\"competitions\":[{}]}]}").isEmpty());
        assertTrue(Football.standings("{}").isEmpty());
        assertNull(Weather.parse("{\"error\":true}"));
    }

    @Test
    public void weatherParse() throws Exception {
        Weather w = Weather.parse(resource("meteo.json"));
        assertNotNull(w);
        assertEquals(23.3, w.temperature, 0.01);
        assertEquals(4, w.days.size());
        assertEquals("صحو غالبًا", Weather.describe(w.code));
    }

    @Test
    public void onlyKnownHostsOverHttps() {
        assertTrue(Net.allowed("https://api.open-meteo.com/v1/forecast?latitude=1"));
        assertTrue(Net.allowed("https://site.api.espn.com/apis/site/v2/sports/soccer/all/teams/382/schedule"));
        assertFalse(Net.allowed("http://api.open-meteo.com/v1/forecast"));
        assertFalse(Net.allowed("https://example.com/"));
        assertFalse(Net.allowed("https://api.open-meteo.com.evil.example/"));
    }

    @Test
    public void defaultClubsAreKnown() {
        for (String id : Teams.DEFAULT.split(",")) assertNotNull(id, Teams.team(id));
        assertEquals("مانشستر سيتي", Teams.team("382").name);
        assertEquals("برشلونة", Teams.team("83").name);
        assertEquals("تشيلسي", Teams.team("363").name);
    }

    /** Every wallpaper comes from an allowed host, with a free licence, an author and a set that exists. */
    @Test
    public void wallpaperManifestIsClean() throws Exception {
        java.io.File f = new java.io.File("src/main/assets/zaytoon/wallpapers.json");
        if (!f.isFile()) f = new java.io.File("launcher/zaytoon/src/main/assets/zaytoon/wallpapers.json");
        org.json.JSONObject m = new org.json.JSONObject(new String(java.nio.file.Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
        java.util.Set<String> sets = new java.util.HashSet<>();
        org.json.JSONArray s = m.getJSONArray("sets");
        for (int i = 0; i < s.length(); i++) sets.add(s.getJSONObject(i).getString("id"));
        org.json.JSONArray items = m.getJSONArray("items");
        assertTrue(items.length() >= 20);
        for (int i = 0; i < items.length(); i++) {
            org.json.JSONObject it = items.getJSONObject(i);
            String url = it.getString("url");
            assertTrue(url, Net.allowed(url));
            assertTrue(url, url.contains("/3840px-"));
            String lic = it.getString("license");
            assertTrue(lic, lic.startsWith("CC BY") || lic.startsWith("CC0") || lic.toLowerCase().contains("public domain"));
            assertFalse(it.getString("artist").isEmpty());
            assertTrue(sets.contains(it.getString("set")));
        }
    }
}
