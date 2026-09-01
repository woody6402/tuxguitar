# Playback delay – technische Dokumentation

## Zweck und Verhalten

Die Funktion verschiebt ausschließlich die visuelle Wiedergabeposition. Die MIDI- und Audioausgabe selbst wird nicht verzögert oder vorgezogen.

- Ein positiver Wert, etwa `300 ms`, lässt Markierung und Transportanzeige 300 ms später erscheinen.
- Ein negativer Wert, etwa `-300 ms`, lässt sie 300 ms früher erscheinen.
- `0 ms` verwendet unverändert die tatsächliche Playerposition.
- Zulässig sind `-5000` bis `+5000 ms`.
- Der Wert wird nur im laufenden Programm gehalten und nicht dauerhaft gespeichert.

Der Datenfluss ist:

1. Der Menüeintrag öffnet den Dialog.
2. Der Dialog liest den aktuellen Wert aus `TGTransportCache`.
3. **OK** schreibt den neuen Wert in `TGTransportCache`.
4. `TGTransportCache` wandelt Millisekunden anhand des aktuellen Tempos in Song-Ticks um.
5. Taktschlag-Markierung und Transportdialog lesen die daraus berechnete visuelle Position.

> Die angegebenen Zeilennummern beziehen sich auf den aktuellen Arbeitsstand. Durch spätere Änderungen können sie sich verschieben.

## 1. Speicherung und Berechnung

Datei: `desktop/TuxGuitar/src/app/tuxguitar/app/transport/TGTransportCache.java`

### Import

| Zeile | Code | Erklärung und Grund |
|---:|---|---|
| 15 | `import app.tuxguitar.song.models.TGTempo;` | Macht den Tempo-Typ verfügbar. Er wird benötigt, weil Millisekunden in die von TuxGuitar verwendeten Song-Ticks umgerechnet werden müssen. |

### Zustand und Initialisierung

| Zeile | Code | Erklärung und Grund |
|---:|---|---|
| 29 | `private int displayDelayMillis;` | Speichert die Verschiebung als ganze Millisekunden. `int` reicht für den kleinen Bereich von ±5000 vollständig aus. Das Feld liegt im Transport-Cache, weil dort auch die visuelle Wiedergabeposition ermittelt wird. |
| 35 | `this.displayDelayMillis = 0;` | Setzt den Startwert auf null. Damit bleibt das ursprüngliche Verhalten aktiv, solange der Benutzer keinen Wert einstellt. Weil keine Konfiguration geschrieben wird, gilt der Wert nur für diese Programmsitzung. |

### Getter

| Zeile | Code | Erklärung und Grund |
|---:|---|---|
| 39 | `public int getDisplayDelayMillis() {` | Öffentliche Lesemethode für Dialog und Menüanzeige. |
| 40 | `return this.displayDelayMillis;` | Liefert den aktuell gespeicherten Wert in Millisekunden zurück. |
| 41 | `}` | Beendet die Getter-Methode. |

### Setter

| Zeile | Code | Erklärung und Grund |
|---:|---|---|
| 43 | `public void setDisplayDelayMillis(int displayDelayMillis) {` | Öffentliche Schreibmethode. Der Parameter ist der gewünschte Wert in Millisekunden. |
| 44 | `int value = Math.max(-5000, Math.min(5000, displayDelayMillis));` | Begrenzt den Wert defensiv auf ±5000. Zuerst begrenzt `Math.min` nach oben auf 5000, danach `Math.max` nach unten auf −5000. Das schützt auch vor ungültigen Aufrufen außerhalb des Dialogs. |
| 45 | `if (this.displayDelayMillis != value) {` | Führt Folgearbeit nur aus, wenn sich der effektive Wert tatsächlich ändert. |
| 46 | `this.displayDelayMillis = value;` | Speichert den begrenzten Wert. |
| 47 | `this.reset();` | Verwirft die zwischengespeicherte visuelle Beat-/Measure-Zuordnung. Ohne Reset könnte kurzzeitig noch die zum alten Delay gehörende Markierung angezeigt werden. |
| 48 | `}` | Beendet die Änderungsprüfung. |
| 49 | `}` | Beendet den Setter. |

### Umrechnung von Millisekunden in Ticks

