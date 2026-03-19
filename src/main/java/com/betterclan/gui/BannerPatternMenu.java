package com.betterclan.gui;

import com.betterclan.clan.Clan;
import com.betterclan.clan.ClanManager;
import com.betterclan.util.ItemBuilder;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public final class BannerPatternMenu {

    private BannerPatternMenu() {}

    @SuppressWarnings("unused")
    public static void openPatternList(ClanManager manager, Player player) {
        openPatternList(manager, player, null);
    }

    public static void openPatternList(ClanManager manager, Player player, String adminClanName) {
        Clan clan = adminClanName != null ? manager.getClanByName(adminClanName) : manager.getClan(player.getUniqueId());
        if (clan == null) return;

        GuiHolder holder = new GuiHolder(MenuType.BANNER_PATTERN_LIST);
        if (adminClanName != null) holder.set("adminClanName", adminClanName);
        Inventory inv = Bukkit.createInventory(holder, 45, "§6§lBanner-Muster");

        GuiHelper.fillBorder(inv, Material.WHITE_STAINED_GLASS_PANE);
        inv.setItem(40, GuiHelper.backButton());

        inv.setItem(4, new ItemBuilder(GuiHelper.createClanBanner(clan))
                .name("§6§lBanner-Vorschau")
                .lore("", "§7Farbe: §f" + GuiHelper.bannerColorName(clan.getBannerColor()),
                "§8| §7Muster: §f" + clan.getBannerPatterns().size() + "/" + manager.getSettings().bannerMaxPatterns(), "")
                .build());

        List<Pattern> patterns = clan.getBannerPatterns();
        for (int i = 0; i < patterns.size() && i < 7; i++) {
            Pattern p = patterns.get(i);
            inv.setItem(10 + i, new ItemBuilder(GuiHelper.bannerMaterial(clan.getBannerColor()))
                    .bannerPatterns(List.of(p))
                    .name("§e" + GuiHelper.patternTypeName(p.getPattern())
                            + " §7(" + GuiHelper.bannerColorName(p.getColor().name()) + ")")
                    .lore("", "§cKlicke zum Entfernen")
                    .build());
        }

        if (patterns.size() < manager.getSettings().bannerMaxPatterns()) {
            inv.setItem(29, new ItemBuilder(Material.LIME_DYE)
                    .name("§a§l+ Muster hinzufügen")
                    .lore("", "§7Füge ein neues Muster", "§7zum Banner hinzu.", "", "§eKlicke zum Hinzufügen")
                    .build());
        }

        if (!patterns.isEmpty()) {
            inv.setItem(33, new ItemBuilder(Material.RED_DYE)
                    .name("§c§l Alle Muster entfernen")
                    .lore("", "§7Entfernt alle Muster", "§7vom Banner.", "", "§cKlicke zum Entfernen")
                    .build());
        }

        player.openInventory(inv);
    }

    @SuppressWarnings("unused")
    public static void openPatternTypePicker(ClanManager manager, Player player, int page) {
        openPatternTypePicker(manager, player, page, null);
    }

    private static final int PATTERNS_PER_PAGE = GuiHelper.CONTENT_SLOTS_28.length;
    private static final int[] PATTERN_SLOTS = GuiHelper.CONTENT_SLOTS_28;

    public static void openPatternTypePicker(ClanManager manager, Player player, int page, String adminClanName) {
        Clan clan = adminClanName != null ? manager.getClanByName(adminClanName) : manager.getClan(player.getUniqueId());
        if (clan == null) return;

        List<PatternType> types = getAvailablePatternTypes();
        int totalPages = Math.max(1, (int) Math.ceil((double) types.size() / PATTERNS_PER_PAGE));
        int p = Math.clamp(page, 1, totalPages);

        GuiHolder holder = new GuiHolder(MenuType.BANNER_PATTERN_TYPE);
        if (adminClanName != null) holder.set("adminClanName", adminClanName);
        holder.set("page", p);
        Inventory inv = Bukkit.createInventory(holder, 54, "§7Muster-Typ wählen §8(§f" + p + "§8/§f" + totalPages + "§8)");

        var _w = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        inv.setItem(0, _w); inv.setItem(1, _w);
        inv.setItem(7, _w); inv.setItem(8, _w);
        inv.setItem(9, _w); inv.setItem(17, _w);
        inv.setItem(36, _w); inv.setItem(44, _w);
        inv.setItem(45, _w); inv.setItem(46, _w);
        inv.setItem(52, _w); inv.setItem(53, _w);

        inv.setItem(4, new ItemBuilder(Material.WHITE_BANNER)
                .name("§6§lMuster-Typ wählen")
                .lore("", "§7Wähle einen Muster-Typ.", "§7Danach wählst du die Farbe.", "")
                .build());

        Material baseMat = GuiHelper.bannerMaterial(clan.getBannerColor());
        DyeColor previewColor = contrastColor(GuiHelper.dyeColorFromName(clan.getBannerColor()));
        int start = (p - 1) * PATTERNS_PER_PAGE;
        for (int i = 0; i < PATTERN_SLOTS.length; i++) {
            int idx = start + i;
            if (idx >= types.size()) break;
            PatternType pt = types.get(idx);
            inv.setItem(PATTERN_SLOTS[i], new ItemBuilder(baseMat)
                    .bannerPatterns(List.of(new Pattern(previewColor, pt)))
                    .name("§e" + GuiHelper.patternTypeName(pt))
                    .lore("", "§eKlicke zum Auswählen")
                    .build());
        }

        inv.setItem(47, p > 1 ? GuiHelper.prevPageButton(p - 1) : GuiHelper.prevPageButtonDisabled());
        inv.setItem(51, p < totalPages ? GuiHelper.nextPageButton(p + 1) : GuiHelper.nextPageButtonDisabled());
        inv.setItem(49, GuiHelper.backButton());
        player.openInventory(inv);
    }

    @SuppressWarnings("unused")
    public static void openPatternColorPicker(ClanManager manager, Player player, String patternKey) {
        openPatternColorPicker(manager, player, patternKey, null);
    }

    public static void openPatternColorPicker(ClanManager manager, Player player, String patternKey, String adminClanName) {
        Clan clan = adminClanName != null ? manager.getClanByName(adminClanName) : manager.getClan(player.getUniqueId());
        if (clan == null) return;

        String normalizedKey = patternKey == null ? null : patternKey.toLowerCase(java.util.Locale.ROOT);
        if (normalizedKey == null || normalizedKey.isBlank()) return;

        GuiHolder holder = new GuiHolder(MenuType.BANNER_PATTERN_COLOR);
        if (adminClanName != null) holder.set("adminClanName", adminClanName);
        holder.set("patternKey", normalizedKey);
        Inventory inv = Bukkit.createInventory(holder, 54, "§6§lMuster-Farbe wählen");

        var _w = GuiHelper.filler(Material.WHITE_STAINED_GLASS_PANE);
        inv.setItem(0, _w); inv.setItem(1, _w); inv.setItem(2, _w);
        inv.setItem(6, _w); inv.setItem(7, _w); inv.setItem(8, _w);
        inv.setItem(9, _w); inv.setItem(17, _w);
        inv.setItem(36, _w); inv.setItem(44, _w);
        inv.setItem(45, _w); inv.setItem(46, _w);
        inv.setItem(52, _w); inv.setItem(53, _w);
        inv.setItem(49, new ItemBuilder(Material.ARROW).name("§7« Zurück").build());

        PatternType pt = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.BANNER_PATTERN)
                .get(org.bukkit.NamespacedKey.minecraft(normalizedKey));
        String typeName = pt != null ? GuiHelper.patternTypeName(pt) : normalizedKey;

        inv.setItem(4, new ItemBuilder(GuiHelper.bannerMaterial(clan.getBannerColor()))
            .name("Farbe für: " + typeName)
                .lore("", "§7Wähle die Farbe für", "§7dieses Muster.", "")
                .build());

        String[] colors = GuiHelper.BANNER_COLORS;
        int[] slots = {10, 11, 15, 16, 19, 20, 24, 25, 28, 29, 33, 34, 37, 38, 42, 43};
        Material baseMat = GuiHelper.bannerMaterial(clan.getBannerColor());

        for (int i = 0; i < colors.length && i < slots.length; i++) {
            DyeColor dc = GuiHelper.dyeColorFromName(colors[i]);
            ItemBuilder builder = new ItemBuilder(baseMat);
            if (pt != null) builder.bannerPatterns(List.of(new Pattern(dc, pt)));
            builder.name(GuiHelper.bannerColorName(colors[i])).lore("", "§eKlicke zum Auswählen");
            inv.setItem(slots[i], builder.build());
        }

        player.openInventory(inv);
    }

    private static List<PatternType> getAvailablePatternTypes() {
        List<PatternType> types = new ArrayList<>();
        RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN).forEach(pt -> {
            if (!"base".equals(GuiHelper.patternTypeKeyValue(pt))) types.add(pt);
        });
        return types;
    }

    private static DyeColor contrastColor(DyeColor base) {
        return base == DyeColor.WHITE ? DyeColor.BLACK : DyeColor.WHITE;
    }
}

