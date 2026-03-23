package com.betterclan.gui.war;

import com.betterclan.gui.GuiHelper;
import com.betterclan.gui.GuiHolder;
import com.betterclan.gui.MenuType;
import com.betterclan.clan.Clan;
import com.betterclan.clan.Manager;
import com.betterclan.clan.Permission;
import com.betterclan.clan.War;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

@SuppressWarnings("deprecation")
public final class Requests {

    private Requests() {}

    private static final int[] SLOTS = {
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        27,28,29,30,31,32,33,34,35,
        37,38,39,40,41,42,43
    };

    public static void open(Manager manager, Player player) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) return;
        boolean canWar = manager.hasPermission(clan, player.getUniqueId(), Permission.WAR);

        List<War> requests = manager.getWarRequests(clan.getOwnerId());

        GuiHolder holder = new GuiHolder(MenuType.WAR_REQUESTS);
        Inventory inv = Bukkit.createInventory(holder, 54, "§4§lKriegsanfragen");

        var _w = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        var _b = GuiHelper.filler(Material.BLACK_STAINED_GLASS_PANE);

        inv.setItem(0, _w); inv.setItem(1, _b); inv.setItem(2, _w);
        inv.setItem(3, _b);                inv.setItem(5, _b);
        inv.setItem(6, _w); inv.setItem(7, _b); inv.setItem(8, _w);

        inv.setItem(9, _b); inv.setItem(17, _b);

        inv.setItem(18, _w); inv.setItem(26, _w);

        inv.setItem(36, _b); inv.setItem(44, _b);

        inv.setItem(45, _w); inv.setItem(46, _b); inv.setItem(47, _w);
        inv.setItem(48, _b);
        inv.setItem(49, GuiHelper.backButton());
        inv.setItem(50, _b);
        inv.setItem(51, _w); inv.setItem(52, _b); inv.setItem(53, _w);

        if (requests.isEmpty()) {
            inv.setItem(4, new ItemBuilder(Material.BARRIER)
                    .name("§7Keine offenen Anfragen")
                    .lore("", "§8Es liegen derzeit keine", "§8Kriegsanfragen vor.")
                    .build());
        } else {
            int max = Math.min(requests.size(), SLOTS.length);
            for (int i = 0; i < max; i++) {
                War req = requests.get(i);
                inv.setItem(SLOTS[i], new ItemBuilder(Material.WRITTEN_BOOK)
                        .name("§c§l Herausforderung von " + req.getClan1Name())
                        .lore("",
                                "§8| §7Initiiert von: §f" + req.getClan1Name(),
                                "§8| §7Ziel: §6" + req.getTargetKills() + " Kills",
                                "",
                                canWar ? "§aLinksklick: §7Annehmen" : "§8Kein Zugriffsrecht",
                                canWar ? "§cRechtsklick: §7Ablehnen" : "",
                                "")
                        .flags(org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                        .build());
            }
        }

        player.openInventory(inv);
    }
}

