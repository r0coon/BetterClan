package com.betterclan.gui;

import com.betterclan.clan.*;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("deprecation")
public final class MainMenu {

    private MainMenu() {}

    public static void open(ClanManager manager, Player player) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) openNoClan(manager, player);
        else openInClan(manager, player, clan);
    }

    private static void openNoClan(ClanManager manager, Player player) {
        GuiHolder holder = new GuiHolder(MenuType.MAIN_NO_CLAN);
        Inventory inv = Bukkit.createInventory(holder, 45, "§b§lClan-Menü");

        ItemStack _w  = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        ItemStack _lb = GuiHelper.filler(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemStack _pk = GuiHelper.filler(Material.PINK_STAINED_GLASS_PANE);

        inv.setItem(0, _w.clone()); inv.setItem(1, _w.clone()); inv.setItem(2, _lb.clone());
        inv.setItem(3, _w.clone()); inv.setItem(5, _w.clone());
        inv.setItem(6, _lb.clone()); inv.setItem(7, _w.clone()); inv.setItem(8, _w.clone());

        inv.setItem(9, _w.clone()); inv.setItem(10, _pk.clone());
        inv.setItem(16, _pk.clone()); inv.setItem(17, _w.clone());

        inv.setItem(27, _w.clone()); inv.setItem(28, _pk.clone());
        inv.setItem(34, _pk.clone()); inv.setItem(35, _w.clone());

        inv.setItem(36, _w.clone()); inv.setItem(37, _w.clone()); inv.setItem(38, _lb.clone());
        inv.setItem(42, _lb.clone()); inv.setItem(43, _w.clone()); inv.setItem(44, _w.clone());

        inv.setItem(4, new ItemBuilder(Material.WHITE_BANNER)
                .name("§b§lClan-System")
                .lore("",
                        "§8| §7Du bist derzeit in keinem Clan.",
                        "",
                        "§8| §7Erstelle oder tritt einem bei!")
                .build());

        int inviteCount = manager.getActiveInvites(player.getUniqueId()).size();
        boolean inviteBlocked = manager.isInviteBlocked(player.getUniqueId());
        inv.setItem(20, new ItemBuilder(Material.WRITABLE_BOOK)
                .name("§e§l Einladungen" + (inviteCount > 0 ? " §c(" + inviteCount + ")" : ""))
                .lore("",
                        "§8| §7Zeige offene Clan-Einladungen.",
                        inviteCount > 0
                                ? "§aDu hast §f" + inviteCount + " §aoffene Einladung(en)!"
                                : "§7Keine offenen Einladungen.",
                        "",
                        "§8| §7Einladungen: " + (inviteBlocked ? "§cBlockiert" : "§aErlaubt"),
                        "",
                        "§eLinksklick: §7Einladungen anzeigen",
                        "§eRechtsklick: §7Einladungen " + (inviteBlocked ? "erlauben" : "blockieren"))
                .build());

        inv.setItem(22, new ItemBuilder(Material.NETHER_STAR)
                .name("§a§l Clan erstellen")
                .lore("",
                        "§8| §7Gründe deinen eigenen Clan",
                        "§8| §7und werde zum Anführer!",
                        "",
                        "§eKlicke zum Erstellen")
                .glow()
                .build());

        inv.setItem(24, new ItemBuilder(Material.SHIELD)
                .name("§b§l Alle Clans")
                .lore("", "§eKlicke zum Durchsuchen")
                .build());

        inv.setItem(40, GuiHelper.closeButton());
        player.openInventory(inv);
    }

    private static void openInClan(ClanManager manager, Player player, Clan clan) {
        Settings settings = manager.getSettings();
        GuiHolder holder = new GuiHolder(MenuType.MAIN_IN_CLAN);
        Inventory inv = Bukkit.createInventory(holder, 54, "§b§l" + clan.getName());

        var _w  = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        var _lb = GuiHelper.filler(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        var _b  = GuiHelper.filler(Material.BLUE_STAINED_GLASS_PANE);

        inv.setItem(0, _b); inv.setItem(1, _lb); inv.setItem(2, _w);
        inv.setItem(6, _w); inv.setItem(7, _b); inv.setItem(8, _lb);

        inv.setItem(9, _lb); inv.setItem(10, _w);
        inv.setItem(16, _w); inv.setItem(17, _b);

        inv.setItem(18, _w); inv.setItem(26, _w);

        inv.setItem(27, _w); inv.setItem(35, _w);

        inv.setItem(36, _b); inv.setItem(37, _w);
        inv.setItem(43, _w); inv.setItem(44, _lb);

        inv.setItem(45, _lb); inv.setItem(46, _b);
        inv.setItem(47, _w);
        inv.setItem(51, _w); inv.setItem(52, _lb); inv.setItem(53, _b);

        boolean isOwner = clan.getOwnerId().equals(player.getUniqueId());
        boolean canManage = manager.canManage(clan, player.getUniqueId());
        boolean hasSettings = manager.hasPermission(clan, player.getUniqueId(), ClanPermission.SETTINGS);
        boolean hasAlly = manager.hasPermission(clan, player.getUniqueId(), ClanPermission.ALLY);
        boolean hasWar = manager.hasPermission(clan, player.getUniqueId(), ClanPermission.WAR);
        boolean hasVault = manager.hasPermission(clan, player.getUniqueId(), ClanPermission.VAULT);

        String ownerName = Bukkit.getOfflinePlayer(clan.getOwnerId()).getName();
        int onlineCount = manager.getOnlineMemberCount(clan);

        inv.setItem(4, new ItemBuilder(GuiHelper.createClanBanner(clan))
                .name("§b§l" + clan.getName())
                .lore("",
                        "§8| §7Anführer: §f" + (ownerName != null ? ownerName : "Unbekannt"),
                        "§8| §7Mitglieder: §f" + clan.getMemberCount() + "§7/§f" + clan.getMaxMembers(settings),
                        "§8| §7Online: §a" + onlineCount + "§7/§f" + clan.getMemberCount(),
                        "",
                        "§8| §7Level: §e" + clan.getLevel() + " §7| §7Siege: §e" + clan.getXp() + "§7/§e" + clan.getXpForNextLevel(settings))
                .build());

        inv.setItem(12, new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(player)
                .name("§b§l Mitglieder")
                .lore("",
                        "§8| §7Aktuell: §f" + clan.getMemberCount() + "§7/§f" + clan.getMaxMembers(settings),
                        "",
                        "§eKlicke zum Anzeigen")
                .build());

        inv.setItem(14, new ItemBuilder(Material.WRITABLE_BOOK)
                .name("§a§l Spieler einladen")
                .lore("", canManage ? "§eKlicke zum Einladen" : "§cKeine Berechtigung")
                .build());

        java.util.List<String> settingsLore = new java.util.ArrayList<>();
        settingsLore.add("");
        if (isOwner) {
            settingsLore.add("§7Verwalte deinen Clan:");
            settingsLore.add("§7• Umbenennen, Banner, Ränge");
            settingsLore.add("§7• Tag-Farbe, Friendly Fire");
            settingsLore.add("§7• Clan auflösen");
        } else if (hasSettings) {
            settingsLore.add("§7• Banner-Design ändern");
        } else {
            settingsLore.add("§7• Clan verlassen");
        }
        settingsLore.add("");
        settingsLore.add("§eKlicke zum Öffnen");
        inv.setItem(22, new ItemBuilder(Material.ANVIL)
                .name("§6§l Einstellungen")
                .lore(settingsLore.toArray(new String[0]))
                .build());

        ClanWar activeWar = manager.getActiveWar(clan.getName());
        int warRequestCount = manager.getWarRequests(clan.getOwnerId()).size();
        inv.setItem(29, new ItemBuilder(Material.IRON_SPEAR)
                .name("§4§l Clan-Krieg" + (activeWar != null ? " §c§lAKTIV" : "")
                        + (warRequestCount > 0 ? " §c(" + warRequestCount + ")" : ""))
                .lore("",
                        activeWar != null
                                ? "§c§lKrieg vs. " + activeWar.getOpponent(clan.getName())
                                : "§7Kein aktiver Krieg.",
                        activeWar != null
                                ? "§7Verbleibend: §f" + activeWar.getRemainingTimeFormatted()
                                : "",
                        "",
                        hasWar ? "§eKlicke zum Anzeigen" : "§7Klicke zum Anzeigen")
                .flags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                .build());

        inv.setItem(31, new ItemBuilder(Material.SPYGLASS)
                .name("§b§l Alle Clans")
                .lore("",
                        "§8| §7Durchsuche alle Clans.",
                        "§8| §7Von dort: Krieg, Historie & mehr.",
                        "",
                        "§eKlicke zum Öffnen")
                .build());

        int allyCount = clan.getAllies().size();
        int requestCount = clan.getAllyRequests().size();
        inv.setItem(33, new ItemBuilder(Material.SHIELD)
                .name("§e§l Bündnisse" + (requestCount > 0 ? " §c(" + requestCount + ")" : ""))
                .lore("",
                        "§8| §7Verbündete Clans: §f" + allyCount,
                        requestCount > 0 ? "§aAnfragen: §f" + requestCount : "",
                        "",
                        hasAlly ? "§eKlicke zum Verwalten" : "§7Klicke zum Ansehen")
                .build());

        inv.setItem(39, new ItemBuilder(Material.COMPASS)
                .name("§b§l Warp-Liste")
                .lore("", "§eKlicke zum Öffnen")
                .build());

        int vaultSize = manager.getSettings().getVaultSize(clan.getLevel());
        inv.setItem(41, new ItemBuilder(Material.BUNDLE)
                .name("§6§l Clan-Vault")
                .lore("",
                        "§8" + vaultSize + " Slots",
                        "",
                        hasVault ? "§eKlicke zum Öffnen" : "§cKeine Berechtigung")
                .flags(org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                .build());

        inv.setItem(49, GuiHelper.closeButton());
        player.openInventory(inv);
    }
}

