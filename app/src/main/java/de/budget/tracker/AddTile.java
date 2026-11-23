package de.budget.tracker;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Schnellzugriff-Kachel „Ausgabe“: funktioniert auch auf dem Sperrbildschirm. */
public class AddTile extends TileService {

    static void refresh(Context c) {
        try {
            TileService.requestListeningState(c, new ComponentName(c, AddTile.class));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onStartListening() {
        Tile t = getQsTile();
        if (t == null) return;
        Double left = Store.remaining(this);
        t.setLabel("Ausgabe");
        t.setSubtitle(left == null ? "Budget festlegen" : Store.euro(left) + " übrig");
        t.setState(Tile.STATE_INACTIVE);
        t.updateTile();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onClick() {
        Intent i = new Intent(this, QuickAddActivity.class);
        i.putExtra(QuickAddActivity.EXTRA_CAT, "");
        i.setData(Uri.parse("budget://add/tile"));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 600, i,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        } else {
            startActivityAndCollapse(i);
        }
    }
}
