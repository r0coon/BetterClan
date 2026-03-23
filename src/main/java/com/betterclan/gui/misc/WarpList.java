package com.betterclan.gui.misc;

import com.betterclan.gui.GuiHelper;
import com.betterclan.gui.GuiHolder;
import com.betterclan.gui.MenuType;
import com.betterclan.BetterClan;
import com.betterclan.clan.Clan;
import com.betterclan.clan.Manager;
import com.betterclan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("deprecation")
public final class WarpList {

    private WarpList() {}

    public static final NamespacedKey WARP_MEMBER_KEY = new NamespacedKey("betterclan", "warp-member-uuid");

    private static final int[] MEMBER_SLOTS = {
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };
    public static final int MEMBERS_PER_PAGE = MEMBER_SLOTS.length;

    @SuppressWarnings("unused")
    public static void open(BetterClan plugin, Manager manager, Player player, int page) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) return;

        List<UUID> members = clan.getOrderedMembers();
        int totalPages = Math.max(1, (int) Math.ceil((double) members.size() / MEMBERS_PER_PAGE));
        page = Math.clamp(page, 1, totalPages);

        GuiHolder holder = new GuiHolder(MenuType.WARP_LIST);
        holder.set("page", page);

        Inventory inv = Bukkit.createInventory(holder, 54, "§b§lWarp-Liste");

        var _w  = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        var _p  = GuiHelper.filler(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        var _bl = GuiHelper.filler(Material.BLUE_STAINED_GLASS_PANE);

        inv.setItem(0, _w); inv.setItem(1, _p); inv.setItem(2, _w);
        inv.setItem(3, _w);  inv.setItem(5, _w);
        inv.setItem(6, _w); inv.setItem(7, _p); inv.setItem(8, _w);

        inv.setItem(9, _p); inv.setItem(13, _w); inv.setItem(17, _p);

        inv.setItem(18, _w);
        for (int s = 19; s <= 25; s++) inv.setItem(s, _bl);
        inv.setItem(26, _w);

        inv.setItem(27, _w); inv.setItem(35, _w);
        inv.setItem(36, _p); inv.setItem(44, _p);

        inv.setItem(45, _w); inv.setItem(46, _p);
        inv.setItem(48, _w);
        inv.setItem(50, _w); inv.setItem(52, _p); inv.setItem(53, _w);

        inv.setItem(4, new ItemBuilder(Material.COMPASS)
                .name("§b§lWarp-Liste")
                .build());

        Location base = clan.getBase();
        inv.setItem(11, new ItemBuilder(Material.IRON_NAUTILUS_ARMOR)
                .name("§b§lClan-Base 1")
                .lore("",
                        base != null
                                ? "§8| §7Welt: §f" + base.getWorld().getName()
                                : "§8| §cKein Ziel gesetzt.",
                        base != null
                                ? String.format("§8| §7Pos: §fX:%.0f Y:%.0f Z:%.0f",
                                        base.getX(), base.getY(), base.getZ())
                                : "",
                        "",
                        base != null ? "§eLinksklick: §7Teleportieren" : "§7Linksklick: §8Kein Ziel gesetzt",
                        "§eRechtsklick: §7Base hier setzen")
                .flags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                        org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                .build());

                Location base2 = clan.getBase2();
                boolean base2Unlocked = clan.getLevel() >= manager.getSettings().secondBaseMinLevel();
                inv.setItem(15, new ItemBuilder(Material.IRON_NAUTILUS_ARMOR)
                        .name(base2Unlocked ? "§b§lClan-Base 2" : "§8§lClan-Base 2")
                        .lore(
                                base2Unlocked
                                        ? (base2 != null
                                            ? "§8| §7Welt: §f" + base2.getWorld().getName()
                                            : "§8| §cKein Ziel gesetzt.")
                                        : "§8| §7Wird ab §eClan Lvl " + manager.getSettings().secondBaseMinLevel() + " §7freigeschaltet.",
                                base2Unlocked
                                        ? (base2 != null
                                            ? String.format("§8| §7Pos: §fX:%.0f Y:%.0f Z:%.0f",
                                                    base2.getX(), base2.getY(), base2.getZ())
                                            : "")
                                        : "",
                                base2Unlocked
                                        ? (base2 != null ? "§eLinksklick: §7Teleportieren" : "§7Linksklick: §8Kein Ziel gesetzt")
                                        : "§7Linksklick: §8Gesperrt",
                                base2Unlocked ? "§eRechtsklick: §7Base hier setzen" : ""
                        )
                        .flags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                                org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                        .build());

        int start = (page - 1) * MEMBERS_PER_PAGE;
        for (int i = 0; i < MEMBERS_PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= members.size()) break;

            UUID memberId = members.get(idx);
            boolean isSelf = memberId.equals(player.getUniqueId());
            Location warp = clan.getMemberWarp(memberId);
            String memberName = Bukkit.getOfflinePlayer(memberId).getName();
            if (memberName == null) memberName = memberId.toString().substring(0, 8);

            ItemBuilder ib = new ItemBuilder(Material.PLAYER_HEAD)
                    .skullOwner(Bukkit.getOfflinePlayer(memberId))
                    .name((isSelf ? "§e§l" : "§f§l") + memberName + (isSelf ? " §8(du)" : ""));

            if (isSelf) {
                if (warp != null) {
                    ib.lore("",
                            "§8| §7Welt: §f" + warp.getWorld().getName(),
                                                        String.format("§8| §7Pos: §fX:%.0f Y:%.0f Z:%.0f", warp.getX(), warp.getY(), warp.getZ()),
                            "",
                            "§eLinksklick: §7Teleportieren",
                            "§eRechtsklick: §7Warp überschreiben");
                } else {
                    ib.lore("",
                                                        "§8| §cKein Warp gesetzt.",
                            "§aKlicke zum Setzen");
                }
            } else {
                if (warp != null) {
                    ib.lore("",
                            "§8| §7Welt: §f" + warp.getWorld().getName(),
                            String.format("§8| §7Pos: §fX:%.0f Y:%.0f Z:%.0f", warp.getX(), warp.getY(), warp.getZ()),
                            "",
                            "§eKlicke zum Teleportieren");
                } else {
                    ib.lore("", "§8| §cKein Warp gesetzt.");
                }
            }

            ib.persistData(WARP_MEMBER_KEY, memberId.toString());
            inv.setItem(MEMBER_SLOTS[i], ib.build());
        }

        inv.setItem(47, page > 1 ? GuiHelper.prevPageButton(page - 1) : GuiHelper.prevPageButtonDisabled());
        inv.setItem(49, GuiHelper.backButton());
        inv.setItem(51, page < totalPages ? GuiHelper.nextPageButton(page + 1) : GuiHelper.nextPageButtonDisabled());
        player.openInventory(inv);
    }
}