| Zeile | Code | Erklärung und Grund |
|---:|---|---|
| 51 | `public long getDisplayTickPosition() {` | Liefert die Position, die die Oberfläche anzeigen soll. Die echte Playerposition wird dabei nicht verändert. |
| 52 | `MidiPlayer player = MidiPlayer.getInstance(this.context);` | Holt den zum aktuellen Anwendungskontext gehörenden MIDI-Player. |
| 53 | `long tick = player.getTickPosition();` | Liest die reale aktuelle Wiedergabeposition in Song-Ticks. |
| 54 | `TGTempo tempo = player.getCurrentTempo();` | Liest das aktuell wirksame Tempo. Das ist wichtig, weil dieselbe Millisekundenzahl bei verschiedenen Tempi unterschiedlich vielen Ticks entspricht. |
| 55 | `if (this.displayDelayMillis == 0 || tempo == null) {` | Prüft die beiden Fälle, in denen keine Umrechnung erfolgen soll: Delay ist ausgeschaltet oder es ist noch kein Tempo verfügbar. |
| 56 | `return tick;` | Gibt dann unverändert die reale Position zurück. Damit ist `0 ms` funktional „aus“. |
| 57 | `}` | Beendet den frühen Rückgabefall. |
| 58 | `long effectiveTempo = ((long) tempo.getQuarterValue() * player.getMode().getCurrentPercent()) / 100L;` | Berechnet das effektive Tempo einschließlich des im Abspielmodus gewählten Prozentwerts. Der Cast auf `long` erfolgt vor der Multiplikation und schützt vor `int`-Überlauf. Beispiel: 120 BPM bei 50 % ergeben effektiv 60 BPM. |
| 59 | `long delayTicks = (this.displayDelayMillis * effectiveTempo * TGDuration.QUARTER_TIME) / 60000L;` | Wandelt Millisekunden in Ticks um. `TGDuration.QUARTER_TIME` ist die Tickzahl einer Viertelnote; `60000` ist die Zahl der Millisekunden pro Minute. Formel: `ms × Viertelnoten/min × Ticks/Viertelnote ÷ ms/min`. |
| 60 | `return Math.max(0, tick - delayTicks);` | Verschiebt die visuelle Position. Positives Delay wird abgezogen und zeigt dadurch eine frühere Songposition, also erscheint die Anzeige später. Negatives Delay wird durch das Minus addiert und zieht die Anzeige vor. `Math.max(0, ...)` verhindert eine Position vor Songbeginn. |
| 61 | `}` | Beendet die Berechnungsmethode. |

### Verwendung für die Taktschlag-Markierung

| Zeile | Code | Erklärung und Grund |
|---:|---|---|
| 89 | `long tick = this.getDisplayTickPosition();` | Ersetzt hier die direkte Playerposition durch die verschobene Anzeigeposition. Alle nachfolgenden Berechnungen zur Auswahl von Takt und Beat arbeiten dadurch mit dem Delay. |
| 90 | `long start = this.playStart + (tick - this.playTick);` | Überträgt die globale Anzeigeposition in die Position innerhalb des aktuell verfolgten Wiedergabeabschnitts. Diese vorhandene Berechnung bleibt gleich, erhält jetzt aber den verschobenen Tick. |

## 2. Eingabedialog

Datei: `desktop/TuxGuitar/src/app/tuxguitar/app/view/dialog/transport/TGTransportDisplayDelayDialog.java`

### Paket und Imports

| Zeile | Code | Erklärung und Grund |
|---:|---|---|
| 1 | `package app.tuxguitar.app.view.dialog.transport;` | Ordnet den neuen Dialog den vorhandenen Transportdialogen zu. |
| 3 | `import app.tuxguitar.app.TuxGuitar;` | Wird zum Lesen übersetzter UI-Texte verwendet. |
| 4 | `import app.tuxguitar.app.transport.TGTransport;` | Ermöglicht Zugriff auf den Transport-Cache und damit den Delay-Wert. |
| 5 | `import app.tuxguitar.app.ui.TGApplication;` | Liefert die plattformspezifische UI-Factory. |
| 6 | `import app.tuxguitar.app.view.main.TGWindow;` | Liefert das Hauptfenster als Elternfenster des Dialogs. |
| 7 | `import app.tuxguitar.app.view.util.TGDialogUtil;` | Öffnet und positioniert den Dialog nach den TuxGuitar-Konventionen. |
| 8 | `import app.tuxguitar.ui.UIFactory;` | Abstrakte Factory zum Erzeugen der UI-Elemente. |
| 9 | `import app.tuxguitar.ui.event.UISelectionEvent;` | Ereignistyp für Klicks auf OK und Abbrechen. |
| 10 | `import app.tuxguitar.ui.event.UISelectionListener;` | Listener-Schnittstelle für diese Klicks. |
| 11 | `import app.tuxguitar.ui.layout.UITableLayout;` | Tabellenlayout für Beschriftung, Eingabefeld und Buttons. |
| 12 | `import app.tuxguitar.ui.widget.UIButton;` | UI-Typ für OK und Abbrechen. |
| 13 | `import app.tuxguitar.ui.widget.UILabel;` | UI-Typ für die Feldbeschriftung. |
| 14 | `import app.tuxguitar.ui.widget.UIPanel;` | Container für die Buttonzeile. |
| 15 | `import app.tuxguitar.ui.widget.UISpinner;` | Numerisches Eingabefeld für Millisekunden. |
| 16 | `import app.tuxguitar.ui.widget.UIWindow;` | UI-Typ des Dialogfensters. |
| 17 | `import app.tuxguitar.util.TGContext;` | Anwendungskontext, über den Player, Transport und Fenster derselben Instanz gefunden werden. |

