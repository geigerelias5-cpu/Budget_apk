package de.budget.tracker;

import android.app.Activity;
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

    @JavascriptInterface
    public void close() {
        activity.runOnUiThread(() -> {
            activity.finish();
            activity.overridePendingTransition(0, 0);
        });
    }
}
