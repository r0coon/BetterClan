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

public final class Dynamic {

    private Dynamic() {}

    static final Object[][] PRESETS = {

        {"<rainbow>",                        "Regenbogen",       Material.MAGENTA_STAINED_GLASS_PANE},
        {"<gradient:#FF4500:#FFD700>",        "Feuer",            Material.ORANGE_STAINED_GLASS_PANE},
        {"<gradient:#0099FF:#00FFD4>",        "Ozean",            Material.CYAN_STAINED_GLASS_PANE},
        {"<gradient:#44FF44:#00FF99>",        "Natur",            Material.LIME_STAINED_GLASS_PANE},
        {"<gradient:#AA00FF:#FF44FF>",        "Neon",             Material.PURPLE_STAINED_GLASS_PANE},
        {"<gradient:#FF6666:#FFDD00>",        "Sonnenuntergang",  Material.YELLOW_STAINED_GLASS_PANE},
        {"<gradient:#FFD700:#FFFFFF>",        "Gold-Weiß",        Material.WHITE_STAINED_GLASS_PANE},

        {"<gradient:#FF0080:#8B0000>",        "Lava",             Material.RED_STAINED_GLASS_PANE},
        {"<gradient:#00BFFF:#E0FFFF>",        "Eisblau",          Material.LIGHT_BLUE_STAINED_GLASS_PANE},
        {"<gradient:#FF00FF:#00FFFF>",        "Cyber",            Material.MAGENTA_STAINED_GLASS_PANE},
        {"<gradient:#8B0000:#DC143C>",        "Blut",             Material.RED_STAINED_GLASS_PANE},
        {"<gradient:#C0C0C0:#FFFFFF>",        "Silber",           Material.WHITE_STAINED_GLASS_PANE},
        {"<gradient:#FF69B4:#FFB6C1>",        "Kirschblüte",      Material.PINK_STAINED_GLASS_PANE},
        {"<gradient:#7FFF00:#ADFF2F>",        "Giftgrün",         Material.LIME_STAINED_GLASS_PANE},

        {"<gradient:#2F2F2F:#808080>",        "Schatten",         Material.GRAY_STAINED_GLASS_PANE},
        {"<gradient:#4169E1:#87CEEB>",        "Himmel",           Material.LIGHT_BLUE_STAINED_GLASS_PANE},
        {"<gradient:#006400:#32CD32>",        "Waldgrün",         Material.GREEN_STAINED_GLASS_PANE},
        {"<gradient:#DAA520:#FFA500>",        "Bernstein",        Material.ORANGE_STAINED_GLASS_PANE},
        {"<gradient:#4B0082:#9400D3>",        "Violett",          Material.PURPLE_STAINED_GLASS_PANE},
        {"<gradient:#FF4500:#FF0000>",        "Korallrot",        Material.RED_STAINED_GLASS_PANE},
        {"<gradient:#20B2AA:#40E0D0>",        "Türkis",           Material.CYAN_STAINED_GLASS_PANE},
        {"<gradient:#DDA0DD:#EE82EE>",        "Pflaume",          Material.MAGENTA_STAINED_GLASS_PANE},

        {"<gradient:#F0E68C:#FAFAD2>",        "Vanille",          Material.YELLOW_STAINED_GLASS_PANE},
        {"<gradient:#FF1493:#FF69B4>",        "Rose",             Material.PINK_STAINED_GLASS_PANE},
        {"<gradient:#483D8B:#7B68EE>",        "Schiefer",         Material.BLUE_STAINED_GLASS_PANE},
        {"<gradient:#00FA9A:#2ECC71>",        "Minze",            Material.GREEN_STAINED_GLASS_PANE},
        {"<gradient:#1C1C1C:#4A4A4A>",        "Nacht",            Material.BLACK_STAINED_GLASS_PANE},
        {"<gradient:#FFD700:#FF8C00:#FF4500>","Sonnenfeuer",      Material.ORANGE_STAINED_GLASS_PANE},
    };

