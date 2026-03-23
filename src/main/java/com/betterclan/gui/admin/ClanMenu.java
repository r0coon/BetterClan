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

import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings("deprecation")
public final class ClanMenu {

    private ClanMenu() {}

    public static void open(Manager manager, Player player, String clanName) {
        Clan clan = manager.getClanByName(clanName);
        if (clan == null) {
            player.sendMessage("§cClan nicht gefunden: " + clanName);
            return;
        }

        GuiHolder holder = new GuiHolder(MenuType.ADMIN_CLAN_MENU);
        holder.set("clanName", clan.getName());

        Inventory inv = Bukkit.createInventory(holder, 54,
                "§c§lAdmin §8| §7" + clan.getName());

        GuiHelper.fillAdminRedGrayBorder(inv);

        OfflinePlayer owner = Bukkit.getOfflinePlayer(clan.getOwnerId());
        String ownerName = owner.getName() != null ? owner.getName() : "Unbekannt";
        String created = new SimpleDateFormat("dd.MM.yyyy").format(new Date(clan.getCreatedAt()));
        String allies = clan.getAllies().isEmpty() ? "§7keine"
                : String.join("§8, §7", clan.getAllies());

        inv.setItem(4, new ItemBuilder(GuiHelper.createClanBanner(clan))
                .name("§e§l" + clan.getName())
                .lore("",
                        "§8| §7Anführer: §f" + ownerName,
                        "§8| §7Mitglieder: §f" + clan.getMemberCount(),
                        "§8| §7Level: §f" + clan.getLevel(),
                        "§8| §7Siege: §f" + clan.getXp(),
                        "§8| §7Friendly Fire: §f" + (clan.isFriendlyFire() ? "§aan" : "§caus"),
                        "§8| §7Gegründet: §f" + created,
                        "§8| §7Bündnisse: §7" + allies)
                .build());

        inv.setItem(10, new ItemBuilder(Material.CHEST)
                .name("§6§l Vault öffnen")
                .lore("", "§7Öffnet den Clan-Vault.", "", "§eKlicke zum Öffnen")
                .build());

        inv.setItem(12, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name("§a§l Level setzen")
                .lore("",
                        "§8| §7Aktuelles Level: §f" + clan.getLevel(),
                        "",
                        "§8| §eDoppelklick: §7+1 Level",
                        "§8| §eShift + Rechts: §7-1 Level")
                .build());

        int curOverride = clan.getMaxMembersOverride();
        int curMax = clan.getMaxMembers(manager.getSettings());
        inv.setItem(14, new ItemBuilder(Material.PLAYER_HEAD)
                .name("§b§l Max. Mitglieder")
                .lore("",
                        "§8| §7Aktuell: §f" + curMax + (curOverride > 0 ? " §8(manuell überschrieben)" : " §8(Level-Standard)"),
                        "§8| §7Mitglieder: §f" + clan.getMemberCount(),
                        "",
                        "§8| §eLinksklick: §7Wert eingeben",
                        "§8| §eRechtsklick: §7Override zurücksetzen")
                .build());

        inv.setItem(16, new ItemBuilder(Material.NAME_TAG)
                .name("§e§l Name ändern")
                .lore("",
                        "§8| §7Aktuell: §f" + clan.getName(),
                        "",
                        "§8| §eKlicke: §7Neuen Namen eingeben")
                .build());

        inv.setItem(20, new ItemBuilder(clan.isFriendlyFire() ? Material.FIRE_CHARGE : Material.SNOWBALL)
                .name("§6§l Friendly Fire")
                .lore("",
                        "§8| §7Status: " + (clan.isFriendlyFire() ? "§aan" : "§caus"),
                        "",
                        "§8| §eDoppelklick: §7Umschalten")
                .build());

        boolean inWar = manager.getActiveWar(clan.getName()) != null;
        inv.setItem(22, new ItemBuilder(inWar ? Material.RED_BANNER : Material.WHITE_BANNER)
                .name("§c§l Krieg beenden")
                .lore("",
                        "§8| §7Status: " + (inWar ? "§cIm Krieg" : "§aKein aktiver Krieg"),
                        "",
                        inWar ? "§eKlicke: §7Krieg sofort beenden" : "§7Kein aktiver Krieg")
                .build());

        int maxRanks = Math.min(manager.getSettings().rankMaxTotal(), GuiHelper.CONTENT_SLOTS_28.length);
        inv.setItem(24, new ItemBuilder(Material.BOOKSHELF)
                .name("§d§l Ränge verwalten")
                .lore("",
                        "§8| §7Ränge: §f" + clan.getOrderedRanks().size() + " §8/ §f" + maxRanks,
                        "",
                        "§8| §eKlicke: §7Rang-Verwaltung öffnen")
                .build());

        inv.setItem(28, new ItemBuilder(clan.getAllies().isEmpty() ? Material.BARRIER : Material.IRON_SWORD)
                .flags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                .name("§c§l Bündnis auflösen")
                .lore("",
                        "§8| §7Aktive Bündnisse: §f" + clan.getAllies().size(),
                        "",
                        clan.getAllies().isEmpty()
                            ? "§7Keine Bündnisse vorhanden."
                            : "§8| §eKlicke: §7Bündnis auswählen")
                .build());

        inv.setItem(34, new ItemBuilder(Material.TNT)
                .name("§4§l Clan auflösen")
                .lore("",
                        "§cLöscht den Clan unwiderruflich!",
                        "",
                        "§c§lKlicke: §cAuflösen")
                .build());

        inv.setItem(31, new ItemBuilder(Material.BOOK)
                .name("§e§l Clan-Log")
                .lore("",
                        "§8| §7Einträge: §f" + clan.getClanLog().size() + " §8/ §f" + manager.getSettings().maxLogEntries(),
                        "",
                        "§8| §eKlicke: §7Log öffnen")
                .build());

        inv.setItem(40, new ItemBuilder(Material.PAINTING)
                .name("§6§l Banner & Tag")
                .lore("",
                        "§8| §7Banner-Farbe: §f" + clan.getBannerColor(),
                        "",
                        "§8| §eLinksklick: §7Tag-Farbe ändern",
                        "§8| §eRechtsklick: §7Banner-Farbe ändern")
                .build());

        inv.setItem(39, new ItemBuilder(Material.PLAYER_HEAD)
                .name("§b§l Mitglieder verwalten")
                .lore("",
                        "§8| §7Mitglieder: §f" + clan.getMemberCount(),
                        "",
                        "§8| §eKlicke: §7Mitgliederliste öffnen")
                .build());

        inv.setItem(41, new ItemBuilder(Material.GOLDEN_HELMET)
                .name("§6§l Clan übertragen")
                .lore("",
                        "§8| §7Aktueller Anführer: §f" + ownerName,
                        "",
                        "§8| §eKlicke: §7Spielernamen eingeben")
                .flags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                .build());

        inv.setItem(49, GuiHelper.backButton());

        player.openInventory(inv);
    }
}

