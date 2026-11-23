package de.budget.tracker;

import android.Manifest;
import android.app.Activity;
import android.app.StatusBarManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.provider.Settings;
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
     * size: "2x1", "2x2", "4x1" oder "4x2". Gibt false zurück, wenn der Launcher das nicht kann.
     */
    @JavascriptInterface
    public boolean pinWidget(String size) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(activity);
            if (!m.isRequestPinAppWidgetSupported()) return false;
            Class<?> cls = "2x1".equals(size) ? BudgetWidget21.class
                    : "2x2".equals(size) ? BudgetWidget22.class
                    : ("4x1".equals(size) || "small".equals(size)) ? BudgetWidgetSmall.class
                    : BudgetWidget.class;
            return m.requestPinAppWidget(new ComponentName(activity, cls), null, null);
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public boolean lockEnabled() {
        return Store.lockEnabled(activity);
    }

    /**
     * Schaltet die Sperrbildschirm-Anzeige ein oder aus.
     * Rückgabe: "on", "off", "asking" (Berechtigungsdialog offen) oder "settings" (Einstellungen geöffnet).
     */
    @JavascriptInterface
    public String setLock(boolean on) {
        if (!on) {
            Store.setLockEnabled(activity, false);
            return "off";
        }
        if (Build.VERSION.SDK_INT >= 33
                && activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            if (activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
                    || !Store.askedNotify(activity)) {
                Store.setAskedNotify(activity);
                activity.runOnUiThread(() -> activity.requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, MainActivity.REQ_NOTIFY));
                return "asking";
            }
            openNotifySettings();
            return "settings";
        }
        Store.setLockEnabled(activity, true);
        if (!activity.getSystemService(android.app.NotificationManager.class).areNotificationsEnabled()) {
            openNotifySettings();
            return "settings";
        }
        return "on";
    }

    private void openNotifySettings() {
        activity.runOnUiThread(() -> {
            Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            i.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
            activity.startActivity(i);
        });
    }

    /** Bietet an, die Schnellzugriff-Kachel hinzuzufügen (ab Android 13). */
    @JavascriptInterface
    public boolean addTile() {
        if (Build.VERSION.SDK_INT < 33) return false;
        try {
            StatusBarManager sbm = activity.getSystemService(StatusBarManager.class);
            if (sbm == null) return false;
            activity.runOnUiThread(() -> sbm.requestAddTileService(
                    new ComponentName(activity, AddTile.class), "Ausgabe",
                    Icon.createWithResource(activity, R.drawable.ic_plus),
                    activity.getMainExecutor(), result -> { }));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Passt Status- und Navigationsleiste an das gewählte Design an (nur in der Haupt-App). */
    @JavascriptInterface
    public void setDesign(String design) {
        if (!(activity instanceof MainActivity)) return;
        boolean night = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int top, bottom;
        if ("ios".equals(design)) {
            top = night ? 0xFF000000 : 0xFFF2F2F7;
            bottom = night ? 0xFF161616 : 0xFFF9F9F9;
        } else if ("glass".equals(design)) {
            top = night ? 0xFF0E1A3E : 0xFFC9DAFB;
            bottom = night ? 0xFF07262A : 0xFFD7F2EA;
        } else {
            top = night ? 0xFF0C0C0C : 0xFFFAFAF8;
            bottom = top;
        }
        activity.runOnUiThread(() -> {
            activity.getWindow().setStatusBarColor(top);
            activity.getWindow().setNavigationBarColor(bottom);
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
