package de.budget.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Stellt nach Neustart oder Update die Sperrbildschirm-Anzeige und die Widgets wieder her. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Store.refreshAll(context);
    }
}
