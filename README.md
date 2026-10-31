# 🍲 Kochideen – Android-App

Fotografiere deine Zutaten, und die App schlägt dir passende Gerichte vor, mit Nährwerten und Zubereitungszeit.

## Funktionen

- 📷 **Zutaten fotografieren**: beliebig viele Fotos (Kamera oder Galerie); die KI erkennt die Lebensmittel
- ✏️ Zutaten von Hand ergänzen oder falsch erkannte entfernen
- ✅ **„Einlesen beenden“**, danach fragt die App nach häufigen Basis-Zutaten (Eier, Butter, Milch, Mehl, Zwiebeln …)
- 🧂 **Gewürze** gelten als vorhanden und werden trotzdem in jedem Rezept mit Menge aufgeführt
- ✨ Rezeptideen mit
  - ⏱ Zubereitungszeit (Vorbereitung, Kochen, gesamt)
  - 🔥 ca. Kalorien, Eiweiß, Kohlenhydrate, Zucker, Fett, gesättigte Fettsäuren, Ballaststoffe (pro Portion)
  - Zutatenliste, Schritt-für-Schritt-Anleitung und Tipp
- ❤️ Rezepte **speichern** (bleiben auf dem Handy)
- Portionen, Anzahl der Ideen und Wünsche (z. B. „vegetarisch“) einstellbar

---

## 1. APK über GitHub bauen

1. Auf [github.com](https://github.com) anmelden → oben rechts **„+“ → „New repository“**.
   Name z. B. `kochideen`. Den Rest unverändert lassen, dann **„Create repository“** klicken.
2. Auf der neuen Seite **„uploading an existing file“** anklicken.
3. Die ZIP-Datei entpacken und **den gesamten Inhalt des Ordners `kochideen`** ins Browserfenster ziehen
   (also die Ordner `app`, `.github` und die Dateien `build.gradle`, `settings.gradle` usw.).
   ⚠️ Der Ordner **`.github`** muss mit hochgeladen werden. Auf dem Mac ist er versteckt; mit **Cmd + Shift + .** machst du ihn sichtbar.
4. Unten auf **„Commit changes“** klicken.
5. Oben auf den Reiter **„Actions“** wechseln. Der Build **„APK bauen“** startet automatisch und dauert etwa 3–5 Minuten.
   Ein grüner Haken ✅ bedeutet, dass der Build fertig ist.

> Falls unter „Actions“ nichts passiert: auf **„APK bauen“ → „Run workflow“** klicken.

## 2. APK aufs Handy laden und installieren

**Am einfachsten direkt am Handy:**
1. Die Repository-Seite am Handy öffnen → rechts bzw. unten auf **„Releases“** tippen.
2. Beim neuesten Release **`Kochideen.apk`** antippen und herunterladen.
3. Die Datei öffnen. Android fragt einmalig, ob der Browser „unbekannte Apps installieren“ darf → **erlauben**.
4. Auf **Installieren** tippen. Fertig!

*(Alternativ findest du die APK auch unter **Actions → letzter Build → Artifacts → „Kochideen-APK“**, dort als ZIP verpackt.)*

Updates: Wenn du später etwas änderst und neu hochlädst, entsteht automatisch eine neue Version.
Sie lässt sich einfach über die alte App installieren; gespeicherte Rezepte bleiben dabei erhalten.

## 3. Kostenlosen Schlüssel eintragen (einmalig, ca. 2 Minuten)

Die Bilderkennung und die Rezepte kommen von **Google Gemini**. Mit dem kostenlosen Kontingent zahlst du nichts, und es ist keine Kreditkarte nötig.

1. Am Handy oder PC [aistudio.google.com/apikey](https://aistudio.google.com/apikey) öffnen und mit deinem Google-Konto anmelden.
2. **„API-Schlüssel erstellen“** antippen und den Schlüssel kopieren (er beginnt mit `AIza…`).
3. In der App unter **⚙️ Einstellungen** einfügen → **Speichern** → **Verbindung testen**.

💡 **Ohne Schlüssel ausprobieren:** Trage als Schlüssel einfach `demo` ein. Dann zeigt die App Beispieldaten.

**Gut zu wissen:**
- Das Gratis-Kontingent hat ein Tageslimit. Für den privaten Gebrauch reicht es normalerweise locker.
  Meldet die App „Kontingent aufgebraucht“, warte eine Minute oder wähle in den Einstellungen **Gemini Flash-Lite**.
- Füge **kein Zahlungsmittel** in Google AI Studio hinzu. Dann kann auch nichts kosten.
- Laut Googles Bedingungen darf Google Anfragen im Gratis-Kontingent zur Verbesserung seiner Dienste nutzen. Bei Fotos vom Kühlschrank ist das meist unproblematisch.
- Optional kannst du in den Einstellungen auch **Claude** auswählen. Das ist kostenpflichtig (ca. 1–3 Cent pro Anfrage).

---

## Technik (für Neugierige)

- Natives Android-Projekt (Java), die Oberfläche ist HTML/JS in `app/src/main/assets/index.html`
- Kamera über den System-Kamera-Dialog, deshalb ist **keine Kamera-Berechtigung** nötig
- KI-Aufrufe (Gemini oder optional Claude) laufen nativ in `MainActivity.java`; der Schlüssel wird nur lokal auf dem Gerät gespeichert
- Der Build läuft per GitHub Actions (`.github/workflows/build-apk.yml`)
- Die App wird mit dem beiliegenden Schlüssel `app/kochideen.keystore` signiert, damit Updates sich über die alte Version installieren lassen.
  Für eine private App ist das in Ordnung. Für eine Veröffentlichung im Play Store solltest du einen eigenen, geheimen Schlüssel verwenden.
- Nährwerte und Zeiten sind KI-Schätzungen und keine Laborwerte.
