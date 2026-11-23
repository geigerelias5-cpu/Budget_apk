package de.budget.tracker;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/** Kompaktes 4×1-Widget: Restbetrag und Plus-Knopf. */
public class BudgetWidgetSmall extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) manager.updateAppWidget(id, BudgetWidget.build(context, BudgetWidget.COMPACT));
    }
}
