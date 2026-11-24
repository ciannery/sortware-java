package com.sortware.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Random;

public class MainActivity extends Activity {
    static abstract class W implements TextWatcher {
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        public void onTextChanged(CharSequence s, int a, int b, int c) {}
    }
    static abstract class SB implements SeekBar.OnSeekBarChangeListener {
        public void onStartTrackingTouch(SeekBar s) {}
        public void onStopTrackingTouch(SeekBar s) {}
    }
    static class Bm { String k; double t; int c, s, w; boolean ok; }

    // ---- settings ----
    SharedPreferences pr; int themeMode, vol, speed; boolean sound;
    // ---- theme ----
    boolean dark; int bg, card, tx, mu, ac, bd; int[] bc;
    // ---- navigation ----
    String screen = "home"; FrameLayout content; LinearLayout nav;
    // ---- data / simulation state ----
    String input = "2, 8, 5, 3, 9, 4, 1", applied = "", randSize = "10", error = "", menuErr = "", algo = "";
    int[] data; Sorter.Result res; int idx; boolean playing, showAll, showSim, busy; double time; int tab;
    final Handler h = new Handler(Looper.getMainLooper());
    // ---- simulation views ----
    FrameLayout simHolder; BarsView bars; TextView tvLab, tvMsg, tvPlain, tvDone, tvStats, tvCode, tvMenuErr; TextView[] tabs;
    Button bPrev, bPlay, bPause, bNext, bRst, bAll; ListView stepsList; BaseAdapter stepsAd; View[] tabViews; int mn, mx;
    // ---- compare state ----
    int scen; String cmpSize = "500", cmpErr = ""; Bm[] cmpRows; boolean cmpBusy;

