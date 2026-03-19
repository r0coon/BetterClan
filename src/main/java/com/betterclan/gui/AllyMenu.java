package com.betterclan.gui;

import com.betterclan.clan.Clan;
import com.betterclan.clan.ClanManager;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public final class AllyMenu {

    @SuppressWarnings("unused")
    private static final int ALLIES_PER_PAGE   = 3;
    private static final int REQUESTS_PER_PAGE = 36;

    private AllyMenu() {}

    @SuppressWarnings("unused")
    public static void open(ClanManager manager, Player player) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) return;
        boolean canManage = manager.canManage(clan, player.getUniqueId());
        int allyCount    = clan.getAllies().size();
        int requestCount = clan.getAllyRequests().size();

        GuiHolder holder = new GuiHolder(MenuType.ALLIES);
        Inventory inv = Bukkit.createInventory(holder, 27, "§e§lBündnisse");

        var _w  = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        var _lb = GuiHelper.filler(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        inv.setItem(2,  _w);  inv.setItem(3,  _lb);
        inv.setItem(5,  _lb); inv.setItem(6,  _w);
        inv.setItem(9,  _w);  inv.setItem(10, _lb);
        inv.setItem(16, _lb); inv.setItem(17, _w);
        inv.setItem(20, _w);  inv.setItem(21, _lb);
        inv.setItem(23, _lb); inv.setItem(24, _w);

        inv.setItem(4, new ItemBuilder(Material.LIGHT_BLUE_BANNER)
                .name("§b§lBündnisse")
                .lore("", "§7Verbündete: §f" + allyCount,
                        "§8| §7Offene Anfragen: §f" + requestCount, "")
                .build());

        inv.setItem(11, new ItemBuilder(Material.SHIELD)
                .name("§a§l Verbündete §8(§f" + allyCount + "§8)")
                .lore("", "§7Alle aktuellen Bündnisse anzeigen.",
                        allyCount > 0 ? "§7Rechtsklick auf einen Verbündeten zum Auflösen." : "",
                        "", "§eKlicke zum Öffnen")
                .build());

        inv.setItem(13, new ItemBuilder(Material.ENDER_EYE)
                    .name("§b§l Neues Bündnis")
                    .lore("", "§7Öffne die Clan-Liste,",
                            "§8| §7um eine Bündnis-Anfrage zu senden.", "", canManage ? "§eKlicke zum Öffnen" : "§cKeine Berechtigung")
                    .build());

        inv.setItem(15, new ItemBuilder(Material.WRITTEN_BOOK)
                .name("§e§lBündnis-Anfragen §8(§f" + requestCount + "§8)")
                .lore("", "§7Eingehende Bündnis-Anfragen verwalten.",
                        requestCount > 0 ? "§aLinksklick: §7Annehmen  §cRechtsklick: §7Ablehnen" : "",
                        "", "§eKlicke zum Öffnen")
                .build());

        inv.setItem(22, GuiHelper.backButton());
        player.openInventory(inv);
    }

    @SuppressWarnings("unused")
    public static void openList(ClanManager manager, Player player, int page) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) return;
        boolean canManage = manager.canManage(clan, player.getUniqueId());

        List<String> allies = new ArrayList<>(clan.getAllies());
        int requestCount = clan.getAllyRequests().size();

        GuiHolder holder = new GuiHolder(MenuType.ALLY_LIST);
        Inventory inv = Bukkit.createInventory(holder, 27, "§e§lBündnisse Liste");

        var _lb = new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).name(" ").build();
        var _w  = new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).name(" ").build();

        inv.setItem(1, _lb); inv.setItem(2, _w);  inv.setItem(3, _lb);
        inv.setItem(5, _lb); inv.setItem(6, _w);  inv.setItem(7, _lb);

        inv.setItem(9,  _lb); inv.setItem(10, _w);
        inv.setItem(12, _w);  inv.setItem(13, _lb); inv.setItem(14, _w);
        inv.setItem(16, _w);  inv.setItem(17, _lb);

        inv.setItem(19, _lb); inv.setItem(20, _w);  inv.setItem(21, _lb);
        inv.setItem(23, _lb); inv.setItem(24, _w);  inv.setItem(25, _lb);
        inv.setItem(22, GuiHelper.backButton());

        inv.setItem(11, new ItemBuilder(Material.WRITTEN_BOOK)
                .name("§e§lBündnis-Anfragen" + (requestCount > 0 ? " §c(" + requestCount + ")" : ""))
                .lore("", requestCount > 0 ? "§a" + requestCount + " offene Anfrage(n)." : "§7Keine offenen Anfragen.",
                        "", "§eKlicke zum Anzeigen")
                .build());

        if (!allies.isEmpty()) {
            List<String> lorePaper = new ArrayList<>();
            lorePaper.add("");
            for (String a : allies) lorePaper.add("§7- §f" + a);
            lorePaper.add("");
            lorePaper.add("§eKlicke zum Verwalten");
            inv.setItem(13, new ItemBuilder(Material.PAPER)
                    .name("§a§l Bündnisse §8(§f" + allies.size() + "§8)")
                    .lore(lorePaper)
                    .build());
        }

        if (canManage) inv.setItem(15, new ItemBuilder(Material.ENDER_EYE)
                .name("§b§l+ Neues Bündnis")
                .lore("", "§7Suche nach einem Clan,", "§7um eine Bündnis-Anfrage zu senden.", "", "§eKlicke zum Öffnen")
                .build());

        player.openInventory(inv);
    }

    public static void openShields(ClanManager manager, Player player) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) return;
        boolean canManage = manager.canManage(clan, player.getUniqueId());

        List<String> allies = new ArrayList<>(clan.getAllies());

        GuiHolder holder = new GuiHolder(MenuType.ALLY_SHIELDS);
        Inventory inv = Bukkit.createInventory(holder, 27, "§a§lBündnisse verwalten");

        GuiHelper.fillRow(inv, 0, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        GuiHelper.fillRow(inv, 2, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        inv.setItem(22, GuiHelper.backButton());

        int count = allies.size();
        int startSlot = 9 + (3 - count);
        for (int i = 0; i < count; i++) {
            String allyName = allies.get(i);
            Clan allyClan = manager.getClanByName(allyName);
            String leaderName = allyClan != null ? Bukkit.getOfflinePlayer(allyClan.getOwnerId()).getName() : "?";
            int members = allyClan != null ? allyClan.getMemberCount() : 0;
            int online  = allyClan != null ? manager.getOnlineMemberCount(allyClan) : 0;

            List<String> lore = buildClanRelationLore(canManage, leaderName, members, online, false);

            inv.setItem(startSlot + i * 2, new ItemBuilder(Material.SHIELD)
                    .name("§a§l " + (allyClan != null ? allyClan.getName() : allyName))
                    .lore(lore).build());
        }

        player.openInventory(inv);
    }

    public static void openRequests(ClanManager manager, Player player, int page) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) return;
        boolean canManage = manager.canManage(clan, player.getUniqueId());

        List<String> requests = new ArrayList<>(clan.getAllyRequests());
        int totalPages = Math.max(1, (int) Math.ceil(requests.size() / (double) REQUESTS_PER_PAGE));
        page = Math.clamp(page, 1, totalPages);

        GuiHolder holder = new GuiHolder(MenuType.ALLY_REQUESTS);
        holder.set("page", page);
        Inventory inv = Bukkit.createInventory(holder, 54, "§e§lBündnis-Anfragen");

        GuiHelper.fillRow(inv, 0, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        GuiHelper.fillRow(inv, 5, Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        inv.setItem(4, new ItemBuilder(Material.WRITTEN_BOOK)
                .name("§e§lBündnis-Anfragen")
                .lore("", "§8| §7Offen: §f" + requests.size(),
                        canManage ? "§aLinksklick: Annehmen  §cRechtsklick: Ablehnen" : "", "")
                .build());

        int start = (page - 1) * REQUESTS_PER_PAGE;
        int end   = Math.min(start + REQUESTS_PER_PAGE, requests.size());
        int slot  = 9;

        if (!requests.isEmpty()) {
            for (int i = start; i < end; i++) {
                String reqName    = requests.get(i);
                Clan   reqClan    = manager.getClanByName(reqName);
                String leaderName = reqClan != null ? Bukkit.getOfflinePlayer(reqClan.getOwnerId()).getName() : "?";
                int    members    = reqClan != null ? reqClan.getMemberCount() : 0;
                int    online     = reqClan != null ? manager.getOnlineMemberCount(reqClan) : 0;

                List<String> lore = buildClanRelationLore(canManage, leaderName, members, online, true);

                String displayClanName = reqClan != null ? reqClan.getName() : reqName;
                inv.setItem(slot++, new ItemBuilder(Material.WRITTEN_BOOK)
                        .name("§e§lAnfrage von §f" + displayClanName)
                        .lore(lore).build());
            }
        }

        inv.setItem(48, page > 1 ? GuiHelper.prevPageButton(page - 1) : GuiHelper.prevPageButtonDisabled());
        inv.setItem(49, GuiHelper.backButton());
        inv.setItem(50, page < totalPages ? GuiHelper.nextPageButton(page + 1) : GuiHelper.nextPageButtonDisabled());
        player.openInventory(inv);
    }

        private static List<String> buildClanRelationLore(boolean canManage, String leaderName, int members, int online,
                                                                                                          boolean requestActions) {
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§7Anführer: §f" + (leaderName != null ? leaderName : "Unbekannt"));
                lore.add("§7Mitglieder: §f" + members);
                lore.add("§7Online: §a" + online);
                lore.add("");
                if (canManage) {
                        if (requestActions) {
                                lore.add("§aLinksklick: §7Annehmen");
                                lore.add("§cRechtsklick: §7Ablehnen");
                        } else {
                                lore.add("§cRechtsklick: Bündnis auflösen");
                        }
                }
                return lore;
        }
}

