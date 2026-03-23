package com.betterclan.gui.warlog;

import com.betterclan.gui.GuiHelper;
import com.betterclan.gui.GuiHolder;
import com.betterclan.gui.MenuType;
import com.betterclan.clan.Clan;
import com.betterclan.clan.Manager;
import com.betterclan.clan.War;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.*;

@SuppressWarnings("deprecation")
public final class Leaderboard {

    private Leaderboard() {}

    public static final int ITEMS_PER_PAGE = 28;

    public static void open(Manager manager, Player player) {
        open(manager, player, 1);
    }

    public static void open(Manager manager, Player player, int page) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) return;
        War war = manager.getActiveWar(clan.getName());

        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>();
        if (war != null) {
            for (Map.Entry<UUID, Integer> e : war.getKillsByPlayer().entrySet()) {
                if (e.getValue() >= 1) sorted.add(e);
            }
            sorted.sort(Map.Entry.<UUID, Integer>comparingByValue().reversed());
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) sorted.size() / ITEMS_PER_PAGE));
        page = Math.clamp(page, 1, totalPages);

        GuiHolder holder = new GuiHolder(MenuType.WAR_LEADERBOARD);
        holder.set("page", page);
        Inventory inv = Bukkit.createInventory(holder, 54, "§6§lKriegs-Leaderboard");

        var _w = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        var _b = GuiHelper.filler(Material.BLACK_STAINED_GLASS_PANE);

        inv.setItem(0, _w); inv.setItem(1, _b); inv.setItem(2, _w);
        inv.setItem(3, _b); inv.setItem(4, _b); inv.setItem(5, _b);
        inv.setItem(6, _w); inv.setItem(7, _b); inv.setItem(8, _w);

        inv.setItem(9, _b); inv.setItem(17, _b);

        inv.setItem(18, _w); inv.setItem(26, _w);

        inv.setItem(27, _w); inv.setItem(35, _w);

        inv.setItem(36, _b); inv.setItem(44, _b);

        inv.setItem(45, _w); inv.setItem(46, _b);
        inv.setItem(48, _b);
        inv.setItem(50, _b);
        inv.setItem(52, _b); inv.setItem(53, _w);

        int[] contentSlots = GuiHelper.CONTENT_SLOTS_28;

        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && (startIndex + i) < sorted.size(); i++) {
            Map.Entry<UUID, Integer> entry = sorted.get(startIndex + i);
            int rank = startIndex + i + 1;
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            String pName = op.getName() != null ? op.getName() : "Unbekannt";
            Clan killerClan = manager.getClan(entry.getKey());
            int deaths = war.getDeathsByPlayer().getOrDefault(entry.getKey(), 0);

            String medal = switch (rank) {
                case 1 -> "§6§l#1 ";
                case 2 -> "§7§l#2 ";
                case 3 -> "§c§l#3 ";
                default -> "§8#" + rank + " ";
            };

            List<String> lore = new ArrayList<>();
            lore.add("");
            if (killerClan != null) lore.add("§7Clan: §f" + killerClan.getName());
            lore.add("§7Kills: §a" + entry.getValue());
            lore.add("§7Tode: §c" + deaths);

            inv.setItem(contentSlots[i], new ItemBuilder(Material.PLAYER_HEAD)
                    .skullOwner(op)
                    .name(medal + pName)
                    .lore(lore)
                    .build());
        }

        inv.setItem(47, page > 1 ? GuiHelper.prevPageButton(page - 1) : GuiHelper.prevPageButtonDisabled());
        inv.setItem(49, GuiHelper.backButton());
        inv.setItem(51, page < totalPages ? GuiHelper.nextPageButton(page + 1) : GuiHelper.nextPageButtonDisabled());
        player.openInventory(inv);
    }
}

