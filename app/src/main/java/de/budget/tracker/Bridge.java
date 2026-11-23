package de.budget.tracker;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.webkit.JavascriptInterface;

/** Wird in der Webansicht als window.Android bereitgestellt. */
final class Bridge {
    private final Activity activity;
    private volatile String startCategory;

    Bridge(Activity activity, String startCategory) {
        this.activity = activity;
        this.startCategory = startCategory;
    }

    void setStartCategory(String cat) {
        startCategory = cat;
    }

    @JavascriptInterface
    public String getData() {
        return Store.read(activity);
    }

    @JavascriptInterface
    public void setData(String json) {
        Store.write(activity, json);
    }

    @JavascriptInterface
    public String startCategory() {
        return startCategory == null ? "" : startCategory;
    }

    /** Öffnet das Schnelleingabe-Fenster (aus der App heraus). */
    @JavascriptInterface
    public void openQuickAdd() {
        activity.runOnUiThread(() -> {
            Intent i = new Intent(activity, QuickAddActivity.class);
            activity.startActivity(i);
            activity.overridePendingTransition(0, 0);
        });
    }

    /** Öffnet die Haupt-App (aus dem Schnelleingabe-Fenster heraus). */
    @JavascriptInterface
    public void openApp() {
        activity.runOnUiThread(() -> {
            Intent i = new Intent(activity, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finish();
        });
    }

    /**
     * Bittet den Launcher, ein Widget auf den Startbildschirm zu legen.
     * size: "large" (4×2) oder "small" (4×1). Gibt false zurück, wenn der Launcher das nicht kann.
     */
    @JavascriptInterface
    public boolean pinWidget(String size) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(activity);
            if (!m.isRequestPinAppWidgetSupported()) return false;
            Class<?> cls = "small".equals(size) ? BudgetWidgetSmall.class : BudgetWidget.class;
            return m.requestPinAppWidget(new ComponentName(activity, cls), null, null);
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public void close() {
        activity.runOnUiThread(() -> {
            activity.finish();
            activity.overridePendingTransition(0, 0);
        });
    }
}
