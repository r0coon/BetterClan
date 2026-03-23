package com.betterclan.clan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.betterclan.storage.Provider;

public final class Storage implements Provider {

    private final Plugin plugin;
    private final File   clanDir;
    private final File   legacyFile;

    public Storage(Plugin plugin) {
        this.plugin     = plugin;
        this.clanDir    = new File(plugin.getDataFolder(), "clan");
        this.legacyFile = new File(plugin.getDataFolder(), "clans.json");
    }

    // ── save ─────────────────────────────────────────────────────────────────

    @Override
    public void save(Manager manager) {
        if (!clanDir.exists() && !clanDir.mkdirs()) {
            plugin.getLogger().warning("Could not create clan directory: " + clanDir);
            return;
        }

        Set<String> writtenFiles = new HashSet<>();
        for (Clan clan : manager.getAllClans()) {
            File clanFile = new File(clanDir, sanitizeName(clan.getName()) + ".yml");
            writtenFiles.add(clanFile.getName());
            saveClan(clan, clanFile, manager);
        }

        // Remove orphaned clan files
        File[] existing = clanDir.listFiles((d, n) -> n.endsWith(".yml") && !n.equals("global.yml"));
        if (existing != null) {
            for (File f : existing) {
                if (!writtenFiles.contains(f.getName())) f.delete();
            }
        }

        saveGlobal(manager, new File(clanDir, "global.yml"));
    }

    private void saveClan(Clan clan, File file, Manager manager) {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("name",              clan.getName());
        cfg.set("owner",             clan.getOwnerId().toString());
        cfg.set("level",             clan.getLevel());
        cfg.set("xp",                clan.getXp());
        cfg.set("maxMembersOverride", clan.getMaxMembersOverride());
        cfg.set("bannerColor",       clan.getBannerColor());
        cfg.set("tagColor",          clan.getTagColor());
        cfg.set("friendlyFire",      clan.isFriendlyFire());
        cfg.set("createdAt",         clan.getCreatedAt());

        if (clan.getBase()  != null) setLoc(cfg, "base",  clan.getBase());
        if (clan.getBase2() != null) setLoc(cfg, "base2", clan.getBase2());

        ConfigurationSection membersSection = cfg.createSection("members");
        for (var e : clan.getMembers().entrySet())
            membersSection.set(e.getKey().toString(), e.getValue());

        cfg.set("allies",       new ArrayList<>(clan.getAllies()));
        cfg.set("allyRequests", new ArrayList<>(clan.getAllyRequests()));

        List<Map<String, Object>> rankList = new ArrayList<>();
        for (Rank rank : clan.getAllRanks().values()) {
            Map<String, Object> rd = new LinkedHashMap<>();
            rd.put("id",          rank.getId());
            rd.put("name",        rank.getName());
            rd.put("color",       rank.getColor());
            rd.put("deletable",   rank.isDeletable());
            List<String> perms = new ArrayList<>();
            for (Permission p : rank.getPermissions()) perms.add(p.name());
            rd.put("permissions", perms);
            rankList.add(rd);
        }
        cfg.set("ranks", rankList);

        List<Map<String, String>> patternList = new ArrayList<>();
        for (Pattern p : clan.getBannerPatterns()) {
            NamespacedKey nk = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.BANNER_PATTERN).getKey(p.getPattern());
            Map<String, String> pm = new LinkedHashMap<>();
            pm.put("type",  nk != null ? nk.getKey() : "base");
            pm.put("color", p.getColor().name());
            patternList.add(pm);
        }
        cfg.set("bannerPatterns", patternList);

        ConfigurationSection warpSection = cfg.createSection("memberWarps");
        for (var e : clan.getMemberWarps().entrySet())
            setLoc(warpSection, e.getKey().toString(), e.getValue());

        ConfigurationSection lastSeenSection = cfg.createSection("memberLastSeen");
        for (var e : clan.getMemberLastSeenMap().entrySet())
            lastSeenSection.set(e.getKey().toString(), e.getValue());

        cfg.set("clanLog", new ArrayList<>(clan.getClanLog()));

