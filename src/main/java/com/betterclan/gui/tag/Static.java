package com.betterclan.gui.tag;

import com.betterclan.gui.GuiHelper;
import com.betterclan.gui.GuiHolder;
import com.betterclan.gui.MenuType;
import com.betterclan.clan.Clan;
import com.betterclan.clan.Manager;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class Static {

    private Static() {}

    static final Object[][] COLORS = {
        {"§0", "Schwarz",       Material.BLACK_DYE},
        {"§1", "Dunkelblau",    Material.BLUE_DYE},
        {"§2", "Dunkelgrün",    Material.GREEN_DYE},
        {"§3", "Dunkeltürkis",  Material.CYAN_DYE},
        {"§4", "Dunkelrot",     Material.RED_DYE},
        {"§5", "Lila",          Material.PURPLE_DYE},
        {"§6", "Gold",          Material.ORANGE_DYE},
        {"§7", "Grau",          Material.LIGHT_GRAY_DYE},
        {"§8", "Dunkelgrau",    Material.GRAY_DYE},
        {"§9", "Blau",          Material.LIGHT_BLUE_DYE},
        {"§a", "Grün",          Material.LIME_DYE},
        {"§b", "Türkis",        Material.CYAN_DYE},
        {"§c", "Rot",           Material.RED_DYE},
        {"§d", "Hellviolett",   Material.MAGENTA_DYE},
        {"§e", "Gelb",          Material.YELLOW_DYE},
        {"§f", "Weiß",          Material.WHITE_DYE},
    };

    private static final int[] SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        30,
        20, 21, 22, 23, 24,
        40,
        31, 32
    };

    @SuppressWarnings("unused")
    public static void open(Manager manager, Player player) {
        open(manager, player, null);
    }

    @SuppressWarnings("deprecation")
    public static void open(Manager manager, Player player, String adminClanName) {
        Clan clan = adminClanName != null ? manager.getClanByName(adminClanName) : manager.getClan(player.getUniqueId());
        if (clan == null) return;

        GuiHolder holder = new GuiHolder(MenuType.TAG_COLOR_STATIC);
        if (adminClanName != null) holder.set("adminClanName", adminClanName);
        Inventory inv = Bukkit.createInventory(holder, 54, "§f§lStatische Farben");

        String current = clan.getTagColor();

        for (int i = 0; i < COLORS.length; i++) {
            String code = (String) COLORS[i][0];
            String name = (String) COLORS[i][1];
            Material mat = (Material) COLORS[i][2];
            boolean selected = code.equals(current);

            ItemBuilder ib = new ItemBuilder(mat)
                    .name(code + "§l" + name + (selected ? " §a§l" : ""))
                    .lore("",
                            "§8| §7Vorschau: " + code + "[" + clan.getName() + "] §f" + player.getName(),
                            "",
                            selected ? "§a§lAusgewählt" : "§eKlicke zum Auswählen",
                            "");
            if (selected) ib.glow();
            inv.setItem(SLOTS[i], ib.build());
        }

        inv.setItem(49, GuiHelper.backButton());
        player.openInventory(inv);
    }
}

