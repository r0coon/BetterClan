package com.betterclan.gui;

import com.betterclan.clan.Clan;
import com.betterclan.clan.ClanManager;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ALL")
public final class AdminAllyRemoveMenu {

    private AdminAllyRemoveMenu() {}

    private static final int[] SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };
    private static final int PAGE_SIZE = SLOTS.length;

    public static void open(ClanManager manager, Player player, String clanName, int page) {
        Clan clan = manager.getClanByName(clanName);
        if (clan == null) { player.sendMessage("§cClan nicht gefunden."); return; }

        List<String> allies = new ArrayList<>(clan.getAllies());
        int totalPages = Math.max(1, (int) Math.ceil((double) allies.size() / PAGE_SIZE));
        int p = Math.max(1, Math.min(page, totalPages));

        GuiHolder holder = new GuiHolder(MenuType.ADMIN_ALLY_REMOVE);
        holder.set("clanName", clanName);
        holder.set("page", p);
        holder.set("totalPages", totalPages);

        Inventory inv = Bukkit.createInventory(holder, 54,
                "§c§lBündnisse §8| §7" + clan.getName());

        ItemBuilder _g = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ");
        GuiHelper.fillAdminRedGrayBorder(inv);

        inv.setItem(4, new ItemBuilder(GuiHelper.createClanBanner(clan))
                .name("§c§l Bündnis auflösen")
                .lore("",
                        "§8| §7Clan: §f" + clan.getName(),
                        "§8| §7Bündnisse: §f" + allies.size(),
                        "",
                        "§7Klicke ein Bündnis um es aufzulösen.")
                .build());

        int start = (p - 1) * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx >= allies.size()) break;
            String allyName = allies.get(idx);
            Clan ally = manager.getClanByName(allyName);
            int memberCount = ally != null ? ally.getMemberCount() : 0;
            int allyLevel = ally != null ? ally.getLevel() : 0;
            org.bukkit.inventory.ItemStack allyBanner = ally != null
                    ? GuiHelper.createClanBanner(ally)
                    : new org.bukkit.inventory.ItemStack(Material.WHITE_BANNER);
            inv.setItem(SLOTS[i], new ItemBuilder(allyBanner)
                    .name("§e§l" + allyName)
                    .lore("",
                            "§8| §7Mitglieder: §f" + memberCount,
                            "§8| §7Level: §f" + allyLevel,
                            "",
                            "§cKlicke: §7Bündnis mit §f" + allyName + " §7auflösen")
                    .persistData(new org.bukkit.NamespacedKey("betterclan", "ally-name"), allyName)
                    .build());
        }

        if (p > 1) {
            inv.setItem(47, new ItemBuilder(Material.LIGHT_GRAY_DYE).name("§7« Vorherige Seite").build());
        } else {
            inv.setItem(47, _g.build());
        }
        inv.setItem(49, GuiHelper.backButton());
        if (p < totalPages) {
            inv.setItem(51, new ItemBuilder(Material.LIGHT_GRAY_DYE).name("§7Nächste Seite »").build());
        } else {
            inv.setItem(51, _g.build());
        }

        player.openInventory(inv);
    }

    public static void open(ClanManager manager, Player player, String clanName) {
        open(manager, player, clanName, 1);
    }
}

