package com.betterclan.clan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.betterclan.storage.StorageProvider;

public final class ClanStorage implements StorageProvider {

    private final Plugin plugin;
    private final Gson gson;
    private final File file;

    public ClanStorage(Plugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.file = new File(plugin.getDataFolder(), "clans.json");
    }

    public void save(ClanManager manager) {
        StoredData data = new StoredData();

        for (Clan clan : manager.getAllClans()) {
            StoredClan sc = new StoredClan();
            sc.name = clan.getName();
            sc.ownerId = clan.getOwnerId().toString();
            sc.level = clan.getLevel();
            sc.xp = clan.getXp();
            sc.maxMembersOverride = clan.getMaxMembersOverride();
            sc.bannerColor = clan.getBannerColor();
            sc.tagColor = clan.getTagColor();
            sc.friendlyFire = clan.isFriendlyFire();
            if (clan.getBase()  != null) sc.base  = serializeLoc(clan.getBase());
            if (clan.getBase2() != null) sc.base2 = serializeLoc(clan.getBase2());
            sc.memberWarps = new HashMap<>();
            for (var entry : clan.getMemberWarps().entrySet())
                sc.memberWarps.put(entry.getKey().toString(), serializeLoc(entry.getValue()));
            sc.memberLastSeen = new HashMap<>();
            for (var lsEntry : clan.getMemberLastSeenMap().entrySet())
                sc.memberLastSeen.put(lsEntry.getKey().toString(), lsEntry.getValue());
            sc.createdAt = clan.getCreatedAt();

            sc.members = new HashMap<>();
            for (var e : clan.getMembers().entrySet()) {
                sc.members.put(e.getKey().toString(), e.getValue());
            }

            sc.allies = new ArrayList<>(clan.getAllies());
            sc.allyRequests = new ArrayList<>(clan.getAllyRequests());

            sc.ranks = new ArrayList<>();
            for (CustomRank rank : clan.getAllRanks().values()) {
                Map<String, Object> rankData = new HashMap<>();
                rankData.put("id", rank.getId());
                rankData.put("name", rank.getName());
                rankData.put("color", rank.getColor());
                rankData.put("deletable", rank.isDeletable());
                List<String> permNames = new ArrayList<>();
                for (ClanPermission p : rank.getPermissions()) permNames.add(p.name());
                rankData.put("permissions", permNames);
                sc.ranks.add(rankData);
            }

            sc.bannerPatterns = new ArrayList<>();
            for (Pattern p : clan.getBannerPatterns()) {
                Map<String, String> entry = new HashMap<>();
                NamespacedKey nk = RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.BANNER_PATTERN).getKey(p.getPattern());
                entry.put("type", nk != null ? nk.getKey() : "base");
                entry.put("color", p.getColor().name());
                sc.bannerPatterns.add(entry);
            }

            sc.clanLog = new ArrayList<>(clan.getClanLog());

            data.clans.add(sc);
        }

        for (ClanWar war : manager.getAllWarsForSave()) {
            StoredWar sw = new StoredWar();
            sw.clan1Name = war.getClan1Name();
            sw.clan2Name = war.getClan2Name();
            sw.clan1Kills = war.getClan1Kills();
            sw.clan2Kills = war.getClan2Kills();
            sw.targetKills = war.getTargetKills();
            sw.startedAt = war.getStartedAt();
            sw.endedAt = war.getEndedAt();
            sw.timeoutMs = war.getTimeoutMs();
            sw.active = war.isActive();
            Map<String, Integer> kbp = new HashMap<>();
            war.getKillsByPlayer().forEach((uuid, kills) -> kbp.put(uuid.toString(), kills));
            sw.killsByPlayer = kbp;
            Map<String, Integer> dbp = new HashMap<>();
            war.getDeathsByPlayer().forEach((uuid, deaths) -> dbp.put(uuid.toString(), deaths));
            sw.deathsByPlayer = dbp;
            Map<String, String> pc = new HashMap<>();
            war.getPlayerClans().forEach((uuid, clanName) -> pc.put(uuid.toString(), clanName));
            sw.playerClans = pc;
            data.wars.add(sw);
        }

