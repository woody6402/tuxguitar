# TuxGuitar

## Feature branches

  - feature/browser-main
      - Adds optional search to the Collection Browser.
      - Adds persistent star ratings for files.
      - Adds a filter for showing rated files.

  - feature/browser-keep-open
      - Hides the Collection Browser instead of disposing it.
      - Preserves the currently opened collection while the application is running.
      - Prevents saved collections from being removed when a browser provider is unloaded.

  - feature/page-up-down
      - Adds Page Up and Page Down scrolling actions.
      - Includes default keyboard shortcuts.
      - Scrolls the score view by approximately one visible page.

  - feature/playback-delay
      - Adds a configurable visual playback delay from -5000 to +5000 milliseconds.
      - Delays or advances the played-beat highlight and transport position.
      - Uses 0 ms to disable the adjustment.
      - Adds the setting under the Player menu.

  - fix/main-window-resize-overflow
      - Prevents the tablature and other child controls from overflowing the main window after resizing.
      - Recalculates the main-window layout after a song is loaded.
      - Ensures score wrapping follows the visible window width.

## Description

TuxGuitar is an open source multitrack tablature editor and player written in Java.

## Download and installation

The official website for TuxGuitar is https://www.tuxguitar.app/.

You can find ready to use installation packages for Linux, Windows, macOS, FreeBSD and Android here: [releases](https://github.com/helge17/tuxguitar/releases/).

Please download TuxGuitar only from the website or the GitHub link provided above.

To build TuxGuitar from source code, refer to the [INSTALL.md](INSTALL.md) file.

## Contribute

If you want to contribute to TuxGuitar, you will find a helpful description in the [CONTRIBUTING.md](docs/CONTRIBUTING.md) file.

## License

TuxGuitar is released under the GNU Lesser General Public License.

Copyright (C) 2005-2022 Julián Casadesús
              2023-2025 guiv42, helge17

## Third party products

TuxGuitar includes the following third party products:

* SWT version: SWT (Standard Widget Toolkit): https://www.eclipse.org/swt/
* JFX version: JavaFX (Java client application platform): https://openjfx.io/
* Gervill (Java Software Synthesizer)
* iText (Free Java-PDF library): https://itextpdf.com/
* Magic Sound Font v2.0 - Contributed by Dennis Deutschmann

## Local build with external plugins

The `ascii-import` and `browser-web` plugins are maintained in separate Git repositories. Clone them into the paths expected by the desktop build:

```bash
git clone git@github.com:woody6402/tuxguitar-ascii-import.git common/TuxGuitar-ascii-import
git clone git@github.com:woody6402/tuxguitar-browser-web.git desktop/TuxGuitar-browser-web
```

For a reproducible build, use these commits:

```text
ascii-import: a4faf2f
browser-web:  da7b9bf
```

Then build the Linux SWT package with:

```bash
cd desktop/build-scripts/tuxguitar-linux-swt
mvn -e package -P native-modules -Dmaven.test.skip=true
```
