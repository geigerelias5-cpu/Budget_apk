package de.budget.tracker;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/** 2×2-Widget: Ring nach Kategorien, Restbetrag und Plus. */
public class BudgetWidget22 extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) manager.updateAppWidget(id, BudgetWidget.build(context, BudgetWidget.RING));
    }
}