        ItemStack[] vault = manager.getAllVaults().get(clan.getName().toLowerCase());
        if (vault != null) cfg.set("vault", serializeInventory(vault));

        try { cfg.save(file); }
        catch (IOException e) {
            plugin.getLogger().severe("Failed to save clan " + clan.getName() + ": " + e.getMessage());
        }
    }

    private void saveGlobal(Manager manager, File file) {
        YamlConfiguration cfg = new YamlConfiguration();

        List<Map<String, Object>> warList = new ArrayList<>();
        for (War war : manager.getAllWarsForSave()) {
            Map<String, Object> wm = new LinkedHashMap<>();
            wm.put("clan1",        war.getClan1Name());
            wm.put("clan2",        war.getClan2Name());
            wm.put("clan1Kills",   war.getClan1Kills());
            wm.put("clan2Kills",   war.getClan2Kills());
            wm.put("targetKills",  war.getTargetKills());
            wm.put("startedAt",    war.getStartedAt());
            wm.put("endedAt",      war.getEndedAt());
            wm.put("timeoutMs",    war.getTimeoutMs());
            wm.put("active",       war.isActive());
            Map<String, Integer> kbp = new LinkedHashMap<>();
            war.getKillsByPlayer().forEach((u, k)  -> kbp.put(u.toString(), k));
            wm.put("killsByPlayer",  kbp);
            Map<String, Integer> dbp = new LinkedHashMap<>();
            war.getDeathsByPlayer().forEach((u, d) -> dbp.put(u.toString(), d));
            wm.put("deathsByPlayer", dbp);
            Map<String, String> pc = new LinkedHashMap<>();
            war.getPlayerClans().forEach((u, c)    -> pc.put(u.toString(), c));
            wm.put("playerClans", pc);
            warList.add(wm);
        }
        cfg.set("wars", warList);

        List<Map<String, Object>> reqList = new ArrayList<>();
        for (var entry : manager.getAllWarRequests().entrySet()) {
            for (War war : entry.getValue()) {
                Map<String, Object> rm = new LinkedHashMap<>();
                rm.put("targetOwner", entry.getKey().toString());
                rm.put("clan1",       war.getClan1Name());
                rm.put("clan2",       war.getClan2Name());
                rm.put("targetKills", war.getTargetKills());
                rm.put("startedAt",   war.getStartedAt());
                rm.put("timeoutMs",   war.getTimeoutMs());
                reqList.add(rm);
            }
        }
        cfg.set("warRequests", reqList);

        List<String> blockedList = new ArrayList<>();
        for (UUID id : manager.getInviteBlocked()) blockedList.add(id.toString());
        cfg.set("inviteBlocked", blockedList);

        List<String> glowList = new ArrayList<>();
        for (UUID id : manager.getGlowDisabled()) glowList.add(id.toString());
        cfg.set("glowDisabled", glowList);

        ConfigurationSection bansSection = cfg.createSection("clanBans");
        for (var entry : manager.getClanBans().entrySet())
            bansSection.set(entry.getKey().toString(), entry.getValue());

        try { cfg.save(file); }
        catch (IOException e) {
            plugin.getLogger().severe("Failed to save global data: " + e.getMessage());
        }
    }


    // ── shutdown ──────────────────────────────────────────────────────────────

    @Override
    public void shutdown() { }

    // ── load ─────────────────────────────────────────────────────────────────

    @Override
    public void load(Manager manager) {
        if (!clanDir.exists()) {
            if (legacyFile.exists()) {
                plugin.getLogger().info("Migrating clans.json to per-clan YAML files...");
                migrateLegacyJson(manager);
                save(manager);
                File backup = new File(legacyFile.getParentFile(), "clans.json.backup");
                legacyFile.renameTo(backup);
                plugin.getLogger().info("Migration complete. Old file renamed to clans.json.backup");
            }
            return;
        }

        manager.clearAllClans();

        File[] clanFiles = clanDir.listFiles((d, n) -> n.endsWith(".yml") && !n.equals("global.yml"));
        if (clanFiles != null) {
            for (File f : clanFiles) loadClanFile(f, manager);
        }

        File globalFile = new File(clanDir, "global.yml");
        if (globalFile.exists()) loadGlobal(globalFile, manager);
    }

    private void loadClanFile(File file, Manager manager) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        String name     = cfg.getString("name");
        String ownerStr = cfg.getString("owner");
        if (name == null || ownerStr == null) return;

        UUID owner;
        try { owner = UUID.fromString(ownerStr); }
        catch (IllegalArgumentException e) { return; }

        Clan clan = new Clan(name, owner);
        clan.setLevel(Math.max(1, cfg.getInt("level", 1)));
        clan.setXp(cfg.getLong("xp", 0));
        int mmo = cfg.getInt("maxMembersOverride", -1);
        if (mmo > 0) clan.setMaxMembersOverride(mmo);
        String bannerColor = cfg.getString("bannerColor");
        if (bannerColor != null) clan.setBannerColor(bannerColor);
        String tagColor = cfg.getString("tagColor");
        if (tagColor != null) clan.setTagColor(tagColor);
        clan.setFriendlyFire(cfg.getBoolean("friendlyFire", false));
        long createdAt = cfg.getLong("createdAt", 0);
        if (createdAt > 0) clan.setCreatedAt(createdAt);

        Location base = getLoc(cfg, "base");
        if (base  != null) clan.setBase(base);
        Location base2 = getLoc(cfg, "base2");
        if (base2 != null) clan.setBase2(base2);

        ConfigurationSection membersSection = cfg.getConfigurationSection("members");
        if (membersSection != null) {
            Map<UUID, String> loadedMembers = new HashMap<>();
            for (String key : membersSection.getKeys(false)) {
                try { loadedMembers.put(UUID.fromString(key), membersSection.getString(key, "member")); }
                catch (IllegalArgumentException ignored) {}
            }
            clan.loadMembers(loadedMembers);
        }

        cfg.getStringList("allies").forEach(clan::addAlly);
        cfg.getStringList("allyRequests").forEach(clan::addAllyRequest);

        List<?> ranksList = cfg.getList("ranks");
        if (ranksList != null && !ranksList.isEmpty()) {
            LinkedHashMap<String, Rank> loadedRanks = new LinkedHashMap<>();
            for (Object obj : ranksList) {
                if (!(obj instanceof Map)) continue;
                @SuppressWarnings("unchecked") Map<String, Object> rd = (Map<String, Object>) obj;
                try {
                    String rid    = (String) rd.get("id");
                    String rname  = (String) rd.get("name");
                    String rcolor = (String) rd.getOrDefault("color", "§7");
                    boolean rdel  = !(rd.get("deletable") instanceof Boolean) || (Boolean) rd.get("deletable");
                    if (rid == null || rname == null) continue;
                    Rank rank = new Rank(rid, rname, rcolor, rdel);
                    Object permsObj = rd.get("permissions");
                    if (permsObj instanceof List) {
                        Set<Permission> perms = EnumSet.noneOf(Permission.class);
                        for (Object pn : (List<?>) permsObj) {
                            try { perms.add(Permission.valueOf((String) pn)); } catch (Exception ignored) {}
                        }
                        rank.setPermissions(perms);
                    }
                    loadedRanks.put(rid, rank);
                } catch (Exception ignored) {}
            }
            if (!loadedRanks.isEmpty()) clan.loadRanks(loadedRanks);
        } else {
            LinkedHashMap<String, Rank> loadedRanks = new LinkedHashMap<>();
            Rank ownerRank = new Rank(Rank.OWNER_ID, "Anführer", "§c", false);
            ownerRank.setPermissions(EnumSet.allOf(Permission.class));
            loadedRanks.put(Rank.OWNER_ID, ownerRank);
            Rank memberRank = new Rank(Rank.MEMBER_ID, "Mitglied", "§a", false);
            memberRank.setPermissions(EnumSet.of(Permission.VAULT));
            loadedRanks.put(Rank.MEMBER_ID, memberRank);
            clan.loadRanks(loadedRanks);
        }

        List<?> patternsList = cfg.getList("bannerPatterns");
        if (patternsList != null) {
            List<Pattern> patterns = new ArrayList<>();
            for (Object obj : patternsList) {
                if (!(obj instanceof Map)) continue;
                @SuppressWarnings("unchecked") Map<String, String> pm = (Map<String, String>) obj;
                try {
                    String typeKey = pm.get("type");
                    if (typeKey == null) continue;
                    PatternType pt = RegistryAccess.registryAccess()
                            .getRegistry(RegistryKey.BANNER_PATTERN)
                            .get(NamespacedKey.minecraft(typeKey.toLowerCase(Locale.ROOT)));
                    DyeColor dc = DyeColor.valueOf(pm.get("color"));
                    if (pt != null) patterns.add(new Pattern(dc, pt));
                } catch (IllegalArgumentException ignored) {}
            }
            clan.setBannerPatterns(patterns);
        }

        ConfigurationSection warpSection = cfg.getConfigurationSection("memberWarps");
        if (warpSection != null) {
            for (String key : warpSection.getKeys(false)) {
                try {
                    UUID warpId = UUID.fromString(key);
                    Location wl = getLoc(warpSection, key);
                    if (wl != null) clan.setMemberWarp(warpId, wl);
                } catch (Exception ignored) {}
            }
        }

        ConfigurationSection lastSeenSection = cfg.getConfigurationSection("memberLastSeen");
        if (lastSeenSection != null) {
            for (String key : lastSeenSection.getKeys(false)) {
                try { clan.setLastSeen(UUID.fromString(key), lastSeenSection.getLong(key)); }
                catch (Exception ignored) {}
            }
        }

        clan.setClanLog(cfg.getStringList("clanLog"));

        List<String> vaultData = cfg.getStringList("vault");
        if (!vaultData.isEmpty()) manager.setVaultContents(clan.getName(), deserializeInventory(vaultData));

        manager.registerClan(clan);
    }

    private void loadGlobal(File file, Manager manager) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        List<?> warsList = cfg.getList("wars");
        if (warsList != null) {
            List<War> wars = new ArrayList<>();
            for (Object obj : warsList) {
                if (!(obj instanceof Map)) continue;
                @SuppressWarnings("unchecked") Map<String, Object> wm = (Map<String, Object>) obj;
                try {
                    String c1 = (String) wm.get("clan1");
                    String c2 = (String) wm.get("clan2");
                    if (c1 == null || c2 == null) continue;
                    int  targetKills = ((Number) wm.get("targetKills")).intValue();
                    long timeoutMs   = wm.containsKey("timeoutMs") ? ((Number) wm.get("timeoutMs")).longValue() : War.DEFAULT_TIMEOUT_MS;
                    War war = new War(c1, c2, targetKills, timeoutMs);
                    if (wm.containsKey("clan1Kills")) war.setClan1Kills(((Number) wm.get("clan1Kills")).intValue());
                    if (wm.containsKey("clan2Kills")) war.setClan2Kills(((Number) wm.get("clan2Kills")).intValue());
                    if (wm.get("killsByPlayer") instanceof Map) {
                        Map<UUID, Integer> map = new HashMap<>();
                        ((Map<?, ?>) wm.get("killsByPlayer")).forEach((k, v) -> {
                            try { map.put(UUID.fromString((String) k), ((Number) v).intValue()); } catch (Exception ignored) {}
                        });
                        war.setKillsByPlayer(map);
                    }
                    if (wm.get("deathsByPlayer") instanceof Map) {
                        Map<UUID, Integer> map = new HashMap<>();
                        ((Map<?, ?>) wm.get("deathsByPlayer")).forEach((k, v) -> {
                            try { map.put(UUID.fromString((String) k), ((Number) v).intValue()); } catch (Exception ignored) {}
                        });
                        war.setDeathsByPlayer(map);
                    }
                    if (wm.get("playerClans") instanceof Map) {
                        Map<UUID, String> map = new HashMap<>();
                        ((Map<?, ?>) wm.get("playerClans")).forEach((k, v) -> {
                            try { map.put(UUID.fromString((String) k), (String) v); } catch (Exception ignored) {}
                        });
                        war.setPlayerClans(map);
                    }
                    long endedAt = wm.containsKey("endedAt") ? ((Number) wm.get("endedAt")).longValue() : 0L;
                    if (endedAt > 0) war.setEndedAt(endedAt);
                    if (!Boolean.TRUE.equals(wm.get("active"))) war.end();
                    wars.add(war);
                } catch (Exception ignored) {}
            }
            manager.setActiveWars(wars);
        }

        List<?> reqsList = cfg.getList("warRequests");
        if (reqsList != null) {
            Map<UUID, List<War>> requests = new HashMap<>();
            for (Object obj : reqsList) {
                if (!(obj instanceof Map)) continue;
                @SuppressWarnings("unchecked") Map<String, Object> rm = (Map<String, Object>) obj;
                try {
                    String toStr = (String) rm.get("targetOwner");
                    String c1    = (String) rm.get("clan1");
                    String c2    = (String) rm.get("clan2");
                    if (toStr == null || c1 == null || c2 == null) continue;
                    UUID targetOwner = UUID.fromString(toStr);
                    int  targetKills = ((Number) rm.get("targetKills")).intValue();
                    long timeoutMs   = rm.containsKey("timeoutMs") ? ((Number) rm.get("timeoutMs")).longValue() : War.DEFAULT_TIMEOUT_MS;
                    War war = new War(c1, c2, targetKills, timeoutMs);
                    requests.computeIfAbsent(targetOwner, k -> new ArrayList<>()).add(war);
                } catch (Exception ignored) {}
            }
            manager.setAllWarRequests(requests);
        }

        Set<UUID> blocked = new HashSet<>();
        for (String s : cfg.getStringList("inviteBlocked")) {
            try { blocked.add(UUID.fromString(s)); } catch (Exception ignored) {}
        }
        manager.setInviteBlocked(blocked);

        Set<UUID> glowDisabled = new HashSet<>();
        for (String s : cfg.getStringList("glowDisabled")) {
            try { glowDisabled.add(UUID.fromString(s)); } catch (Exception ignored) {}
        }
        manager.setGlowDisabled(glowDisabled);

        ConfigurationSection bansSection = cfg.getConfigurationSection("clanBans");
        if (bansSection != null) {
            Map<UUID, Long> bans = new HashMap<>();
            for (String key : bansSection.getKeys(false)) {
                try { bans.put(UUID.fromString(key), bansSection.getLong(key)); } catch (Exception ignored) {}
            }
            manager.setClanBans(bans);
        }
    }

    // ── Legacy JSON migration ─────────────────────────────────────────────────

    private void migrateLegacyJson(Manager manager) {
        Gson gson = new GsonBuilder().create();
        try (var reader = new BufferedReader(new FileReader(legacyFile, StandardCharsets.UTF_8))) {
            StoredData data = gson.fromJson(reader, StoredData.class);
            if (data == null || data.clans == null) return;

            manager.clearAllClans();
            for (StoredClan sc : data.clans) {
                if (sc.name == null || sc.ownerId == null) continue;
                UUID owner;
                try { owner = UUID.fromString(sc.ownerId); }
                catch (IllegalArgumentException e) { continue; }

                Clan clan = new Clan(sc.name, owner);
                clan.setLevel(sc.level > 0 ? sc.level : 1);
                clan.setXp(sc.xp);
                if (sc.bannerColor != null) clan.setBannerColor(sc.bannerColor);
                if (sc.tagColor    != null) clan.setTagColor(sc.tagColor);
                if (sc.maxMembersOverride > 0) clan.setMaxMembersOverride(sc.maxMembersOverride);
                clan.setFriendlyFire(sc.friendlyFire);
                if (sc.createdAt > 0) clan.setCreatedAt(sc.createdAt);

                Location base = deserializeLocMap(sc.base);
                if (base  != null) clan.setBase(base);
                Location base2 = deserializeLocMap(sc.base2);
                if (base2 != null) clan.setBase2(base2);

                if (sc.memberWarps != null) {
                    for (var e : sc.memberWarps.entrySet()) {
                        try {
                            UUID wId = UUID.fromString(e.getKey());
                            Location wl = deserializeLocMap(e.getValue());
                            if (wl != null) clan.setMemberWarp(wId, wl);
                        } catch (Exception ignored) {}
                    }
                }

                if (sc.memberLastSeen != null) {
                    for (var e : sc.memberLastSeen.entrySet()) {
                        try { clan.setLastSeen(UUID.fromString(e.getKey()), e.getValue()); }
                        catch (Exception ignored) {}
                    }
                }

                if (sc.members != null) {
                    Map<UUID, String> loadedMembers = new HashMap<>();
                    for (var e : sc.members.entrySet()) {
                        try { loadedMembers.put(UUID.fromString(e.getKey()), e.getValue()); }
                        catch (IllegalArgumentException ignored) {}
                    }
                    clan.loadMembers(loadedMembers);
                }

                if (sc.allies != null)       sc.allies.forEach(clan::addAlly);
                if (sc.allyRequests != null) sc.allyRequests.forEach(clan::addAllyRequest);

                if (sc.ranks != null && !sc.ranks.isEmpty()) {
                    LinkedHashMap<String, Rank> loadedRanks = new LinkedHashMap<>();
                    for (Map<String, Object> rd : sc.ranks) {
                        try {
                            String rid    = (String) rd.get("id");
                            String rname  = (String) rd.get("name");
                            String rcolor = (String) rd.getOrDefault("color", "§7");
                            boolean rdel  = !(rd.get("deletable") instanceof Boolean) || (Boolean) rd.get("deletable");
                            if (rid == null || rname == null) continue;
                            Rank rank = new Rank(rid, rname, rcolor, rdel);
                            Object permsObj = rd.get("permissions");
                            if (permsObj instanceof List) {
                                Set<Permission> perms = EnumSet.noneOf(Permission.class);
                                for (Object pn : (List<?>) permsObj) {
                                    try { perms.add(Permission.valueOf((String) pn)); } catch (Exception ignored) {}
                                }
                                rank.setPermissions(perms);
                            }
                            loadedRanks.put(rid, rank);
                        } catch (Exception ignored) {}
                    }
                    if (!loadedRanks.isEmpty()) clan.loadRanks(loadedRanks);
                } else {
                    LinkedHashMap<String, Rank> loadedRanks = new LinkedHashMap<>();
                    Rank ownerRank = new Rank(Rank.OWNER_ID, "Anführer", "§c", false);
                    ownerRank.setPermissions(EnumSet.allOf(Permission.class));
                    loadedRanks.put(Rank.OWNER_ID, ownerRank);
                    String memberName = (sc.customRoleNames != null && sc.customRoleNames.containsKey("MEMBER"))
                            ? sc.customRoleNames.get("MEMBER") : "Mitglied";
                    Rank memberRank = new Rank(Rank.MEMBER_ID, memberName, "§a", false);
                    Set<Permission> memberPerms = EnumSet.of(Permission.VAULT);
                    if (sc.rolePermissions != null && sc.rolePermissions.containsKey("MEMBER")) {
                        memberPerms = EnumSet.noneOf(Permission.class);
                        for (String pn : sc.rolePermissions.get("MEMBER")) {
                            try { memberPerms.add(Permission.valueOf(pn)); } catch (Exception ignored) {}
                        }
                    }
                    memberRank.setPermissions(memberPerms);
                    loadedRanks.put(Rank.MEMBER_ID, memberRank);
                    clan.loadRanks(loadedRanks);
                }

                if (sc.bannerPatterns != null) {
                    List<Pattern> patterns = new ArrayList<>();
                    for (Map<String, String> entry : sc.bannerPatterns) {
                        try {
                            String typeKey = entry.get("type");
                            if (typeKey == null) continue;
                            PatternType pt = RegistryAccess.registryAccess()
                                    .getRegistry(RegistryKey.BANNER_PATTERN)
                                    .get(NamespacedKey.minecraft(typeKey.toLowerCase(Locale.ROOT)));
                            DyeColor dc = DyeColor.valueOf(entry.get("color"));
                            if (pt != null) patterns.add(new Pattern(dc, pt));
                        } catch (IllegalArgumentException ignored) {}
                    }
                    clan.setBannerPatterns(patterns);
                }

                if (sc.clanLog != null) clan.setClanLog(sc.clanLog);
                manager.registerClan(clan);
            }

            if (data.wars != null) {
                List<War> wars = new ArrayList<>();
                for (StoredWar sw : data.wars) {
                    if (sw.clan1Name == null || sw.clan2Name == null) continue;
                    long timeout = sw.timeoutMs > 0 ? sw.timeoutMs : War.DEFAULT_TIMEOUT_MS;
                    War war = new War(sw.clan1Name, sw.clan2Name, sw.targetKills, timeout);
                    war.setClan1Kills(sw.clan1Kills);
                    war.setClan2Kills(sw.clan2Kills);
                    if (sw.killsByPlayer != null) {
                        Map<UUID, Integer> map = new HashMap<>();
                        sw.killsByPlayer.forEach((s, k) -> { try { map.put(UUID.fromString(s), k); } catch (Exception ignored) {} });
                        war.setKillsByPlayer(map);
                    }
                    if (sw.deathsByPlayer != null) {
                        Map<UUID, Integer> map = new HashMap<>();
                        sw.deathsByPlayer.forEach((s, d) -> { try { map.put(UUID.fromString(s), d); } catch (Exception ignored) {} });
                        war.setDeathsByPlayer(map);
                    }
                    if (sw.playerClans != null) {
                        Map<UUID, String> map = new HashMap<>();
                        sw.playerClans.forEach((s, c) -> { try { map.put(UUID.fromString(s), c); } catch (Exception ignored) {} });
                        war.setPlayerClans(map);
                    }
                    if (sw.endedAt > 0) war.setEndedAt(sw.endedAt);
                    if (!sw.active) war.end();
                    wars.add(war);
                }
                manager.setActiveWars(wars);
            }

            if (data.warRequests != null) {
                Map<UUID, List<War>> requests = new HashMap<>();
                for (StoredWarRequest swr : data.warRequests) {
                    if (swr.clan1Name == null || swr.clan2Name == null || swr.targetOwnerId == null) continue;
                    try {
                        UUID targetOwner = UUID.fromString(swr.targetOwnerId);
                        long timeout = swr.timeoutMs > 0 ? swr.timeoutMs : War.DEFAULT_TIMEOUT_MS;
                        War war = new War(swr.clan1Name, swr.clan2Name, swr.targetKills, timeout);
                        requests.computeIfAbsent(targetOwner, k -> new ArrayList<>()).add(war);
                    } catch (IllegalArgumentException ignored) {}
                }
                manager.setAllWarRequests(requests);
            }

            if (data.inviteBlocked != null) {
                Set<UUID> blocked = new HashSet<>();
                for (String s : data.inviteBlocked) { try { blocked.add(UUID.fromString(s)); } catch (Exception ignored) {} }
                manager.setInviteBlocked(blocked);
            }
            if (data.glowDisabled != null) {
                Set<UUID> glowDisabled = new HashSet<>();
                for (String s : data.glowDisabled) { try { glowDisabled.add(UUID.fromString(s)); } catch (Exception ignored) {} }
                manager.setGlowDisabled(glowDisabled);
            }
            if (data.clanBanned != null) {
                Map<UUID, Long> bans = new HashMap<>();
                for (var e : data.clanBanned.entrySet()) {
                    try { bans.put(UUID.fromString(e.getKey()), e.getValue()); } catch (Exception ignored) {}
                }
                manager.setClanBans(bans);
            }
            if (data.vaults != null) {
                for (StoredVault sv : data.vaults) {
                    if (sv.clanName == null || sv.items == null) continue;
                    manager.setVaultContents(sv.clanName, deserializeInventory(sv.items));
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to read clans.json for migration: " + e.getMessage());
        }
    }

    // ── Location helpers ──────────────────────────────────────────────────────

    private static void setLoc(ConfigurationSection section, String key, Location loc) {
        ConfigurationSection s = section.createSection(key);
        s.set("world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
        s.set("x",     loc.getX());
        s.set("y",     loc.getY());
        s.set("z",     loc.getZ());
        s.set("yaw",   (double) loc.getYaw());
        s.set("pitch", (double) loc.getPitch());
    }

    private static Location getLoc(ConfigurationSection section, String key) {
        ConfigurationSection s = section.getConfigurationSection(key);
        if (s == null) return null;
        try {
            World world = Bukkit.getWorld(s.getString("world", "world"));
            if (world == null) return null;
            return new Location(world,
                    s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                    (float) s.getDouble("yaw", 0), (float) s.getDouble("pitch", 0));
        } catch (Exception e) { return null; }
    }

    private static Location deserializeLocMap(Map<String, Object> m) {
        if (m == null) return null;
        try {
            String wn = (String) m.get("world");
            World w = Bukkit.getWorld(wn != null ? wn : "world");
            if (w == null) return null;
            double x = ((Number) m.get("x")).doubleValue();
            double y = ((Number) m.get("y")).doubleValue();
            double z = ((Number) m.get("z")).doubleValue();
            float yaw   = m.get("yaw")   != null ? ((Number) m.get("yaw")).floatValue()   : 0f;
            float pitch = m.get("pitch") != null ? ((Number) m.get("pitch")).floatValue() : 0f;
            return new Location(w, x, y, z, yaw, pitch);
        } catch (Exception e) { return null; }
    }

    // ── Inventory serialization ───────────────────────────────────────────────

    private static List<String> serializeInventory(ItemStack[] contents) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                try {
                    list.add(i + ":" + Base64.getEncoder().encodeToString(contents[i].serializeAsBytes()));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }

    private static ItemStack[] deserializeInventory(List<String> serialized) {
        ItemStack[] items = new ItemStack[54];
        if (serialized == null) return items;
        for (String entry : serialized) {
            int colonIdx = entry.indexOf(':');
            if (colonIdx < 0) continue;
            try {
                int slot = Integer.parseInt(entry.substring(0, colonIdx));
                items[slot] = ItemStack.deserializeBytes(Base64.getDecoder().decode(entry.substring(colonIdx + 1)));
            } catch (Exception ignored) {}
        }
        return items;
    }

    // ── Name sanitization ─────────────────────────────────────────────────────

    private static String sanitizeName(String name) {
        return name.replaceAll("[/\\\\:*?\"<>|]", "_");
    }

    // ── Legacy JSON DTOs (migration only) ────────────────────────────────────

    private static final class StoredData {
        List<StoredClan>       clans         = new ArrayList<>();
        List<StoredWar>        wars          = new ArrayList<>();
        List<StoredWarRequest> warRequests   = new ArrayList<>();
        List<String>           inviteBlocked = new ArrayList<>();
        List<String>           glowDisabled  = new ArrayList<>();
        Map<String, Long>      clanBanned    = new HashMap<>();
        List<StoredVault>      vaults        = new ArrayList<>();
    }

    @SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
    private static final class StoredClan {
        String name, ownerId, bannerColor, tagColor;
        int    level, maxMembersOverride = -1;
        long   xp, createdAt;
        boolean friendlyFire;
        Map<String, String>              members;
        List<String>                     allies, allyRequests;
        List<Map<String, Object>>        ranks;
        Map<String, Object>              base, base2;
        Map<String, Map<String, Object>> memberWarps;
        Map<String, Long>                memberLastSeen;
        Map<String, List<String>>        rolePermissions;
        Map<String, String>              customRoleNames;
        List<Map<String, String>>        bannerPatterns;
        List<String>                     clanLog;
    }

    private static final class StoredWar {
        String clan1Name, clan2Name;
        int    clan1Kills, clan2Kills, targetKills;
        @SuppressWarnings("unused") long startedAt;
        long   endedAt, timeoutMs;
        boolean active;
        Map<String, Integer> killsByPlayer, deathsByPlayer;
        Map<String, String>  playerClans;
    }

    private static final class StoredWarRequest {
        String targetOwnerId, clan1Name, clan2Name;
        int    targetKills;
        @SuppressWarnings("unused") long startedAt;
        long   timeoutMs;
    }

    private static final class StoredVault {
        String       clanName;
        List<String> items;
    }
}
