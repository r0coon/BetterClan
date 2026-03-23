package com.betterclan.gui.war;

import com.betterclan.gui.GuiHelper;
import com.betterclan.gui.GuiHolder;
import com.betterclan.gui.MenuType;
import com.betterclan.clan.Manager;
import com.betterclan.clan.War;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@SuppressWarnings("deprecation")
public final class PartySelect {

    private PartySelect() {}

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("dd.MM.yyyy")
            .withZone(ZoneId.systemDefault());

    @SuppressWarnings("unused")
    public static void open(Manager manager, Player player, War war) {
        open(manager, player, war, null);
    }

    public static void open(Manager manager, Player player, War war, String viewClanName) {
        GuiHolder holder = new GuiHolder(MenuType.WAR_HISTORY_DETAIL);
        holder.set("war", war);
        if (viewClanName != null) holder.set("viewClanName", viewClanName);

        Inventory inv = Bukkit.createInventory(holder, 27, "§7Krieg: §6" + war.getClan1Name() + " §7vs §6" + war.getClan2Name());
        GuiHelper.fillRow(inv, 0, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        GuiHelper.fillRow(inv, 2, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        inv.setItem(22, GuiHelper.backButton());

        String date = war.getEndedAt() > 0
                ? DATE_FORMAT.format(Instant.ofEpochMilli(war.getEndedAt()))
                : "?";
        String winner = war.getWinnerName();
        String resultLine = winner != null
                ? "§7Sieger: §a" + winner
                : "§7Unentschieden";

        inv.setItem(4, new ItemBuilder(Material.WRITTEN_BOOK)
                .name("§e§lKriegs-Info")
                .lore("",
                        "§8| §7" + war.getClan1Name() + ": §a" + war.getClan1Kills() + " §7Kills",
                        "§8| §7" + war.getClan2Name() + ": §a" + war.getClan2Kills() + " §7Kills",
                        "§8| §7Datum: §f" + date,
                        resultLine,
                        "",
                        "§8| §7Wähle eine Partei für das Leaderboard:")
                .flags(org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                .build());

        var clan1 = manager.getClanByName(war.getClan1Name());
        Material mat1 = clan1 != null ? GuiHelper.bannerMaterial(clan1.getBannerColor()) : Material.WHITE_BANNER;
        inv.setItem(11, new ItemBuilder(mat1)
                .name("§6§l" + war.getClan1Name())
                .lore("",
                        "§8| §7Kills: §a" + war.getClan1Kills(),
                        "",
                        "§eKlicke für Leaderboard")
                .build());

        var clan2 = manager.getClanByName(war.getClan2Name());
        Material mat2 = clan2 != null ? GuiHelper.bannerMaterial(clan2.getBannerColor()) : Material.WHITE_BANNER;
        inv.setItem(15, new ItemBuilder(mat2)
                .name("§6§l" + war.getClan2Name())
                .lore("",
                        "§8| §7Kills: §a" + war.getClan2Kills(),
                        "",
                        "§eKlicke für Leaderboard")
                .build());

        player.openInventory(inv);
    }
}

