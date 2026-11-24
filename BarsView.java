package com.sortware.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Draws the colour-coded bars. Fixed size, so the screen never moves while a simulation plays. */
public class BarsView extends View {
    private Sorter.Step s; private int mn, mx;
    private final int[] bc; // blue, yellow, red, green, purple
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float d;

    BarsView(Context c, int[] bc, int textColor) {
        super(c); this.bc = bc; d = c.getResources().getDisplayMetrics().density;
        txt.setTextAlign(Paint.Align.CENTER); txt.setColor(textColor);
    }
    void set(Sorter.Step s, int mn, int mx) { this.s = s; this.mn = mn; this.mx = mx; invalidate(); }

    @Override protected void onDraw(Canvas cv) {
        if (s == null) return;
        int n = s.a.length; float gap = (n > 40 ? 1 : 4) * d, W = getWidth(), H = getHeight();
        float bw = (W - gap * (n - 1)) / n, base = H - 22 * d, avail = base - 20 * d;
        float rng = Math.max(1f, (float) ((long) mx - mn));
        txt.setTextSize((n <= 12 ? 14 : 10) * d);
        for (int i = 0; i < n; i++) {
            int k = Sorter.has(s.swp, i) ? 2 : Sorter.has(s.cmp, i) ? 1 : Sorter.has(s.exm, i) ? 4 : s.srt[i] ? 3 : 0;
            float h = avail * (0.1f + 0.9f * (float) ((long) s.a[i] - mn) / rng), x = i * (bw + gap);
            fill.setColor(bc[k]); cv.drawRect(x, base - h, x + bw, base, fill);
            if (n <= 20) { // numbers and symbols (so colour is never the only cue)
                cv.drawText(String.valueOf(s.a[i]), x + bw / 2, base - h - 4 * d, txt);
                cv.drawText(k == 2 ? "⇄" : k == 1 ? "?" : k == 4 ? "★" : k == 3 ? "✓" : "", x + bw / 2, base + 16 * d, txt);
            }
        }
    }
}