### Klasse und Fenster

| Zeile | Code | Erklärung und Grund |
|---:|---|---|
| 19 | `public class TGTransportDisplayDelayDialog {` | Definiert den neuen Dialogtyp. |
| 21 | `public void show(final TGContext context) {` | Öffentliche Einstiegsmethode. `final` erlaubt den Zugriff auf `context` aus den anonymen Listener-Klassen. |
| 22 | `UIFactory factory = TGApplication.getInstance(context).getFactory();` | Holt die UI-Factory der aktiven Plattform, hier typischerweise SWT. So bleibt der Code vom konkreten Toolkit unabhängig. |
| 23 | `final UIWindow dialog = factory.createWindow(TGWindow.getInstance(context).getWindow(), true, false);` | Erzeugt ein Kindfenster des Hauptfensters. Der erste boolesche Wert macht es modal; der zweite deaktiviert den üblichen Größenänderungsmodus. `final` wird für die Button-Listener benötigt. |
| 24 | `UITableLayout layout = new UITableLayout();` | Erzeugt das Hauptlayout. |
| 25 | `dialog.setLayout(layout);` | Weist dem Fenster dieses Layout zu. |
| 26 | `dialog.setText(TuxGuitar.getProperty("transport.display-delay"));` | Setzt den lokalisierten Fenstertitel „Playback delay“. |

### Beschriftung und Wertefeld

| Zeile | Code | Erklärung und Grund |
|---:|---|---|
| 28 | `UILabel label = factory.createLabel(dialog);` | Erzeugt die Beschriftung als Kind des Dialogs. |
| 29 | `label.setText(TuxGuitar.getProperty("transport.display-delay.milliseconds") + ":");` | Setzt den lokalisierten Hinweis auf Millisekunden und `0 = aus`; der Doppelpunkt trennt Beschriftung und Feld. |
| 30 | `layout.set(label, 1, 1, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_CENTER, false, true);` | Platziert die Beschriftung in Zeile 1, Spalte 1, rechtsbündig und vertikal zentriert. |
| 32 | `final UISpinner delay = factory.createSpinner(dialog);` | Erzeugt das numerische Eingabefeld. `final` erlaubt den Zugriff aus dem OK-Listener. |
| 33 | `delay.setMinimum(-5000);` | Legt den kleinsten eingebbaren Wert fest: Anzeige maximal fünf Sekunden vorziehen. |
| 34 | `delay.setMaximum(5000);` | Legt den größten eingebbaren Wert fest: Anzeige maximal fünf Sekunden verzögern. |
| 35 | `delay.setIncrement(10);` | Pfeiltasten bzw. Spinnerpfeile verändern den Wert in 10-ms-Schritten. Direkte numerische Eingabe bleibt möglich. |
| 36 | `delay.setValue(TGTransport.getInstance(context).getCache().getDisplayDelayMillis());` | Zeigt beim Öffnen den aktuell verwendeten Wert statt immer null. |
| 37 | `layout.set(delay, 1, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, true);` | Platziert das Feld rechts neben der Beschriftung und lässt es die verfügbare Breite nutzen. |

### Buttonbereich und OK