    private static final int[] SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    static final Object[][] PRESETS_PAGE2 = {

        {"<gradient:#FF0000:#FF00CC:#BC00FF>",               "Drachenaura",     Material.MAGENTA_STAINED_GLASS_PANE},
        {"<gradient:#0044FF:#00CFFF>",                        "Saphirsturm",     Material.BLUE_STAINED_GLASS_PANE},
        {"<gradient:#FF4500:#FFD700:#FF4500>",                "Vulkanglut",      Material.ORANGE_STAINED_GLASS_PANE},
        {"<gradient:#FAD961:#F76B1C>",                        "Sonnenblume",     Material.YELLOW_STAINED_GLASS_PANE},
        {"<gradient:#FF1C1C:#FFC0C0>",                        "Glutkern",        Material.RED_STAINED_GLASS_PANE},
        {"<gradient:#00F5FF:#FFFFFF:#00F5FF>",                "Eiscrash",        Material.CYAN_STAINED_GLASS_PANE},
        {"<gradient:#CC00FF:#330066>",                        "Dunkle Magie",    Material.PURPLE_STAINED_GLASS_PANE},

        {"<gradient:#330000:#FF0000>",                        "Hellblut",        Material.RED_STAINED_GLASS_PANE},
        {"<gradient:#000033:#0033CC>",                        "Tiefsee",         Material.BLUE_STAINED_GLASS_PANE},
        {"<gradient:#FFF9C4:#F9A825>",                        "Kerzenlicht",     Material.ORANGE_STAINED_GLASS_PANE},
        {"<gradient:#2D1B69:#8B5CF6>",                        "Abyss",           Material.PURPLE_STAINED_GLASS_PANE},
        {"<gradient:#00416A:#E4E5E6>",                        "Polarnacht",      Material.LIGHT_BLUE_STAINED_GLASS_PANE},
        {"<gradient:#7F6000:#FFFF77>",                        "Honiggold",       Material.YELLOW_STAINED_GLASS_PANE},
        {"<gradient:#FF0066:#FFAACC>",                        "Erdbeersorbet",   Material.PINK_STAINED_GLASS_PANE},

        {"<gradient:#FF0000:#FF7700:#FFFF00:#00FF00:#8B00FF>","Prisma",          Material.MAGENTA_STAINED_GLASS_PANE},
        {"<gradient:#00FFA3:#7B00FF>",                        "Aurora",          Material.CYAN_STAINED_GLASS_PANE},
        {"<gradient:#F64F59:#C0392B:#FF6B6B>",                "Rost",            Material.RED_STAINED_GLASS_PANE},
        {"<gradient:#B2FEFA:#0ED2F7>",                        "Kristallblau",    Material.LIGHT_BLUE_STAINED_GLASS_PANE},
        {"<gradient:#F7971E:#FFD200>",                        "Gold Rush",       Material.YELLOW_STAINED_GLASS_PANE},
        {"<gradient:#4776E6:#8E54E9>",                        "Galaxis",         Material.BLUE_STAINED_GLASS_PANE},
        {"<gradient:#11998E:#38EF7D>",                        "Jade",            Material.GREEN_STAINED_GLASS_PANE},

        {"<gradient:#FFECD2:#FCB69F>",                        "Pfirsich",        Material.ORANGE_STAINED_GLASS_PANE},
        {"<gradient:#A1C4FD:#C2E9FB>",                        "Morgentau",       Material.LIGHT_BLUE_STAINED_GLASS_PANE},
        {"<gradient:#FDDB92:#D1FDFF>",                        "Sonnenwolke",     Material.YELLOW_STAINED_GLASS_PANE},
        {"<gradient:#FF9A9E:#FAD0C4>",                        "Rosenduft",       Material.PINK_STAINED_GLASS_PANE},
        {"<gradient:#A18CD1:#FBC2EB>",                        "Lavendel",        Material.PURPLE_STAINED_GLASS_PANE},
        {"<gradient:#0F2027:#2C5364>",                        "Stahl",           Material.CYAN_STAINED_GLASS_PANE},
        {"<gradient:#FFD700:#FF69B4:#9400D3:#1E90FF>",        "Neonparade",      Material.MAGENTA_STAINED_GLASS_PANE},
    };

