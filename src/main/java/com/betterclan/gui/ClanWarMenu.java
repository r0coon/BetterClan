package com.betterclan.gui;

import com.betterclan.clan.*;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public final class ClanWarMenu {

    private ClanWarMenu() {}

    public static void open(ClanManager manager, Player player) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) return;
        boolean canWar = manager.hasPermission(clan, player.getUniqueId(), ClanPermission.WAR);

        GuiHolder holder = new GuiHolder(MenuType.WAR_MENU);
        Inventory inv = Bukkit.createInventory(holder, 27, "§4§lClan-Krieg");

        var _w = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        var _r = GuiHelper.filler(Material.RED_STAINED_GLASS_PANE);

        inv.setItem(1, _w); inv.setItem(4, _r); inv.setItem(7, _w);

        inv.setItem(9, _w); inv.setItem(11, _w);
        inv.setItem(12, _r);
        inv.setItem(14, _r);
        inv.setItem(15, _w);
        inv.setItem(17, _w);

        inv.setItem(18, GuiHelper.backButton());
        inv.setItem(19, _w); inv.setItem(22, _r); inv.setItem(25, _w);

        ClanWar activeWar = manager.getActiveWar(clan.getName());

        if (activeWar != null) {
            String opponentName = activeWar.getOpponent(clan.getName());
            int ownKills = activeWar.getKillsFor(clan.getName());
            int enemyKills = activeWar.getKillsFor(opponentName);

            inv.setItem(13, new ItemBuilder(Material.NETHERITE_SWORD)
                    .name("§c§l Aktiver Krieg")
                    .lore("", "§7vs. §f" + opponentName,
                            "§8| §7Deine Kills: §a" + ownKills, "§8| §7Gegner Kills: §c" + enemyKills,
                            "§8| §7Ziel: §6" + activeWar.getTargetKills() + " Kills", "",
                            GuiHelper.progressBar((double) ownKills / activeWar.getTargetKills(), 20), "")
                    .flags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                    .build());

            if (canWar) {
                inv.setItem(16, new ItemBuilder(Material.WHITE_BANNER)
                        .name("§c§l Aufgeben")
                        .lore("", "§7Gib den Krieg auf.", "§7Der Gegner gewinnt.", "", "§cKlicke zum Aufgeben")
                        .build());
            }
        } else {
            if (canWar) {
                int minSize = manager.getSettings().warMinClanSize();
                if (clan.getMemberCount() < minSize) {
                    inv.setItem(13, new ItemBuilder(Material.BARRIER)
                            .name("§c§l Zu wenig Mitglieder")
                            .lore("", "§7Der Clan braucht mind.", "§c" + minSize + " Mitglieder §7für Clan-Kriege.",
                                    "", "§8Aktuell: §c" + clan.getMemberCount() + "§8/§c" + minSize)
                            .build());
                } else {
                    long cooldownMs = manager.getWarCooldownRemaining(clan.getName());
                    if (cooldownMs > 0) {
                        long mins = cooldownMs / 60000;
                        long secs = (cooldownMs % 60000) / 1000;
                        int cooldownCfg = manager.getSettings().warCooldownMinutes();
                        inv.setItem(13, new ItemBuilder(Material.CLOCK)
                                .name("§7§l Krieg-Cooldown")
                                .lore("", "§7Du musst noch warten:",
                                        "§c" + mins + "m " + secs + "s",
                                "", "§8Cooldown nach Krieg: " + cooldownCfg + " Min.")
                                .build());
                    } else {
                        int defaultKills = manager.getSettings().warTargetKills();
                        inv.setItem(13, new ItemBuilder(Material.NETHERITE_SWORD)
                                .name("§c§l Krieg starten")
                            .lore("", "§7Ziel: §6" + defaultKills + " Kills", "", "§eKlicke zum Auswählen")
                                .flags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                                .build());
                    }
                }
            }
        }

        if (activeWar == null) {
            List<ClanWar> requests = new ArrayList<>(manager.getWarRequests(clan.getOwnerId()));
            inv.setItem(16, new ItemBuilder(Material.WRITTEN_BOOK)
                .name("§e§lKriegsanfragen" + (requests.isEmpty() ? "" : " §c(" + requests.size() + ")"))
                    .lore("", requests.isEmpty() ? "§7Keine offenen Anfragen." : "§aDu hast §f" + requests.size() + " §aAnfrage(n).",
                            "", "§eKlicke zum Öffnen")
                    .build());
        }

        inv.setItem(10, new ItemBuilder(Material.BOOK)
                .name("§7§l Kriegs-Verlauf")
                .lore("", "§eKlicke zum Öffnen")
                .build());

        player.openInventory(inv);
    }
}