| Zeile | Code | Erklärung und Grund |
|---:|---|---|
| 39 | `UITableLayout buttonsLayout = new UITableLayout(0f);` | Erzeugt ein Layout ohne zusätzlichen Standardabstand für die Buttonzeile. |
| 40 | `UIPanel buttons = factory.createPanel(dialog, false);` | Erzeugt einen gemeinsamen Container für beide Buttons. |
| 41 | `buttons.setLayout(buttonsLayout);` | Verknüpft Container und Buttonlayout. |
| 42 | `layout.set(buttons, 2, 1, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_FILL, true, true, 1, 2);` | Platziert die Buttonzeile in Zeile 2 und lässt sie über beide Spalten laufen; die Buttons werden rechts ausgerichtet. |
| 44 | `UIButton ok = factory.createButton(buttons);` | Erzeugt den OK-Button. |
| 45 | `ok.setText(TuxGuitar.getProperty("ok"));` | Verwendet den vorhandenen lokalisierten OK-Text. |
| 46 | `ok.setDefaultButton();` | Macht OK zum Standardbutton, sodass Enter die Eingabe übernimmt. |
| 47 | `ok.addSelectionListener(new UISelectionListener() {` | Registriert die Reaktion auf den OK-Klick. |
| 48 | `public void onSelect(UISelectionEvent event) {` | Callback, den das UI-Toolkit beim Auslösen des Buttons aufruft. |
| 49 | `TGTransport.getInstance(context).getCache().setDisplayDelayMillis(delay.getValue());` | Liest den Wert aus dem Feld und schreibt ihn über den begrenzenden Setter in den Transport-Cache. Erst diese Zeile übernimmt die Änderung. |
| 50 | `dialog.dispose();` | Schließt und gibt den Dialog nach erfolgreicher Übernahme frei. |
| 51–52 | `}` / `});` | Beenden Callback und Listenerregistrierung. |
| 53–54 | `buttonsLayout.set(...)` | Platziert OK in der ersten Buttonspalte und gibt ihm 80 × 25 Layout-Einheiten als Zielgröße. |

### Abbrechen und Öffnen

| Zeile | Code | Erklärung und Grund |
|---:|---|---|
| 56 | `UIButton cancel = factory.createButton(buttons);` | Erzeugt den Abbrechen-Button. |
| 57 | `cancel.setText(TuxGuitar.getProperty("cancel"));` | Verwendet den vorhandenen lokalisierten Abbrechen-Text. |
| 58 | `cancel.addSelectionListener(new UISelectionListener() {` | Registriert die Reaktion auf Abbrechen. |
| 59 | `public void onSelect(UISelectionEvent event) {` | Callback für den Klick. |
| 60 | `dialog.dispose();` | Schließt den Dialog, ohne den Cache zu verändern. |
| 61–62 | `}` / `});` | Beenden Callback und Listenerregistrierung. |
| 63–64 | `buttonsLayout.set(...)` | Platziert Abbrechen rechts neben OK, ebenfalls mit 80 × 25 Layout-Einheiten. |
| 66 | `TGDialogUtil.openDialog(dialog, TGDialogUtil.OPEN_STYLE_CENTER \| TGDialogUtil.OPEN_STYLE_PACK);` | Passt das Fenster an seinen Inhalt an (`PACK`), zentriert es (`CENTER`) und öffnet es. |
| 67 | `}` | Beendet `show`. |
| 68 | `}` | Beendet die Klasse. |

## 3. Eintrag im Player-Menü

Datei: `desktop/TuxGuitar/src/app/tuxguitar/app/view/menu/impl/TransportMenuItem.java`

| Zeile | Code | Erklärung und Grund |
|---:|---|---|
| 12 | `import app.tuxguitar.app.view.dialog.transport.TGTransportDisplayDelayDialog;` | Macht den neuen Dialog im Menücode verfügbar. |
| 25 | `import app.tuxguitar.ui.event.UISelectionEvent;` | Ereignistyp des neuen Menü-Listeners. |
| 26 | `import app.tuxguitar.ui.event.UISelectionListener;` | Listener-Schnittstelle für das Anklicken des Eintrags. |
| 41 | `private UIMenuActionItem displayDelay;` | Hält den Menüeintrag als Feld, damit Text und Icon später aktualisiert werden können. |
| 92 | `this.displayDelay = this.transportMenuItem.getMenu().createActionItem();` | Erzeugt einen normalen anklickbaren Eintrag im Player-Menü. Es ist bewusst kein separater Ein/Aus-Schalter, weil `0 ms` bereits „aus“ bedeutet. |
| 93 | `this.displayDelay.addSelectionListener(new UISelectionListener() {` | Registriert die Klickbehandlung. |
| 94 | `public void onSelect(UISelectionEvent event) {` | Callback für die Auswahl des Menüeintrags. |
| 95 | `new TGTransportDisplayDelayDialog().show(findContext());` | Erzeugt und öffnet den Dialog im aktuellen Anwendungskontext. |
| 96–97 | `}` / `});` | Beenden Callback und Listenerregistrierung. |
| 120 | `this.loadDisplayDelayText();` | Aktualisiert den angezeigten Wert bei regulären Menü-Updates. Dadurch zeigt das nächste Öffnen des Menüs den zuletzt übernommenen Wert. |
| 136 | `this.loadDisplayDelayText();` | Setzt denselben Text beim Laden bzw. Neuladen der Spracheigenschaften. |
| 139 | `private void loadDisplayDelayText() {` | Kapselt den dynamischen Menütext in einer Hilfsmethode. |
| 140–141 | `int value = ...getDisplayDelayMillis();` | Liest den aktuellen Millisekundenwert aus dem Transport-Cache. |
| 142 | `this.displayDelay.setText(TuxGuitar.getProperty("transport.display-delay") + "... (" + value + " ms)");` | Baut beispielsweise `Playback delay... (300 ms)`. Der Wert ist damit sichtbar, ohne den Dialog zu öffnen. |
| 143 | `}` | Beendet die Hilfsmethode. |
| 154 | `this.displayDelay.setImage(...TGIconManager.TRANSPORT_TIME_COUNTER);` | Verwendet das vorhandene Zeitanzeige-Icon. Dadurch ist keine zusätzliche Bilddatei nötig und das Aussehen bleibt konsistent. |