    static final Object[][] PRESETS_PAGE3 = {

        {"<gradient:#0D0D0D:#6A0DAD:#FF00FF>",               "Kosmos",          Material.PURPLE_STAINED_GLASS_PANE},
        {"<gradient:#1A1A2E:#16213E:#0F3460:#533483>",       "Nebula",          Material.BLUE_STAINED_GLASS_PANE},
        {"<gradient:#FF6B35:#FFE66D>",                        "Sonnenaufgang",   Material.YELLOW_STAINED_GLASS_PANE},
        {"<gradient:#2C3E50:#FD746C>",                        "Dämmerung",       Material.ORANGE_STAINED_GLASS_PANE},
        {"<gradient:#00C9FF:#92FE9D>",                        "Frühlingswind",   Material.CYAN_STAINED_GLASS_PANE},
        {"<gradient:#FC466B:#3F5EFB>",                        "Magnetfeld",      Material.MAGENTA_STAINED_GLASS_PANE},
        {"<gradient:#FFEFBA:#FFFFFF>",                        "Mondlicht",       Material.WHITE_STAINED_GLASS_PANE},

        {"<gradient:#FF0099:#00FFCC>",                        "Cyberpunk",       Material.CYAN_STAINED_GLASS_PANE},
        {"<gradient:#F953C6:#B91D73>",                        "Hot Pink",        Material.PINK_STAINED_GLASS_PANE},
        {"<gradient:#43C6AC:#191654>",                        "Mitternacht",     Material.BLUE_STAINED_GLASS_PANE},
        {"<gradient:#56CCF2:#2F80ED>",                        "Himmelsklar",     Material.LIGHT_BLUE_STAINED_GLASS_PANE},
        {"<gradient:#F2994A:#F2C94C>",                        "Karamell",        Material.ORANGE_STAINED_GLASS_PANE},
        {"<gradient:#6FCF97:#27AE60>",                        "Smaragd",         Material.GREEN_STAINED_GLASS_PANE},
        {"<gradient:#EB5757:#FF8C69>",                        "Lachs",           Material.RED_STAINED_GLASS_PANE},

        {"<gradient:#5D4037:#8D6E63:#BCAAA4>",               "Sandstein",       Material.BROWN_STAINED_GLASS_PANE},
        {"<gradient:#1B5E20:#66BB6A:#A5D6A7>",               "Tiefwald",        Material.GREEN_STAINED_GLASS_PANE},
        {"<gradient:#880E4F:#E91E63:#F48FB1>",               "Kirsche",         Material.PINK_STAINED_GLASS_PANE},
        {"<gradient:#3E2723:#6D4C41:#A1887F>",               "Mahagoni",        Material.BROWN_STAINED_GLASS_PANE},
        {"<gradient:#0288D1:#29B6F6:#B3E5FC>",               "Arkteis",         Material.CYAN_STAINED_GLASS_PANE},
        {"<gradient:#F57F17:#FFCA28:#FFF9C4>",               "Sonnenhonig",     Material.YELLOW_STAINED_GLASS_PANE},
        {"<gradient:#4A148C:#7B1FA2:#CE93D8>",               "Amethyst",        Material.PURPLE_STAINED_GLASS_PANE},

        {"<rainbow:5>",                                       "Langsam-Rainbow", Material.MAGENTA_STAINED_GLASS_PANE},
        {"<gradient:#FFFDE7:#FFF176:#FFEE58:#FDD835:#F9A825>","Honigwaben",      Material.YELLOW_STAINED_GLASS_PANE},
        {"<gradient:#E8F5E9:#C8E6C9:#A5D6A7:#81C784:#4CAF50>","Minzfrost",      Material.GREEN_STAINED_GLASS_PANE},
        {"<gradient:#FF6F00:#FFA000:#FFCA28:#FFE082>",        "Bernsteinglut",   Material.ORANGE_STAINED_GLASS_PANE},
        {"<gradient:#B71C1C:#E53935:#EF9A9A:#FFFFFF>",        "Blutmond",        Material.RED_STAINED_GLASS_PANE},
        {"<gradient:#263238:#455A64:#90A4AE:#ECEFF1>",        "Polarfrost",      Material.LIGHT_BLUE_STAINED_GLASS_PANE},
        {"<gradient:#FF1744:#FF9100:#FFEA00:#00E676:#2979FF:#D500F9>","Spektrum",Material.MAGENTA_STAINED_GLASS_PANE},
    };

