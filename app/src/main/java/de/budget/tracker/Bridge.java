package de.budget.tracker;

import android.Manifest;
import android.app.Activity;
import android.app.StatusBarManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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

    /**
     * Speichert eine Sicherung im Ordner „Download/Budget“ und zusätzlich in der App selbst.
     * Gibt den Speicherort zurück (leer bei Fehler).
     */
    @JavascriptInterface
    public String exportData(String json, String name) {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        // Kopie in der App (bleibt, solange die App installiert ist)
        try {
            File dir = new File(activity.getFilesDir(), "sicherungen");
            if (!dir.exists()) dir.mkdirs();
            try (FileOutputStream out = new FileOutputStream(new File(dir, name))) { out.write(bytes); }
            File[] all = dir.listFiles();
            if (all != null && all.length > 10) {
                java.util.Arrays.sort(all, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
                for (int i = 0; i < all.length - 10; i++) all[i].delete();
            }
        } catch (Exception ignored) {
        }
        // Sichtbare Datei im Download-Ordner (ohne Berechtigung ab Android 10)
        try {
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            v.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Budget");
            Uri uri = activity.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) return "";
            try (OutputStream out = activity.getContentResolver().openOutputStream(uri)) {
                if (out == null) return "";
                out.write(bytes);
            }
            return "Download/Budget/" + name;
        } catch (Exception e) {
            return "";
        }
    }

    /** Öffnet die Dateiauswahl; das Ergebnis kommt über window.importResult(text) zurück. */
    @JavascriptInterface
    public void importData() {
        activity.runOnUiThread(() -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain", "application/octet-stream"});
            try {
                activity.startActivityForResult(i, MainActivity.REQ_IMPORT);
            } catch (Exception e) {
                // keine Dateiauswahl verfügbar
            }
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
