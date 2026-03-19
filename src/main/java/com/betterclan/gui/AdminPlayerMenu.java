package com.betterclan.gui;

import com.betterclan.clan.ClanManager;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@SuppressWarnings("deprecation")
public final class AdminPlayerMenu {

    private AdminPlayerMenu() {}

    public static void open(ClanManager manager, Player admin, UUID targetId) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        String targetName = target.getName() != null ? target.getName() : targetId.toString();

        boolean inviteBlocked = manager.isInviteBlocked(targetId);
        boolean clanBanned    = manager.isClanBanned(targetId);
        long banExpiry        = manager.getClanBanExpiry(targetId);

        GuiHolder holder = new GuiHolder(MenuType.ADMIN_PLAYER_MENU);
        holder.set("targetId", targetId.toString());

        Inventory inv = Bukkit.createInventory(holder, 27, "§c§lAdmin §8| §7" + targetName);

        ItemBuilder _r = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name(" ");
        ItemBuilder _g = new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).name(" ");
        for (int i = 0; i < 9; i++)  inv.setItem(i, i == 0 || i == 8 ? _r.build() : _g.build());
        for (int i = 18; i < 27; i++) inv.setItem(i, i == 18 || i == 26 ? _r.build() : _g.build());

        String lastSeen = target.getLastPlayed() > 0
                ? new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(target.getLastPlayed()))
                : "§7unbekannt";
        String banStatus;
        if (!clanBanned) {
            banStatus = "§akein Bann";
        } else if (banExpiry == -1L) {
            banStatus = "§cpermanent gesperrt";
        } else {
            banStatus = "§egesperrt bis: §f" + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(banExpiry));
        }

        inv.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(target)
                .name("§e§l" + targetName)
                .lore("",
                        "§8| §7Zuletzt online: §f" + lastSeen,
                        "§8| §7Clan: §f" + (manager.getClan(targetId) != null ? "§a" + manager.getClan(targetId).getName() : "§7-"),
                        "§8| §7Clan-Bann: " + banStatus,
                        "§8| §7Einladungs-Sperre: " + (inviteBlocked ? "§can" : "§aaus"))
                .build());

        inv.setItem(10, new ItemBuilder(clanBanned ? Material.BARRIER : Material.IRON_BARS)
                .name(clanBanned ? "§c§l Clan-Bann aufheben" : "§4§l Permanent sperren")
                .lore("",
                        clanBanned ? "§eKlicke: §7Bann aufheben" : "§c§lKlicke: §cPermanent sperren")
                .build());

        inv.setItem(13, new ItemBuilder(Material.CLOCK)
                .name("§6§l Temporär sperren")
                .lore("",
                        "§eKlicke: §7Stunden eingeben")
                .build());

        inv.setItem(16, new ItemBuilder(inviteBlocked ? Material.LIME_DYE : Material.RED_DYE)
                .name("§e§l Einladungs-Sperre")
                .lore("",
                        "§7Verhindert, dass der Spieler",
                        "§7Clan-Einladungen erhalten kann.",
                        "",
                        "§8| §7Status: " + (inviteBlocked ? "§cgesperrt" : "§afreigeschaltet"),
                        "",
                        "§eKlicke: §7Umschalten")
                .build());

        inv.setItem(22, GuiHelper.backButton());

        admin.openInventory(inv);
    }
}