    static final Object[][] PRESETS_PAGE4 = {

        {"<gradient:#E100FF:#7F00FF>",                        "Elektrisch",      Material.PURPLE_STAINED_GLASS_PANE},
        {"<gradient:#FF416C:#FF4B2B>",                        "Glut",            Material.RED_STAINED_GLASS_PANE},
        {"<gradient:#1FA2FF:#12D8FA:#A6FFCB>",               "Aquawave",        Material.CYAN_STAINED_GLASS_PANE},
        {"<gradient:#FDC830:#F37335>",                        "Paprika",         Material.ORANGE_STAINED_GLASS_PANE},
        {"<gradient:#74EBD5:#ACB6E5>",                        "Pastelltraum",    Material.LIGHT_BLUE_STAINED_GLASS_PANE},
        {"<gradient:#E44D26:#F16529>",                        "Fuchs",           Material.ORANGE_STAINED_GLASS_PANE},
        {"<gradient:#B5451B:#E27802>",                        "Kupfer",          Material.BROWN_STAINED_GLASS_PANE},

        {"<gradient:#373B44:#4286F4>",                        "Regen",           Material.BLUE_STAINED_GLASS_PANE},
        {"<gradient:#D3CCE3:#E9E4F0>",                        "Schneeweiß",      Material.WHITE_STAINED_GLASS_PANE},
        {"<gradient:#5C258D:#4389A2>",                        "Ionenstrahl",     Material.PURPLE_STAINED_GLASS_PANE},
        {"<gradient:#AA3A38:#721B65>",                        "Weinrot",         Material.RED_STAINED_GLASS_PANE},
        {"<gradient:#00B09B:#96C93D>",                        "Tropenwald",      Material.GREEN_STAINED_GLASS_PANE},
        {"<gradient:#654EA3:#EAAFC8>",                        "Marshmallow",     Material.PINK_STAINED_GLASS_PANE},
        {"<gradient:#108DC7:#EF8E38>",                        "Blut-Orange",     Material.ORANGE_STAINED_GLASS_PANE},

        {"<gradient:#FECC64:#D0470D>",                        "Nacho",           Material.YELLOW_STAINED_GLASS_PANE},
        {"<gradient:#6A3093:#A044FF>",                        "Dunkelviolett",   Material.PURPLE_STAINED_GLASS_PANE},
        {"<gradient:#1DE9B6:#1565C0>",                        "Biolumineszenz",  Material.CYAN_STAINED_GLASS_PANE},
        {"<gradient:#20002C:#CBB4D4>",                        "Dunkle Orchidee", Material.PURPLE_STAINED_GLASS_PANE},
        {"<gradient:#3494E6:#EC6EAD>",                        "Candy",           Material.PINK_STAINED_GLASS_PANE},
        {"<gradient:#C94B4B:#4B134F>",                        "Schwarzkirsche",  Material.RED_STAINED_GLASS_PANE},
        {"<gradient:#D4FC79:#96E6A1>",                        "Frühlingsgrün", Material.LIME_STAINED_GLASS_PANE},

        {"<gradient:#04619F:#064360>",                        "Tintenfisch",     Material.BLUE_STAINED_GLASS_PANE},
        {"<gradient:#F0C27F:#FC4A1A>",                        "Feuerhonig",      Material.ORANGE_STAINED_GLASS_PANE},
        {"<gradient:#B3FFAB:#12FFF7>",                        "Gletscher",       Material.CYAN_STAINED_GLASS_PANE},
        {"<gradient:#4DA0B0:#D39D38>",                        "Goldstrom",       Material.YELLOW_STAINED_GLASS_PANE},
        {"<gradient:#7312C7:#C97B12>",                        "Nachtgold",       Material.PURPLE_STAINED_GLASS_PANE},
        {"<gradient:#FF6E7F:#BFE9FF>",                        "Zuckerwatte",     Material.PINK_STAINED_GLASS_PANE},
        {"<rainbow:10>",                                       "Mega-Rainbow",    Material.MAGENTA_STAINED_GLASS_PANE},
    };

