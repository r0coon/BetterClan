package com.betterclan.gui.misc;

import com.betterclan.gui.GuiHelper;
import com.betterclan.gui.GuiHolder;
import com.betterclan.gui.MenuType;
import com.betterclan.clan.Clan;
import com.betterclan.clan.Manager;
import com.betterclan.clan.Permission;
import com.betterclan.listener.ChatInput;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

@SuppressWarnings("deprecation")
public final class SettingsMenu {

    private SettingsMenu() {}

    @SuppressWarnings("unused")
    public static void open(Manager manager, ChatInput chatInput, Player player) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) return;
        boolean isOwner      = clan.getOwnerId().equals(player.getUniqueId());
        boolean hasSettings  = manager.hasPermission(clan, player.getUniqueId(), Permission.SETTINGS);
        boolean hasRename    = manager.hasPermission(clan, player.getUniqueId(), Permission.RENAME);
        boolean hasManageRanks = manager.hasPermission(clan, player.getUniqueId(), Permission.MANAGE_RANKS);
        boolean hasFF        = manager.hasPermission(clan, player.getUniqueId(), Permission.FRIENDLY_FIRE);
        boolean hasSetTag    = manager.hasPermission(clan, player.getUniqueId(), Permission.SET_TAG);
        boolean hasLog       = manager.hasPermission(clan, player.getUniqueId(), Permission.VIEW_LOG);
        boolean ffOn         = clan.isFriendlyFire();

        GuiHolder holder = new GuiHolder(MenuType.SETTINGS);
        Inventory inv = Bukkit.createInventory(holder, 54, "§6§lClan-Einstellungen");

        var _g = GuiHelper.filler(Material.GRAY_STAINED_GLASS_PANE);
        var _w = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);

        inv.setItem(0, _g); inv.setItem(1, _w); inv.setItem(2, _g);
        inv.setItem(6, _g); inv.setItem(7, _w); inv.setItem(8, _g);

        inv.setItem(9,  _w); inv.setItem(10, _g);
        inv.setItem(16, _g); inv.setItem(17, _w);

        inv.setItem(18, _g); inv.setItem(26, _g);

        inv.setItem(27, _g); inv.setItem(35, _g);

        inv.setItem(36, _w);
        inv.setItem(40, _g);
        inv.setItem(43, _g); inv.setItem(44, _w);

        inv.setItem(45, _g); inv.setItem(46, _w); inv.setItem(47, _w);
        inv.setItem(48, _g);
        inv.setItem(50, _g); inv.setItem(51, _w); inv.setItem(52, _w);
        inv.setItem(53, _g);

        inv.setItem(4, new ItemBuilder(Material.ANVIL)
                .name("§6§lClan-Einstellungen")
                .lore("",
                        "§8| §7Clan: §f" + clan.getName(),
                        "§8| §7Deine Rolle: §f" + (isOwner ? "§6Anführer" : "Mitglied"))
                .build());

        Material ffMat = ffOn ? Material.FIRE_CHARGE : Material.SNOWBALL;
        inv.setItem(15, new ItemBuilder(ffMat)
                .name("§c§l Friendly Fire: " + (ffOn ? "§aAN" : "§cAUS"))
                .lore("",
                        "§8| §7Erlaubt PvP zwischen",
                        "§8| §7Clan-Mitgliedern.",
                        "",
                        "§8| §7Status: " + (ffOn ? "§aAktiviert" : "§cDeaktiviert"),
                        "",
                        hasFF ? "§eDoppelklick zum Umschalten" : "§cKeine Berechtigung")
                .build());

        inv.setItem(22, new ItemBuilder(Material.CLOCK)
                .name("§b§l Tag-Farbe")
                .lore("",
                        "§8| §7Ändere die Farbe des",
                        "§8| §7Clan-Tags im Chat.",
                        "",
                        hasSetTag ? "§eKlicke zum Öffnen" : "§cKeine Berechtigung")
                .build());

        boolean glowDisabled = manager.isGlowDisabled(player.getUniqueId());
        inv.setItem(34, new ItemBuilder(glowDisabled ? Material.ENDER_EYE : Material.ENDER_PEARL)
                .name("§d§l Glow-Effekt: " + (glowDisabled ? "§cAUS" : "§aAN"))
                .lore("",
                        "§8| §7Zeigt den Glow-Effekt",
                        "§8| §7von Gegnern im Krieg.",
                        "",
                        "§8| §7Status: " + (glowDisabled ? "§cDeaktiviert" : "§aAktiviert"),
                        "§8| §7Dein eigener Glow bleibt sichtbar!",
                        "",
                        "§eKlicke zum Umschalten")
                .build());

        inv.setItem(30, new ItemBuilder(Material.TRIAL_KEY)
                .name("§6§l Rang-Verwaltung")
                .lore("",
                        "§8| §7Erstelle und verwalte",
                        "§8| §7eigene Ränge für deinen Clan.",
                        "",
                        hasManageRanks ? "§eKlicke zum Öffnen" : "§cKeine Berechtigung")
                .build());

        inv.setItem(11, new ItemBuilder(Material.BOOK)
                .name("§7§l Clan-Log")
                .lore("",
                        "§8| §7Sieh wichtige Clan-Aktionen ein.",
                        "",
                        hasLog ? "§eKlicke zum Öffnen" : "§cKeine Berechtigung")
                .build());

        inv.setItem(32, new ItemBuilder(Material.LIGHT_BLUE_BANNER)
                .name("§b§l Clan-Banner")
                .lore("",
                        "§8| §7Passe Farbe und Muster",
                        "§8| §7des Clan-Banners an.",
                        "",
                        hasSettings ? "§eKlicke zum Öffnen" : "§cKeine Berechtigung")
                .build());

        inv.setItem(38, new ItemBuilder(Material.NAME_TAG)
                .name("§e§l Clan umbenennen")
                .lore("",
                        "§8| §7Aktueller Name: §f" + clan.getName(),
                        "",
                        hasRename ? "§eKlicke zum Umbenennen" : "§cKeine Berechtigung")
                .build());

        if (isOwner) {
            inv.setItem(42, new ItemBuilder(Material.TNT)
                    .name("§c§l Clan auflösen")
                    .lore("",
                            "§8| §7Löst den Clan permanent auf.",
                            "§8| §7Alle Mitglieder werden entlassen.",
                            "§8| §cStelle sicher, dass der Vault leer ist!",
                            "",
                            "§c§lACHTUNG: §cUnwiderruflich!",
                            "",
                            "§cKlicke zum Auflösen")
                    .build());
        } else {
            inv.setItem(42, new ItemBuilder(Material.DARK_OAK_DOOR)
                    .name("§c§l Clan verlassen")
                    .lore("",
                            "§8| §7Verlasse deinen aktuellen Clan.",
                            "",
                            "§cKlicke zum Verlassen")
                    .build());
        }

        inv.setItem(49, GuiHelper.backButton());

        player.openInventory(inv);
    }

    @SuppressWarnings("unused")
    static String tagColorName(String code) {
        return switch (code) {
            case "§0" -> "Schwarz";
            case "§1" -> "Dunkelblau";
            case "§2" -> "Dunkelgrün";
            case "§3" -> "Dunkeltürkis";
            case "§4" -> "Dunkelrot";
            case "§5" -> "Lila";
            case "§6" -> "Gold";
            case "§7" -> "Grau";
            case "§8" -> "Dunkelgrau";
            case "§9" -> "Blau";
            case "§a" -> "Grün";
            case "§b" -> "Türkis";
            case "§c" -> "Rot";
            case "§d" -> "Hellviolett";
            case "§e" -> "Gelb";
            case "§f" -> "Weiß";
            default -> code;
        };
    }
}

