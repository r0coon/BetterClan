package com.betterclan.gui.invite;

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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("deprecation")
public final class Players {

    public static final int ITEMS_PER_PAGE = 30;

    private Players() {}

    private static NamespacedKey targetKey(Plugin plugin) {
        return new NamespacedKey(plugin, "invite-target-id");
    }

    public static void open(Plugin plugin, Manager manager, Player player, int page) {
        open(plugin, manager, player, page, null);
    }

    public static void open(Plugin plugin, Manager manager, Player player, int page, String searchQuery) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null || !manager.canManage(clan, player.getUniqueId())) {
            player.sendMessage("§cDu hast keine Berechtigung.");
            return;
        }

        List<Player> eligible = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) continue;
            if (manager.isInClan(online.getUniqueId())) continue;
            if (searchQuery != null && !searchQuery.isBlank()
                    && !online.getName().toLowerCase().contains(searchQuery.toLowerCase())) continue;
            eligible.add(online);
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) eligible.size() / ITEMS_PER_PAGE));
        page = Math.clamp(page, 1, totalPages);

        GuiHolder holder = new GuiHolder(MenuType.INVITE_PLAYERS);
        holder.set("page", page);
        if (searchQuery != null && !searchQuery.isBlank()) holder.set("searchQuery", searchQuery);

        String titleSuffix = (searchQuery != null && !searchQuery.isBlank())
                ? "§7Suche: §f\"" + searchQuery + "\" §8(" + eligible.size() + ")"
                : "§7(" + eligible.size() + ")";
        Inventory inv = Bukkit.createInventory(holder, 54, "§a§lSpieler einladen " + titleSuffix);

        var _w = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        var _lb = GuiHelper.filler(Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        inv.setItem(0, _w); inv.setItem(1, _lb); inv.setItem(2, _lb);
        inv.setItem(6, _lb); inv.setItem(7, _lb); inv.setItem(8, _w);

        inv.setItem(9, _lb); inv.setItem(17, _lb);

        inv.setItem(18, _lb); inv.setItem(26, _lb);

        inv.setItem(36, _lb); inv.setItem(44, _lb);

        inv.setItem(45, _w); inv.setItem(46, _lb);
        inv.setItem(52, _lb); inv.setItem(53, _w);

        inv.setItem(4, new ItemBuilder(Material.PAPER)
            .name("§a§l Spieler einladen")
                .lore("", "§8| §7Verfügbar: §f" + eligible.size() + " §7Spieler")
                .build());

        inv.setItem(35, new ItemBuilder(Material.NAME_TAG)
            .name("§b§l Spieler suchen")
                .lore((searchQuery != null && !searchQuery.isBlank())
                        ? new String[]{"§8| §7Aktuell: §f\"" + searchQuery + "\""}
                        : new String[]{})
                .build());

        int[] contentSlots = GuiHelper.CONTENT_SLOTS_30;
        NamespacedKey key = targetKey(plugin);
        int startIndex = (page - 1) * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEMS_PER_PAGE && (startIndex + i) < eligible.size(); i++) {
            Player target = eligible.get(startIndex + i);
            inv.setItem(contentSlots[i], new ItemBuilder(Material.PLAYER_HEAD)
                    .skullOwner(target)
                    .name("§a§l " + target.getName())
                    .lore("", "§8| §7Status: §aOnline", "", "§eKlicke zum Einladen")
                    .persistData(key, target.getUniqueId().toString())
                    .build());
        }

        inv.setItem(47, page > 1 ? GuiHelper.prevPageButton(page - 1) : GuiHelper.prevPageButtonDisabled());
        inv.setItem(49, GuiHelper.backButton());
        inv.setItem(51, page < totalPages ? GuiHelper.nextPageButton(page + 1) : GuiHelper.nextPageButtonDisabled());
        player.openInventory(inv);
    }

    public static UUID extractTargetId(Plugin plugin, ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        String raw = meta.getPersistentDataContainer().get(targetKey(plugin), PersistentDataType.STRING);
        if (raw == null) return null;
        try { return UUID.fromString(raw); }
        catch (IllegalArgumentException e) { return null; }
    }
}

