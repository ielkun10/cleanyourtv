package sa.zaytoon;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * What the launcher's MainActivity calls: the Zaytoon clock, the glass cards under it, and the panel.
 * Every call is guarded, so a fault in Zaytoon's own code can never take Home down.
 */
public final class Zaytoon {
    /** Rebuilds Home (MainActivity.recreate), used once the first wallpapers have arrived. */
    public interface Host {
        void reload();
    }

    public static final int PANEL_REQUEST = 0x2A7;

    private final Activity activity;
    private final Host host;
    private HomeStrip strip;
    private final Runnable onData = this::refreshStrip;
    private final Handler main = new Handler(Looper.getMainLooper());

    private Zaytoon(Activity activity, Host host) {
        this.activity = activity;
        this.host = host;
    }

    /**
     * @param header the RelativeLayout holding the clock block and the top buttons
     * @param clockBlock the clock, greeting and date block
     * @param clock the TextClock
     * @param body the launcher's Arabic body font
     */
    public static Zaytoon attach(Activity activity, FrameLayout root, ViewGroup header, View clockBlock, TextView clock,
                                 View topButtons, Typeface body, int accent, Host host) {
        Zaytoon z = new Zaytoon(activity, host);
        try {
            z.styleClock(clockBlock, clock);
            z.mirrorHeader(header, clockBlock, topButtons);
            if (Store.prefs(activity).getBoolean(Store.STRIP, true)) z.addStrip(root, header, body, accent == 0 ? Ui.GOLD : accent);
            if (Wallpapers.count(activity) == 0 && !Wallpapers.chosen(activity).isEmpty()) {
                Wallpapers.get().sync(activity, () -> z.main.post(z::reloadIfShown));
            }
        } catch (RuntimeException e) {
            android.util.Log.w("Zaytoon", "attach", e);
        }
        return z;
    }

    private void reloadIfShown() {
        if (!activity.isFinishing() && !activity.isDestroyed()) host.reload();
    }

    /** The big clock in Katibeh (or the Kufi body font), on a pane of olive glass. */
    private void styleClock(View clockBlock, TextView clock) {
        boolean kufi = "kufi".equals(Store.prefs(activity).getString(Store.CLOCK_FONT, "katibeh"));
        if (!kufi) {
            Typeface k = activity.getResources().getFont(R.font.katibeh);
            clock.setTypeface(k);
            clock.setLetterSpacing(0f);
            clock.setFontFeatureSettings(null);
            clock.setIncludeFontPadding(true);
        }
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, Store.clockSizeSp(activity));
        clock.setShadowLayer(Ui.dp(activity, 12), 0, Ui.dp(activity, 2), 0x66000000);
        int ph = Ui.dp(activity, 28), pv = Ui.dp(activity, 14);
        clockBlock.setBackground(Ui.glass(activity, 28, 0x1AFFFFFF, 0x2EFFFFFF));
        clockBlock.setPadding(ph, pv, ph, pv);
    }

    /** Arabic reads from the right: the clock moves to the right edge and the buttons to the left. */
    private void mirrorHeader(ViewGroup header, View clockBlock, View topButtons) {
        if (!(header instanceof RelativeLayout)) return;
        RelativeLayout.LayoutParams c = (RelativeLayout.LayoutParams) clockBlock.getLayoutParams();
        c.removeRule(RelativeLayout.ALIGN_PARENT_START);
        c.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
        c.removeRule(RelativeLayout.START_OF);
        c.removeRule(RelativeLayout.LEFT_OF);
        c.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        c.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        c.setMarginStart(0);
        c.setMarginEnd(0);
        clockBlock.setLayoutParams(c);
        RelativeLayout.LayoutParams b = (RelativeLayout.LayoutParams) topButtons.getLayoutParams();
        b.removeRule(RelativeLayout.ALIGN_PARENT_END);
        b.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        b.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        topButtons.setLayoutParams(b);
    }

    private void addStrip(FrameLayout root, ViewGroup header, Typeface body, int accent) {
        strip = new HomeStrip(activity, body, accent);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT);
        lp.rightMargin = Ui.dp(activity, 58);
        // Above the dock and the grid in drawing order only: it takes no focus and no keys.
        root.addView(strip, lp);
        header.addOnLayoutChangeListener((v, l, t, r, bottom, ol, ot, or, ob) -> {
            int[] at = new int[2];
            int[] rootAt = new int[2];
            v.getLocationInWindow(at);
            root.getLocationInWindow(rootAt);
            int y = at[1] - rootAt[1] + v.getHeight() + Ui.dp(activity, 22);
            if ((int) strip.getTranslationY() != y) strip.setTranslationY(y);
        });
        strip.update();
    }

    private void refreshStrip() {
        if (strip != null) strip.update();
    }

    public void resume() {
        try {
            Hub hub = Hub.get(activity);
            hub.listen(onData);
            hub.refresh(false);
            refreshStrip();
        } catch (RuntimeException e) {
            android.util.Log.w("Zaytoon", "resume", e);
        }
    }

    public void pause() {
        Hub.get(activity).unlisten(onData);
    }

    /** Once a minute from the launcher's clock tick. */
    public void minute() {
        try {
            Hub.get(activity).refresh(false);
            if (strip != null) strip.tick();
        } catch (RuntimeException e) {
            android.util.Log.w("Zaytoon", "minute", e);
        }
    }

    public static Intent panel(Context c) {
        return new Intent(c, ZaytoonActivity.class);
    }

    /** Whether the panel changed something Home has to be rebuilt for. */
    public static boolean needsReload(int requestCode, int resultCode) {
        return requestCode == PANEL_REQUEST && resultCode == Activity.RESULT_OK;
    }
}
