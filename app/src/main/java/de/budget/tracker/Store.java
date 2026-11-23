package de.budget.tracker;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

/** Speichert alle Daten als JSON: {"budget":900,"entries":[{"id","amt","cat","ts"}]} */
final class Store {
    private static final String PREFS = "budget";
    private static final String KEY = "state";

    static final String[] CAT_IDS = {"essen", "einkauf", "transport", "freizeit", "wohnen", "sonst"};
    static final String[] CAT_NAMES = {"Essen", "Einkauf", "Transport", "Freizeit", "Wohnen", "Sonstiges"};
    static final int[] CAT_COLORS = {0xFFD71921, 0xFFF2F2F2, 0xFF8A8A8A, 0xFFF08C90, 0xFF6B6B6B, 0xFFC4C4BF};

    private Store() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String read(Context c) {
        return prefs(c).getString(KEY, "{\"budget\":0,\"entries\":[]}");
    }

    static void write(Context c, String json) {
        prefs(c).edit().putString(KEY, json).apply();
        BudgetWidget.updateAll(c);
    }

    /** Budget minus Ausgaben im aktuellen Monat. Gibt null zurück, wenn noch kein Budget gesetzt ist. */
    static Double remaining(Context c) {
        try {
            JSONObject o = new JSONObject(read(c));
            double budget = o.optDouble("budget", 0);
            if (budget <= 0) return null;
            JSONArray arr = o.optJSONArray("entries");
            Calendar now = Calendar.getInstance();
            Calendar d = Calendar.getInstance();
            double spent = 0;
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject e = arr.getJSONObject(i);
                    d.setTimeInMillis(e.optLong("ts"));
                    if (d.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                            && d.get(Calendar.MONTH) == now.get(Calendar.MONTH)) {
                        spent += e.optDouble("amt", 0);
                    }
                }
            }
            return budget - spent;
        } catch (Exception ex) {
            return null;
        }
    }

    static String euro(double v) {
        NumberFormat f = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        String s = f.format(v);
        return s.replace(",00 ", " ").replace(",00 ", " ");
    }
}
