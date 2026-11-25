package sa.zaytoon;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DecimalStyle;
import java.util.Locale;

/** Arabic text helpers: Eastern Arabic digits, Arabic day and month names, 12-hour times with ص and م. */
public final class Ar {
    public static final Locale LOCALE = Locale.forLanguageTag("ar-SA-u-nu-arab");
    private static final DecimalStyle DIGITS = DecimalStyle.of(LOCALE).withZeroDigit('٠');

    private Ar() {
    }

    /** Western digits in s turned into Eastern Arabic ones (٠١٢٣٤٥٦٧٨٩). */
    public static String digits(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            b.append(c >= '0' && c <= '9' ? (char) ('٠' + (c - '0')) : c);
        }
        return b.toString();
    }

    public static String num(long n) {
        return digits(Long.toString(n));
    }

    public static String num(double n, int decimals) {
        NumberFormat f = NumberFormat.getNumberInstance(Locale.US);
        f.setMaximumFractionDigits(decimals);
        f.setMinimumFractionDigits(0);
        f.setGroupingUsed(false);
        return digits(f.format(n));
    }

    private static DateTimeFormatter fmt(String pattern) {
        return DateTimeFormatter.ofPattern(pattern, LOCALE).withDecimalStyle(DIGITS);
    }

    /** "٣:٤٥ م" */
    public static String time(ZonedDateTime t) {
        return fmt("h:mm a").format(t);
    }

    public static String time(long epochMs, ZoneId zone) {
        return time(Instant.ofEpochMilli(epochMs).atZone(zone));
    }

    /** Minutes after midnight as "٥:٠٢ ص". */
    public static String clock(int minutes) {
        int m = ((minutes % 1440) + 1440) % 1440;
        int h = m / 60;
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return digits(h12 + ":" + (m % 60 < 10 ? "0" : "") + (m % 60)) + " " + (h < 12 ? "ص" : "م");
    }

    /** "السبت ٢٩ نوفمبر" */
    public static String day(ZonedDateTime t) {
        return fmt("EEEE d MMMM").format(t);
    }

    /** "سبت ٢٩/١١" */
    public static String shortDay(ZonedDateTime t) {
        return fmt("EEE d/M").format(t);
    }

    /** "٢ س ١٥ د" for a span of minutes, or "١٥ د" under an hour. */
    public static String span(long minutes) {
        if (minutes < 0) minutes = 0;
        long h = minutes / 60, m = minutes % 60;
        if (h == 0) return num(m) + " د";
        return num(h) + " س " + num(m) + " د";
    }
}
