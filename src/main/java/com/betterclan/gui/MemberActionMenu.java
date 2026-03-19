package com.betterclan.gui;

import com.betterclan.clan.Clan;
import com.betterclan.clan.ClanManager;
import com.betterclan.clan.CustomRank;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

@SuppressWarnings("deprecation")
public final class MemberActionMenu {

    private MemberActionMenu() {}

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
        boolean isAdmin = adminClanName != null;

        String targetName = Bukkit.getOfflinePlayer(targetId).getName();
        if (targetName == null) targetName = targetId.toString().substring(0, 8);

        CustomRank viewerRank = clan.getEffectiveRank(viewer.getUniqueId());
        CustomRank targetRank = clan.getEffectiveRank(targetId);
        boolean isOwner = !isAdmin && clan.getOwnerId().equals(viewer.getUniqueId());
        boolean canManage = isAdmin || (viewerRank != null && targetRank != null
                && viewerRank.getPermissions().size() > targetRank.getPermissions().size());
        boolean isSelf = viewer.getUniqueId().equals(targetId);
        Player targetPlayer = Bukkit.getPlayer(targetId);
        boolean targetOnline = targetPlayer != null;

        GuiHolder holder = new GuiHolder(MenuType.MEMBER_ACTIONS);
        holder.set("targetId", targetId.toString());
        holder.set("targetName", targetName);
        if (adminClanName != null) holder.set("adminClanName", adminClanName);

        Inventory inv = Bukkit.createInventory(holder, 45, "§e§lMitglied: " + targetName);

        var _w  = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        var _bl = GuiHelper.filler(Material.BLUE_STAINED_GLASS_PANE);
        var _lb = GuiHelper.filler(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        inv.setItem(0, _w); inv.setItem(1, _bl); inv.setItem(2, _lb);
        inv.setItem(6, _lb); inv.setItem(7, _bl); inv.setItem(8, _w);
        inv.setItem(9, _bl); inv.setItem(17, _bl);
        inv.setItem(18, _lb); inv.setItem(26, _lb);
        inv.setItem(27, _bl); inv.setItem(35, _bl);
        inv.setItem(36, _w); inv.setItem(37, _bl); inv.setItem(38, _lb);
        inv.setItem(40, GuiHelper.backButton());
        inv.setItem(42, _lb); inv.setItem(43, _bl); inv.setItem(44, _w);

        java.util.List<String> infoLore = new java.util.ArrayList<>();
        infoLore.add("");
        infoLore.add("§7Rang: " + (targetRank != null ? targetRank.getColoredName() : "§7Unbekannt"));
        infoLore.add("§7Status: " + (targetOnline ? "§aOnline" : "§cOffline"));
        if (!targetOnline) {
            long lastSeen = clan.getLastSeen(targetId);
            if (lastSeen <= 0) lastSeen = Bukkit.getOfflinePlayer(targetId).getLastPlayed();
            if (lastSeen > 0) infoLore.add("§7Zuletzt: §f" + formatAge(lastSeen));
        } else {
            org.bukkit.Location loc = targetPlayer.getLocation();
            String dim = switch (targetPlayer.getWorld().getEnvironment()) {
                case NETHER -> "§cNether";
                case THE_END -> "§5End";
                default -> "§aOverworld";
            };
            infoLore.add("§7Dimension: " + dim);
            infoLore.add(String.format("§7Position: §f%.0f§7, §f%.0f§7, §f%.0f",
                    loc.getX(), loc.getY(), loc.getZ()));
        }
        inv.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(Bukkit.getOfflinePlayer(targetId))
                .name("§f§l" + targetName)
                .lore(infoLore)
                .build());
        if (!isSelf) {
            org.bukkit.Location memberWarp = clan.getMemberWarp(targetId);
            if (memberWarp != null) {
                inv.setItem(22, new ItemBuilder(Material.COMPASS)
                        .name("§a§l Zum Heim teleportieren")
                        .lore("", "§7Welt: §f" + memberWarp.getWorld().getName(),
                                String.format("§7Pos: §f%.0f§7, §f%.0f§7, §f%.0f",
                                        memberWarp.getX(), memberWarp.getY(), memberWarp.getZ()))
                        .build());
            }
        }

        if (canManage && !isSelf) {
            inv.setItem(20, new ItemBuilder(Material.NAME_TAG)
                    .name("§6§l Rang setzen")
                    .lore("", "§7Aktuell: " + (targetRank != null ? targetRank.getColoredName() : "§7Unbekannt"),
                            "", "§eKlicke zum Ändern")
                    .build());

            inv.setItem(24, new ItemBuilder(Material.BARRIER)
                    .name("§c§l Kicken")
                    .lore("", "§7Entferne §f" + targetName, "§7aus dem Clan.", "", "§cKlicke zum Kicken")
                    .build());
        }

        if (isOwner && !isSelf) {
            inv.setItem(31, new ItemBuilder(Material.GOLDEN_HELMET)
                    .name("§6§l Führung übertragen")
                    .lore("", "§7Übertrage die Clan-Führung", "§7an §f" + targetName + "§7.", "",
                            "§c§lACHTUNG: §cDu verlierst", "§cdie Anführer-Rechte!", "", "§eKlicke zum Übertragen")
                    .flags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                    .build());
        }

        viewer.openInventory(inv);
    }

    static String formatAge(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / 60_000;
        long hours   = diff / 3_600_000;
        long days    = diff / 86_400_000;
        if (days >= 1)    return "vor " + days + (days == 1 ? " Tag" : " Tagen");
        if (hours >= 1)   return "vor " + hours + (hours == 1 ? " Stunde" : " Stunden");
        if (minutes >= 1) return "vor " + minutes + (minutes == 1 ? " Minute" : " Minuten");
        return "gerade eben";
    }
}

