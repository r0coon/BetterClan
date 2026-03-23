package com.betterclan.gui.clan;

import com.betterclan.gui.GuiHelper;
import com.betterclan.gui.GuiHolder;
import com.betterclan.gui.MenuType;
import com.betterclan.clan.Clan;
import com.betterclan.clan.Manager;
import com.betterclan.clan.Permission;
import com.betterclan.clan.War;
import com.betterclan.clan.Settings;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("deprecation")
public final class ClanList {

    public static final int ITEMS_PER_PAGE = 28;

    private ClanList() {}

    public static void open(Manager manager, Player player, int page) {
        Clan playerClan = manager.getClan(player.getUniqueId());
        List<Clan> allClans = new ArrayList<>(manager.getAllClans());
        if (playerClan != null) allClans.removeIf(c -> c.getName().equals(playerClan.getName()));
        allClans.sort(Comparator.comparingInt(Clan::getMemberCount).reversed()
                .thenComparing(Clan::getName, String.CASE_INSENSITIVE_ORDER));
        buildMenu(manager, player, allClans, page, null, false);
    }

    public static void openForWar(Manager manager, Player player, int page) {
        Clan playerClan = manager.getClan(player.getUniqueId());
        List<Clan> allClans = new ArrayList<>(manager.getAllClans());

        if (playerClan != null) allClans.removeIf(c -> c.getName().equals(playerClan.getName()));
        allClans.sort(Comparator.comparingInt(Clan::getMemberCount).reversed()
                .thenComparing(Clan::getName, String.CASE_INSENSITIVE_ORDER));
        buildMenu(manager, player, allClans, page, null, true);
    }

    public static void openSearch(Manager manager, Player player, List<Clan> results, String query) {
        buildMenu(manager, player, results, 1, query, false);
    }

    private static void buildMenu(Manager manager, Player player, List<Clan> clans,
                                   int page, String query, boolean warMode) {
        Settings settings = manager.getSettings();
        int totalPages = Math.max(1, (int) Math.ceil((double) clans.size() / ITEMS_PER_PAGE));
        page = Math.clamp(page, 1, totalPages);

        GuiHolder holder = new GuiHolder(MenuType.CLAN_LIST);
        holder.set("page", page);
        if (warMode) holder.set("warMode", "true");

        String title = query != null
                ? "§b§lSuche: §7" + query + " §8(" + clans.size() + ")"
                : "§b§lAlle Clans §7(" + clans.size() + ")";
        Inventory inv = Bukkit.createInventory(holder, 54, title);

        var _w = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        var _lb = GuiHelper.filler(Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        inv.setItem(0, _w); inv.setItem(8, _w);
        inv.setItem(1, _lb); inv.setItem(7, _lb);

        inv.setItem(9, _lb); inv.setItem(17, _lb);

        inv.setItem(36, _lb); inv.setItem(44, _lb);

        inv.setItem(45, _w); inv.setItem(53, _w);
        inv.setItem(46, _lb); inv.setItem(52, _lb);

        inv.setItem(3, new ItemBuilder(Material.SHIELD)
                .name("§b§lServer-Clans")
                .lore("", query != null ? "§7Ergebnisse für: §f\"" + query + "\"" : "§7Alle Clans auf dem Server.",
                        "§8| §7Gesamt: §f" + clans.size() + " Clans")
                .build());

        inv.setItem(5, new ItemBuilder(Material.NAME_TAG)
                .name("§e§l Clan suchen")
                .lore("", "§7Suche nach einem Clan", "§7anhand des Namens.", "", "§eKlicke zum Suchen")
                .build());

        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int slot = 10;
        int placed = 0;

        for (int i = startIndex; i < clans.size() && placed < ITEMS_PER_PAGE; i++) {
            Clan clan = clans.get(i);
            String ownerName = Bukkit.getOfflinePlayer(clan.getOwnerId()).getName();
            int online = manager.getOnlineMemberCount(clan);

            inv.setItem(slot, new ItemBuilder(GuiHelper.createClanBanner(clan))
                    .name("§6§l" + clan.getName())
                    .lore("",
                            "§8| §7Anführer: §f" + (ownerName != null ? ownerName : "Unbekannt"),
                            "§8| §7Mitglieder: §f" + clan.getMemberCount() + "§7/§f" + clan.getMaxMembers(settings),
                            "§8| §7Online: §a" + online,
                            "§8| §7Level: §e" + clan.getLevel() + " §7| §7Siege: §e" + clan.getXp() + "§7/§e" + clan.getXpForNextLevel(settings),
                            "§8| §7Allianzen: §f" + clan.getAllies().size())
                    .flags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                    .build());

            placed++;
            slot++;

            if (slot % 9 == 8) slot += 2;
        }

        inv.setItem(47, page > 1 ? GuiHelper.prevPageButton(page - 1) : GuiHelper.prevPageButtonDisabled());
        inv.setItem(49, GuiHelper.backButton());
        inv.setItem(51, page < totalPages ? GuiHelper.nextPageButton(page + 1) : GuiHelper.nextPageButtonDisabled());
        player.openInventory(inv);
    }

    public static void openInfo(Manager manager, Player player, Clan targetClan, int warTargetKills) {
        Settings settings = manager.getSettings();
        GuiHolder infoHolder = new GuiHolder(MenuType.CLAN_LIST_INFO);
        infoHolder.set("clanName", targetClan.getName());

        Clan playerClan = manager.getClan(player.getUniqueId());
        boolean canAlly = playerClan != null
                && manager.hasPermission(playerClan, player.getUniqueId(), Permission.ALLY)
                && !playerClan.isAlly(targetClan.getName());
        boolean alreadyRequested = canAlly && targetClan.hasAllyRequest(playerClan.getName());
        boolean canWar = playerClan != null
                && manager.hasPermission(playerClan, player.getUniqueId(), Permission.WAR)
                && !manager.isAtWar(playerClan.getName())
                && !manager.isAtWar(targetClan.getName());
        boolean isAlly = playerClan != null && playerClan.isAlly(targetClan.getName());

        String ownerName = Bukkit.getOfflinePlayer(targetClan.getOwnerId()).getName();
        int online = manager.getOnlineMemberCount(targetClan);

        Inventory inv = Bukkit.createInventory(infoHolder, 36, "§b§l" + targetClan.getName());
        for (int i = 0; i < 9; i++)  inv.setItem(i, GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE));
        for (int i = 27; i < 36; i++) inv.setItem(i, GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE));
        inv.setItem(31, GuiHelper.backButton());

