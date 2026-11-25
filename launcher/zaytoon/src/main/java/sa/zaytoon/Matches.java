package sa.zaytoon;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** How a match reads in Arabic, on the home card and in the panel. */
final class Matches {
    private Matches() {
    }

    /** "مانشستر سيتي ٢ - ١ آرسنال", or "آرسنال × مانشستر سيتي" before kick-off. Home side first, on the right. */
    static String line(Football.Match m) {
        if (m.live() || m.finished()) {
            return m.home + "  " + Ar.digits(m.homeScore.isEmpty() ? "0" : m.homeScore) + " - "
                    + Ar.digits(m.awayScore.isEmpty() ? "0" : m.awayScore) + "  " + m.away;
        }
        return m.home + "  ×  " + m.away;
    }

    /** "اليوم ٧:٣٠ م", "غدًا ...", "السبت ٢٩ نوفمبر ...", or the live minute, or "انتهت". */
    static String when(Football.Match m, ZoneId zone, LocalDate today) {
        if (m.live()) return "الدقيقة " + Ar.digits(m.detail.replace("'", "")) + "'";
        ZonedDateTime t = Instant.ofEpochMilli(m.startMs).atZone(zone);
        if (m.finished()) return "انتهت · " + Ar.day(t);
        LocalDate d = t.toLocalDate();
        String day = d.equals(today) ? "اليوم" : d.equals(today.plusDays(1)) ? "غدًا" : Ar.day(t);
        return day + " · " + Ar.time(t);
    }

    /** "فوز" / "تعادل" / "خسارة" for the club in a finished match. */
    static String outcome(Football.Match m, String teamId) {
        try {
            int h = Integer.parseInt(m.homeScore.trim());
            int a = Integer.parseInt(m.awayScore.trim());
            if (h == a) return "تعادل";
            boolean home = teamId.equals(m.homeId);
            return (h > a) == home ? "فوز" : "خسارة";
        } catch (NumberFormatException e) {
            return "";
        }
    }
}
