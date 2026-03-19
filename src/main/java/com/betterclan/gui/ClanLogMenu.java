package com.betterclan.gui;

import com.betterclan.clan.Clan;
import com.betterclan.clan.ClanManager;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public final class ClanLogMenu {

    private ClanLogMenu() {}

    private static final int[] SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };
    private static final int PAGE_SIZE = SLOTS.length;

    public static void open(ClanManager manager, Player player, int page) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) return;

        GuiHolder holder = new GuiHolder(MenuType.CLAN_LOG);
        openWithLog(player, clan.getClanLog(), clan.getName(), null, page, holder, manager.getSettings().maxLogEntries());
    }

    public static void open(ClanManager manager, Player player) {
        open(manager, player, 1);
    }

    public static void openForAdmin(ClanManager manager, Player player, String clanName) {
        openForAdmin(manager, player, clanName, 1);
    }

    public static void openForAdmin(ClanManager manager, Player player, String clanName, int page) {
        com.betterclan.clan.Clan clan = manager.getClanByName(clanName);
        if (clan == null) { player.sendMessage("§cClan nicht gefunden."); return; }
        GuiHolder holder = new GuiHolder(MenuType.CLAN_LOG);
        openWithLog(player, clan.getClanLog(), clan.getName(), clanName, page, holder, manager.getSettings().maxLogEntries());
    }

    @SuppressWarnings({"deprecation", "unused"})
    private static void openWithLog(Player player, List<String> log, String title, String adminClanName,
                        int page, GuiHolder holder, int maxLogEntries) {
        int totalPages = Math.max(1, (int) Math.ceil((double) log.size() / PAGE_SIZE));
        int p = Math.clamp(page, 1, totalPages);
        holder.set("page", p);
        holder.set("totalPages", totalPages);
        if (adminClanName != null) holder.set("adminClanName", adminClanName);

        Inventory inv = Bukkit.createInventory(holder, 54, "§7Clan-Log §8(§f" + p + "§8/§f" + totalPages + "§8)");

        var _w  = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        var _lb = GuiHelper.filler(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        inv.setItem(0, _w); inv.setItem(1, _lb); inv.setItem(2, _lb);
        inv.setItem(3, _lb); inv.setItem(5, _lb);
        inv.setItem(6, _lb); inv.setItem(7, _lb); inv.setItem(8, _w);
        inv.setItem(9,  _lb); inv.setItem(17, _lb);
        inv.setItem(18, _lb); inv.setItem(26, _lb);
        inv.setItem(27, _lb); inv.setItem(35, _lb);
        inv.setItem(36, _lb); inv.setItem(44, _lb);
        inv.setItem(47, _lb); inv.setItem(48, _lb);
        inv.setItem(50, _lb); inv.setItem(51, _lb);
        inv.setItem(49, GuiHelper.backButton());

        inv.setItem(4, new ItemBuilder(Material.BOOK)
                .name("§e§lClan-Log")
            .lore("", "§7Einträge: §f" + log.size() + " §8/ §f" + maxLogEntries, "",
                        "§8Seite §7" + p + " §8/ §7" + totalPages)
                .build());

        int start = (p - 1) * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx >= log.size()) break;
            inv.setItem(SLOTS[i], new ItemBuilder(Material.PAPER).name(log.get(idx)).build());
        }

        inv.setItem(45, _lb);
        if (p > 1) {
            inv.setItem(46, new ItemBuilder(Material.LIGHT_GRAY_DYE).name("§7« Vorherige Seite").build());
        } else {
            inv.setItem(46, new ItemBuilder(Material.GRAY_DYE).name("§8« Vorherige Seite").build());
        }
        if (p < totalPages) {
            inv.setItem(52, new ItemBuilder(Material.LIGHT_GRAY_DYE).name("§7Nächste Seite »").build());
        } else {
            inv.setItem(52, new ItemBuilder(Material.GRAY_DYE).name("§8Nächste Seite »").build());
        }
        inv.setItem(53, _lb);

        player.openInventory(inv);
    }
}

