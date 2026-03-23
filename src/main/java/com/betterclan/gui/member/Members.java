package com.betterclan.gui.member;

import com.betterclan.gui.GuiHelper;
import com.betterclan.gui.GuiHolder;
import com.betterclan.gui.MenuType;
import com.betterclan.BetterClan;
import com.betterclan.clan.Clan;
import com.betterclan.clan.Manager;
import com.betterclan.clan.Rank;
import com.betterclan.clan.Settings;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("deprecation")
public final class Members {

    public static final int ITEMS_PER_PAGE = 30;

    private Members() {}

    public static NamespacedKey memberKey(BetterClan plugin) {
        return new NamespacedKey(plugin, "clan-member-id");
    }

    public static void openForAdmin(BetterClan plugin, Manager manager, Player player, String clanName, int page) {
        Clan clan = manager.getClanByName(clanName);
        if (clan == null) { player.sendMessage("§cClan nicht gefunden: " + clanName); return; }
        openWithClan(plugin, manager, player, clan, clanName, page);
    }

    public static void open(BetterClan plugin, Manager manager, Player player, int page) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) { player.sendMessage("§cDu bist in keinem Clan."); return; }
        openWithClan(plugin, manager, player, clan, null, page);
    }

    private static void openWithClan(BetterClan plugin, Manager manager, Player player, Clan clan, String adminClanName, int page) {
        Settings settings = manager.getSettings();

        List<UUID> ordered = clan.getOrderedMembers();
        int totalPages = Math.max(1, (int) Math.ceil((double) ordered.size() / ITEMS_PER_PAGE));
        page = Math.clamp(page, 1, totalPages);

        GuiHolder holder = new GuiHolder(MenuType.MEMBERS);
        holder.set("page", page);
        if (adminClanName != null) holder.set("adminClanName", adminClanName);
        Inventory inv = Bukkit.createInventory(holder, 54, "§b§lMitglieder");

        var _w = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        var _lb = GuiHelper.filler(Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        inv.setItem(0, _w); inv.setItem(1, _lb); inv.setItem(2, _lb);
        inv.setItem(6, _lb); inv.setItem(7, _lb); inv.setItem(8, _w);

        inv.setItem(9, _lb); inv.setItem(17, _lb);

        inv.setItem(18, _w); inv.setItem(26, _w);

        inv.setItem(36, _lb); inv.setItem(44, _lb);

        inv.setItem(45, _w); inv.setItem(46, _lb); inv.setItem(47, _w);
        inv.setItem(51, _w); inv.setItem(52, _lb); inv.setItem(53, _w);

        inv.setItem(4, new ItemBuilder(Material.LIGHT_BLUE_BANNER)
            .name("§b§l Clan-Mitglieder")
                .lore("", "§7Clan: §f" + clan.getName(),
                        "§8| §7Mitglieder: §f" + clan.getMemberCount() + "§7/§f" + clan.getMaxMembers(settings),
                        "§8| §7Online: §a" + manager.getOnlineMemberCount(clan), "")
                .build());

        int[] contentSlots = GuiHelper.CONTENT_SLOTS_30;
        NamespacedKey key = memberKey(plugin);
        int startIndex = (page - 1) * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEMS_PER_PAGE && (startIndex + i) < ordered.size(); i++) {
            UUID memberId = ordered.get(startIndex + i);
            String name = Bukkit.getOfflinePlayer(memberId).getName();
            if (name == null) name = memberId.toString().substring(0, 8);

            Rank role = clan.getEffectiveRank(memberId);
            Player onlinePlayer = Bukkit.getPlayer(memberId);
            boolean isOnline = onlinePlayer != null;
            boolean isSelf = memberId.equals(player.getUniqueId());

            List<String> loreLines = new ArrayList<>();
            loreLines.add("");
            loreLines.add("§7Rang: " + (role != null ? role.getColoredName() : "§7Unbekannt"));
            loreLines.add("§7Status: " + (isOnline ? "§aOnline" : "§cOffline"));
            if (!isOnline) {
                long lastSeen = clan.getLastSeen(memberId);
                if (lastSeen <= 0) lastSeen = Bukkit.getOfflinePlayer(memberId).getLastPlayed();
                if (lastSeen > 0) loreLines.add("§7Zuletzt gesehen: §f" + formatLastSeen(lastSeen));
            }
            if (isOnline) {
                org.bukkit.Location loc = onlinePlayer.getLocation();
                String dim = switch (onlinePlayer.getWorld().getEnvironment()) {
                    case NETHER -> "§cNether";
                    case THE_END -> "§5End";
                    default -> "§aOverworld";
                };
                loreLines.add("§7Dimension: " + dim);
                loreLines.add(String.format("§7Position: §f%.0f§7, §f%.0f§7, §f%.0f",
                        loc.getX(), loc.getY(), loc.getZ()));
            }
            loreLines.add("");
            if (!isSelf) loreLines.add("§eKlicke für Aktionen");

            ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD)
                    .skullOwner(Bukkit.getOfflinePlayer(memberId))
                    .name("§b● §f" + name + (isSelf ? " §7(du)" : ""))
                    .lore(loreLines)
                    .persistData(key, memberId.toString());

            if (role != null && Rank.OWNER_ID.equals(role.getId())) builder.glow();
            inv.setItem(contentSlots[i], builder.build());
        }

        inv.setItem(49, GuiHelper.backButton());
        player.openInventory(inv);
    }

    public static UUID extractMemberId(BetterClan plugin, ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        String raw = meta.getPersistentDataContainer().get(memberKey(plugin), PersistentDataType.STRING);
        if (raw == null) return null;
        try { return UUID.fromString(raw); }
        catch (IllegalArgumentException e) { return null; }
    }

    private static String formatLastSeen(long timestamp) {
        return MemberActions.formatAge(timestamp);
    }
}

