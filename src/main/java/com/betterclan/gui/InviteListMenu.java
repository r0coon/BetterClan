package com.betterclan.gui;

import com.betterclan.clan.ClanManager;
import com.betterclan.clan.ClanManager.ClanInvite;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@SuppressWarnings("deprecation")
public final class InviteListMenu {

    private InviteListMenu() {}

    public static void open(ClanManager manager, Player player) {
        List<ClanInvite> invites = manager.getActiveInvites(player.getUniqueId());

        GuiHolder holder = new GuiHolder(MenuType.INVITE_LIST);
        Inventory inv = Bukkit.createInventory(holder, 54,
                "§a§lEinladungen §7(" + invites.size() + ")");

        var _w = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        var _lb = GuiHelper.filler(Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        inv.setItem(0, _w); inv.setItem(1, _lb); inv.setItem(2, _lb);
        inv.setItem(3, _w); inv.setItem(5, _w);
        inv.setItem(6, _lb); inv.setItem(7, _lb); inv.setItem(8, _w);

        inv.setItem(9, _lb); inv.setItem(17, _lb);

        inv.setItem(18, _lb); inv.setItem(26, _lb);

        inv.setItem(36, _lb); inv.setItem(44, _lb);

        inv.setItem(45, _w); inv.setItem(46, _lb);
        inv.setItem(47, _w);
        inv.setItem(51, _w);
        inv.setItem(52, _lb); inv.setItem(53, _w);

        inv.setItem(4, new ItemBuilder(Material.WRITABLE_BOOK)
                .name("§e§l Clan-Einladungen")
                .lore("", invites.isEmpty()
                                ? "§7Keine offenen Einladungen."
                                : "§7§f" + invites.size() + " §7Einladung(en) offen.")
                .build());

        int[] contentSlots = GuiHelper.CONTENT_SLOTS_30;
        Instant now = Instant.now();
        for (int i = 0; i < invites.size() && i < contentSlots.length; i++) {
            ClanInvite invite = invites.get(i);
            OfflinePlayer inviter = Bukkit.getOfflinePlayer(invite.inviter());
            String inviterName = inviter.getName() != null ? inviter.getName() : "Unbekannt";
            Duration remaining = Duration.between(now, invite.expiresAt());
            long hours = Math.max(0, remaining.toHours());
            long minutes = Math.max(0, remaining.toMinutes() % 60);

            inv.setItem(contentSlots[i], new ItemBuilder(Material.PAPER)
                    .name("§a§l " + invite.clanName())
                    .lore("", "§7Von: §f" + inviterName, "§7Läuft ab in: §f" + hours + "h " + minutes + "min",
                            "", "§aLinksklick: §7Beitritt bestätigen", "§cRechtsklick: §7Einladung ablehnen")
                    .glow().build());
        }

        inv.setItem(49, GuiHelper.backButton());
        player.openInventory(inv);
    }
}

