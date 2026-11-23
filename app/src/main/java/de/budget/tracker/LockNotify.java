package de.budget.tracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** „Budget als Nachricht“: dauerhafte Benachrichtigung mit Restbetrag, Balken und „+ Ausgabe“ (auch auf dem Sperrbildschirm). */
final class LockNotify {
    private static final String CHANNEL = "lock";
    private static final int ID = 1;

    private LockNotify() {}

    static void update(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        double[] s = Store.summary(c);
        if (!Store.lockEnabled(c) || s == null || !nm.areNotificationsEnabled()) {
            nm.cancel(ID);
            return;
        }

        NotificationChannel ch = new NotificationChannel(CHANNEL, "Budget als Nachricht",
                NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Zeigt den Restbetrag und einen Knopf zum Eintragen");
        ch.setShowBadge(false);
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);

        double budget = s[0], spent = s[1], left = budget - spent;
        int pct = (int) Math.max(0, Math.min(100, Math.round(spent / budget * 100)));
        String month = new SimpleDateFormat("MMMM", Locale.GERMANY).format(new Date());
        String title = left >= 0 ? Store.euro(left) + " übrig" : Store.euro(-left) + " über Budget";
        String text = Store.euro(spent) + " von " + Store.euro(budget) + " ausgegeben · " + month;

        Intent add = new Intent(c, QuickAddActivity.class);
        add.putExtra(QuickAddActivity.EXTRA_CAT, "");
        add.setData(Uri.parse("budget://add/lock"));
        add.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent addPi = PendingIntent.getActivity(c, 500, add,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent open = new Intent(c, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent openPi = PendingIntent.getActivity(c, 501, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title)
                .setContentText(text)
                .setProgress(100, pct, false)
                .setColor(0xFFD71921)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_STATUS)
                .setContentIntent(addPi)
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(c, R.drawable.ic_plus), "+ Ausgabe", addPi).build())
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(c, R.drawable.ic_stat), "App öffnen", openPi).build())
                .build();
        nm.notify(ID, n);
    }
}