## 4. Transportdialog und Fortschrittsanzeige

Datei: `desktop/TuxGuitar/src/app/tuxguitar/app/view/dialog/transport/TGTransportDialog.java`

| Zeile | Code | Erklärung und Grund |
|---:|---|---|
| 471 | `TGTransportCache transportCache = TGTransport.getInstance(this.context).getCache();` | Holt den Cache, der neben den bestehenden Transportdaten jetzt auch die visuelle Delay-Position liefert. Diese Zeile bestand bereits, wird aber für die neue Folgelinie benötigt. |
| 475 | `long position = (transportCache.getPlayStart()` | Beginnt die Berechnung der im Transportdialog angezeigten Position relativ zum Wiedergabestart. |
| 476 | `+ (transportCache.getDisplayTickPosition() - transportCache.getPlayTick()));` | Verwendet die visuell verschobene Position anstelle von `player.getTickPosition()`. Dadurch folgen Tick-Text und Fortschrittsbalken demselben Delay wie die Beat-Markierung. |
| 477 | `this.updateTickLabel(Long.toString(position));` | Zeigt die berechnete Position als Text an; diese vorhandene Zeile erhält nun den verschobenen Wert. |
| 478 | `this.tickProgress.setValue((int) position);` | Setzt den Fortschrittsbalken auf denselben verschobenen Wert. |

## 5. Übersetzungstexte

Dateien:

- `common/resources/lang/messages.properties`
- `common/resources/lang/messages_de.properties`

| Schlüssel | Englisch | Deutsch | Verwendung |
|---|---|---|---|
| `transport.display-delay` | `Playback delay` | `Playback delay` | Menütext und Fenstertitel. Der gewünschte englische Funktionsname bleibt auch in der deutschen Oberfläche erhalten. |
| `transport.display-delay.milliseconds` | `Delay in milliseconds (0 = off)` | `Verzögerung in Millisekunden (0 = aus)` | Beschriftung des Zahlenfeldes. Der Wertebereich wird absichtlich nicht im sichtbaren Text genannt. |

## 6. Mathematisches Beispiel

Angenommen:

- Tempo: 120 Viertelnoten pro Minute
- Abspielgeschwindigkeit: 100 %
- Delay: `500 ms`
- `TGDuration.QUARTER_TIME`: Tickzahl einer Viertelnote

Dann gilt:

```text
effectiveTempo = 120 × 100 / 100 = 120
delayTicks = 500 × 120 × QUARTER_TIME / 60000
           = 1 × QUARTER_TIME
```

Bei 120 BPM entsprechen 500 ms genau einer Viertelnote. Diese Tickzahl wird von der echten Playerposition abgezogen. Die Oberfläche zeigt daher die Position von vor 500 ms.

Bei `-500 ms` wird `delayTicks` negativ. Das Subtrahieren eines negativen Werts addiert eine Viertelnote; die Oberfläche zeigt die zukünftige Position und wird damit vorgezogen.

## 7. Bewusste Grenzen

- Die Funktion synchronisiert die Anzeige manuell; Bluetooth wird nicht automatisch erkannt.
- Sie verändert weder MIDI-Timing noch Audioausgabe.
- Der Wert ist nicht dauerhaft gespeichert und steht nach einem Neustart wieder auf `0 ms`.
- Vor dem Songbeginn wird die berechnete Position auf Tick `0` begrenzt.
- Ein negatives Delay kann am Songende rechnerisch über die aktuelle Playerposition hinausweisen. Die normale TuxGuitar-Logik entscheidet dann, ob noch ein passender Beat vorhanden ist.
