# Budget: Android-App mit Homescreen-Widget

Eine einfache Budget-App für dein Nothing Phone (Android 10 oder neuer).

- **Widget** (4×2): zeigt, was diesen Monat noch übrig ist. Tippst du auf eine Kategorie oder auf **+**, öffnet sich über dem Startbildschirm ein kleines Fenster mit Ziffernblock.
- **App**: Kreisdiagramm nach Kategorien, Restbetrag, letzte Einträge (löschen mit Rückgängig), Einstellungen fürs Budget.
- Alle Daten bleiben nur auf dem Handy, es gibt keinen Internetzugriff.

## APK bauen: Weg A, GitHub (ohne Installation)

1. Lege auf github.com ein Konto an und erstelle ein neues Repository, z. B. `budget-app` (privat ist ok).
2. Lade den **gesamten Inhalt** dieses Ordners hoch. Am einfachsten geht das mit **GitHub Desktop** (Repository klonen, Dateien hineinkopieren, „Commit“ und „Push“).
   - Wichtig: Der versteckte Ordner `.github` muss mit. Beim Hochladen über den Browser wird er manchmal weggelassen. Dann im Repository auf **Actions → „set up a workflow yourself“** gehen, den Inhalt von `.github/workflows/build.yml` einfügen und speichern.
3. Unter **Actions** startet der Build automatisch (ca. 3–5 Minuten). Wenn er grün ist, klickst du auf den Lauf und lädst unten unter „Artifacts“ **Budget-APK** herunter. Das ist eine ZIP-Datei, darin liegt `Budget.apk`.

## APK bauen: Weg B, Android Studio

Öffne den Ordner in Android Studio, warte, bis Gradle fertig ist, und wähle dann **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.

## Installieren auf dem Handy

1. Kopiere `Budget.apk` aufs Handy und tippe sie an.
2. Erlaube einmalig „Unbekannte Apps installieren“ für die App, mit der du die Datei öffnest (z. B. Dateien oder Chrome).
3. Widget hinzufügen: lange auf den Startbildschirm drücken → **Widgets** → **Budget**.

## Updates

Das Projekt signiert mit einem festen Schlüssel (`app/budget.keystore`). Neue Versionen kannst du deshalb einfach über die alte App installieren, deine Daten bleiben erhalten. Erhöhe dafür in `app/build.gradle` die Zahl bei `versionCode`.

## Wo was liegt

| Datei | Inhalt |
|---|---|
| `app/src/main/assets/index.html` | Oberfläche der App (Diagramm, Liste, Einstellungen) |
| `app/src/main/assets/quick.html` | Schnelleingabe-Fenster mit Ziffernblock |
| `app/src/main/assets/common.js` | Kategorien und Speichern |
| `app/src/main/java/.../BudgetWidget.java` | Homescreen-Widget |
| `app/src/main/res/layout/widget.xml` | Aussehen des Widgets |

Die Kategorien änderst du in `common.js` **und** in `Store.java` (gleiche Reihenfolge).
