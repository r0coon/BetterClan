package com.betterclan.gui;

import com.betterclan.clan.Clan;
import com.betterclan.clan.ClanManager;
import com.betterclan.clan.CustomRank;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("deprecation")
public final class MemberRankPickerMenu {

    private MemberRankPickerMenu() {}

    public static void openForAdmin(ClanManager manager, Player viewer, UUID targetId, String adminClanName) {
        Clan clan = manager.getClanByName(adminClanName);
        if (clan == null) return;
        openWithClan(manager, viewer, targetId, clan, adminClanName);
    }

    public static void open(ClanManager manager, Player viewer, UUID targetId) {
        Clan clan = manager.getClan(viewer.getUniqueId());
        if (clan == null) return;
        openWithClan(manager, viewer, targetId, clan, null);
    }

    @SuppressWarnings("unused")
    private static void openWithClan(ClanManager manager, Player viewer, UUID targetId, Clan clan, String adminClanName) {
        String targetName = Bukkit.getOfflinePlayer(targetId).getName();
        if (targetName == null) targetName = targetId.toString().substring(0, 8);

        CustomRank viewerRank = clan.getEffectiveRank(viewer.getUniqueId());
        String currentRankId = clan.getRankId(targetId);

        GuiHolder holder = new GuiHolder(MenuType.MEMBER_RANK_PICKER);
        holder.set("targetId", targetId.toString());
        holder.set("targetName", targetName);
        if (adminClanName != null) holder.set("adminClanName", adminClanName);

        Inventory inv = Bukkit.createInventory(holder, 54, "§6§lRang setzen: " + targetName);

        var _w = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        var _lb = GuiHelper.filler(Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        inv.setItem(1, _w); inv.setItem(2, _w);
        inv.setItem(6, _w); inv.setItem(7, _w);
        inv.setItem(9, _w); inv.setItem(17, _w);
        inv.setItem(18, _lb); inv.setItem(26, _lb);
        inv.setItem(27, _lb); inv.setItem(35, _lb);
        inv.setItem(36, _w); inv.setItem(44, _w);
        inv.setItem(46, _w); inv.setItem(47, _w);
        inv.setItem(49, GuiHelper.backButton());
        inv.setItem(51, _w); inv.setItem(52, _w);

        inv.setItem(4, new ItemBuilder(Material.NAME_TAG)
                .name("§6§lRang setzen")
                .lore("", "§8| §7Spieler: §f" + targetName, "")
                .build());

        List<CustomRank> assignable = clan.getOrderedRanks().stream()
                .filter(r -> !CustomRank.OWNER_ID.equals(r.getId()))
                .filter(r -> viewerRank == null || r.getPermissions().size() < viewerRank.getPermissions().size())
                .toList();

        int[] slots = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
        };
        for (int i = 0; i < assignable.size() && i < slots.length; i++) {
            CustomRank rank = assignable.get(i);
            boolean isCurrent = rank.getId().equals(currentRankId);
            inv.setItem(slots[i], new ItemBuilder(isCurrent ? Material.LIME_DYE : Material.GRAY_DYE)
                    .name(rank.getColoredName() + (isCurrent ? " §7(aktuell)" : ""))
                    .build());
        }

        viewer.openInventory(inv);
    }
}

