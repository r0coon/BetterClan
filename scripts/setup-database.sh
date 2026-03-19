#!/usr/bin/env bash
set -euo pipefail

# --- Hilfen: Vorhandensein & optionale Installation ---

have_cmd() { command -v "$1" >/dev/null 2>&1; }

detect_pm() {
    if have_cmd apt-get; then echo apt
    elif have_cmd dnf; then echo dnf
    elif have_cmd yum; then echo yum
    elif have_cmd pacman; then echo pacman
    elif have_cmd apk; then echo apk
    elif have_cmd zypper; then echo zypper
    else echo unknown
    fi
}

can_sudo() { have_cmd sudo && sudo -n true 2>/dev/null || sudo -v 2>/dev/null; }

try_install_pkgs() {
    local pm pkgs
    pm="$(detect_pm)"
    pkgs=("$@")
    case "$pm" in
        apt)
            sudo apt-get update -qq && sudo apt-get install -y "${pkgs[@]}"
            ;;
        dnf)
            sudo dnf install -y "${pkgs[@]}"
            ;;
        yum)
            sudo yum install -y "${pkgs[@]}"
            ;;
        pacman)
            sudo pacman -S --noconfirm "${pkgs[@]}"
            ;;
        apk)
            sudo apk add "${pkgs[@]}"
            ;;
        zypper)
            sudo zypper install -y "${pkgs[@]}"
            ;;
        *)
            return 1
            ;;
    esac
}

offer_install_python3() {
    echo ""
    echo "Hinweis: python3 fehlt — wird benötigt, um config.yml zu schreiben."
    read -rp "Soll installiert werden (Paketmanager wird erkannt)? [j/N]: " ans
    local a; a="$(printf '%s' "$ans" | tr '[:upper:]' '[:lower:]')"
    if [[ "$a" != j && "$a" != ja && "$a" != y && "$a" != yes ]]; then
        echo "Abbruch: ohne python3 kann die Config nicht automatisch angepasst werden."
        exit 1
    fi
    if ! can_sudo; then
        echo "Kein sudo verfügbar. Bitte manuell installieren, z. B.:"
        echo "  Debian/Ubuntu: sudo apt install python3"
        echo "  Fedora:        sudo dnf install python3"
        exit 1
    fi
    local pm; pm="$(detect_pm)"
    case "$pm" in
        apt)  try_install_pkgs python3 python3-minimal || try_install_pkgs python3 ;;
        dnf|yum) try_install_pkgs python3 ;;
        pacman) try_install_pkgs python3 ;;
        apk) try_install_pkgs python3 ;;
        zypper) try_install_pkgs python3 ;;
        *)
            echo "Paketmanager unbekannt. Bitte python3 selbst installieren."
            exit 1
            ;;
    esac
    if ! have_cmd python3; then
        echo "Installation schlug fehl oder python3 nicht im PATH."
        exit 1
    fi
    echo "python3 ist jetzt verfügbar."
}

# Client für „DB automatisch erstellen“ (mysql/psql)
DB_CLIENT_OK=1

