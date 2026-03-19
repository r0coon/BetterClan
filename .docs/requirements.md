# BetterClan — Anforderungen

## Server-Software

| Anforderung | Version |
|------------|---------|
| **Paper** (oder Fork davon, z. B. Purpur) | **1.21.x** (getested auf 1.21.11) |
| **Java** | **21** oder neuer |

Spigot und CraftBukkit werden **nicht** offiziell unterstützt — das Plugin nutzt Paper-spezifische APIs (`AsyncChatEvent`, Adventure-native Components, `ItemStack.serializeAsBytes()`).

---

## Erforderliche Plugins

### ProtocolLib
- **Zweck:** Sendet per-Spieler Glow-Pakete (`ENTITY_METADATA`) damit Kriegsgegner für den jeweiligen Viewer leuchten, ohne global den Glow zu setzen. Ohne ProtocolLib funktioniert der Glow-Effekt **nicht** — alle anderen Funktionen laufen trotzdem.
- **Download:** https://www.spigotmc.org/resources/protocollib.1997/
- **Getestete Version:** 5.3.0

---

## Optionale Plugins

### PlaceholderAPI
- **Zweck:** Stellt alle Clan-Daten als `%betterclan_*%`-Platzhalter für andere Plugins bereit (z. B. TAB, CMI, DeluxeChat, Scoreboards).
- Ohne PAPI werden die Platzhalter schlicht nicht registriert; das Plugin läuft weiterhin normal.
- **Download:** https://www.spigotmc.org/resources/placeholderapi.6245/
- **Getestete Version:** 2.11.6

---

## Build-Abhängigkeiten (nur zum Kompilieren)

Die folgenden Abhängigkeiten werden beim Build über Gradle geladen und sind **nicht** als separate Plugin-JARs auf dem Server erforderlich:

| Abhängigkeit | Scope | Verwendung |
|-------------|-------|------------|
| `paper-api:1.21.11-R0.1-SNAPSHOT` | `compileOnly` | Minecraft-API |
| `ProtocolLib:5.3.0` | `compileOnly` | Glow-Pakete |
| `placeholderapi:2.11.6` | `compileOnly` | PAPI-Expansion |
| `gson:2.10.1` | `implementation` (gebundelt) | JSON-Serialisierung (`clans.json`) |

Gson wird in die Plugin-JAR eingebettet (`implementation`), muss also **nicht** separat installiert werden.

---

## Gradle / Build

```bash
# Plugin bauen
./gradlew build

# Testserver starten (Paper 1.21.11 wird automatisch heruntergeladen)
./gradlew runServer
```

- Java-Toolchain: **JDK 21**
- Ausgabe-JAR: `build/libs/BetterClan-*.jar`
- Testserver-Root: `run/` im Projektordner

---

## Zusammenfassung: Was muss auf dem Server liegen?

```
plugins/
  BetterClan.jar          ← dieses Plugin
  ProtocolLib.jar        ← zwingend erforderlich
  PlaceholderAPI.jar     ← optional, für %betterclan_*% Platzhalter
```
