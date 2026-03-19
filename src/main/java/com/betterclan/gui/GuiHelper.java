package com.betterclan.gui;

import com.betterclan.clan.Clan;
import com.betterclan.util.ItemBuilder;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class GuiHelper {

    private GuiHelper() {
    }

    public static ItemStack filler(Material glassPaneColor) {
        return new ItemBuilder(glassPaneColor)
                .name(" ")
                .build();
    }

    public static void fillBorder(Inventory inv, Material glassPaneColor) {
        ItemStack pane = filler(glassPaneColor);
        ItemStack corner = filler(Material.GRAY_STAINED_GLASS_PANE);
        int size = inv.getSize();
        int cols = 9;
        int rows = size / cols;

        for (int i = 0; i < cols; i++) {
            boolean isCorner = (i == 0 || i == cols - 1);
            inv.setItem(i, isCorner ? corner.clone() : pane.clone());
            inv.setItem(size - cols + i, isCorner ? corner.clone() : pane.clone());
        }
        for (int row = 1; row < rows - 1; row++) {
            inv.setItem(row * cols, pane.clone());
            inv.setItem(row * cols + cols - 1, pane.clone());
        }
    }

    public static void fillRow(Inventory inv, int row, Material glassPaneColor) {
        ItemStack pane = filler(glassPaneColor);
        for (int i = 0; i < 9; i++) {
            inv.setItem(row * 9 + i, pane.clone());
        }
    }

    public static void fillAdminRedGrayBorder(Inventory inv) {
        ItemStack red = filler(Material.RED_STAINED_GLASS_PANE);
        ItemStack gray = filler(Material.GRAY_STAINED_GLASS_PANE);

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, (i == 0 || i == 8) ? red.clone() : gray.clone());
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, (i == 45 || i == 53) ? red.clone() : gray.clone());
        }

        inv.setItem(9, gray.clone());
        inv.setItem(17, gray.clone());
        inv.setItem(18, gray.clone());
        inv.setItem(26, gray.clone());
        inv.setItem(27, gray.clone());
        inv.setItem(35, gray.clone());
        inv.setItem(36, gray.clone());
        inv.setItem(44, gray.clone());
    }

    public static ItemStack backButton() {
        return new ItemBuilder(Material.ARROW)
                .name("§7« Zurück")
                .build();
    }

    public static ItemStack closeButton() {
        return new ItemBuilder(Material.BARRIER)
                .name("§c Schließen")
                .build();
    }

    public static ItemStack prevPageButton(int currentPage) {
        return new ItemBuilder(Material.LIGHT_GRAY_DYE)
                .name("§7« Seite " + currentPage)
                .build();
    }

    public static ItemStack nextPageButton(int nextPage) {
        return new ItemBuilder(Material.LIGHT_GRAY_DYE)
                .name("§7Seite " + nextPage + " »")
                .build();
    }

    public static ItemStack prevPageButtonDisabled() {
        return new ItemBuilder(Material.GRAY_DYE)
                .name("§8« Vorherige Seite")
                .build();
    }

    public static ItemStack nextPageButtonDisabled() {
        return new ItemBuilder(Material.GRAY_DYE)
                .name("§8Nächste Seite »")
                .build();
    }

    public static ItemStack pageInfo(int current, int max) {
        return new ItemBuilder(Material.PAPER)
                .name("§6Seite " + current + " / " + max)
                .build();
    }

    public static final int[] CONTENT_SLOTS_30 = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        27, 28, 29, 30, 31, 32, 33, 34, 35,
        37, 38, 39, 40, 41, 42, 43
    };

    public static final int[] CONTENT_SLOTS_28 = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    public static void fillPaginationRow(Inventory inv, int page, int totalPages) {
        inv.setItem(47, page > 1 ? prevPageButton(page - 1) : prevPageButtonDisabled());
        inv.setItem(49, backButton());
        inv.setItem(51, page < totalPages ? nextPageButton(page + 1) : nextPageButtonDisabled());
    }

    public static void applyWarBannerLore(ItemMeta meta, NamespacedKey key,
                                          String loserName, String scoreStr, String dateStr) {
        var mm = MiniMessage.miniMessage();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (loserName != null) {
            lore.add(mm.deserialize("<dark_gray>| <gray>Gegner: <white><loser>",
                    Placeholder.unparsed("loser", loserName)));
            lore.add(mm.deserialize("<dark_gray>| <gray>Ergebnis: <green><score>",
                    Placeholder.unparsed("score", scoreStr)));
        }
        lore.add(mm.deserialize("<dark_gray>| <gray>Datum: <yellow><date>",
                Placeholder.unparsed("date", dateStr)));
        lore.add(Component.empty());
        meta.lore(lore);
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING,
                (loserName != null ? loserName : "") + "\u0001" + scoreStr + "\u0001" + dateStr);
    }

    public static String progressBar(double ratio, int length) {
        int filled = (int) Math.round(ratio * length);
        StringBuilder sb = new StringBuilder();
        sb.append("§a");
        sb.repeat("▌", Math.max(0, filled));
        sb.append("§7");
        sb.repeat("▌", Math.max(0, length - filled));
        return sb.toString();
    }

    public static Material bannerMaterial(String colorName) {
        if (colorName == null) colorName = "WHITE";
        try {
            return Material.valueOf(colorName.toUpperCase() + "_BANNER");
        } catch (IllegalArgumentException e) {
            return Material.WHITE_BANNER;
        }
    }

    public static final String[] BANNER_COLORS = {
            "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE",
            "YELLOW", "LIME", "PINK", "GRAY",
            "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE",
            "BROWN", "GREEN", "RED", "BLACK"
    };

    public static String bannerColorName(String color) {
        return switch (color.toUpperCase()) {
            case "WHITE" -> "Weiß";
            case "ORANGE" -> "Orange";
            case "MAGENTA" -> "Magenta";
            case "LIGHT_BLUE" -> "Hellblau";
            case "YELLOW" -> "Gelb";
            case "LIME" -> "Limette";
            case "PINK" -> "Rosa";
            case "GRAY" -> "Grau";
            case "LIGHT_GRAY" -> "Hellgrau";
            case "CYAN" -> "Cyan";
            case "PURPLE" -> "Lila";
            case "BLUE" -> "Blau";
            case "BROWN" -> "Braun";
            case "GREEN" -> "Grün";
            case "RED" -> "Rot";
            case "BLACK" -> "Schwarz";
            default -> color;
        };
    }

    @SuppressWarnings("deprecation")
    public static ItemStack createClanBanner(Clan clan) {
        Material mat = bannerMaterial(clan.getBannerColor());
        ItemStack banner = new ItemStack(mat);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        if (meta != null) {
            if (!clan.getBannerPatterns().isEmpty()) {
                for (Pattern p : clan.getBannerPatterns()) meta.addPattern(p);
            }
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            banner.setItemMeta(meta);
        }
        return banner;
    }

    public static String patternTypeKeyValue(PatternType type) {
        NamespacedKey nk = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.BANNER_PATTERN)
                .getKey(type);
        return nk != null ? nk.getKey() : "base";
    }

    public static DyeColor dyeColorFromName(String name) {
        if (name == null) return DyeColor.WHITE;
        try {
            return DyeColor.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DyeColor.WHITE;
        }
    }

    public static String patternTypeName(PatternType type) {
        String key = patternTypeKeyValue(type);
        return switch (key) {
            case "stripe_bottom" -> "Streifen Unten";
            case "stripe_top" -> "Streifen Oben";
            case "stripe_left" -> "Streifen Links";
            case "stripe_right" -> "Streifen Rechts";
            case "stripe_center" -> "Streifen Mitte (V)";
            case "stripe_middle" -> "Streifen Mitte (H)";
            case "stripe_downright" -> "Diagonaler Streifen R";
            case "stripe_downleft" -> "Diagonaler Streifen L";
            case "small_stripes" -> "Kleine Streifen";
            case "cross" -> "Diagonales Kreuz";
            case "straight_cross" -> "Kreuz";
            case "triangle_bottom" -> "Dreieck Unten";
            case "triangle_top" -> "Dreieck Oben";
            case "triangles_bottom" -> "Zacken Unten";
            case "triangles_top" -> "Zacken Oben";
            case "diagonal_left" -> "Diagonale Links";
            case "diagonal_right" -> "Diagonale Rechts";
            case "diagonal_up_left" -> "Diagonale Oben L";
            case "diagonal_up_right" -> "Diagonale Oben R";
            case "half_vertical" -> "Halbe Vertikale L";
            case "half_vertical_right" -> "Halbe Vertikale R";
            case "half_horizontal" -> "Halbe Horizontale O";
            case "half_horizontal_bottom" -> "Halbe Horizontale U";
            case "circle" -> "Kreis";
            case "rhombus" -> "Raute";
            case "border" -> "Rahmen";
            case "curly_border" -> "Geschwungener Rahmen";
            case "bricks" -> "Ziegel";
            case "gradient" -> "Verlauf";
            case "gradient_up" -> "Verlauf Oben";
            case "creeper" -> "Creeper";
            case "skull" -> "Schädel";
            case "flower" -> "Blume";
            case "mojang" -> "Mojang";
            case "globe" -> "Globus";
            case "piglin" -> "Piglin";
            case "flow" -> "Fluss";
            case "guster" -> "Guster";
            case "square_bottom_right" -> "Ecke Unten-Rechts";
            case "square_bottom_left"  -> "Ecke Unten-Links";
            case "square_top_right"    -> "Ecke Oben-Rechts";
            case "square_top_left"     -> "Ecke Oben-Links";
            default -> key.replace('_', ' ');
        };
    }

    public static String colorNameToKey(String displayName) {
        for (String key : BANNER_COLORS) {
            if (bannerColorName(key).equals(displayName)) {
                return key;
            }
        }
        return "WHITE";
    }

    public static String toLegacy(String format, String text) {
        if (format.startsWith("<")) {
            return LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(format + text));
        }
        return format + text;
    }

    public static Component tagColored(String tagFormat, String text) {
        if (tagFormat != null && tagFormat.startsWith("<")) {
            return MiniMessage.miniMessage().deserialize(tagFormat + text);
        }
        return LegacyComponentSerializer.legacySection()
                .deserialize((tagFormat != null ? tagFormat : "§7") + text);
    }
}

