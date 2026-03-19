package com.betterclan.gui;

import com.betterclan.clan.Clan;
import com.betterclan.clan.ClanManager;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

@SuppressWarnings("deprecation")
public final class BannerColorMenu {

    private BannerColorMenu() {}

    @SuppressWarnings("unused")
    public static void open(ClanManager manager, Player player) {
        open(manager, player, null);
    }

    public static void open(ClanManager manager, Player player, String adminClanName) {
        Clan clan = adminClanName != null ? manager.getClanByName(adminClanName) : manager.getClan(player.getUniqueId());
        if (clan == null) return;

        GuiHolder holder = new GuiHolder(MenuType.BANNER_COLOR);
        if (adminClanName != null) holder.set("adminClanName", adminClanName);
        Inventory inv = Bukkit.createInventory(holder, 54, "§6§lBanner-Farbe wählen");

        var _w = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        var _lb = GuiHelper.filler(Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        inv.setItem(0, _w); inv.setItem(1, _lb); inv.setItem(2, _w);
        inv.setItem(6, _w); inv.setItem(7, _lb); inv.setItem(8, _w);

        inv.setItem(9, _lb); inv.setItem(17, _lb);

        inv.setItem(18, _w); inv.setItem(26, _w);

        inv.setItem(36, _lb); inv.setItem(44, _lb);

        inv.setItem(45, _w); inv.setItem(46, _lb);
        inv.setItem(49, GuiHelper.backButton());
        inv.setItem(52, _lb); inv.setItem(53, _w);

        inv.setItem(4, new ItemBuilder(GuiHelper.bannerMaterial(clan.getBannerColor()))
                .name("§6Aktuelle Farbe: §f" + GuiHelper.bannerColorName(clan.getBannerColor()))
                .lore("", "§7Wähle eine neue", "§7Banner-Farbe für deinen Clan.", "")
                .build());

        String[][] colorSlots = {

            {"BLACK", "10"}, {"BLUE", "11"}, {"PINK", "15"}, {"RED", "16"},

            {"BROWN", "19"}, {"CYAN", "20"}, {"MAGENTA", "24"}, {"ORANGE", "25"},

            {"LIGHT_GRAY", "28"}, {"LIGHT_BLUE", "29"}, {"PURPLE", "33"}, {"LIME", "34"},

            {"GRAY", "37"}, {"WHITE", "38"}, {"GREEN", "42"}, {"YELLOW", "43"},
        };

        for (String[] cs : colorSlots) {
            String color = cs[0];
            int slot = Integer.parseInt(cs[1]);
            boolean current = color.equalsIgnoreCase(clan.getBannerColor());
            ItemBuilder builder = new ItemBuilder(GuiHelper.bannerMaterial(color))
                    .name((current ? "§a§l " : "§f") + GuiHelper.bannerColorName(color))
                    .lore("", current ? "§aAktuell ausgewählt" : "§eKlicke zum Auswählen");
            if (current) builder.glow();
            inv.setItem(slot, builder.build());
        }

        int patternCount = clan.getBannerPatterns().size();
        int maxPatterns = manager.getSettings().bannerMaxPatterns();
        inv.setItem(31, new ItemBuilder(Material.LOOM)
            .name("§d§l Muster bearbeiten" + (patternCount > 0 ? " §7(" + patternCount + "/" + maxPatterns + ")" : ""))
                .lore("", "§7Füge Muster zum Banner hinzu", "§7oder entferne sie.", "", "§eKlicke zum Bearbeiten")
                .build());

        player.openInventory(inv);
    }
}

