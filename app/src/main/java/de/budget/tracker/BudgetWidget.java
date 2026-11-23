package de.budget.tracker;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.RemoteViews;

public class BudgetWidget extends AppWidgetProvider {

    private static final int[] CHIP_IDS = {
            R.id.c_essen, R.id.c_einkauf, R.id.c_transport,
            R.id.c_freizeit, R.id.c_wohnen, R.id.c_sonst
    };

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) manager.updateAppWidget(id, build(context, false));
        LockNotify.update(context);
    }

    /** Aktualisiert alle Widgets beider Größen. */
    static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        update(c, m, BudgetWidget.class, false);
        update(c, m, BudgetWidgetSmall.class, true);
    }

    private static void update(Context c, AppWidgetManager m, Class<?> cls, boolean small) {
        int[] ids = m.getAppWidgetIds(new ComponentName(c, cls));
        if (ids == null || ids.length == 0) return;
        RemoteViews v = build(c, small);
        for (int id : ids) m.updateAppWidget(id, v);
    }

    static RemoteViews build(Context c, boolean small) {
        RemoteViews v = new RemoteViews(c.getPackageName(), small ? R.layout.widget_small : R.layout.widget);

        Double left = Store.remaining(c);
        if (left == null) {
            v.setTextViewText(R.id.w_left, "Budget festlegen");
            v.setTextColor(R.id.w_left, 0xFFF2F2F2);
        } else {
            v.setTextViewText(R.id.w_left, Store.euro(left));
            v.setTextColor(R.id.w_left, left < 0 ? 0xFFD71921 : 0xFFF2F2F2);
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

        if (small) return v;

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
}
