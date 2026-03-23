package com.betterclan.gui.rank;

import com.betterclan.gui.GuiHelper;
import com.betterclan.gui.GuiHolder;
import com.betterclan.gui.MenuType;
import com.betterclan.clan.Clan;
import com.betterclan.clan.Manager;
import com.betterclan.clan.Rank;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@SuppressWarnings("deprecation")
public final class Management {

    private Management() {}

    public static void open(Manager manager, Player player) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) return;
        if (!manager.hasPermission(clan, player.getUniqueId(), com.betterclan.clan.Permission.MANAGE_RANKS)) {
            player.sendMessage("§cDu hast keine Berechtigung, Ränge zu verwalten.");
            return;
        }
        openForClan(manager, clan, player);
    }

    public static void openForAdmin(Manager manager, Clan clan, Player player) {
        openForClan(manager, clan, player);
    }

    private static void openForClan(Manager manager, Clan clan, Player player) {
        GuiHolder holder = new GuiHolder(MenuType.RANK_MANAGEMENT);
        Inventory inv = Bukkit.createInventory(holder, 54, "§d§lRang-Verwaltung");
        int maxRanks = Math.min(manager.getSettings().rankMaxTotal(), GuiHelper.CONTENT_SLOTS_28.length);

        var _w  = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);

        inv.setItem(1, _w); inv.setItem(2, _w);
        inv.setItem(6, _w); inv.setItem(7, _w);

        inv.setItem(9, _w); inv.setItem(17, _w);

        inv.setItem(18, _w); inv.setItem(26, _w);
        inv.setItem(27, _w); inv.setItem(35, _w);

        inv.setItem(36, _w); inv.setItem(44, _w);

        inv.setItem(46, _w); inv.setItem(47, _w);
        inv.setItem(48, GuiHelper.backButton());
        inv.setItem(49, _w);
        inv.setItem(51, _w); inv.setItem(52, _w);

        inv.setItem(4, new ItemBuilder(Material.BOOKSHELF)
            .name("§d§lRang-Verwaltung")
            .lore("", "§8| §7Ränge: §f" + clan.getOrderedRanks().size() + " §8/ §f" + maxRanks, "",
                        "§eRechtsklick §7auf einen Rang für Optionen.",
                        "§eKlicke «+ Neuer Rang» um einen Rang zu erstellen.", "")
                .build());

        int[] contentSlots = GuiHelper.CONTENT_SLOTS_28;
        List<Rank> ranks = clan.getOrderedRanks();
        for (int i = 0; i < ranks.size() && i < contentSlots.length; i++) {
            inv.setItem(contentSlots[i], buildRankItem(ranks.get(i)));
        }

        if (ranks.size() < maxRanks) {
                inv.setItem(50, new ItemBuilder(Material.LIME_DYE)
                    .name("§a§l+ Neuer Rang")
                    .lore("", "§7Erstelle einen neuen Rang.", "",
                            "§eKlicke zum Erstellen")
                    .build());
        }

        player.openInventory(inv);
    }

    static ItemStack buildRankItem(Rank rank) {
        Material mat = rankMaterial(rank);
        String deletableInfo = rank.isDeletable()
                ? "§7Rechtsklick: §eOptionen (Umbenennen, Berechtigungen, Löschen)"
                : "§7Rechtsklick: §eOptionen (Umbenennen, Berechtigungen)";
        return new ItemBuilder(mat)
                .name(rank.getColoredName())
                .lore("",
                        "§8| §7Berechtigungen: §f" + rank.getPermissions().size(),
                        "",
                        deletableInfo)
                .build();
    }

    static Material rankMaterial(Rank rank) {
        if (Rank.OWNER_ID.equals(rank.getId())) return Material.NETHER_STAR;
        if (!rank.isDeletable()) return Material.IRON_INGOT;
        return Material.GOLD_INGOT;
    }
}

