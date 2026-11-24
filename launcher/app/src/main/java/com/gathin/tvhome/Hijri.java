package com.gathin.tvhome;

import android.icu.text.DateFormat;
import android.icu.util.IslamicCalendar;
import android.icu.util.ULocale;

import java.util.Locale;

/**
 * Today's Hijri date by the Umm al-Qura calendar (Saudi Arabia's civil calendar), e.g. "٣ جمادى الآخرة ١٤٤٨".
 * Android's ICU (API 24+) does the arithmetic and the month names; an Arabic locale gives Arabic-Indic digits.
 */
final class Hijri {
    private Hijri() {
    }

    static String today(Locale locale) {
        ULocale u = ULocale.forLocale(locale).setKeywordValue("calendar", "islamic-umalqura");
        IslamicCalendar cal = new IslamicCalendar(u);
        cal.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
        DateFormat f = DateFormat.getInstanceForSkeleton(cal, "yMMMMd", u);
        return f.format(cal);
    }
}
