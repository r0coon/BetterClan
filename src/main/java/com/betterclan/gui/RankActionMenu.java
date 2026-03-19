package com.betterclan.gui;

import com.betterclan.clan.Clan;
import com.betterclan.clan.ClanManager;
import com.betterclan.clan.CustomRank;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

@SuppressWarnings("deprecation")
public final class RankActionMenu {

    private RankActionMenu() {}

    public static void open(ClanManager manager, Player player, String rankId) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) return;
        if (!clan.getOwnerId().equals(player.getUniqueId())) return;
        CustomRank rank = clan.getRank(rankId);
        if (rank == null) return;

        GuiHolder holder = new GuiHolder(MenuType.RANK_ACTION);
        holder.set("rankId", rankId);

        Inventory inv = Bukkit.createInventory(holder, 27,
                "§d§l" + rank.getName() + " — Optionen");

        var _bl = GuiHelper.filler(Material.BLUE_STAINED_GLASS_PANE);
        var _w  = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);

        inv.setItem(1, _bl); inv.setItem(2, _w); inv.setItem(3, _w);
        inv.setItem(5, _w); inv.setItem(6, _w); inv.setItem(7, _bl);

        inv.setItem(9, _bl); inv.setItem(17, _bl);

        inv.setItem(19, _bl); inv.setItem(20, _w); inv.setItem(21, _w);
        inv.setItem(23, _w); inv.setItem(24, _w); inv.setItem(25, _bl);

        inv.setItem(4, new ItemBuilder(RankManagementMenu.rankMaterial(rank))
                .name(rank.getColoredName())
                .lore("", "§7Berechtigungen: §f" + rank.getPermissions().size(), "")
                .build());

        inv.setItem(11, new ItemBuilder(Material.NAME_TAG)
                .name("§a§l Umbenennen")
                .lore("", "§7Gibt dem Rang einen neuen Namen.", "",
                        "§eKlicke zum Umbenennen")
                .build());

        if (!CustomRank.OWNER_ID.equals(rank.getId())) {
            inv.setItem(15, new ItemBuilder(Material.COMMAND_BLOCK)
                    .name("§d§l Berechtigungen")
                    .lore("", "§7Konfiguriere die Berechtigungen", "§7dieses Rangs.", "",
                            "§eKlicke zum Bearbeiten")
                    .build());
        }

        if (rank.isDeletable()) {
            inv.setItem(13, new ItemBuilder(Material.BARRIER)
                    .name("§c§l Rang löschen")
                    .lore("", "§7Löscht diesen Rang dauerhaft.",
                            "§8| §7Alle Mitglieder mit diesem Rang", "§7werden zu Mitglied zurückgesetzt.", "",
                            "§c§lACHTUNG: §cUnwiderruflich!", "",
                            "§cKlicke zum Löschen")
                    .build());
        }

        inv.setItem(22, GuiHelper.backButton());

        player.openInventory(inv);
    }
}

