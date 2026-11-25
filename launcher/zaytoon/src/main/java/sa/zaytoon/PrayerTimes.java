package sa.zaytoon;

import java.time.LocalDate;

/**
 * The five prayer times and sunrise, worked out on the TV with no network (the standard astronomical method of
 * praytimes.org). Umm al-Qura is the default: Fajr at 18.5 degrees, Isha 90 minutes after Maghrib, 120 in Ramadan.
 */
public final class PrayerTimes {
    /** Calculation methods: Fajr angle, then Isha as an angle or (ishaMinutes > 0) minutes after Maghrib. */
    public enum Method {
        UMM_AL_QURA(18.5, 0, 90),
        MWL(18, 17, 0),
        EGYPT(19.5, 17.5, 0),
        KARACHI(18, 18, 0),
        ISNA(15, 15, 0);

        final double fajrAngle;
        final double ishaAngle;
        final int ishaMinutes;

        Method(double fajrAngle, double ishaAngle, int ishaMinutes) {
            this.fajrAngle = fajrAngle;
            this.ishaAngle = ishaAngle;
            this.ishaMinutes = ishaMinutes;
        }
    }

    public static final int FAJR = 0, SUNRISE = 1, DHUHR = 2, ASR = 3, MAGHRIB = 4, ISHA = 5;

    private PrayerTimes() {
    }

    /**
     * Minutes after local midnight (in the zone utcOffsetHours) for Fajr, sunrise, Dhuhr, Asr, Maghrib and Isha.
     * @param hanafi Asr when the shadow is twice an object's length (Hanafi), else once (the majority).
     * @param ramadan Umm al-Qura's Isha moves to 120 minutes after Maghrib in Ramadan.
     */
    public static int[] minutes(LocalDate date, double lat, double lng, double utcOffsetHours, Method method,
                                boolean hanafi, boolean ramadan) {
        double jd = julian(date.getYear(), date.getMonthValue(), date.getDayOfMonth()) - lng / (15.0 * 24.0);
        // Two passes: a first guess, then each time worked out with the sun where it is at that time.
        double[] t = {5, 6, 12, 13, 18, 18};
        for (int pass = 0; pass < 2; pass++) {
            double[] d = new double[6];
            for (int i = 0; i < 6; i++) d[i] = t[i] / 24.0;
            t = new double[] {
                    sunAngleTime(jd, method.fajrAngle, d[0], lat, true),
                    sunAngleTime(jd, 0.833, d[1], lat, true),
                    midDay(jd, d[2]),
                    asrTime(jd, hanafi ? 2 : 1, d[3], lat),
                    sunAngleTime(jd, 0.833, d[4], lat, false),
                    method.ishaMinutes > 0 ? 0 : sunAngleTime(jd, method.ishaAngle, d[5], lat, false),
            };
        }
        double shift = utcOffsetHours - lng / 15.0;
        int[] out = new int[6];
        for (int i = 0; i < 6; i++) out[i] = (int) Math.round((t[i] + shift) * 60.0);
        if (method.ishaMinutes > 0) out[ISHA] = out[MAGHRIB] + (ramadan && method == Method.UMM_AL_QURA ? 120 : method.ishaMinutes);
        return out;
    }

    static double julian(int year, int month, int day) {
        if (month <= 2) {
            year -= 1;
            month += 12;
        }
        double a = Math.floor(year / 100.0);
        double b = 2 - a + Math.floor(a / 4.0);
        return Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + b - 1524.5;
    }

    /** {declination in degrees, equation of time in hours}. */
    static double[] sun(double jd) {
        double d = jd - 2451545.0;
        double g = fixAngle(357.529 + 0.98560028 * d);
        double q = fixAngle(280.459 + 0.98564736 * d);
        double l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g));
        double e = 23.439 - 0.00000036 * d;
        double ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15.0;
        double eqt = q / 15.0 - fixHour(ra);
        double decl = darcsin(dsin(e) * dsin(l));
        return new double[] {decl, eqt};
    }

    static double midDay(double jd, double t) {
        return fixHour(12 - sun(jd + t)[1]);
    }

    static double sunAngleTime(double jd, double angle, double t, double lat, boolean beforeNoon) {
        double decl = sun(jd + t)[0];
        double noon = midDay(jd, t);
        double cos = (-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat));
        cos = Math.max(-1, Math.min(1, cos));
        double h = Math.toDegrees(Math.acos(cos)) / 15.0;
        return noon + (beforeNoon ? -h : h);
    }

    static double asrTime(double jd, int factor, double t, double lat) {
        double decl = sun(jd + t)[0];
        double angle = -Math.toDegrees(Math.atan(1.0 / (factor + Math.tan(Math.toRadians(Math.abs(lat - decl))))));
        return sunAngleTime(jd, angle, t, lat, false);
    }

    private static double dsin(double d) {
        return Math.sin(Math.toRadians(d));
    }

    private static double dcos(double d) {
        return Math.cos(Math.toRadians(d));
    }

    private static double darcsin(double x) {
        return Math.toDegrees(Math.asin(x));
    }

    private static double darctan2(double y, double x) {
        return Math.toDegrees(Math.atan2(y, x));
    }

    private static double fixAngle(double a) {
        a = a - 360.0 * Math.floor(a / 360.0);
        return a < 0 ? a + 360.0 : a;
    }

    private static double fixHour(double a) {
        a = a - 24.0 * Math.floor(a / 24.0);
        return a < 0 ? a + 24.0 : a;
    }
}
