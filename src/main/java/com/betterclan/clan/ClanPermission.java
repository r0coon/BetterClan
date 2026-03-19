package com.betterclan.clan;

public enum ClanPermission {
    INVITE("Einladen", "§a", "Spieler in den Clan einladen"),
    KICK("Kicken", "§c", "Spieler aus dem Clan kicken"),
    PROMOTE("Befördern", "§6", "Spieler befördern/degradieren"),
    VAULT("Vault", "§b", "Zugriff auf den Clan-Vault"),
    SETTINGS("Einstellungen", "§d", "Clan-Einstellungen ändern"),
    WAR("Krieg", "§4", "Clan-Kriege starten/beenden"),
    ALLY("Allianzen", "§9", "Allianzen verwalten"),
    SET_BASE("Base setzen", "§2", "Clan-Base-Position setzen"),
    VIEW_LOG("Log einsehen", "§8", "Clan-Log einsehen"),
    TELEPORT("Teleportieren", "§a", "Zur Clan-Base & Warps teleportieren"),
    CHAT_ALLY("Ally-Chat", "§3", "Den Ally-Chat einsehen und darin schreiben"),
    CLAN_CHAT("Clan-Chat", "§7", "Den Clan-Chat einsehen und darin schreiben"),
    RENAME("Umbenennen", "§d", "Clan umbenennen"),
    SET_TAG("Tag-Farbe", "§5", "Tag-Farbe ändern"),
    FRIENDLY_FIRE("Friendly Fire", "§6", "Friendly Fire umschalten"),
    MANAGE_RANKS("Ränge verwalten", "§e", "Rang-Management öffnen");

    private final String displayName;
    private final String color;
    private final String description;

    ClanPermission(String displayName, String color, String description) {
        this.displayName = displayName;
        this.color = color;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    @SuppressWarnings("unused")
    public String getColoredName() {
        return color + displayName;
    }

    public String getDescription() {
        return description;
    }
}