offer_install_db_client() {
    local kind="$1"
    echo ""
    if [[ "$kind" == "postgres" ]]; then
        echo "Hinweis: psql (PostgreSQL-Client) ist nicht installiert."
        echo "         Ohne psql kann die Option „Datenbank automatisch erstellen“ nicht genutzt werden."
    else
        echo "Hinweis: mysql (MariaDB/MySQL-Client) ist nicht installiert."
        echo "         Ohne mysql kann die Option „Datenbank automatisch erstellen“ nicht genutzt werden."
    fi
    read -rp "Client jetzt installieren (Paketmanager wird erkannt)? [j/N]: " ans
    local a; a="$(printf '%s' "$ans" | tr '[:upper:]' '[:lower:]')"
    if [[ "$a" != j && "$a" != ja && "$a" != y && "$a" != yes ]]; then
        echo "Okay — du kannst die DB manuell anlegen; config.yml wird trotzdem geschrieben."
        DB_CLIENT_OK=0
        return 0
    fi
    if ! can_sudo; then
        echo "Kein sudo. Bitte Client selbst installieren, z. B.:"
        echo "  Debian/Ubuntu: sudo apt install mariadb-client   # oder default-mysql-client"
        echo "                 sudo apt install postgresql-client"
        echo "  Fedora:        sudo dnf install mariadb / postgresql"
        DB_CLIENT_OK=0
        return 0
    fi
    local pm; pm="$(detect_pm)"
    if [[ "$kind" == "postgres" ]]; then
        case "$pm" in
            apt) try_install_pkgs postgresql-client ;;
            dnf|yum) try_install_pkgs postgresql ;;
            pacman) try_install_pkgs postgresql ;; # enthält psql
            apk) try_install_pkgs postgresql-client ;;
            zypper) try_install_pkgs postgresql ;;
            *) echo "Unbekannter Paketmanager."; DB_CLIENT_OK=0; return 0 ;;
        esac
        if have_cmd psql; then echo "psql ist jetzt verfügbar."; DB_CLIENT_OK=1
        else echo "Installation von psql fehlgeschlagen oder nicht im PATH."; DB_CLIENT_OK=0; fi
    else
        case "$pm" in
            apt)
                try_install_pkgs mariadb-client || try_install_pkgs default-mysql-client || try_install_pkgs mysql-client
                ;;
            dnf|yum)
                try_install_pkgs mariadb || try_install_pkgs mysql
                ;;
            pacman)
                try_install_pkgs mariadb-clients || try_install_pkgs mysql-clients
                ;;
            apk)
                try_install_pkgs mariadb-client || try_install_pkgs mysql-client
                ;;
            zypper)
                try_install_pkgs mariadb-client || try_install_pkgs mysql-client
                ;;
            *)
                echo "Unbekannter Paketmanager."; DB_CLIENT_OK=0; return 0
                ;;
        esac
        if have_cmd mysql; then echo "mysql-Client ist jetzt verfügbar."; DB_CLIENT_OK=1
        else echo "Installation des mysql-Clients fehlgeschlagen oder nicht im PATH."; DB_CLIENT_OK=0; fi
    fi
}

ensure_python3() {
    have_cmd python3 && return 0
    offer_install_python3
}

ensure_db_client_for_type() {
    DB_CLIENT_OK=1
    if [[ "$1" == "postgres" ]]; then
        have_cmd psql && return 0
        offer_install_db_client postgres
    else
        have_cmd mysql && return 0
        offer_install_db_client mysql
    fi
}

# Standardwerte
DB_TYPE=""
DB_HOST="localhost"
DB_PORT=""
DB_NAME="betterclan"
DB_USER="betterclan"
DB_PASS=""

# Versucht automatisch die config.yml zu finden (Pfade relativ zum Repo-Root)
find_config() {
    local script_dir="$(cd "$(dirname "$0")" && pwd)"
    local root_dir="$(cd "$script_dir/.." && pwd)"

    for path in \
        "$root_dir/run/plugins/BetterClan/config.yml" \
        "$root_dir/src/main/resources/config.yml" \
        "$root_dir/config.yml" \
        "/opt/minecraft/plugins/BetterClan/config.yml"; do

        if [[ -f "$path" ]]; then
            echo "$path"
            return 0
        fi
    done

    # Breite Suche als Fallback
    local found
    found="$(find "$root_dir" /opt/minecraft/plugins -maxdepth 8 -name 'config.yml' -path '*/BetterClan/config.yml' 2>/dev/null | head -1)"
    if [[ -n "$found" ]]; then
        echo "$found"
        return 0
    fi

    return 1
}

# Schreibt oder ersetzt den storage-Bereich in der config.yml
update_config() {
    local file="$1"

    python3 - "$file" "$DB_TYPE" "$DB_HOST" "$DB_PORT" "$DB_NAME" "$DB_USER" "$DB_PASS" <<'PYTHON'
import sys, re

file_path, dialect, host, port, db_name, user, pw = sys.argv[1:8]

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

new_block = f"""storage:
  type: database
  auto-save-interval: 10
  database:
    dialect: {dialect}
    host: {host}
    port: {port}
    database: {db_name}
    username: {user}
    password: {pw}
    pool-size: 5"""

pattern = r'^storage:.*?(?=\n[a-zA-Z]|\Z)'

if re.search(pattern, content, re.MULTILINE | re.DOTALL):
    content = re.sub(pattern, new_block, content, count=1, flags=re.MULTILINE | re.DOTALL)
else:
    content = content.rstrip() + '\n\n' + new_block + '\n'

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
PYTHON
}

echo ""
echo "======================================"
echo "         Setup Database Script"
echo "======================================"
echo ""