    ToneGenerator tg;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        pr = getSharedPreferences("sortware", MODE_PRIVATE);
        themeMode = pr.getInt("theme", 0); sound = pr.getBoolean("sound", false); vol = pr.getInt("vol", 40); speed = pr.getInt("speed", 5);
        applyTheme(); setContentView(buildRoot()); show("home");
    }
    @Override public void onConfigurationChanged(Configuration c) { super.onConfigurationChanged(c); if (themeMode == 0) rebuild(); }
    @Override protected void onDestroy() { super.onDestroy(); stop(); if (tg != null) tg.release(); }
    @Override public void onBackPressed() {
        if (screen.equals("vis") && showSim) { stop(); showSim = false; show("vis"); }
        else if (!screen.equals("home")) show("home");
        else super.onBackPressed();
    }
    void save() { pr.edit().putInt("theme", themeMode).putBoolean("sound", sound).putInt("vol", vol).putInt("speed", speed).apply(); }
    void rebuild() { stop(); applyTheme(); setContentView(buildRoot()); show(screen); }

    void applyTheme() {
        dark = themeMode == 2 || (themeMode == 0 && (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES);
        if (dark) { bg = 0xFF0F1115; card = 0xFF1A1D24; tx = 0xFFE6E8EC; mu = 0xFF9AA3B2; ac = 0xFF5B8DEF; bd = 0xFF2C313B;
            bc = new int[]{0xFF5B8DEF, 0xFFD4A20A, 0xFFE0575C, 0xFF3FB56A, 0xFFA78BFA}; }
        else { bg = 0xFFF5F6F8; card = 0xFFFFFFFF; tx = 0xFF1B1E24; mu = 0xFF5B6472; ac = 0xFF2563EB; bd = 0xFFD9DDE4;
            bc = new int[]{0xFF3B82F6, 0xFFEAB308, 0xFFEF4444, 0xFF22C55E, 0xFF8B5CF6}; }
        getWindow().setStatusBarColor(card);
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    // ---------- sound (off by default; only plays after the user interacts) ----------
    void tone(int kind, int ms) {
        if (!sound || vol == 0) return;
        try { if (tg == null) tg = new ToneGenerator(AudioManager.STREAM_MUSIC, vol); tg.startTone(kind, ms); } catch (Exception e) { }
    }
    void click() { tone(ToneGenerator.TONE_DTMF_5, 40); }
    void stepSound() {
        Sorter.Step s = res.steps.get(idx);
        if (idx == res.steps.size() - 1) tone(ToneGenerator.TONE_PROP_ACK, 200);
        else if (s.swp.length > 0) tone(ToneGenerator.TONE_DTMF_0, 70);
        else if (s.cmp.length > 0) tone(ToneGenerator.TONE_DTMF_9, 30);
    }

    // ---------- small UI helpers ----------
    int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    GradientDrawable box(int fill, int stroke, float r) { GradientDrawable g = new GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(r)); if (stroke != 0) g.setStroke(dp(1), stroke); return g; }
    TextView tv(String s, float sp, boolean bold, int color) { TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); if (bold) t.setTypeface(null, Typeface.BOLD); return t; }
    TextView head(String s) { return tv(s, 17, true, tx); }
    TextView muted(String s) { return tv(s, 13, false, mu); }
    Button btn(String s, View.OnClickListener l) {
        Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setTextColor(0xFFFFFFFF); b.setBackground(box(ac, 0, 10));
        b.setMinHeight(0); b.setMinimumHeight(0); b.setPadding(dp(12), dp(10), dp(12), dp(10)); b.setStateListAnimator(null); b.setOnClickListener(l); return b;
    }
    Button obtn(String s, View.OnClickListener l) { Button b = btn(s, l); b.setTextColor(ac); b.setBackground(box(0, ac, 10)); return b; }
    void sel(Button b, boolean on) { b.setTextColor(on ? 0xFFFFFFFF : ac); b.setBackground(box(on ? ac : 0, ac, 10)); }
    void en(Button b, boolean e) { b.setEnabled(e); b.setAlpha(e ? 1f : 0.4f); }
    TextView link(String s, View.OnClickListener l) { TextView t = tv(s, 14, true, ac); t.setPadding(dp(10), dp(10), dp(10), dp(10)); t.setOnClickListener(l); return t; }
    LinearLayout cardView() { LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setBackground(box(card, bd, 12)); c.setPadding(dp(12), dp(10), dp(12), dp(10)); return c; }
    LinearLayout col() { LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(16), dp(16), dp(16), dp(16)); return c; }
    ScrollView wrap(View c) { ScrollView sv = new ScrollView(this); sv.setFillViewport(true); sv.addView(c); return sv; }
    LinearLayout.LayoutParams lp(int w, int hh, float wt) { return new LinearLayout.LayoutParams(w, hh, wt); }
    void add(LinearLayout p, View v) { LinearLayout.LayoutParams l = lp(-1, -2, 0); l.bottomMargin = dp(10); p.addView(v, l); }
    LinearLayout row() { LinearLayout r = new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); return r; }
    EditText edit(String text, String hint) {
        EditText e = new EditText(this); e.setText(text); e.setHint(hint); e.setTextColor(tx); e.setHintTextColor(mu);
        e.setBackground(box(card, bd, 8)); e.setPadding(dp(10), dp(8), dp(10), dp(8)); return e;
    }
    static String join(int[] a, int max) { StringBuilder b = new StringBuilder(); for (int i = 0; i < a.length && i < max; i++) { if (i > 0) b.append(", "); b.append(a[i]); } if (a.length > max) b.append(" …"); return b.toString(); }

    // ---------- root + navigation ----------
    View buildRoot() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(bg);
        LinearLayout top = row(); top.setBackgroundColor(card); top.setPadding(dp(14), dp(4), dp(4), dp(4));
        top.addView(tv("📊 SORTWARE", 18, true, tx), lp(0, -2, 1));
        top.addView(link("❓ Guide", v -> show("guide")));
        root.addView(top);
        content = new FrameLayout(this); root.addView(content, lp(-1, 0, 1));
        nav = new LinearLayout(this); nav.setBackgroundColor(card);
        String[][] items = {{"home", "🏠", "Home"}, {"vis", "▶", "Visualize"}, {"algo", "📚", "Algorithms"}, {"cmp", "⚖", "Compare"}, {"set", "⚙", "Settings"}};
        for (String[] it : items) {
            TextView t = tv(it[1] + "\n" + it[2], 11, false, mu); t.setGravity(Gravity.CENTER); t.setPadding(0, dp(6), 0, dp(6)); t.setTag(it[0]);
            t.setOnClickListener(v -> { click(); show(it[0]); }); nav.addView(t, lp(0, -2, 1));
        }
        root.addView(nav);
        return root;
    }
    void show(String s) {
        stop(); screen = s; bars = null; tvMenuErr = null; content.removeAllViews();
        View v;
        switch (s) {
            case "vis": v = (showSim && !algo.isEmpty() && data != null) ? sim() : setup(); break;
            case "algo": v = algoScreen(); break;
            case "cmp": v = compare(); break;
            case "set": v = settings(); break;
            case "guide": v = guide(); break;
            default: v = home();
        }
        content.addView(v, new FrameLayout.LayoutParams(-1, -1));
        for (int i = 0; i < nav.getChildCount(); i++) {
            TextView t = (TextView) nav.getChildAt(i); boolean on = t.getTag().equals(s);
            t.setTextColor(on ? ac : mu); t.setTypeface(null, on ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    // ---------- Home ----------
    View home() {
        LinearLayout c = col();
        add(c, tv("Learn sorting, one step at a time", 22, true, tx));
        add(c, tv("Sorting means putting a list in order, like smallest to largest. Watch six famous methods do it live, with a plain-English explanation of every move.", 15, false, tx));
        add(c, btn("▶ Start visualizing", v -> { click(); show("vis"); }));
        add(c, obtn("New here? Read the guide", v -> show("guide")));
        LinearLayout k = cardView();
        add(k, head("Not sure where to start?"));
        add(k, tv("Try Bubble Sort (the simplest idea) or Insertion Sort (just like sorting playing cards).", 14, false, tx));
        add(k, btn("Watch Bubble Sort", v -> pick("bubble")));
        add(k, obtn("Watch Insertion Sort", v -> pick("insertion")));
        add(c, k);
        return wrap(c);
    }

    // ---------- Setup: unlimited comma-separated numbers + numbered menu (type OR tap) ----------
    boolean applyInput(String text) {
        try { data = Sorter.parse(text); applied = text; error = ""; return true; }
        catch (IllegalArgumentException e) { error = e.getMessage(); return false; }
    }
    void random() {
        int n; try { n = Integer.parseInt(randSize.trim()); } catch (Exception e) { n = 10; }
        if (n < 2) n = 2;
        int hi = n <= 20 ? 100 : 1000; Random r = new Random(); StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) { if (i > 0) b.append(", "); b.append(r.nextInt(hi)); }
        input = b.toString(); applyInput(input); click(); show("vis");
    }
    void clearAll() { stop(); input = ""; applied = ""; error = ""; data = null; algo = ""; res = null; showSim = false; show("vis"); }

    View setup() {
        LinearLayout c = col();
        add(c, head("1. Your numbers"));
        EditText et = edit(input, "Numbers separated by commas");
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE); et.setMinLines(2); et.setMaxLines(6); et.setGravity(Gravity.TOP);
        et.addTextChangedListener(new W() { public void afterTextChanged(Editable e) { input = e.toString(); } });
        add(c, et);
        LinearLayout r1 = row();
        Button use = btn("Use these numbers", v -> { click(); applyInput(input); show("vis"); });
        Button clr = obtn("Clear", v -> clearAll());
        r1.addView(use, lp(0, -2, 1)); LinearLayout.LayoutParams cl = lp(-2, -2, 0); cl.leftMargin = dp(8); r1.addView(clr, cl); add(c, r1);
        LinearLayout r2 = row();
        Button rnd = obtn("🎲 Random numbers", v -> random());
        EditText rs = edit(randSize, "How many"); rs.setInputType(InputType.TYPE_CLASS_NUMBER); rs.setSingleLine(true);
        rs.addTextChangedListener(new W() { public void afterTextChanged(Editable e) { randSize = e.toString(); } });
        r2.addView(rnd, lp(0, -2, 1)); LinearLayout.LayoutParams rl = lp(dp(100), -2, 0); rl.leftMargin = dp(8); r2.addView(rs, rl); add(c, r2);
        if (!error.isEmpty()) add(c, tv(error, 14, false, 0xFFE0575C));
        add(c, muted(data != null && input.equals(applied) ? "✔ " + data.length + " numbers ready."
            : "Type as many numbers as you like. Lists up to " + Sorter.ANIM_LIMIT + " numbers are animated; bigger lists show the real results only."));
        add(c, head("2. Pick an algorithm"));
        add(c, muted("Tap a button, or type its number below."));
        for (int i = 0; i < 6; i++) {
            final String k = Sorter.KEYS[i];
            Button b = obtn((i + 1) + ".  " + Sorter.NAME[i], v -> pick(k)); b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START); add(c, b);
        }
        LinearLayout r3 = row();
        EditText num = edit("", "Or type 1–6"); num.setInputType(InputType.TYPE_CLASS_NUMBER); num.setSingleLine(true);
        num.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2)});
        num.setImeOptions(EditorInfo.IME_ACTION_GO);
        num.setOnEditorActionListener((v, a, e) -> { pickNumber(num.getText().toString()); return true; });
        Button go = btn("Go", v -> pickNumber(num.getText().toString()));
        r3.addView(num, lp(0, -2, 1)); LinearLayout.LayoutParams gl = lp(-2, -2, 0); gl.leftMargin = dp(8); r3.addView(go, gl); add(c, r3);
        tvMenuErr = tv(menuErr, 14, false, 0xFFE0575C); add(c, tvMenuErr);
        return wrap(c);
    }
    void pickNumber(String t) {
        int n; try { n = Integer.parseInt(t.trim()); } catch (Exception e) { n = -1; }
        if (n < 1 || n > 6) { menuErr = "Type a number from 1 to 6."; if (tvMenuErr != null) tvMenuErr.setText(menuErr); }
        else pick(Sorter.KEYS[n - 1]);
    }
    void pick(String k) {
        if (data == null || !input.equals(applied)) { if (!applyInput(input)) { show("vis"); return; } }
        click(); algo = k; menuErr = ""; showSim = true; compute(k); show("vis");
    }
    void compute(final String k) {
        stop(); final int[] d = data; busy = true; res = null;
        new Thread(null, () -> {
            Sorter.Result r = Sorter.run(k, d, d.length <= Sorter.ANIM_LIMIT); double t = Sorter.timeIt(k, d);
            runOnUiThread(() -> {
                if (!k.equals(algo) || d != data) return;
                res = r; time = t; idx = 0; busy = false;
                if (screen.equals("vis") && showSim) fillSim();
            });
        }, "sortware-worker", 256L * 1024 * 1024).start(); // big stack: deep recursion on huge lists
    }

    // ---------- Simulation: fixed layout, nothing scrolls or jumps while it plays ----------
    View sim() {
        LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.VERTICAL); r.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout hd = row();
        hd.addView(link("◀ Change", v -> { stop(); showSim = false; show("vis"); }));
        hd.addView(tv(Sorter.NAME[Sorter.keyIndex(algo)], 16, true, tx), lp(0, -2, 1));
        hd.addView(muted(data.length + " numbers")); r.addView(hd);
        simHolder = new FrameLayout(this); r.addView(simHolder, lp(-1, 0, 1)); fillSim();
        return r;
    }
    void fillSim() {
        simHolder.removeAllViews();
        if (busy || res == null) { simHolder.addView(new ProgressBar(this), new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER)); return; }
        if (res.steps.isEmpty()) { simHolder.addView(bigResult(), new FrameLayout.LayoutParams(-1, -1)); return; }
        mn = Integer.MAX_VALUE; mx = Integer.MIN_VALUE; for (int v : data) { mn = Math.min(mn, v); mx = Math.max(mx, v); }
        simHolder.addView(simBody(), new FrameLayout.LayoutParams(-1, -1)); updateSim(false);
    }
    View bigResult() {
        LinearLayout c = col(); c.setPadding(0, dp(8), 0, dp(8));
        LinearLayout k = cardView(); k.addView(tv("This list has more than " + Sorter.ANIM_LIMIT + " numbers, which is too many to animate clearly. Here are the real results from sorting all " + data.length + " numbers.", 14, false, tx)); add(c, k);
        add(c, tv("Comparisons: " + res.c + "\nSwaps: " + res.s + "\nAssignments / moves: " + res.w + "\nPasses / iterations: " + res.p + "\nExecution time: " + String.format("%.4f", time) + " ms", 15, true, tx));
        add(c, muted("First 100 sorted values:")); add(c, tv(join(res.a, 100), 13, false, tx));
        return wrap(c);
    }
    View simBody() {
        LinearLayout b = new LinearLayout(this); b.setOrientation(LinearLayout.VERTICAL);
        HorizontalScrollView hs = new HorizontalScrollView(this); hs.setHorizontalScrollBarEnabled(false);
        LinearLayout lg = row(); String[] ln = {"Waiting", "Comparing ?", "Swap/move ⇄", "Sorted ✓", "Special ★"};
        for (int i = 0; i < 5; i++) { View sq = new View(this); sq.setBackground(box(bc[i], 0, 2)); lg.addView(sq, new LinearLayout.LayoutParams(dp(11), dp(11))); lg.addView(tv(" " + ln[i] + "   ", 12, false, tx)); }
        hs.addView(lg); b.addView(hs, lp(-1, -2, 0));
        bars = new BarsView(this, bc, tx); b.addView(bars, lp(-1, 0, 1));
        LinearLayout ex = cardView(); tvLab = tv("", 12, false, mu); tvMsg = tv("", 15, true, tx); tvPlain = tv("", 13, false, mu);
        tvMsg.setMaxLines(2); tvMsg.setEllipsize(TextUtils.TruncateAt.END); tvPlain.setMaxLines(3); tvPlain.setEllipsize(TextUtils.TruncateAt.END);
        ex.addView(tvLab); ex.addView(tvMsg); ex.addView(tvPlain); b.addView(ex, lp(-1, dp(108), 0));
        tvDone = tv("", 13, true, bc[3]); tvDone.setGravity(Gravity.CENTER_VERTICAL); b.addView(tvDone, lp(-1, dp(28), 0));

        LinearLayout r1 = new LinearLayout(this);
        bPrev = sbtn("◀ Prev", v -> { stop(); if (idx > 0) { idx--; updateSim(true); } });
        bPlay = sbtn("▶ Play", v -> { if (idx >= res.steps.size() - 1) idx = 0; playing = true; updateSim(false); h.postDelayed(tick, delay()); });
        bPause = sbtn("⏸ Pause", v -> { stop(); });
        bNext = sbtn("Next ▶", v -> { stop(); if (idx < res.steps.size() - 1) { idx++; updateSim(true); } });
        for (Button x : new Button[]{bPrev, bPlay, bPause, bNext}) { LinearLayout.LayoutParams l = lp(0, -2, 1); l.setMargins(dp(2), 0, dp(2), 0); r1.addView(x, l); }
        b.addView(r1, lp(-1, -2, 0));
        LinearLayout r2 = row(); bRst = sbtn("⟲ Restart", v -> { stop(); idx = 0; updateSim(false); }); r2.addView(bRst);
        r2.addView(tv(" 🐢", 12, false, tx));
        SeekBar sk = new SeekBar(this); sk.setMax(9); sk.setProgress(speed - 1);
        sk.setOnSeekBarChangeListener(new SB() { public void onProgressChanged(SeekBar s, int p, boolean u) {
            if (!u) return; speed = p + 1; save(); if (playing) { h.removeCallbacks(tick); h.postDelayed(tick, delay()); } } });
        r2.addView(sk, lp(0, -2, 1)); r2.addView(tv("🐇 ", 12, false, tx)); b.addView(r2, lp(-1, -2, 0));

        // bottom panel: Steps | Stats | Code | Learn (each scrolls inside its own box)
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        LinearLayout tr = row(); String[] tn = {"Steps", "Stats", "Code", "Learn"}; tabs = new TextView[4];
        for (int i = 0; i < 4; i++) { final int ti = i; tabs[i] = link(tn[i], v -> showTab(ti)); tr.addView(tabs[i]); }
        tr.addView(new View(this), lp(0, 1, 1));
        bAll = sbtn("Show all steps", v -> { showAll = !showAll; bAll.setText(showAll ? "Up to current" : "Show all steps"); updateSim(false); });
        bAll.setBackground(box(0, ac, 8)); bAll.setTextColor(ac); tr.addView(bAll); panel.addView(tr);
        FrameLayout fr = new FrameLayout(this);
        stepsList = new ListView(this); stepsList.setDivider(null);
        stepsAd = new BaseAdapter() {
            public int getCount() { return res == null ? 0 : (showAll ? res.steps.size() : idx + 1); }
            public Object getItem(int i) { return null; }
            public long getItemId(int i) { return i; }
            public View getView(int i, View cv, ViewGroup p) {
                TextView t = cv instanceof TextView ? (TextView) cv : new TextView(MainActivity.this);
                Sorter.Step st = res.steps.get(i);
                t.setText("#" + i + " " + st.lab + ": " + st.msg + " → [" + join(st.a, 100) + "]"); t.setTextSize(12); t.setTypeface(Typeface.MONOSPACE);
                t.setTextColor(i == idx ? 0xFF000000 : tx); t.setBackgroundColor(i == idx ? bc[1] : 0); t.setPadding(dp(4), dp(3), dp(4), dp(3)); return t;
            }
        };
        stepsList.setAdapter(stepsAd);
        stepsList.setOnItemClickListener((p, v, pos, id) -> { stop(); idx = pos; updateSim(false); });
        tvStats = tv("", 13, false, tx); tvCode = tv("", 11, false, tx); tvCode.setTypeface(Typeface.MONOSPACE); tvCode.setHorizontallyScrolling(true);
        TextView learn = tv(Sorter.OV[Sorter.keyIndex(algo)][0] + "\n\n" + Sorter.OV[Sorter.keyIndex(algo)][1] + "\n\n" + learnInfo(), 13, false, tx);
        HorizontalScrollView hc = new HorizontalScrollView(this); hc.addView(tvCode);
        tabViews = new View[]{stepsList, wrap0(tvStats), wrap0(hc), wrap0(learn)};
        for (View v : tabViews) fr.addView(v, new FrameLayout.LayoutParams(-1, -1));
        panel.addView(fr, lp(-1, 0, 1)); b.addView(panel, lp(-1, 0, 0.9f));
        showTab(tab);
        return b;
    }
    String learnInfo() { String[] f = Sorter.INFO[Sorter.keyIndex(algo)]; return "Best " + f[0] + " · Average " + f[1] + " · Worst " + f[2] + " · Memory " + f[3] + " · " + f[4]; }
    ScrollView wrap0(View v) { ScrollView sv = new ScrollView(this); sv.addView(v); return sv; }
    Button sbtn(String s, View.OnClickListener l) { Button b = btn(s, l); b.setTextSize(12); b.setPadding(dp(4), dp(8), dp(4), dp(8)); b.setSingleLine(true); return b; }
    void showTab(int i) {
        tab = i; for (int k = 0; k < 4; k++) { tabViews[k].setVisibility(k == i ? View.VISIBLE : View.GONE); tabs[k].setTextColor(k == i ? ac : mu); tabs[k].setTypeface(null, k == i ? Typeface.BOLD : Typeface.NORMAL); }
        bAll.setVisibility(i == 0 ? View.VISIBLE : View.INVISIBLE);
    }
    int delay() { return 1100 - speed * 100; }
    final Runnable tick = new Runnable() { public void run() {
        if (!playing || res == null) return;
        if (idx < res.steps.size() - 1) { idx++; if (idx >= res.steps.size() - 1) playing = false; updateSim(true); if (playing) h.postDelayed(this, delay()); }
        else { playing = false; updateSim(false); }
    } };
    void stop() { playing = false; h.removeCallbacks(tick); if (bars != null && res != null) refreshButtons(); }
    void refreshButtons() {
        boolean last = idx >= res.steps.size() - 1;
        en(bPrev, idx > 0); en(bNext, !last); en(bPlay, !playing); en(bPause, playing); en(bRst, idx > 0 || playing);
        bPlay.setText(last ? "↻ Replay" : "▶ Play");
    }
    void stat(SpannableStringBuilder s, String title, String desc) {
        int a = s.length(); s.append(title).append('\n'); s.setSpan(new StyleSpan(Typeface.BOLD), a, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (!desc.isEmpty()) { int b = s.length(); s.append(desc).append('\n'); s.setSpan(new RelativeSizeSpan(0.85f), b, s.length(), 0); s.setSpan(new ForegroundColorSpan(mu), b, s.length(), 0); }
        s.append('\n');
    }
    void updateSim(boolean snd) {
        if (res == null || bars == null) return;
        Sorter.Step s = res.steps.get(idx); boolean last = idx >= res.steps.size() - 1;
        bars.set(s, mn, mx); tvLab.setText(s.lab); tvMsg.setText(s.msg); tvPlain.setText(Sorter.plain(s.msg));
        tvDone.setText(last ? "✅ Sorted! " + s.c + " comparisons, " + s.s + " swaps, " + s.w + " moves." : "");
        refreshButtons();
        SpannableStringBuilder st = new SpannableStringBuilder();
        stat(st, "Comparisons: " + s.c, "Times two numbers were checked against each other.");
        stat(st, "Swaps: " + s.s, "Times two numbers traded places.");
        stat(st, "Assignments / moves: " + s.w, "Times a number was written into a position (a swap counts as 2).");
        stat(st, "Passes / iterations: " + s.p, "Rounds the algorithm repeated.");
        stat(st, "Step: " + idx + " / " + (res.steps.size() - 1), "");
        stat(st, "Execution time: " + String.format("%.4f", time) + " ms", "Real measured time without the animation.");
        stat(st, "Original: " + join(data, 100), ""); stat(st, "Sorted result: " + (last ? join(res.a, 100) : "— (finish to reveal)"), "");
        tvStats.setText(st);
        String[] ps = Sorter.PSEUDO[Sorter.keyIndex(algo)]; int ln = Sorter.lineOf(algo, s);
        SpannableStringBuilder cs = new SpannableStringBuilder();
        for (int i = 0; i < ps.length; i++) {
            int a = cs.length(); cs.append(String.format("%2d  ", i + 1)).append(ps[i]).append('\n');
            if (i == ln) { cs.setSpan(new BackgroundColorSpan(bc[1]), a, cs.length() - 1, 0); cs.setSpan(new ForegroundColorSpan(0xFF000000), a, cs.length() - 1, 0); }
        }
        tvCode.setText(cs);
        stepsAd.notifyDataSetChanged(); stepsList.post(() -> stepsList.setSelection(Math.max(0, idx - 3)));
        if (snd) stepSound();
    }

    // ---------- Algorithms ----------
    View algoScreen() {
        LinearLayout c = col();
        add(c, head("The six algorithms"));
        add(c, muted("Each one is a different strategy for putting a list in order. Smaller growth (like O(n)) means faster."));
        for (int i = 0; i < 6; i++) {
            final String k = Sorter.KEYS[i]; String[] f = Sorter.INFO[i];
            LinearLayout cv = cardView();
            add(cv, head((i + 1) + ". " + Sorter.NAME[i]));
            add(cv, tv(Sorter.OV[i][0], 14, false, tx));
            add(cv, muted("Best " + f[0] + " · Average " + f[1] + " · Worst " + f[2] + " · Memory " + f[3] + " · " + f[4]));
            add(cv, muted("👍 " + f[7] + "\n👎 " + f[8]));
            TextView more = tv(Sorter.OV[i][1] + "\n\n" + f[6], 13, false, tx); more.setVisibility(View.GONE); add(cv, more);
            LinearLayout r = row();
            r.addView(link("Learn more", v -> { boolean o = more.getVisibility() == View.GONE; more.setVisibility(o ? View.VISIBLE : View.GONE); ((TextView) v).setText(o ? "Show less" : "Learn more"); }));
            r.addView(new View(this), lp(0, 1, 1));
            r.addView(btn("▶ Watch it work", v -> pick(k))); cv.addView(r);
            add(c, cv);
        }
        return wrap(c);
    }

    // ---------- Compare (real measurements) ----------
    View compare() {
        LinearLayout c = col();
        add(c, head("⚖ Speed test")); add(c, muted("Runs every algorithm on the same real data and measures it. Nothing is faked."));
        LinearLayout sr = new LinearLayout(this); String[] sn = {"Random", "Sorted", "Reverse", "Mine"};
        for (int i = 0; i < 4; i++) { final int si = i; Button b = obtn(sn[i], v -> { scen = si; show("cmp"); }); b.setTextSize(13); sel(b, scen == i);
            LinearLayout.LayoutParams l = lp(0, -2, 1); l.setMargins(dp(2), 0, dp(2), 0); sr.addView(b, l); }
        add(c, sr);
        EditText sz = edit(cmpSize, "How many numbers"); sz.setInputType(InputType.TYPE_CLASS_NUMBER); sz.setSingleLine(true);
        sz.addTextChangedListener(new W() { public void afterTextChanged(Editable e) { cmpSize = e.toString(); } });
        if (scen < 3) add(c, sz); else add(c, muted("Uses the numbers from the Visualize tab."));
        add(c, btn(cmpBusy ? "Running…" : "Run test", v -> runCompare()));
        if (!cmpErr.isEmpty()) add(c, tv(cmpErr, 14, false, 0xFFE0575C));
        if (cmpBusy) add(c, new ProgressBar(this));
        if (cmpRows != null && !cmpBusy) {
            double bt = Double.MAX_VALUE; int bcmp = Integer.MAX_VALUE, bsw = Integer.MAX_VALUE;
            for (Bm m : cmpRows) { bt = Math.min(bt, m.t); bcmp = Math.min(bcmp, m.c); bsw = Math.min(bsw, m.s); }
            StringBuilder f1 = new StringBuilder(), f2 = new StringBuilder(), f3 = new StringBuilder();
            for (Bm m : cmpRows) { String nm = Sorter.NAME[Sorter.keyIndex(m.k)];
                if (m.t == bt) f1.append(f1.length() > 0 ? ", " : "").append(nm); if (m.c == bcmp) f2.append(f2.length() > 0 ? ", " : "").append(nm); if (m.s == bsw) f3.append(f3.length() > 0 ? ", " : "").append(nm); }
            LinearLayout sm = cardView(); add(sm, head("Results"));
            add(sm, tv("🏆 Fastest measured: " + f1 + "\nFewest comparisons: " + f2 + "\nFewest swaps: " + f3, 14, false, tx)); add(c, sm);
            for (Bm m : cmpRows) { LinearLayout cv = cardView();
                add(cv, tv(Sorter.NAME[Sorter.keyIndex(m.k)] + (m.ok ? "  ✓ verified" : "  ✗ NOT sorted"), 15, true, tx));
                add(cv, muted("Time " + String.format("%.4f", m.t) + " ms · Comparisons " + m.c + " · Swaps " + m.s + " · Moves " + m.w + " · Memory " + Sorter.AUX[Sorter.keyIndex(m.k)])); add(c, cv); }
            add(c, muted("No algorithm is always best: it depends on size, arrangement and hardware. A swap counts as 2 moves."));
        }
        return wrap(c);
    }
    void runCompare() {
        cmpErr = ""; int n; try { n = Integer.parseInt(cmpSize.trim()); } catch (Exception e) { n = 0; }
        final int[] d;
        if (scen == 3) { if (data == null) { cmpErr = "Go to Visualize and enter your numbers first."; show("cmp"); return; } d = data; }
        else {
            if (n < 2) { cmpErr = "Enter at least 2."; show("cmp"); return; }
            d = new int[n]; Random r = new Random();
            for (int i = 0; i < n; i++) d[i] = scen == 0 ? r.nextInt(1000) : scen == 1 ? i : n - i;
        }
        cmpBusy = true; show("cmp");
        new Thread(null, () -> {
            Bm[] out = new Bm[6];
            for (int i = 0; i < 6; i++) {
                Bm m = new Bm(); m.k = Sorter.KEYS[i]; Sorter.Result r = Sorter.run(m.k, d, false); m.ok = true;
                for (int j = 1; j < r.a.length; j++) if (r.a[j - 1] > r.a[j]) m.ok = false;
                m.c = r.c; m.s = r.s; m.w = r.w; m.t = Sorter.timeIt(m.k, d); out[i] = m;
            }
            runOnUiThread(() -> { cmpRows = out; cmpBusy = false; if (screen.equals("cmp")) show("cmp"); });
        }, "sortware-compare", 256L * 1024 * 1024).start();
    }

    // ---------- Settings ----------
    View settings() {
        LinearLayout c = col();
        add(c, head("Settings"));
        add(c, tv("Theme", 15, false, tx));
        LinearLayout tr = new LinearLayout(this); String[] tn = {"Match device", "Light", "Dark"};
        for (int i = 0; i < 3; i++) { final int ti = i; Button b = obtn(tn[i], v -> { themeMode = ti; save(); rebuild(); }); sel(b, themeMode == i);
            LinearLayout.LayoutParams l = lp(0, -2, 1); l.setMargins(dp(2), 0, dp(2), 0); tr.addView(b, l); }
        add(c, tr);
        Switch sw = new Switch(this); sw.setText("Sound effects (soft sounds, off by default)"); sw.setTextColor(tx); sw.setChecked(sound);
        sw.setOnCheckedChangeListener((v, on) -> { sound = on; save(); click(); }); add(c, sw);
        TextView vl = tv("Volume: " + vol + "%", 15, false, tx); add(c, vl);
        SeekBar vs = new SeekBar(this); vs.setMax(100); vs.setProgress(vol);
        vs.setOnSeekBarChangeListener(new SB() { public void onProgressChanged(SeekBar s, int p, boolean u) { vol = p; vl.setText("Volume: " + vol + "%"); if (tg != null) { tg.release(); tg = null; } save(); } }); add(c, vs);
        TextView sl = tv("Animation speed: " + speed + "/10", 15, false, tx); add(c, sl);
        SeekBar ss = new SeekBar(this); ss.setMax(9); ss.setProgress(speed - 1);
        ss.setOnSeekBarChangeListener(new SB() { public void onProgressChanged(SeekBar s, int p, boolean u) { speed = p + 1; sl.setText("Animation speed: " + speed + "/10"); save(); } }); add(c, ss);
        LinearLayout r = new LinearLayout(this);
        Button t = obtn("🔊 Test sound", v -> tone(ToneGenerator.TONE_DTMF_5, 80));
        Button rs = obtn("Reset settings", v -> { themeMode = 0; sound = false; vol = 40; speed = 5; save(); rebuild(); });
        LinearLayout.LayoutParams l1 = lp(0, -2, 1); l1.rightMargin = dp(4); LinearLayout.LayoutParams l2 = lp(0, -2, 1); l2.leftMargin = dp(4);
        r.addView(t, l1); r.addView(rs, l2); add(c, r);
        return wrap(c);
    }

    // ---------- Guide ----------
    View guide() {
        LinearLayout c = col();
        add(c, head("How to use SORTWARE"));
        add(c, tv("1. Open Visualize and type numbers separated by commas (as many as you like), or tap 🎲 Random.\n2. Pick an algorithm by tapping its button or typing its number (1–6).\n3. Press Play, or use Next / Prev to go one step at a time.\n4. Read the box under the bars: it explains each move in plain words.\n\nThe animation screen never scrolls while it plays: the bars, explanation and controls stay in place, and only the step log scrolls inside its own box.", 14, false, tx));
        add(c, head("What the colors mean"));
        add(c, tv("🔵 Blue: waiting\n🟡 Yellow: being compared (?)\n🔴 Red: swapped or moved (⇄)\n🟢 Green: in its final place (✓)\n🟣 Purple: special, like the pivot (★)", 14, false, tx));
        add(c, head("Words you might meet"));
        add(c, tv("Comparison: checking which of two numbers is bigger.\nSwap: two numbers trade places.\nPass: one full trip through the list.\nPivot: a reference number Quick Sort uses to split the list.\nStable: equal numbers keep their original order.\nIn-place: sorts without a big extra copy.\nBig-O, like O(n²): how the work grows as the list grows; smaller is faster.", 14, false, tx));
        return wrap(c);
    }
}
