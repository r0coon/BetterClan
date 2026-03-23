package com.betterclan.gui.rank;

import com.betterclan.gui.GuiHelper;
import com.betterclan.gui.GuiHolder;
import com.betterclan.gui.MenuType;
import com.betterclan.clan.Clan;
import com.betterclan.clan.Manager;
import com.betterclan.clan.Permission;
import com.betterclan.clan.Rank;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

@SuppressWarnings("deprecation")
public final class Permissions {

    private Permissions() {}

    public static void openPermissions(Manager manager, Player player, String rankId) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) return;
        Rank rank = clan.getRank(rankId);
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

        Permission[] permOrder = {
            Permission.INVITE, Permission.KICK, Permission.PROMOTE,
            Permission.VAULT,  Permission.SETTINGS, Permission.WAR, Permission.ALLY,
            Permission.TELEPORT, Permission.CHAT_ALLY, Permission.CLAN_CHAT,
            Permission.RENAME, Permission.SET_TAG,
            Permission.VIEW_LOG, Permission.MANAGE_RANKS, Permission.FRIENDLY_FIRE,
            Permission.SET_BASE
        };
        for (int i = 0; i < permOrder.length; i++) {
            Permission perm = permOrder[i];
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

