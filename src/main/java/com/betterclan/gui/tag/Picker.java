package com.betterclan.gui.tag;

import com.betterclan.gui.GuiHelper;
import com.betterclan.gui.GuiHolder;
import com.betterclan.gui.MenuType;
import com.betterclan.clan.Clan;
import com.betterclan.clan.Manager;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class Picker {

    private Picker() {}

    public static final NamespacedKey TAG_FORMAT_KEY = new NamespacedKey("betterclan", "tag-format");

    public static void open(Manager manager, Player player) {
        open(manager, player, null);
    }

    @SuppressWarnings("deprecation")
    public static void open(Manager manager, Player player, String adminClanName) {
        Clan clan = adminClanName != null ? manager.getClanByName(adminClanName) : manager.getClan(player.getUniqueId());
        if (clan == null) return;

        GuiHolder holder = new GuiHolder(MenuType.TAG_COLOR);
        if (adminClanName != null) holder.set("adminClanName", adminClanName);
        Inventory inv = Bukkit.createInventory(holder, 27, "§6§lTag-Farbe");

        var _w = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        var _b = GuiHelper.filler(Material.BLUE_STAINED_GLASS_PANE);

        inv.setItem(1, _b); inv.setItem(2, _w); inv.setItem(3, _b);
        inv.setItem(4, _w);
        inv.setItem(5, _b); inv.setItem(6, _w); inv.setItem(7, _b);

        inv.setItem(9, _b); inv.setItem(10, _w);
        inv.setItem(12, _w); inv.setItem(13, _b); inv.setItem(14, _w);
        inv.setItem(16, _w); inv.setItem(17, _b);

        inv.setItem(19, _b); inv.setItem(20, _w);
        inv.setItem(21, _b);
        inv.setItem(23, _b);
        inv.setItem(24, _w); inv.setItem(25, _b);

        String current = clan.getTagColor();
        boolean isDynamic = current.startsWith("<");

        inv.setItem(11, new ItemBuilder(Material.WHITE_DYE)
                .name("§f§lStatische Farben")
                .lore("",
                        "§8| §7Feste Farben wie §cRot§7, §eGelb§7, §aGrün§7.",
                        "",
                        !isDynamic ? "§a§l✔ Aktuell ausgewählt" : "§eKlicke zum Öffnen")
                .build());

        inv.setItem(15, new ItemBuilder(Material.NETHER_STAR)
                .name("§d§lDynamische Farben")
                .lore("",
                        "§8| §7Farbverläufe wie §d§lRegenbogen§7,",
                        "§8| §7§6Feuer§7, §b§lOzean §7und mehr.",
                        "",
                        isDynamic ? "§a§l✔ Aktuell ausgewählt" : "§eKlicke zum Öffnen")
                .build());

        inv.setItem(22, GuiHelper.backButton());

        inv.setItem(13, new ItemBuilder(Material.PAPER)
                .name("§6§lAktueller Modus")
                .lore("", isDynamic ? "§dDynamische Farben" : "§fStatische Farben")
                .build());

        player.openInventory(inv);
    }
}

