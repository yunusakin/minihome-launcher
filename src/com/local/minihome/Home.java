package com.local.minihome;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Minimal TV launcher: a plain grid of apps. No rows, no recommendations, no animations.
public class Home extends Activity {
    // Hidden from the grid (they stay installed). Edit and rebuild to change.
    // Exact labels, label fragments (lowercase) and package names. Settings is shown as a header button instead.
    static final Set<String> HIDE = new HashSet<>(Arrays.asList(
            "Wi-Fi Display Sink", "Play Games", "Play Movies & TV", "Settings", "Ayarlar"));
    static final String[] HIDE_SUB = {"hotkey", "disclaimer"};   // vendor service entries on some TV firmwares
    static final Set<String> HIDE_PKG = new HashSet<>(Arrays.asList(
            "com.android.tv.settings", "com.android.settings"));

    static final int COLS = 5;

    static class Item { String key, label; Drawable img; boolean banner; Intent intent; }

    final List<Item> items = new ArrayList<>();
    BaseAdapter adapter;
    GridView g;
    TextView hint;
    TextClock date;
    int moving = -1;          // index being moved, -1 = normal mode
    boolean swallowUp;        // eat the OK key-up that follows a long press / drop

    static boolean tr() { return "tr".equals(java.util.Locale.getDefault().getLanguage()); }

    int px(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }

    GradientDrawable round(int fill, int stroke, int strokePx, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(px(radiusDp));
        if (strokePx > 0) d.setStroke(strokePx, stroke);
        return d;
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {0xFF060912, 0xFF141C36});
        root.setBackground(bg);
        root.setPadding(px(48), px(28), px(48), 0);