    private static final int MAX_PAGES = 4;

    @SuppressWarnings("unused")
    public static void open(Manager manager, Player player) {
        open(manager, player, null, 1);
    }

    public static void open(Manager manager, Player player, String adminClanName) {
        open(manager, player, adminClanName, 1);
    }

    @SuppressWarnings("deprecation")
    public static void open(Manager manager, Player player, String adminClanName, int page) {
        Clan clan = adminClanName != null ? manager.getClanByName(adminClanName) : manager.getClan(player.getUniqueId());
        if (clan == null) return;
        page = Math.clamp(page, 1, MAX_PAGES);

        GuiHolder holder = new GuiHolder(MenuType.TAG_COLOR_DYNAMIC);
        if (adminClanName != null) holder.set("adminClanName", adminClanName);
        holder.set("page", page);
        Inventory inv = Bukkit.createInventory(holder, 54, "§d§lDyn. Farben §8— §eSeite " + page + "§8/" + MAX_PAGES);

        Object[][] presets = switch (page) {
            case 2 -> PRESETS_PAGE2;
            case 3 -> PRESETS_PAGE3;
            case 4 -> PRESETS_PAGE4;
            default -> PRESETS;
        };
        String current = clan.getTagColor();
        boolean locked = adminClanName == null && clan.getLevel() < manager.getSettings().dynamicColorsMinLevel();

        for (int i = 0; i < presets.length && i < SLOTS.length; i++) {
            String format = (String) presets[i][0];
            String label  = (String) presets[i][1];
            Material mat  = (Material) presets[i][2];
            boolean selected = format.equals(current);

            ItemBuilder ib;
            if (locked) {
                int minLvl = manager.getSettings().dynamicColorsMinLevel();
                ib = new ItemBuilder(mat)
                        .name("§7" + label + " §8[Gesperrt]")
                        .lore("",
                                "§cDynamische Farben sind erst ab Clan Lvl. " + minLvl + " verfügbar.",
                                "§cDu musst mind. " + manager.getSettings().xpForLevel(minLvl) + "x Kriege gewonnen haben.",
                                "");
            } else {
                String preview = GuiHelper.toLegacy(format, "[" + clan.getName() + "]");
                ib = new ItemBuilder(mat)
                        .name(preview + (selected ? " §a§l" : ""))
                        .lore("",
                                "§8| §7" + label,
                                "",
                                selected ? "§a§lAusgewählt" : "§eKlicke zum Auswählen",
                                "")
                        .persistData(Picker.TAG_FORMAT_KEY, format);
                if (selected) ib.glow();
            }
            inv.setItem(SLOTS[i], ib.build());
        }

        if (page > 1) {
            inv.setItem(45, new ItemBuilder(Material.LIGHT_GRAY_DYE)
                    .name("§7« Seite " + (page - 1))
                    .build());
        } else {
            inv.setItem(45, new ItemBuilder(Material.GRAY_DYE)
                    .name("§8« Vorherige Seite")
                    .build());
        }
        inv.setItem(49, GuiHelper.backButton());
        if (page < MAX_PAGES) {
            inv.setItem(53, new ItemBuilder(Material.LIGHT_GRAY_DYE)
                    .name("§7Seite " + (page + 1) + " »")
                    .build());
        } else {
            inv.setItem(53, new ItemBuilder(Material.GRAY_DYE)
                    .name("§8Nächste Seite »")
                    .build());
        }
        player.openInventory(inv);
    }

}

