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
    static final String[] CAT_NAMES = {"Essen", "Einkauf", "Sprit", "Freizeit", "Versicherung", "Sonstiges"};
    static final int[] CAT_COLORS = {0xFFD71921, 0xFFF2F2F2, 0xFF8A8A8A, 0xFFF08C90, 0xFF6B6B6B, 0xFFC4C4BF};
    /** Farbe der eigenen Kategorie „Dauerbeträge“ (Index 6 in byCat). */
    static final int FIX_COLOR = 0xFF8C1016;

    /** Zahlen des laufenden Monats. byCat: 6 Kategorien + Dauerbeträge. */
    static final class Month {
        double budget, spent;
        final double[] byCat = new double[7];
    }

    private Store() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String read(Context c) {
        return prefs(c).getString(KEY, "{\"budget\":0,\"entries\":[]}");
    }

    static void write(Context c, String json) {
        prefs(c).edit().putString(KEY, json).commit();
        refreshAll(c);
    }

    /** Widgets, Sperrbildschirm-Anzeige und Kachel auf den neuesten Stand bringen. */
    static void refreshAll(Context c) {
        BudgetWidget.updateAll(c);
        LockNotify.update(c);
        AddTile.refresh(c);
    }

    static boolean askedNotify(Context c) {
        return prefs(c).getBoolean("askedNotify", false);
    }

    static void setAskedNotify(Context c) {
        prefs(c).edit().putBoolean("askedNotify", true).apply();
    }

    static boolean lockEnabled(Context c) {
        return prefs(c).getBoolean("lock", false);
    }

    static void setLockEnabled(Context c, boolean on) {
        prefs(c).edit().putBoolean("lock", on).commit();
        LockNotify.update(c);
    }

    /** Budget minus Ausgaben im aktuellen Monat. Gibt null zurück, wenn noch kein Budget gesetzt ist. */
    static Double remaining(Context c) {
        double[] s = summary(c);
        return s == null ? null : s[0] - s[1];
    }

    /** {Budget, ausgegeben in diesem Monat} oder null, wenn kein Budget gesetzt ist. */
    static double[] summary(Context c) {
        Month m = month(c);
        return m == null ? null : new double[]{m.budget, m.spent};
    }

    /** Laufender Monat inkl. Dauerbeträgen, die die App noch nicht als Eintrag angelegt hat. */
    static Month month(Context c) {
        try {
            JSONObject o = new JSONObject(read(c));
            double budget = o.optDouble("budget", 0);
            if (budget <= 0) return null;
            Month m = new Month();
            m.budget = budget;
            Calendar now = Calendar.getInstance();
            String ym = String.format(Locale.ROOT, "%04d-%02d", now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1);
            Calendar d = Calendar.getInstance();
            java.util.HashSet<String> fixedSeen = new java.util.HashSet<>();
            JSONArray arr = o.optJSONArray("entries");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject e = arr.getJSONObject(i);
                    d.setTimeInMillis(e.optLong("ts"));
                    if (d.get(Calendar.YEAR) != now.get(Calendar.YEAR)
                            || d.get(Calendar.MONTH) != now.get(Calendar.MONTH)) continue;
                    double amt = e.optDouble("amt", 0);
                    m.spent += amt;
                    String fixedId = e.optString("fixed", "");
                    if (!fixedId.isEmpty()) fixedSeen.add(fixedId);
                    m.byCat[catIndex(fixedId.isEmpty() ? e.optString("cat") : "fix")] += amt;
                }
            }
            JSONArray fixed = o.optJSONArray("fixed");
            if (fixed != null) {
                for (int i = 0; i < fixed.length(); i++) {
                    JSONObject f = fixed.getJSONObject(i);
                    String id = f.optString("id");
                    if (fixedSeen.contains(id)) continue;
                    if (f.optString("from", ym).compareTo(ym) > 0) continue;
                    JSONArray skip = f.optJSONArray("skip");
                    boolean skipped = false;
                    if (skip != null) for (int j = 0; j < skip.length(); j++) if (ym.equals(skip.optString(j))) skipped = true;
                    if (skipped) continue;
                    double amt = f.optDouble("amt", 0);
                    m.spent += amt;
                    m.byCat[6] += amt;
                }
            }
            return m;
        } catch (Exception ex) {
            return null;
        }
    }

    /** Gewähltes Design der App: "nothing", "glass" oder "ios". */
    static String design(Context c) {
        try {
            String d = new JSONObject(read(c)).optString("design", "nothing");
            return ("glass".equals(d) || "ios".equals(d)) ? d : "nothing";
        } catch (Exception e) {
            return "nothing";
        }
    }

    static int catIndex(String id) {
        if ("fix".equals(id)) return 6;
        for (int i = 0; i < CAT_IDS.length; i++) if (CAT_IDS[i].equals(id)) return i;
        return 5;
    }

    static String euro(double v) {
        NumberFormat f = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        String s = f.format(v).replace('\u202F', ' ').replace('\u00A0', ' ');
        s = s.replace(",00 ", " ");
        // €-Zeichen eng an die Zahl (schmales geschütztes Leerzeichen)
        return s.replace(" €", "\u202F€");
    }
}
