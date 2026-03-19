package com.betterclan.gui;

import com.betterclan.clan.Clan;
import com.betterclan.clan.ClanManager;
import com.betterclan.clan.ClanPermission;
import com.betterclan.clan.CustomRank;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

@SuppressWarnings("deprecation")
public final class RolePermissionMenu {

    private RolePermissionMenu() {}

    public static void openPermissions(ClanManager manager, Player player, String rankId) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) return;
        CustomRank rank = clan.getRank(rankId);
        if (rank == null) return;

        GuiHolder holder = new GuiHolder(MenuType.ROLE_PERM_EDIT);
        holder.set("rankId", rankId);

        Inventory inv = Bukkit.createInventory(holder, 54,
                "§d§lBerechtigungen: " + rank.getColoredName());

        var _w  = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        var _lb = GuiHelper.filler(Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        inv.setItem(0, _lb); inv.setItem(1, _w);
        inv.setItem(7, _w); inv.setItem(8, _lb);

        inv.setItem(9, _w); inv.setItem(17, _w);

        inv.setItem(36, _w); inv.setItem(44, _w);

        inv.setItem(45, _lb); inv.setItem(46, _w);
        inv.setItem(52, _w); inv.setItem(53, _lb);

        inv.setItem(49, GuiHelper.backButton());

        inv.setItem(4, new ItemBuilder(Material.COMMAND_BLOCK)
            .name("§d§l " + rank.getName() + " §7— §dBerechtigungen")
            .lore("", "§8| §7Klicke, um eine Berechtigung", "§8| §7ein- oder auszuschalten.", "")
                .build());

        int[] permSlots = {
            10, 11, 12, 13, 14, 15, 16,
            20, 21, 22, 23, 24,
            30, 31, 32,
            40
        };

        ClanPermission[] permOrder = {
            ClanPermission.INVITE, ClanPermission.KICK, ClanPermission.PROMOTE,
            ClanPermission.VAULT,  ClanPermission.SETTINGS, ClanPermission.WAR, ClanPermission.ALLY,
            ClanPermission.TELEPORT, ClanPermission.CHAT_ALLY, ClanPermission.CLAN_CHAT,
            ClanPermission.RENAME, ClanPermission.SET_TAG,
            ClanPermission.VIEW_LOG, ClanPermission.MANAGE_RANKS, ClanPermission.FRIENDLY_FIRE,
            ClanPermission.SET_BASE
        };
        for (int i = 0; i < permOrder.length; i++) {
            ClanPermission perm = permOrder[i];
            boolean enabled = rank.hasPermission(perm);
            inv.setItem(permSlots[i], new ItemBuilder(enabled ? Material.LIME_DYE : Material.GRAY_DYE)
                .name((enabled ? "§a§l" : "§7") + perm.getColor() + perm.getDisplayName())
                .lore("", "§8| §7" + perm.getDescription(), "",
                    enabled ? "§a§l✔ Aktiviert" : "§c§l✖ Deaktiviert", "",
                            "§eKlicke zum Umschalten")
                    .build());
        }

        player.openInventory(inv);
    }
}

