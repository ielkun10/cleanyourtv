package sa.zaytoon;

import android.content.Context;
import android.icu.util.IslamicCalendar;
import android.icu.util.ULocale;

import java.time.ZonedDateTime;

/** Today's prayer times for the chosen city, and which one comes next. */
final class Prayer {
    static final String[] NAMES = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};

    static final class Next {
        int index;
        int minuteOfDay;
        long minutesLeft;
    }

    private Prayer() {
    }

    static boolean ramadan(ZonedDateTime now) {
        try {
            IslamicCalendar cal = new IslamicCalendar(ULocale.forLanguageTag("ar-SA"));
            cal.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
            cal.setTimeInMillis(now.toInstant().toEpochMilli());
            return cal.get(IslamicCalendar.MONTH) == IslamicCalendar.RAMADAN;
        } catch (RuntimeException e) {
            return false;
        }
    }

    static int[] today(Context c, ZonedDateTime now) {
        Places.City city = Store.city(c);
        boolean hanafi = Store.prefs(c).getBoolean(Store.HANAFI, false);
        // Times are worked out in Saudi time and shown on the TV's clock: the same thing on a set kept on Riyadh time.
        return PrayerTimes.minutes(now.toLocalDate(), city.lat, city.lng, Places.UTC_OFFSET, Store.method(c), hanafi, ramadan(now));
    }

    /** The next of the five prayers (sunrise is skipped), tomorrow's Fajr after Isha. */
    static Next next(Context c, ZonedDateTime now) {
        int offsetMin = now.getOffset().getTotalSeconds() / 60 - (int) (Places.UTC_OFFSET * 60);
        int nowMin = now.getHour() * 60 + now.getMinute();
        int[] t = today(c, now);
        Next n = new Next();
        for (int i = 0; i < t.length; i++) {
            if (i == PrayerTimes.SUNRISE) continue;
            int local = t[i] + offsetMin;
            if (local > nowMin) {
                n.index = i;
                n.minuteOfDay = local;
                n.minutesLeft = local - nowMin;
                return n;
            }
        }
        int[] tomorrow = today(c, now.plusDays(1));
        n.index = PrayerTimes.FAJR;
        n.minuteOfDay = tomorrow[PrayerTimes.FAJR] + offsetMin;
        n.minutesLeft = 1440 - nowMin + n.minuteOfDay;
        return n;
    }

    /** "٤ جمادى الآخرة ١٤٤٨ هـ" */
    static String hijri() {
        try {
            ULocale u = ULocale.forLanguageTag("ar-SA-u-nu-arab").setKeywordValue("calendar", "islamic-umalqura");
            IslamicCalendar cal = new IslamicCalendar(u);
            cal.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
            android.icu.text.DateFormat f = android.icu.text.DateFormat.getInstanceForSkeleton(cal, "yMMMMd", u);
            return f.format(cal);
        } catch (RuntimeException e) {
            return "";
        }
    }
}
