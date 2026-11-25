package sa.zaytoon;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

/** Zaytoon's look in code: olive glass, quiet gold, soft white text. */
final class Ui {
    static final int GOLD = 0xFFC9A961;
    static final int TEXT = 0xF2FFFFFF;
    static final int DIM = 0x99FFFFFF;
    static final int FAINT = 0x66FFFFFF;
    static final int GLASS = 0x1FFFFFFF;
    static final int GLASS_EDGE = 0x33FFFFFF;
    static final int OLIVE = 0xFF141A12;
    static final int LIVE = 0xFFE0675A;

    private Ui() {
    }

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    static GradientDrawable glass(Context c, float radiusDp, int fill, int edge) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, radiusDp));
        g.setColor(fill);
        g.setStroke(Math.max(1, dp(c, 1)), edge);
        return g;
    }

    static GradientDrawable glass(Context c) {
        return glass(c, 22, GLASS, GLASS_EDGE);
    }

    /** A focusable row: plain glass, and gold-edged brighter glass while focused. */
    static StateListDrawable focusable(Context c, float radiusDp, int accent) {
        StateListDrawable s = new StateListDrawable();
        GradientDrawable on = glass(c, radiusDp, 0x40FFFFFF, accent);
        on.setStroke(dp(c, 2), accent);
        s.addState(new int[] {android.R.attr.state_focused}, on);
        s.addState(new int[] {android.R.attr.state_activated}, glass(c, radiusDp, 0x2EFFFFFF, Color.argb(120, Color.red(accent), Color.green(accent), Color.blue(accent))));
        s.addState(new int[] {}, glass(c, radiusDp, 0x14FFFFFF, 0x1FFFFFFF));
        return s;
    }

    static TextView text(Context c, Typeface face, float sp, int color) {
        TextView t = new TextView(c);
        t.setTypeface(face);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        t.setTextDirection(TextView.TEXT_DIRECTION_RTL);
        t.setIncludeFontPadding(true);
        return t;
    }
}