        inv.setItem(4, new ItemBuilder(GuiHelper.createClanBanner(targetClan))
                .name("§6§l" + targetClan.getName())
                .lore("",
                        "§8| §7Anführer: §f" + (ownerName != null ? ownerName : "Unbekannt"),
                        "§8| §7Mitglieder: §f" + targetClan.getMemberCount() + "§7/§f" + targetClan.getMaxMembers(settings),
                        "§8| §7Online: §a" + online,
                        "§8| §7Level: §e" + targetClan.getLevel(),
                        "§8| §7Bündnisse: §f" + targetClan.getAllies().size(),
                        "")
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        if (canAlly && !alreadyRequested) {
            inv.setItem(12, new ItemBuilder(Material.ENDER_EYE)
                    .name("§a§l Bündnis anfragen")
                    .lore("", "§7an §f" + targetClan.getName() + "§7.", "", "§eKlicke zum Bestätigen")
                    .build());
        } else if (canAlly) {
            inv.setItem(12, new ItemBuilder(Material.GRAY_DYE)
                    .name("§7Anfrage bereits gesendet")
                    .lore("§8Bereits verbündet oder Anfrage gestellt.")
                    .build());
        }

        War activeWar = manager.getActiveWar(targetClan.getName());
        if (activeWar != null) {
            String warOpp = activeWar.getOpponent(targetClan.getName());
            int theirKills = activeWar.getKillsFor(targetClan.getName());
            int oppKills = activeWar.getKillsFor(warOpp != null ? warOpp : "");
            inv.setItem(14, new ItemBuilder(Material.IRON_SWORD)
                    .name("§c§l Im Krieg")
                    .lore("",
                            "§8| §7Gegner: §f" + (warOpp != null ? warOpp : "?"),
                            "§8| §7Score: §a" + theirKills + " §7: §c" + oppKills,
                            "§8| §7Ziel: §6" + activeWar.getTargetKills() + " Kills",
                            "§8| §7Verbleibend: §e" + activeWar.getRemainingTimeFormatted())
                    .flags(ItemFlag.HIDE_ATTRIBUTES)
                    .build());
        } else if (canWar) {
            long myCooldown = manager.getWarCooldownRemaining(playerClan.getName());
            long theirCooldown = manager.getWarCooldownRemaining(targetClan.getName());
            long cooldown = Math.max(myCooldown, theirCooldown);
            if (cooldown > 0) {
                long mins = cooldown / 60000;
                long secs = (cooldown % 60000) / 1000;
                int cooldownCfg = settings.warCooldownMinutes();
                inv.setItem(14, new ItemBuilder(Material.CLOCK)
                        .name("§7§l Krieg-Cooldown")
                        .lore("", (myCooldown > 0 ? "§7Dein Clan" : "§7Ziel-Clan") + " hat noch:",
                                "§c" + mins + "m " + secs + "s",
                                "", "§8Cooldown nach Krieg: " + cooldownCfg + " Min.")
                        .build());
            } else {

                if (manager.hasSentWarRequestTo(playerClan, targetClan)) {
                    inv.setItem(14, new ItemBuilder(Material.ORANGE_DYE)
                            .name("§6§l Kriegsanfrage gesendet")
                            .lore("", "§7Du hast bereits eine", "§7Kriegsanfrage an §f" + targetClan.getName() + "§7 gesendet.",
                                    "", "§8Warte auf Antwort...")
                            .build());
                } else {
                    inv.setItem(14, new ItemBuilder(Material.NETHERITE_SWORD)
                            .name("§c§l Krieg erklären" + (isAlly ? " §7§o(Allianz lösen)" : ""))
                            .lore("", "§7Sende eine Kriegsanfrage", "§7an §f" + targetClan.getName() + "§7.",
                                    isAlly ? "§c§lACHTUNG: §cDie Allianz wird automatisch aufgelöst!" : "",
                                    "§8| §7Ziel: §6" + warTargetKills + " Kills", "", "§eKlicke zum Bestätigen")
                            .flags(ItemFlag.HIDE_ATTRIBUTES)
                            .build());
                }
            }
        } else {
            inv.setItem(14, new ItemBuilder(Material.GREEN_DYE)
                    .name("§a§l Im Frieden")
                    .lore("", "§7Kein aktiver Krieg.")
                    .build());
        }
        inv.setItem(22, new ItemBuilder(Material.BOOK)
                .name("§7§l Kriegs-Verlauf")
                .lore("", "§eKlicke zum Öffnen")
                .build());

        player.openInventory(inv);
    }
}

