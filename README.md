# BetterClan

## Setup

**Voraussetzungen:** 
- DK 21, ein Paper-Server (1.21.1+),
- ProtocolLib

```bash
# Jar bauen
chmod +x PRESTART
./PRESTART          # einmalig: optionale DB-Konfiguration + Build
./gradlew build     # danach für Updates
```

Das fertige Jar liegt unter `.build/libs/BetterClan-*.jar` — einfach in den `plugins/`-Ordner des Live-Servers kopieren und neu starten.

> `./gradlew runServer` 

### Optionales Plugin
| Plugin | Funktion |
|---|---|
| **PlaceholderAPI** | Platzhalter für andere Plugins |

### Speicher
Standardmäßig wird alles als YAML gespeichert (`plugins/BetterClan/clan/<clanname>.yml`).
Für Datenbank-Betrieb in der `config.yml` auf `type: database` umstellen und
MariaDB/MySQL/PostgreSQL-Zugangsdaten eintragen.

### Admin-Befehle
| Befehl | Beschreibung |
|---|---|
| `/sclan` | Admin-GUI öffnen |
| `/sclan user <name>` | Spieler direkt verwalten |
| `/sclan clan <name>` | Clan direkt verwalten |
| `/sclan reload` | config.yml neu einlesen |

---

## PlaceholderAPI
| Platzhalter | Beschreibung |
|-------------|----------------|
| `%betterclan_has_clan%` | `ja` / `nein` |
| `%betterclan_name%` | Clanname oder `-` |
| `%betterclan_tag%` | Tag mit Farbformat (Legacy) oder `§7[]` |
| `%betterclan_tag_mini%` | Kürzeres Tag `[Name]` mit Farbe oder `§7[]` |
| `%betterclan_tag_format%` | Roh-Format-String der Tag-Farbe oder leer |
| `%betterclan_level%` | Level (`0` ohne Clan) |
| `%betterclan_xp%` | XP (`0` ohne Clan) |
| `%betterclan_members%` | Mitgliederanzahl |
| `%betterclan_online%`  Online-Mitglieder im Clan |
| `%betterclan_allies%` | Anzahl Allianzen |
| `%betterclan_rank%` | Rang des Spielers (farbig) oder `-` |
| `%betterclan_rank_plain%` | Rang ohne Farbcodes oder `-` |
| `%betterclan_in_war%` | `ja` / `nein` |
| `%betterclan_war_opponent%` | Gegner-Clan im Krieg oder `-` |
| `%betterclan_war_kills%` | Kills des Clans im aktiven Krieg |
| `%betterclan_base_world%` | Welt der Clan-Basis oder `-` |
| `%betterclan_base_x%` | X der Basis (Block) oder `-` |
| `%betterclan_base_y%` | Y der Basis oder `-` |
| `%betterclan_base_z%` | Z der Basis oder `-` |
|