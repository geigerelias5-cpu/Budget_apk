# Budget – einfacher Budget-Tracker für Android

Ausgaben schnell eintragen, sehen was vom Monatsbudget übrig ist. Ohne Konto, ohne Werbung, ohne Internet – alle Daten bleiben auf deinem Handy.

**[⬇ Budget.apk herunterladen (neueste Version)](https://github.com/geigerelias5-cpu/Budget_apk/releases/latest/download/Budget.apk)**

iPhone? → [Web-App für iPhone](https://geigerelias5-cpu.github.io/Budget-web/)

## Funktionen

- **Kreisdiagramm** mit Restbetrag und Ausgaben nach Kategorie – Einnahmen eintragen erhöht das Monatsbudget – Kategorien frei einstellbar: Name, Farbe, Reihenfolge, bis zu 8 Stück
- **Widgets** für den Startbildschirm in vier Größen (2×1, 2×2 rund, 4×1, 4×2) – mit „+“ zum schnellen Eintragen
- **Verlauf**: alle Monate mit Diagramm, im Budget geblieben oder drüber
- **Dauerbeträge** für Abos und Versicherungen, zählen jeden Monat automatisch
- **Budget als Nachricht** auf dem Sperrbildschirm und eine Schnellzugriff-Kachel
- **Export & Import**: Sicherungsdatei zum Umziehen auf ein anderes Gerät (auch Android ↔ iPhone)
- **Automatische Sicherung** jede Woche in Download/Budget
- **Einträge bearbeiten** (antippen) und **„Heute noch X € pro Tag“**
- **Gespart**: was am Monatsende übrig bleibt, zählt im Verlauf als gespart

## Installieren

1. Auf dem Android-Handy (Android 10 oder neuer) den Link oben öffnen, `Budget.apk` wird heruntergeladen.
2. Datei antippen → einmalig **„Unbekannte Apps installieren“** für den Browser bzw. die Dateien-App erlauben → **Installieren**.
3. Falls Google Play Protect warnt („Unbekannter Entwickler“): **Trotzdem installieren** – die App ist nicht im Play Store, deshalb die Warnung.
4. Widget hinzufügen: in der App auf den Widget-Knopf oben rechts tippen, oder lange auf den Startbildschirm drücken → Widgets → Budget.

Updates: neue `Budget.apk` einfach über die alte installieren, die Daten bleiben erhalten.

## Datenschutz

Die App hat keinen Internetzugriff und sammelt keine Daten. Alles wird nur lokal auf dem Gerät gespeichert. Sicherungsdateien entstehen nur, wenn du selbst auf „Exportieren“ tippst.

## Selbst bauen

Das Projekt baut automatisch per GitHub Actions (`.github/workflows/build.yml`). Für eigene Builds brauchst du einen eigenen Signier-Schlüssel; das Passwort wird über das Secret `BUDGET_KEY_PASSWORD` gesetzt.