# Datenbanktyp auswählen
echo "Welche Datenbank möchtest du verwenden?"
echo "1) MariaDB"
echo "2) MySQL"
echo "3) PostgreSQL"
echo ""

read -rp "Auswahl [1-3]: " choice

case "$choice" in
    1) DB_TYPE="mariadb"; DB_PORT=3306 ;;
    2) DB_TYPE="mysql"; DB_PORT=3306 ;;
    3) DB_TYPE="postgres"; DB_PORT=5432 ;;
    *) echo "Ungültige Auswahl."; exit 1 ;;
esac

echo "Gewählt: $DB_TYPE"
echo ""

ensure_python3

if [[ "$DB_TYPE" == "postgres" ]]; then
    ensure_db_client_for_type postgres
else
    ensure_db_client_for_type mysql
fi

# Verbindungsdaten abfragen
read -rp "Host [$DB_HOST]: " input; DB_HOST="${input:-$DB_HOST}"
read -rp "Port [$DB_PORT]: " input; DB_PORT="${input:-$DB_PORT}"
read -rp "Datenbankname [$DB_NAME]: " input; DB_NAME="${input:-$DB_NAME}"
read -rp "Benutzername [$DB_USER]: " input; DB_USER="${input:-$DB_USER}"

# Passwort (darf nicht leer sein)
while [[ -z "$DB_PASS" ]]; do
    read -rsp "Passwort: " DB_PASS
    echo ""
    [[ -z "$DB_PASS" ]] && echo "Passwort darf nicht leer sein."
done

echo ""

# Optional: DB direkt erstellen
read -rp "Soll die Datenbank automatisch erstellt werden? [j/N]: " create_db

if [[ "$create_db" =~ ^[jJyY]$ ]]; then

    if [[ "$DB_CLIENT_OK" -ne 1 ]]; then
        echo ""
        echo "Der passende DB-Client (mysql bzw. psql) fehlt — automatische Erstellung wird übersprungen."
        echo "Lege die Datenbank manuell an; config.yml wird unten trotzdem gesetzt."
        echo ""
    elif [[ "$DB_TYPE" == "postgres" ]]; then
        read -rp "PostgreSQL Admin-User [postgres]: " pg_admin
        pg_admin="${pg_admin:-postgres}"

        echo "Erstelle PostgreSQL Datenbank und Benutzer..."

        sudo -u "$pg_admin" psql <<SQL
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '${DB_USER}') THEN
        CREATE ROLE ${DB_USER} LOGIN PASSWORD '${DB_PASS}';
    END IF;
END
\$\$;

SELECT 'CREATE DATABASE ${DB_NAME} OWNER ${DB_USER}'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${DB_NAME}')\gexec

GRANT ALL PRIVILEGES ON DATABASE ${DB_NAME} TO ${DB_USER};
SQL

        echo "Fertig."

    elif [[ "$DB_TYPE" == "mariadb" || "$DB_TYPE" == "mysql" ]]; then
        echo -n "Root-Passwort für $DB_TYPE (leer = keines): "
        read -rs root_pass
        echo ""

        echo "Erstelle Datenbank und Benutzer..."

        mysql_args=(-u root)
        [[ -n "$root_pass" ]] && mysql_args+=("-p${root_pass}")

        mysql "${mysql_args[@]}" <<SQL
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'%';
FLUSH PRIVILEGES;
SQL

        echo "Fertig."
    fi

    echo ""
fi

# config.yml finden
if CONFIG_FILE="$(find_config 2>/dev/null)"; then
    echo "Config gefunden: $CONFIG_FILE"
else
    echo "FEHLER: config.yml konnte nicht gefunden werden. Starte den Server einmal, damit sie erzeugt wird."
    exit 1
fi

# config bearbeiten
if [[ -f "$CONFIG_FILE" ]]; then
    update_config "$CONFIG_FILE"
    echo "config.yml wurde aktualisiert."
else
    echo ""
    echo "Keine config.yml gefunden. Trage folgende Werte manuell ein:"
    echo ""
    cat <<EOF
storage:
  type: database
  auto-save-interval: 10
  database:
    dialect: $DB_TYPE
    host: $DB_HOST
    port: $DB_PORT
    database: $DB_NAME
    username: $DB_USER
    password: $DB_PASS
    pool-size: 5
EOF
fi