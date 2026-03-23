package com.betterclan.gui.misc;

import com.betterclan.gui.GuiHelper;
import com.betterclan.gui.GuiHolder;
import com.betterclan.gui.MenuType;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

@SuppressWarnings("deprecation")
public final class Confirm {

    private Confirm() {}

    public static void open(Player player, String title,
                            String confirmText, String action, String... extraData) {
        GuiHolder holder = new GuiHolder(MenuType.CONFIRM);
        holder.set("action", action);
        for (int i = 0; i + 1 < extraData.length; i += 2) {
            holder.set(extraData[i], extraData[i + 1]);
        }

        Inventory inv = Bukkit.createInventory(holder, 27, title);

        for (int i = 0; i < 27; i++) {
            int row = i / 9;
            int col = i % 9;
            Material mat;
            if (row == 1) {

                mat = (col == 0 || col == 4 || col == 8)
                        ? Material.GRAY_STAINED_GLASS_PANE
                        : Material.WHITE_STAINED_GLASS_PANE;
            } else {

                mat = (col == 2 || col == 6)
                        ? Material.WHITE_STAINED_GLASS_PANE
                        : Material.GRAY_STAINED_GLASS_PANE;
            }
            inv.setItem(i, GuiHelper.filler(mat));
        }

        inv.setItem(11, new ItemBuilder(Material.LIME_CONCRETE)
                .name("§a§l " + confirmText)
                .build());

        inv.setItem(15, new ItemBuilder(Material.RED_CONCRETE)
            .name("§c§l Abbrechen")
            .build());

        player.openInventory(inv);
    }
}