        // Header: clock + date (TextClock updates once a minute, no animation)
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextClock time = new TextClock(this);
        time.setFormat24Hour("HH:mm");
        time.setFormat12Hour("HH:mm");
        time.setTextSize(46);
        time.setTypeface(android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL));
        time.setTextColor(0xFFFFFFFF);
        hint = new TextView(this);
        hint.setText(tr() ? "Taşıma modu: yön tuşlarıyla taşıyın, OK ile bırakın" : "Move mode: use the arrow keys, press OK to drop");
        hint.setTextSize(16);
        hint.setTextColor(0xFF9DBBFF);
        hint.setPadding(px(16), px(8), 0, 0);
        hint.setVisibility(View.GONE);
        date = new TextClock(this);
        date.setFormat24Hour("EEEE, d MMMM");
        date.setFormat12Hour("EEEE, d MMMM");
        date.setTextSize(18);
        date.setTextColor(0xFF8A94AE);
        date.setPadding(px(20), px(14), 0, 0);
        head.addView(time);
        head.addView(date);
        head.addView(hint);
        View sp = new View(this);
        head.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));
        TextView set = new TextView(this);
        set.setText(tr() ? "⚙  Ayarlar" : "⚙  Settings");
        set.setTextSize(16);
        set.setTextColor(0xFFE6E9F2);
        set.setTextSize(18);
        set.setPadding(px(16), px(8), px(16), px(8));
        set.setFocusable(true);
        set.setBackground(round(0x1AFFFFFF, 0, 0, 22));
        set.setOnFocusChangeListener((v, f) -> v.setBackground(round(f ? 0x55FFFFFF : 0x1AFFFFFF, 0, 0, 22)));
        set.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Exception e) { }
        });
        head.addView(set);
        root.addView(head);

        g = new GridView(this);
        g.setNumColumns(COLS);
        g.setPadding(px(8), px(16), px(8), px(24));
        g.setClipToPadding(false);
        g.setClipChildren(false);
        g.setHorizontalSpacing(px(16));
        g.setVerticalSpacing(px(12));
        g.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        g.setSelector(new android.graphics.drawable.ColorDrawable(0));
        g.setVerticalScrollBarEnabled(false);
        adapter = new BaseAdapter() {
            public int getCount() { return items.size(); }
            public Object getItem(int i) { return items.get(i); }
            public long getItemId(int i) { return i; }
            public View getView(int i, View v, ViewGroup p) {
                if (v == null) {
                    LinearLayout l = new LinearLayout(Home.this);
                    l.setOrientation(LinearLayout.VERTICAL);
                    l.setGravity(Gravity.CENTER_HORIZONTAL);
                    ImageView im = new ImageView(Home.this);
                    im.setClipToOutline(true);
                    im.setOutlineProvider(new ViewOutlineProvider() {
                        public void getOutline(View vw, Outline o) { o.setRoundRect(0, 0, vw.getWidth(), vw.getHeight(), px(12)); }
                    });
                    l.addView(im, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(92)));
                    TextView t = new TextView(Home.this);
                    t.setTextColor(0xFFFFFFFF);
                    t.setTextSize(16);
                    t.setSingleLine();
                    t.setEllipsize(TextUtils.TruncateAt.END);
                    t.setGravity(Gravity.CENTER);
                    t.setPadding(px(10), px(3), px(10), px(3));
                    l.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    ((LinearLayout.LayoutParams) t.getLayoutParams()).topMargin = px(8);
                    l.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(136)));
                    v = l;
                }
                Item it = items.get(i);
                ImageView im = (ImageView) ((LinearLayout) v).getChildAt(0);
                im.setBackground(round(0xFF2A3350, 0, 0, 12));
                im.setImageDrawable(it.img);
                if (it.banner) { im.setScaleType(ImageView.ScaleType.CENTER_CROP); im.setPadding(0, 0, 0, 0); }
                else { im.setScaleType(ImageView.ScaleType.FIT_CENTER); im.setPadding(px(30), px(16), px(30), px(16)); }
                style(v, i);
                return v;
            }
        };
        g.setAdapter(adapter);
        g.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { restyle(); }
            public void onNothingSelected(android.widget.AdapterView<?> p) { restyle(); }
        });
        g.setOnItemClickListener((p, v, i, id) -> {
            try { startActivity(items.get(i).intent); } catch (Exception e) { /* app gone */ }
        });
        root.addView(g, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    @Override protected void onStart() {
        super.onStart();
        setMoving(-1);   // never come back stuck in move mode (e.g. after Home pressed mid-move)
        load();
    }

    // Back / Home while already home: do nothing.
    @Override public void onBackPressed() { }

    // Long-press OK = pick up the selected app; arrows move it; OK (or Back) drops it and saves the order.
    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        boolean ok = k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER;
        if (ok && e.getAction() == KeyEvent.ACTION_UP && swallowUp) { swallowUp = false; return true; }
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            if (moving < 0 && ok && e.getRepeatCount() == 1) {      // held past the system long-press delay
                int sel = g.getSelectedItemPosition();
                if (sel >= 0 && sel < items.size()) { setMoving(sel); swallowUp = true; }
                return true;
            }
            if (moving >= 0) {
                if (ok && e.getRepeatCount() > 0) return true;
                int d = k == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : k == KeyEvent.KEYCODE_DPAD_RIGHT ? 1
                        : k == KeyEvent.KEYCODE_DPAD_UP ? -COLS : k == KeyEvent.KEYCODE_DPAD_DOWN ? COLS : 0;
                if (d != 0) {
                    int to = Math.max(0, Math.min(items.size() - 1, moving + d));
                    items.add(to, items.remove(moving));
                    moving = to;
                    adapter.notifyDataSetChanged();
                    g.setSelection(to);
                    return true;
                }
                if (ok || k == KeyEvent.KEYCODE_BACK) {
                    setMoving(-1);
                    saveOrder();
                    swallowUp = ok;
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(e);
    }

    // Selected tile: 10% larger, label on a colored pill, other tiles dimmed. Applied immediately, no animation.
    void style(View v, int pos) {
        boolean sel = pos == g.getSelectedItemPosition();
        boolean mv = pos == moving;
        LinearLayout l = (LinearLayout) v;
        ImageView im = (ImageView) l.getChildAt(0);
        TextView t = (TextView) l.getChildAt(1);
        l.setScaleX(sel ? 1.1f : 1f);
        l.setScaleY(sel ? 1.1f : 1f);
        l.setTranslationZ(sel ? 1 : 0);
        l.setAlpha(mv ? 0.6f : sel ? 1f : 0.7f);
        t.setTextColor(sel ? 0xFFFFFFFF : 0xFF7F89A3);
        t.setTypeface(null, sel ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        t.setBackground(sel ? round(0xFF2F80FF, 0, 0, 14) : null);
        t.setText(mv ? "◀   ▲   ▼   ▶" : items.get(pos).label);
    }

    void restyle() {
        int first = g.getFirstVisiblePosition();
        for (int k = 0; k < g.getChildCount(); k++) style(g.getChildAt(k), first + k);
    }

    void setMoving(int m) {
        moving = m;
        hint.setVisibility(m >= 0 ? View.VISIBLE : View.GONE);
        date.setVisibility(m >= 0 ? View.GONE : View.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    void saveOrder() {
        StringBuilder sb = new StringBuilder();
        for (Item it : items) sb.append(it.key).append('\n');
        getSharedPreferences("home", MODE_PRIVATE).edit().putString("order", sb.toString()).apply();
    }

    // Saved order first; apps not in it (newly installed) keep their default order after.
    void applySavedOrder() {
        String saved = getSharedPreferences("home", MODE_PRIVATE).getString("order", "");
        if (saved.isEmpty()) return;
        Map<String, Item> byKey = new HashMap<>();
        for (Item it : items) byKey.put(it.key, it);
        List<Item> out = new ArrayList<>();
        for (String k : saved.split("\n")) {
            Item it = byKey.remove(k);
            if (it != null) out.add(it);
        }
        for (Item it : items) if (byKey.containsKey(it.key)) out.add(it);
        items.clear();
        items.addAll(out);
    }

    static boolean hidden(String label, String pkg) {
        if (HIDE.contains(label) || HIDE_PKG.contains(pkg)) return true;
        String l = label.toLowerCase();
        for (String h : HIDE_SUB) if (l.contains(h)) return true;
        return false;
    }

    void load() {
        PackageManager pm = getPackageManager();
        items.clear();
        Set<String> seen = new HashSet<>();
        seen.add(getPackageName());
        for (String cat : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            List<Item> part = new ArrayList<>();
            for (ResolveInfo r : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat), 0)) {
                String pkg = r.activityInfo.packageName;
                String label = r.loadLabel(pm).toString();
                if (hidden(label, pkg) || !seen.add(pkg)) continue;
                Item it = new Item();
                it.key = pkg;
                it.label = label;
                Drawable d = r.activityInfo.loadBanner(pm);
                it.banner = d != null;
                it.img = it.banner ? d : r.loadIcon(pm);
                it.intent = new Intent(Intent.ACTION_MAIN).setClassName(pkg, r.activityInfo.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                part.add(it);
            }
            Collections.sort(part, (a, c) -> a.label.compareToIgnoreCase(c.label));
            items.addAll(part);
        }
        applySavedOrder();
        adapter.notifyDataSetChanged();
    }
}
