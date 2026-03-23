package com.betterclan.gui.admin;

import com.betterclan.gui.GuiHelper;
import com.betterclan.gui.GuiHolder;
import com.betterclan.gui.MenuType;
import com.betterclan.clan.Clan;
import com.betterclan.clan.Manager;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("deprecation")
public final class AdminClanList {

    public static final int ITEMS_PER_PAGE = 28;

    private AdminClanList() {}

    public static void open(Manager manager, Player player, int page) {
        List<Clan> clans = new ArrayList<>(manager.getAllClans());
        clans.sort(Comparator.comparing(Clan::getName, String.CASE_INSENSITIVE_ORDER));
        build(manager, player, clans, page, null);
    }

    public static void openSearch(Manager manager, Player player, List<Clan> results, String query) {
        build(manager, player, results, 1, query);
    }

    private static void build(Manager manager, Player player, List<Clan> clans, int page, String query) {
        int totalPages = Math.max(1, (int) Math.ceil((double) clans.size() / ITEMS_PER_PAGE));
        page = Math.clamp(page, 1, totalPages);

        GuiHolder holder = new GuiHolder(MenuType.ADMIN_CLAN_LIST);
        holder.set("page", page);

        String title = query != null
                ? "§c§lAdmin §8| §7Suche: §f" + query + " §8(" + clans.size() + ")"
                : "§c§lAdmin §8| §7Alle Clans §8(" + clans.size() + ")";
        Inventory inv = Bukkit.createInventory(holder, 54, title);

        fillAdminListBorder(inv);

        inv.setItem(4, new ItemBuilder(Material.NETHER_STAR)
                .name("§c§l Admin-Panel")
                .lore("",
                        "§8| §7Clans gesamt: §f" + manager.getAllClans().size(),
                        "",
                        "§8| §eLinksklick: §7Clan verwalten",
                        "§8| §eRechtsklick: §7Vault öffnen")
                .build());

        inv.setItem(47, new ItemBuilder(Material.COMPASS)
                .name("§e§l Clan suchen")
                .lore("", "§eKlicke zum Suchen")
                .build());

        inv.setItem(51, new ItemBuilder(Material.PLAYER_HEAD)
                .name("§b§l Spieler verwalten")
                .lore("",
                        "§eKlicke: §7Spielernamen eingeben")
                .build());

        if (page > 1) {
            inv.setItem(45, GuiHelper.prevPageButton(page - 1));
        } else {
            inv.setItem(45, GuiHelper.prevPageButtonDisabled());
        }
        if (page < totalPages) {
            inv.setItem(53, GuiHelper.nextPageButton(page + 1));
        } else {
            inv.setItem(53, GuiHelper.nextPageButtonDisabled());
        }
        inv.setItem(49, GuiHelper.pageInfo(page, totalPages));

        int[] slots = GuiHelper.CONTENT_SLOTS_28;
        int start = (page - 1) * ITEMS_PER_PAGE;
        for (int i = 0; i < slots.length; i++) {
            int idx = start + i;
            if (idx >= clans.size()) break;
            Clan clan = clans.get(idx);
            OfflinePlayer owner = Bukkit.getOfflinePlayer(clan.getOwnerId());
            String ownerName = owner.getName() != null ? owner.getName() : "Unbekannt";

            ItemBuilder builder = new ItemBuilder(GuiHelper.createClanBanner(clan))
                    .name("§e§l" + clan.getName())
                    .lore("",
                            "§8| §7Anführer: §f" + ownerName,
                            "§8| §7Mitglieder: §f" + clan.getMemberCount(),
                            "§8| §7Level: §f" + clan.getLevel(),
                            "§8| §7Bündnisse: §f" + clan.getAllies().size(),
                            "",
                            "§eLinksklick: §7Verwalten",
                            "§eRechtsklick: §7Vault öffnen");
            inv.setItem(slots[i], builder.build());
        }

        player.openInventory(inv);
    }

    private static void fillAdminListBorder(Inventory inv) {
        var red  = GuiHelper.filler(Material.RED_STAINED_GLASS_PANE);
        var gray = GuiHelper.filler(Material.GRAY_STAINED_GLASS_PANE);
        inv.setItem(0, red);
        inv.setItem(8, red);
        inv.setItem(1, gray);
        inv.setItem(7, gray);
        inv.setItem(9, gray);
        inv.setItem(17, gray);
        inv.setItem(36, gray);
        inv.setItem(44, gray);
        inv.setItem(45, red);
        inv.setItem(53, red);
        inv.setItem(46, gray);
        inv.setItem(52, gray);
    }
}

