package com.betterclan.gui;

import com.betterclan.clan.ClanWar;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.*;

@SuppressWarnings("deprecation")
public final class WarPastLeaderboardMenu {

    private WarPastLeaderboardMenu() {}

    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    @SuppressWarnings("unused")
    public static void open(Player player, ClanWar war, String clanName) {
        open(player, war, clanName, null);
    }

    public static void open(Player player, ClanWar war, String clanName, String viewClanName) {
        GuiHolder holder = new GuiHolder(MenuType.WAR_PAST_LEADERBOARD);
        holder.set("war", war);
        holder.set("clanName", clanName);
        if (viewClanName != null) holder.set("viewClanName", viewClanName);

        Inventory inv = Bukkit.createInventory(holder, 45,
                "§6" + clanName + " §7— Leaderboard");

        GuiHelper.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(40, GuiHelper.backButton());

        Map<UUID, String> playerClans = war.getPlayerClans();
        Map<UUID, Integer> kills = war.getKillsByPlayer();
        Map<UUID, Integer> deaths = war.getDeathsByPlayer();

        Set<UUID> members = new LinkedHashSet<>();
        for (Map.Entry<UUID, String> e : playerClans.entrySet()) {
            if (e.getValue().equalsIgnoreCase(clanName)) members.add(e.getKey());
        }

        if (members.isEmpty()) {
            members.addAll(kills.keySet());
        }

        List<UUID> sorted = new ArrayList<>(members);
        sorted.sort(Comparator
                .comparingInt((UUID u) -> kills.getOrDefault(u, 0)).reversed()
                .thenComparingInt(u -> deaths.getOrDefault(u, 0)));

        for (int i = 0; i < Math.min(sorted.size(), SLOTS.length); i++) {
            UUID uuid = sorted.get(i);
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String name = op.getName() != null ? op.getName() : "Unbekannt";
            int k = kills.getOrDefault(uuid, 0);
            int d = deaths.getOrDefault(uuid, 0);

            String medal = switch (i) {
                case 0 -> "§6§l#1 ";
                case 1 -> "§7§l#2 ";
                case 2 -> "§c§l#3 ";
                default -> "§8#" + (i + 1) + " ";
            };

            inv.setItem(SLOTS[i], new ItemBuilder(Material.PLAYER_HEAD)
                    .skullOwner(op)
                    .name(medal + name)
                    .lore("",
                            "§8| §7Kills: §a" + k,
                            "§8| §7Tode: §c" + d)
                    .build());
        }

        if (sorted.isEmpty()) {
            inv.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name("§7Keine Daten verfügbar")
                    .lore("", "§8Für diesen Clan wurden", "§8keine Kills erfasst.")
                    .build());
        }

        player.openInventory(inv);
    }
}

