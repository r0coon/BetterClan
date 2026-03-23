package com.betterclan.gui.warlog;

import com.betterclan.gui.GuiHelper;
import com.betterclan.gui.GuiHolder;
import com.betterclan.gui.MenuType;
import com.betterclan.clan.Clan;
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
import java.util.List;

public final class History {

    private History() {}

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("dd.MM.yyyy")
            .withZone(ZoneId.systemDefault());

    private static final int[] SLOTS = {
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        28,29,30,31,32,33,34
    };
    private static final int ITEMS_PER_PAGE = SLOTS.length;

    public static void open(Manager manager, Player player) {
        open(manager, player, 1);
    }

    public static void open(Manager manager, Player player, int page) {
        var clan = manager.getClan(player.getUniqueId());
        if (clan == null) return;
        openFor(manager, player, clan.getName(), null, page);
    }

    public static void open(Manager manager, Player player, String clanName) {
        open(manager, player, clanName, 1);
    }

    public static void open(Manager manager, Player player, String clanName, int page) {
        openFor(manager, player, clanName, clanName, page);
    }

    @SuppressWarnings("deprecation")
    private static void openFor(Manager manager, Player player, String clanName, String viewClanName, int page) {
        List<War> history = manager.getWarHistory(clanName);

        GuiHolder holder = new GuiHolder(MenuType.WAR_HISTORY);
        if (viewClanName != null) holder.set("viewClanName", viewClanName);
        holder.set("page", page);

        String title = viewClanName != null
            ? "§6" + clanName + " §7— Kriegsverlauf"
            : "§4§lKriegs-Verlauf";
        Inventory inv = Bukkit.createInventory(holder, 54, title);

        var _w = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        var _r = GuiHelper.filler(Material.RED_STAINED_GLASS_PANE);

        inv.setItem(1, _r); inv.setItem(2, _w);
        inv.setItem(3, _w); inv.setItem(4, _w);

        inv.setItem(5, _w);
        inv.setItem(6, _w); inv.setItem(7, _r);

        inv.setItem(9, _r); inv.setItem(17, _r);

        inv.setItem(18, _r); inv.setItem(26, _r);

        inv.setItem(27, _r); inv.setItem(35, _r);

        inv.setItem(36, _r); inv.setItem(44, _r);

        inv.setItem(46, _r);

        inv.setItem(48, _w);
        inv.setItem(49, GuiHelper.backButton());
        inv.setItem(50, _w);

        inv.setItem(52, _r);

        int totalWars = history.size();
        int wins = 0;
        int losses = 0;
        int draws = 0;
        for (War w : history) {
            String winner = w.getWinnerName();
            if (w.isDraw() || winner == null) {
                draws++;
            } else if (winner.equalsIgnoreCase(clanName)) {
                wins++;
            } else {
                losses++;
            }
        }

        inv.setItem(4, new ItemBuilder(Material.PAPER)
            .name("§6§lStatistik")
                .lore("",
                        "§aGewonnen: §f" + wins,
                        "§cVerloren: §f" + losses,
                        "§7Unentschieden: §f" + draws,
                        "",
                        "§7Kriege: §f" + totalWars)
                .build());

        if (history.isEmpty()) {
            inv.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name("§7Keine vergangenen Kriege")
                    .lore("", viewClanName != null ? "§8Dieser Clan hat noch" : "§8Dein Clan hat noch",
                            "§8keine Kriege beendet.")
                    .build());

            inv.setItem(47, GuiHelper.prevPageButtonDisabled());
            inv.setItem(51, GuiHelper.nextPageButtonDisabled());
        } else {
            int totalPages = (int) Math.ceil((double) history.size() / ITEMS_PER_PAGE);
            if (page < 1) page = 1;
            if (page > totalPages) page = totalPages;

            int startIndex = (page - 1) * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, history.size());

            for (int i = startIndex; i < endIndex; i++) {
                War war = history.get(i);
                String opponent = war.getOpponent(clanName);
                int ownKills   = war.getKillsFor(clanName);
                int enemyKills = war.getKillsFor(opponent != null ? opponent : "");
                String winner  = war.getWinnerName();

                String itemName;
                Material mat;
                if (war.isDraw() || winner == null) {
                    itemName = "§7§l Unentschieden §8— §f" + (opponent != null ? opponent : "?");
                    mat = Material.BOOK;
                } else if (winner.equalsIgnoreCase(clanName)) {
                    itemName = "§a§l Gewonnen §8— §f" + (opponent != null ? opponent : "?");
                    mat = Material.KNOWLEDGE_BOOK;
                } else {

                    String forced = war.getForcedWinner();
                    boolean surrendered = forced != null && !forced.equalsIgnoreCase(clanName);
                    itemName = (surrendered ? "§c§l Kapituliert" : "§c§l Verloren") + " §8— §f" + (opponent != null ? opponent : "?");
                    mat = Material.WRITTEN_BOOK;
                }

                String date = war.getEndedAt() > 0
                        ? DATE_FORMAT.format(Instant.ofEpochMilli(war.getEndedAt()))
                        : "?";

                int slotIndex = i - startIndex;
                inv.setItem(SLOTS[slotIndex], new ItemBuilder(mat)
                        .name(itemName)
                        .lore("",
                                "§8| §7Score: §a" + ownKills + " §7: §c" + enemyKills,
                                "§8| §7Datum: §f" + date,
                                "",
                                "§eKlicke für Leaderboard")
                        .flags(org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                        .build());
                holder.set("war_" + SLOTS[slotIndex], war);
            }

            inv.setItem(47, page > 1 ? GuiHelper.prevPageButton(page - 1) : GuiHelper.prevPageButtonDisabled());
            inv.setItem(51, page < totalPages ? GuiHelper.nextPageButton(page + 1) : GuiHelper.nextPageButtonDisabled());
        }

        player.openInventory(inv);
    }
}