        for (var entry : manager.getAllWarRequests().entrySet()) {
            for (ClanWar war : entry.getValue()) {
                StoredWarRequest swr = new StoredWarRequest();
                swr.targetOwnerId = entry.getKey().toString();
                swr.clan1Name = war.getClan1Name();
                swr.clan2Name = war.getClan2Name();
                swr.targetKills = war.getTargetKills();
                swr.startedAt = war.getStartedAt();
                swr.timeoutMs = war.getTimeoutMs();
                data.warRequests.add(swr);
            }
        }

        for (UUID id : manager.getInviteBlocked()) {
            data.inviteBlocked.add(id.toString());
        }

        for (UUID id : manager.getGlowDisabled()) {
            data.glowDisabled.add(id.toString());
        }

        for (var entry : manager.getClanBans().entrySet()) {
            data.clanBanned.put(entry.getKey().toString(), entry.getValue());
        }

        for (var entry : manager.getAllVaults().entrySet()) {
            StoredVault sv = new StoredVault();
            sv.clanName = entry.getKey();
            sv.items = serializeInventory(entry.getValue());
            data.vaults.add(sv);
        }

        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            plugin.getLogger().warning("Konnte Datenverzeichnis nicht erstellen: " + file.getParentFile());
            return;
        }

        try (var writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der Clan-Daten: " + e.getMessage());
        }
    }

    @Override
    public void shutdown() {  }

    @Override
    public void load(ClanManager manager) {

        if (!file.exists()) return;

        try (var reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
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
                if (sc.tagColor != null) clan.setTagColor(sc.tagColor);
                if (sc.maxMembersOverride > 0) clan.setMaxMembersOverride(sc.maxMembersOverride);
                clan.setFriendlyFire(sc.friendlyFire);

                Location base = deserializeLoc(sc.base);
                if (base != null) clan.setBase(base);
                Location base2 = deserializeLoc(sc.base2);
                if (base2 != null) clan.setBase2(base2);
                if (sc.createdAt > 0) clan.setCreatedAt(sc.createdAt);

                if (sc.memberWarps != null) {
                    for (var mwE : sc.memberWarps.entrySet()) {
                        try {
                            UUID mwId = UUID.fromString(mwE.getKey());
                            Location wl = deserializeLoc(mwE.getValue());
                            if (wl != null) clan.setMemberWarp(mwId, wl);
                        } catch (Exception ignored) {}
                    }
                }

                if (sc.memberLastSeen != null) {
                    for (var lsE : sc.memberLastSeen.entrySet()) {
                        try { clan.setLastSeen(UUID.fromString(lsE.getKey()), lsE.getValue()); }
                        catch (Exception ignored) {}
                    }
                }

                if (sc.members != null) {
                    Map<UUID, String> loadedMembers = new HashMap<>();
                    for (var e : sc.members.entrySet()) {
                        try {
                            loadedMembers.put(UUID.fromString(e.getKey()), e.getValue());
                        } catch (IllegalArgumentException ignored) {}
                    }
                    clan.loadMembers(loadedMembers);
                }

                if (sc.allies != null) sc.allies.forEach(clan::addAlly);
                if (sc.allyRequests != null) sc.allyRequests.forEach(clan::addAllyRequest);

                if (sc.ranks != null && !sc.ranks.isEmpty()) {
                    LinkedHashMap<String, CustomRank> loadedRanks = new LinkedHashMap<>();
                    for (Map<String, Object> rd : sc.ranks) {
                        try {
                            String rid = (String) rd.get("id");
                            String rname = (String) rd.get("name");
                            String rcolor = (String) rd.getOrDefault("color", "§7");
                            boolean rdel = !(rd.get("deletable") instanceof Boolean) || (Boolean) rd.get("deletable");
                            if (rid == null || rname == null) continue;
                            CustomRank rank = new CustomRank(rid, rname, rcolor, rdel);
                            Object permsObj = rd.get("permissions");
                            if (permsObj instanceof List) {
                                Set<ClanPermission> perms = EnumSet.noneOf(ClanPermission.class);
                                for (Object pName : (List<?>) permsObj) {
                                    try { perms.add(ClanPermission.valueOf((String) pName)); }
                                    catch (Exception ignored) {}
                                }
                                rank.setPermissions(perms);
                            }
                            loadedRanks.put(rid, rank);
                        } catch (Exception ignored) {}
                    }
                    if (!loadedRanks.isEmpty()) clan.loadRanks(loadedRanks);
                } else {

                    LinkedHashMap<String, CustomRank> loadedRanks = new LinkedHashMap<>();

                    CustomRank ownerRank = new CustomRank(CustomRank.OWNER_ID, "Anführer", "§c", false);
                    ownerRank.setPermissions(EnumSet.allOf(ClanPermission.class));
                    loadedRanks.put(CustomRank.OWNER_ID, ownerRank);

                    String memberName = (sc.customRoleNames != null && sc.customRoleNames.containsKey("MEMBER"))
                            ? sc.customRoleNames.get("MEMBER") : "Mitglied";
                    CustomRank memberRank = new CustomRank(CustomRank.MEMBER_ID, memberName, "§a", false);
                    Set<ClanPermission> memberPerms = EnumSet.of(ClanPermission.VAULT);
                    if (sc.rolePermissions != null && sc.rolePermissions.containsKey("MEMBER")) {
                        memberPerms = EnumSet.noneOf(ClanPermission.class);
                        for (String pn : sc.rolePermissions.get("MEMBER")) {
                            try { memberPerms.add(ClanPermission.valueOf(pn)); } catch (Exception ignored) {}
                        }
                    }
                    memberRank.setPermissions(memberPerms);
                    loadedRanks.put(CustomRank.MEMBER_ID, memberRank);
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
                                    .get(NamespacedKey.minecraft(typeKey.toLowerCase(java.util.Locale.ROOT)));
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
                List<ClanWar> wars = new ArrayList<>();
                for (StoredWar sw : data.wars) {
                    if (sw.clan1Name == null || sw.clan2Name == null) continue;
                    long timeout = sw.timeoutMs > 0 ? sw.timeoutMs : ClanWar.DEFAULT_TIMEOUT_MS;
                    ClanWar war = new ClanWar(sw.clan1Name, sw.clan2Name, sw.targetKills, timeout);
                    war.setClan1Kills(sw.clan1Kills);
                    war.setClan2Kills(sw.clan2Kills);
                    if (sw.killsByPlayer != null) {
                        Map<java.util.UUID, Integer> map = new HashMap<>();
                        sw.killsByPlayer.forEach((s, k) -> { try { map.put(java.util.UUID.fromString(s), k); } catch (Exception ignored) {} });
                        war.setKillsByPlayer(map);
                    }
                    if (sw.deathsByPlayer != null) {
                        Map<java.util.UUID, Integer> map = new HashMap<>();
                        sw.deathsByPlayer.forEach((s, d) -> { try { map.put(java.util.UUID.fromString(s), d); } catch (Exception ignored) {} });
                        war.setDeathsByPlayer(map);
                    }
                    if (sw.playerClans != null) {
                        Map<java.util.UUID, String> map = new HashMap<>();
                        sw.playerClans.forEach((s, c) -> { try { map.put(java.util.UUID.fromString(s), c); } catch (Exception ignored) {} });
                        war.setPlayerClans(map);
                    }
                    if (sw.endedAt > 0) war.setEndedAt(sw.endedAt);
                    if (!sw.active) war.end();
                    wars.add(war);
                }
                manager.setActiveWars(wars);
            }

            if (data.warRequests != null) {
                Map<UUID, List<ClanWar>> requests = new HashMap<>();
                for (StoredWarRequest swr : data.warRequests) {
                    if (swr.clan1Name == null || swr.clan2Name == null || swr.targetOwnerId == null) continue;
                    try {
                        UUID targetOwner = UUID.fromString(swr.targetOwnerId);
                        long timeout = swr.timeoutMs > 0 ? swr.timeoutMs : ClanWar.DEFAULT_TIMEOUT_MS;
                        ClanWar war = new ClanWar(swr.clan1Name, swr.clan2Name, swr.targetKills, timeout);
                        requests.computeIfAbsent(targetOwner, k -> new ArrayList<>()).add(war);
                    } catch (IllegalArgumentException ignored) {}
                }
                manager.setAllWarRequests(requests);
            }

            if (data.inviteBlocked != null) {
                Set<UUID> blocked = new HashSet<>();
                for (String idStr : data.inviteBlocked) {
                    try { blocked.add(UUID.fromString(idStr)); }
                    catch (IllegalArgumentException ignored) {}
                }
                manager.setInviteBlocked(blocked);
            }

            if (data.glowDisabled != null) {
                Set<UUID> disabled = new HashSet<>();
                for (String idStr : data.glowDisabled) {
                    try { disabled.add(UUID.fromString(idStr)); }
                    catch (IllegalArgumentException ignored) {}
                }
                manager.setGlowDisabled(disabled);
            }

            if (data.clanBanned != null) {
                Map<UUID, Long> bans = new java.util.HashMap<>();
                for (var entry : data.clanBanned.entrySet()) {
                    try { bans.put(UUID.fromString(entry.getKey()), entry.getValue()); }
                    catch (IllegalArgumentException ignored) {}
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
            plugin.getLogger().severe("Fehler beim Laden der Clan-Daten: " + e.getMessage());
        }
    }

    private static Map<String, Object> serializeLoc(Location loc) {
        Map<String, Object> m = new HashMap<>();
        m.put("world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
        m.put("x", loc.getX()); m.put("y", loc.getY()); m.put("z", loc.getZ());
        m.put("yaw", (double) loc.getYaw()); m.put("pitch", (double) loc.getPitch());
        return m;
    }

    private static Location deserializeLoc(Map<String, Object> m) {
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

    private List<String> serializeInventory(ItemStack[] contents) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                try {
                    byte[] data = contents[i].serializeAsBytes();
                    list.add(i + ":" + Base64.getEncoder().encodeToString(data));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }

    private ItemStack[] deserializeInventory(List<String> serialized) {
        ItemStack[] items = new ItemStack[54];
        if (serialized == null) return items;
        for (String entry : serialized) {
            int colonIdx = entry.indexOf(':');
            if (colonIdx < 0) continue;
            try {
                int slot = Integer.parseInt(entry.substring(0, colonIdx));
                byte[] data = Base64.getDecoder().decode(entry.substring(colonIdx + 1));
                items[slot] = ItemStack.deserializeBytes(data);
            } catch (Exception ignored) {}
        }
        return items;
    }

    @SuppressWarnings("CanBeFinal")
    private static final class StoredData {
        List<StoredClan> clans = new ArrayList<>();
        List<StoredWar> wars = new ArrayList<>();
        List<StoredWarRequest> warRequests = new ArrayList<>();
        List<String> inviteBlocked = new ArrayList<>();
        List<String> glowDisabled = new ArrayList<>();
        Map<String, Long> clanBanned = new HashMap<>();
        List<StoredVault> vaults = new ArrayList<>();
    }

    @SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
    private static final class StoredClan {
        String name, ownerId, bannerColor, tagColor;
        int level;
        int maxMembersOverride = -1;
        long xp, createdAt;
        boolean friendlyFire;
        Map<String, String> members;
        List<String> allies, allyRequests;
        List<Map<String, Object>> ranks;
        Map<String, Object> base;
        Map<String, Object> base2;
        Map<String, Map<String, Object>> memberWarps;
        Map<String, Long> memberLastSeen;

        Map<String, List<String>> rolePermissions;
        Map<String, String> customRoleNames;
        List<Map<String, String>> bannerPatterns;
        List<String> clanLog;
    }

    private static final class StoredWar {
        String clan1Name, clan2Name;
        int clan1Kills, clan2Kills, targetKills;
        @SuppressWarnings("unused") long startedAt;
        long endedAt, timeoutMs;
        boolean active;
        Map<String, Integer> killsByPlayer;
        Map<String, Integer> deathsByPlayer;
        Map<String, String> playerClans;
    }

    private static final class StoredWarRequest {
        String targetOwnerId, clan1Name, clan2Name;
        int targetKills;
        @SuppressWarnings("unused") long startedAt;
        long timeoutMs;
    }

    private static final class StoredVault {
        String clanName;
        List<String> items;
    }
}

