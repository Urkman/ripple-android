# Ripple für Android

Ripple ist ein Wasser-Tracker für Android und Wear OS. Dieses Projekt ist die **KI-gestützte, eigenständige Umsetzung** der bestehenden iOS-App für Android. Produktverhalten, Datenregeln und Gestaltung werden anhand gemeinsamer Spezifikationen übertragen; Bedienung, Navigation und Systemintegration folgen den Android-Konventionen.

- [iOS-App im App Store](https://apps.apple.com/us/app/ripple-water-tracker/id6808143149)
- [Quellcode der iOS-App auf GitHub](https://github.com/Urkman/ripple)

## Die App

Ripple hilft dabei, die tägliche Wasseraufnahme unkompliziert festzuhalten und den Überblick über das persönliche Trinkziel zu behalten.

- **Heute:** Getrunkene Menge, Tagesziel und verbleibende Menge auf einen Blick; häufig genutzte Behälter und eigene Mengen lassen sich schnell eintragen. Der Wasserstand visualisiert den Fortschritt.
- **Verlauf:** Kalender und Tagesansicht mit einzelnen Einträgen, die bearbeitet, gelöscht oder wiederhergestellt werden können.
- **Statistiken:** Auswertungen für Woche, Monat und Jahr, unter anderem zu Trinkmenge und Zielerreichung.
- **Einstellungen:** Einheiten (ml oder fl oz), Trinkziel, Behälter, Erinnerungen, optionale Health-Connect-Anbindung und Datenexport.
- **Schnelle Zugänge:** Android-Widget, Schnelleinstellung, Benachrichtigungsaktionen und App-Verknüpfungen. Die Wear-OS-App bietet eigene Ansichten für Heute, Verlauf und Statistiken sowie schnelles Erfassen am Handgelenk.

Ripple ist auf Wasser ausgerichtet. Die Android-App verwendet einen lokalen Datenspeicher; ein Konto oder ein eigener Backend-Dienst ist nicht erforderlich. Die Android- und iOS-Versionen teilen keine Nutzerdaten.

## KI-gestützte Konvertierung

Ausgangspunkt ist die veröffentlichte iOS-App. Ihr öffentliches Repository und die gemeinsamen Produktunterlagen dienen als Referenz für Funktionen und Nutzerabläufe. Die KI-gestützte Android-Entwicklung setzt die App in Kotlin und Jetpack Compose eigenständig um und überprüft die Ergebnisse anhand von Builds, Tests und Emulatorläufen. Eine identische Oberfläche oder eine direkte Übernahme von Swift-Code ist nicht das Ziel.

Die verbindlichen Produkt- und Plattformverträge liegen unter [`docs/shared/`](docs/shared/README.md). Besonders relevant sind die [Produktbeschreibung](docs/shared/Ripple_PRD.md), der [Screen-Katalog](docs/shared/Ripple_SCREEN_CATALOG.md), das [Datenmodell](docs/shared/Ripple_DATA_MODEL.md), das [Designsystem](docs/shared/Ripple_DESIGN_SYSTEM.md) sowie die [Android-Architektur](docs/shared/Android/ANDROID_ARCHITECTURE.md) und [Android-UI-Spezifikation](docs/shared/Android/ANDROID_UI_SPEC.md). Die [Zuordnung von Dokumentation zu Code](docs/ANDROID_DOCUMENTATION_TO_CODE_MAP.md) hält Implementierung und Verifikationsstand der einzelnen Oberflächen fest.

## Architektur

Das Projekt ist ein modularer Gradle-Build mit einer Android-App und einer separaten Wear-OS-App:

| Modul | Aufgabe |
| --- | --- |
| `app` | Einstiegspunkt, Abhängigkeitsaufbau und adaptive Navigation für Smartphone und Tablet |
| `feature:*` | Compose-Oberflächen und ViewModels für Heute, Verlauf, Statistiken, Einstellungen und Onboarding |
| `core:domain` | Datenmodelle, Regeln und Anwendungsfälle wie `LogIntake` und Undo |
| `core:storage` | Room-Datenbank und Repository für die lokalen Produktdaten |
| `core:designsystem` | Gemeinsame Farben, Typografie und UI-Komponenten |
| `core:preferences`, `core:health`, `core:wear-sync` | Widget-Projektion, Health Connect und Datenaustausch mit Wear OS |
| `system:*` | Widget, Schnelleinstellung, Erinnerungen, App-Aktionen und Export |
| `wear` | Wear-OS-App mit Compose for Wear OS und lokalem Datenspeicher |

Die Oberfläche stellt Zustände dar und sendet Aktionen an ViewModels. Die Domäne definiert die fachlichen Operationen; das Repository speichert Einträge in Room. Systemflächen verwenden dieselben Operationen wie die Haupt-App. Health Connect erhält Daten nur optional als Projektion und ist keine zweite Datenquelle. Smartphone und Wear OS gleichen ihre lokalen Daten über die Wear Data Layer ab; iOS nutzt einen davon unabhängigen Datenbestand.

## Projekt lokal bauen

Benötigt werden Android Studio beziehungsweise ein Android SDK mit API 37 und JDK 17. Der Gradle Wrapper ist im Projekt enthalten.

```bash
./gradlew :app:assembleDebug :wear:assembleDebug
```

Die erzeugten Debug-APKs liegen unter `app/build/outputs/apk/debug/` und `wear/build/outputs/apk/debug/`. Für die Prüfung von Domänenregeln und Codequalität können die vorhandenen Gradle-Aufgaben für Tests und Lint ausgeführt werden; den zuletzt dokumentierten Prüfstand und noch offene Laufzeitprüfungen beschreibt die [Dokumentation-zu-Code-Zuordnung](docs/ANDROID_DOCUMENTATION_TO_CODE_MAP.md).
