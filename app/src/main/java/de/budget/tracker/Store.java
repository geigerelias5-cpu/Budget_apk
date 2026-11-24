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

    /** Standard-Kategorien, solange in den Einstellungen nichts geändert wurde. */
    static final String[] CAT_IDS = {"essen", "einkauf", "transport", "freizeit", "wohnen", "sonst"};
    static final String[] CAT_NAMES = {"Essen", "Einkauf", "Sprit", "Freizeit", "Versicherung", "Sonstiges"};
    static final int[] CAT_COLORS = {0xFFD71921, 0xFFF2F2F2, 0xFF8A8A8A, 0xFFF08C90, 0xFF6B6B6B, 0xFFC4C4BF};
    /** Farbe der eigenen Kategorie „Dauerbeträge“ (letzter Platz in byCat). */
    static final int FIX_COLOR = 0xFF8C1016;
    static final int MAX_CATS = 8;

    static final class Cat {
        final String id, name;
        final int color;
        final boolean note;
        Cat(String id, String name, int color, boolean note) {
            this.id = id; this.name = name; this.color = color; this.note = note;
        }
    }

    /** Kategorien aus den Einstellungen (Feld "cats"), sonst die Standardliste. */
    static Cat[] cats(String json) {
        try {
            JSONArray a = new JSONObject(json).optJSONArray("cats");
            if (a != null && a.length() > 0) {
                int n = Math.min(a.length(), MAX_CATS);
                Cat[] out = new Cat[n];
                for (int i = 0; i < n; i++) {
                    JSONObject o = a.getJSONObject(i);
                    out[i] = new Cat(o.optString("id"), o.optString("n", "?"),
                            color(o.optString("c", "")), o.optInt("q", 0) == 1 || o.optBoolean("q", false));
                }
                return out;
            }
        } catch (Exception ignored) {
        }
        Cat[] d = new Cat[CAT_IDS.length];
        for (int i = 0; i < d.length; i++) d[i] = new Cat(CAT_IDS[i], CAT_NAMES[i], CAT_COLORS[i], "sonst".equals(CAT_IDS[i]));
        return d;
    }

    /** CSS-Farbe → Android-Farbe auf dunklem Grund ("light-dark(...)" = Schwarz/Weiß → hell). */
    static int color(String css) {
        try {
            if (css.startsWith("#")) return android.graphics.Color.parseColor(css);
        } catch (Exception ignored) {
        }
        return 0xFFF2F2F2;
    }

    /** Zahlen des laufenden Monats. byCat: alle Kategorien + Dauerbeträge am Ende. */
    static final class Month {
        double budget, spent;
        Cat[] cats;
        double[] byCat;
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
        autoBackup(c);
        BudgetWidget.updateAll(c);
        LockNotify.update(c);
        AddTile.refresh(c);
    }

    static long lastAutoBackup(Context c) {
        return prefs(c).getLong("lastAuto", 0);
    }

    /**
     * Automatische Sicherung: höchstens einmal pro Woche eine Datei
     * „Budget-Auto-Sicherung.json“ in Download/Budget (wird überschrieben) plus Kopie in der App.
     */
    static void autoBackup(Context c) {
        long now = System.currentTimeMillis();
        if (now - lastAutoBackup(c) < 7L * 24 * 60 * 60 * 1000) return;
        try {
            JSONObject state = new JSONObject(read(c));
            if (state.optDouble("budget", 0) <= 0) return;
            java.text.SimpleDateFormat iso = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
            iso.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            String json = new JSONObject().put("app", "budget").put("format", 1)
                    .put("exported", iso.format(new java.util.Date(now))).put("auto", true)
                    .put("data", state).toString(1);
            byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            String name = "Budget-Auto-Sicherung.json";

            // Kopie in der App
            java.io.File dir = new java.io.File(c.getFilesDir(), "sicherungen");
            if (!dir.exists()) dir.mkdirs();
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(dir, name))) {
                out.write(bytes);
            }

            // Sichtbare Datei in Download/Budget: vorhandene überschreiben, sonst neu anlegen
            android.content.ContentResolver cr = c.getContentResolver();
            android.net.Uri coll = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI;
            android.net.Uri uri = null;
            try (android.database.Cursor cur = cr.query(coll,
                    new String[]{android.provider.MediaStore.MediaColumns._ID},
                    android.provider.MediaStore.MediaColumns.DISPLAY_NAME + "=? AND "
                            + android.provider.MediaStore.MediaColumns.RELATIVE_PATH + "=?",
                    new String[]{name, "Download/Budget/"}, null)) {
                if (cur != null && cur.moveToFirst())
                    uri = android.content.ContentUris.withAppendedId(coll, cur.getLong(0));
            } catch (Exception ignored) {
            }
            if (uri == null) {
                android.content.ContentValues v = new android.content.ContentValues();
                v.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name);
                v.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json");
                v.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/Budget");
                uri = cr.insert(coll, v);
            }
            if (uri != null) {
                try (java.io.OutputStream out = cr.openOutputStream(uri, "wt")) {
                    if (out != null) out.write(bytes);
                }
            }
            prefs(c).edit().putLong("lastAuto", now).apply();
        } catch (Exception ignored) {
            // Sicherung ist Komfort – Fehler dürfen die App nicht stören
        }
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

    /** Tag, an dem der Budget-Monat beginnt (1–28), z. B. 26 für Gehalt am 26. */
    static int startDay(String json) {
        try {
            return Math.max(1, Math.min(28, (int) Math.round(new JSONObject(json).optDouble("startDay", 1))));
        } catch (Exception e) {
            return 1;
        }
    }

    /** Schlüssel "JJJJ-MM" des Budget-Monats, in den der Zeitpunkt fällt. */
    static String periodKey(long ts, int startDay) {
        Calendar d = Calendar.getInstance();
        d.setTimeInMillis(ts);
        if (d.get(Calendar.DAY_OF_MONTH) < startDay) {
            d.set(Calendar.DAY_OF_MONTH, 1);
            d.add(Calendar.MONTH, -1);
        }
        return String.format(Locale.ROOT, "%04d-%02d", d.get(Calendar.YEAR), d.get(Calendar.MONTH) + 1);
    }

    /** Anfang des nächsten Budget-Monats (Mitternacht). */
    static Calendar periodEnd(String key, int startDay) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Integer.parseInt(key.substring(0, 4)), Integer.parseInt(key.substring(5, 7)) - 1, startDay);
        c.add(Calendar.MONTH, 1);
        return c;
    }

    /** Verbleibende Tage im laufenden Budget-Monat, heute mitgezählt. */
    static int daysLeft(Context c) {
        int sd = startDay(read(c));
        Calendar end = periodEnd(periodKey(System.currentTimeMillis(), sd), sd);
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0);
        long days = Math.round((end.getTimeInMillis() - today.getTimeInMillis()) / 86400000.0);
        return (int) Math.max(1, days);
    }

    /** Name des laufenden Budget-Monats (nach der Mitte des Zeitraums), z. B. „Dezember“. */
    static String periodName(Context c) {
        int sd = startDay(read(c));
        Calendar end = periodEnd(periodKey(System.currentTimeMillis(), sd), sd);
        end.add(Calendar.DAY_OF_MONTH, -15);
        return new java.text.SimpleDateFormat("MMMM", Locale.GERMANY).format(end.getTime());
    }

    /** Laufender Monat inkl. Dauerbeträgen, die die App noch nicht als Eintrag angelegt hat. */
    static Month month(Context c) {
        try {
            JSONObject o = new JSONObject(read(c));
            double budget = o.optDouble("budget", 0);
            if (budget <= 0) return null;
            Month m = new Month();
            m.budget = budget;
            m.cats = cats(o.toString());
            m.byCat = new double[m.cats.length + 1];
            int sd = startDay(o.toString());
            String ym = periodKey(System.currentTimeMillis(), sd);
            java.util.HashSet<String> fixedSeen = new java.util.HashSet<>();
            JSONArray arr = o.optJSONArray("entries");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject e = arr.getJSONObject(i);
                    if (!ym.equals(periodKey(e.optLong("ts"), sd))) continue;
                    double amt = e.optDouble("amt", 0);
                    m.spent += amt;
                    String fixedId = e.optString("fixed", "");
                    if (!fixedId.isEmpty()) fixedSeen.add(fixedId);
                    m.byCat[fixedId.isEmpty() ? catIndex(m.cats, e.optString("cat")) : m.cats.length] += amt;
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
                    m.byCat[m.cats.length] += amt;
                }
            }
            return m;
        } catch (Exception ex) {
            return null;
        }
    }

    static int catIndex(Cat[] cats, String id) {
        if ("fix".equals(id)) return cats.length;
        for (int i = 0; i < cats.length; i++) if (cats[i].id.equals(id)) return i;
        for (int i = 0; i < cats.length; i++) if (cats[i].note) return i;
        return cats.length - 1;
    }

    static String euro(double v) {
        NumberFormat f = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        String s = f.format(v).replace('\u202F', ' ').replace('\u00A0', ' ');
        s = s.replace(",00 ", " ");
        // €-Zeichen eng an die Zahl (schmales geschütztes Leerzeichen)
        return s.replace(" €", "\u202F€");
    }
}
