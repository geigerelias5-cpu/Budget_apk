package de.budget.tracker;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 4×2-Widget. Baut außerdem die Ansichten der anderen Größen. */
public class BudgetWidget extends AppWidgetProvider {

    static final int LARGE = 0, COMPACT = 1, SLIM = 2, RING = 3;

    private static final int[] CHIP_IDS = {
            R.id.c_essen, R.id.c_einkauf, R.id.c_transport,
            R.id.c_freizeit, R.id.c_wohnen, R.id.c_sonst
    };

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) manager.updateAppWidget(id, build(context, LARGE));
        LockNotify.update(context);
    }

    /** Aktualisiert alle Widgets aller Größen. */
    static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        update(c, m, BudgetWidget.class, LARGE);
        update(c, m, BudgetWidgetSmall.class, COMPACT);
        update(c, m, BudgetWidget21.class, SLIM);
        update(c, m, BudgetWidget22.class, RING);
    }

    private static void update(Context c, AppWidgetManager m, Class<?> cls, int type) {
        int[] ids = m.getAppWidgetIds(new ComponentName(c, cls));
        if (ids == null || ids.length == 0) return;
        RemoteViews v = build(c, type);
        for (int id : ids) m.updateAppWidget(id, v);
    }

    static RemoteViews build(Context c, int type) {
        int layout = type == COMPACT ? R.layout.widget_small
                : type == SLIM ? R.layout.widget_21
                : type == RING ? R.layout.widget_22 : R.layout.widget;
        RemoteViews v = new RemoteViews(c.getPackageName(), layout);
        boolean narrow = type == SLIM || type == RING;

        Store.Month mo = Store.month(c);
        Double left = mo == null ? null : mo.budget - mo.spent;
        if (left == null) {
            v.setTextViewText(R.id.w_left, narrow ? "–" : "Budget festlegen");
            v.setTextColor(R.id.w_left, 0xFFF2F2F2);
        } else {
            v.setTextViewText(R.id.w_left, Store.euro(left));
            v.setTextColor(R.id.w_left, left < 0 ? 0xFFD71921 : 0xFFF2F2F2);
        }

        if (type == SLIM) {
            int pct = mo == null ? 0 : (int) Math.max(0, Math.min(100, Math.round(mo.spent / mo.budget * 100)));
            boolean over = left != null && left < 0;
            v.setViewVisibility(R.id.w_bar, over ? View.GONE : View.VISIBLE);
            v.setViewVisibility(R.id.w_bar_red, over ? View.VISIBLE : View.GONE);
            v.setProgressBar(over ? R.id.w_bar_red : R.id.w_bar, 100, pct, false);
        }
        if (type == RING) {
            // Unter dem Plus: was heute noch pro Tag ausgegeben werden kann
            String perDay = "übrig";
            if (left != null) {
                int daysLeft = Store.daysLeft(c);
                perDay = left < 0 ? "über Budget" : Store.euro(Math.floor(left / daysLeft * 100) / 100) + "/Tag";
            }
            v.setTextViewText(R.id.w_month, perDay);
            // Glatte Beträge (ohne Cent) sind kürzer und dürfen größer sein
            boolean whole = left != null && Math.round(Math.abs(left) * 100) % 100 == 0;
            v.setTextViewTextSize(R.id.w_left, android.util.TypedValue.COMPLEX_UNIT_SP, whole ? 21f : 16f);
            v.setImageViewBitmap(R.id.w_ring, ring(mo));
        }

        // Tippen auf den Betrag öffnet die App
        Intent open = new Intent(c, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        v.setOnClickPendingIntent(R.id.w_open, PendingIntent.getActivity(c, 100, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        // Wenn noch kein Budget gesetzt ist, führen alle Knöpfe in die App
        if (left == null) {
            PendingIntent pi = PendingIntent.getActivity(c, 101, open,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            v.setOnClickPendingIntent(R.id.w_add, pi);
        } else {
            v.setOnClickPendingIntent(R.id.w_add, quick(c, "", 200));
        }

        if (type != LARGE) return v;

        for (int i = 0; i < CHIP_IDS.length; i++) {
            String label = "●  " + Store.CAT_NAMES[i];
            SpannableString s = new SpannableString(label);
            s.setSpan(new ForegroundColorSpan(Store.CAT_COLORS[i]), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            v.setTextViewText(CHIP_IDS[i], s);
            v.setOnClickPendingIntent(CHIP_IDS[i], left == null
                    ? PendingIntent.getActivity(c, 300 + i, open,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT)
                    : quick(c, Store.CAT_IDS[i], 210 + i));
        }
        return v;
    }

    private static PendingIntent quick(Context c, String cat, int requestCode) {
        Intent i = new Intent(c, QuickAddActivity.class);
        i.putExtra(QuickAddActivity.EXTRA_CAT, cat);
        i.setData(Uri.parse("budget://add/" + (cat.isEmpty() ? "any" : cat)));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(c, requestCode, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Zeichnet das runde Widget: schwarzer Kreis, farbige Bögen je Kategorie, gepunktet für den Rest. */
    private static Bitmap ring(Store.Month mo) {
        final int size = 360;
        final float c = size / 2f;
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);

        // Hintergrund-Kreis mit leichtem Verlauf und feinem Rand
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setShader(new RadialGradient(c, size * 0.35f, size * 0.55f,
                0xFF161616, 0xFF000000, Shader.TileMode.CLAMP));
        cv.drawCircle(c, c, c - 2f, bg);
        Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(2.5f);
        edge.setColor(0x26FFFFFF);
        cv.drawCircle(c, c, c - 2f, edge);

        float inner = size * (1f - 14f / 150f);
        float radius = inner * 0.44f;
        float stroke = inner * 0.07f;
        RectF r = new RectF(c - radius, c - radius, c + radius, c + radius);

        Paint dots = new Paint(Paint.ANTI_ALIAS_FLAG);
        dots.setStyle(Paint.Style.STROKE);
        dots.setStrokeWidth(inner * 0.026f);
        dots.setStrokeCap(Paint.Cap.ROUND);
        dots.setColor(0xFF555555);
        dots.setPathEffect(new DashPathEffect(new float[]{0.1f, inner * 0.039f}, 0));
        cv.drawArc(r, 0, 360, false, dots);
        if (mo == null) return b;

        double base = Math.max(mo.budget, mo.spent);
        if (base <= 0) return b;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(stroke);
        p.setStrokeCap(Paint.Cap.BUTT);
        float start = -90f;
        for (int i = 0; i < mo.byCat.length; i++) {
            double val = mo.byCat[i];
            if (val <= 0) continue;
            float sweep = (float) (val / base * 360.0);
            float gap = sweep > 4f ? 1.6f : 0f;
            p.setColor(i == 6 ? Store.FIX_COLOR : Store.CAT_COLORS[i]);
            cv.drawArc(r, start, Math.max(sweep - gap, 0.5f), false, p);
            start += sweep;
        }
        return b;
    }
}
