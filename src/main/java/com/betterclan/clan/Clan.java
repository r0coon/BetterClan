package com.betterclan.clan;

import org.bukkit.Location;
import org.bukkit.block.banner.Pattern;

import java.util.*;
import java.util.stream.Collectors;

public class Clan {

    private String name;
    private UUID ownerId;
    private int level;
    private long xp;
    private long createdAt;
    private String bannerColor;
    private String tagColor = "§7";
    private final List<Pattern> bannerPatterns = new ArrayList<>();
    private final Map<UUID, String> members = new HashMap<>();
    private final Map<String, CustomRank> ranks = new LinkedHashMap<>();
    private final Set<String> allies = new HashSet<>();
    private final Set<String> allyRequests = new HashSet<>();
    private Location base = null;
    private Location base2 = null;
    private boolean friendlyFire;
    private final Map<UUID, Location> memberWarps = new HashMap<>();
    private final Map<UUID, Long> memberLastSeen = new HashMap<>();
    private final List<String> clanLog = new ArrayList<>();
    private int maxMembersOverride = -1;

    private static int MAX_LOG_ENTRIES = 200;
    private static int MAX_BANNER_SLOTS = 6;
    private static boolean DEFAULT_FRIENDLY_FIRE = false;
    private static Set<ClanPermission> DEFAULT_MEMBER_PERMISSIONS = EnumSet.of(
            ClanPermission.VAULT, ClanPermission.VIEW_LOG, ClanPermission.TELEPORT,
            ClanPermission.CHAT_ALLY, ClanPermission.CLAN_CHAT
    );

    @SuppressWarnings("unused")
    public static void configure(int maxLog, int maxBanner) {
        MAX_LOG_ENTRIES = maxLog;
        MAX_BANNER_SLOTS = maxBanner;
    }

    public static void configure(int maxLog, int maxBanner, boolean defaultFF, Set<ClanPermission> defaultMemberPerms) {
        MAX_LOG_ENTRIES = maxLog;
        MAX_BANNER_SLOTS = maxBanner;
        DEFAULT_FRIENDLY_FIRE = defaultFF;
        if (defaultMemberPerms != null && !defaultMemberPerms.isEmpty())
            DEFAULT_MEMBER_PERMISSIONS = defaultMemberPerms;
    }

    public Clan(String name, UUID ownerId) {
        this.name = name;
        this.ownerId = ownerId;
        this.level = 1;
        this.xp = 0;
        this.bannerColor = "WHITE";
        this.friendlyFire = DEFAULT_FRIENDLY_FIRE;
        this.createdAt = System.currentTimeMillis();
        initDefaultRanks();
        members.put(ownerId, CustomRank.OWNER_ID);
    }

    private void initDefaultRanks() {
        CustomRank owner = new CustomRank(CustomRank.OWNER_ID, "Anführer", "§c", false);
        owner.setPermissions(EnumSet.allOf(ClanPermission.class));

        CustomRank member = new CustomRank(CustomRank.MEMBER_ID, "Mitglied", "§a", false);
        member.setPermissions(DEFAULT_MEMBER_PERMISSIONS);

        ranks.put(CustomRank.OWNER_ID, owner);
        ranks.put(CustomRank.MEMBER_ID, member);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

    public CustomRank getRank(String rankId) {
        return ranks.get(rankId);
    }

    public List<CustomRank> getOrderedRanks() {
        return ranks.values().stream()
                .sorted((a, b) -> {
                    if (CustomRank.OWNER_ID.equals(a.getId())) return -1;
                    if (CustomRank.OWNER_ID.equals(b.getId())) return 1;
                    return Integer.compare(b.getPermissions().size(), a.getPermissions().size());
                })
                .collect(Collectors.toList());
    }

    public void addCustomRank(CustomRank rank) {
        ranks.put(rank.getId(), rank);
    }

    public boolean deleteRank(String rankId) {
        CustomRank rank = ranks.get(rankId);
        if (rank == null || !rank.isDeletable()) return false;
        ranks.remove(rankId);
        members.replaceAll((uuid, rid) -> rid.equals(rankId) ? CustomRank.MEMBER_ID : rid);
        return true;
    }

    public String getDefaultOfficerRankId() {
        if (ranks.containsKey("OFFICER")) return "OFFICER";
        return ranks.values().stream()
                .filter(r -> !CustomRank.OWNER_ID.equals(r.getId()) && !CustomRank.MEMBER_ID.equals(r.getId()))
                .max(Comparator.comparingInt(a -> a.getPermissions().size()))
                .map(CustomRank::getId)
                .orElse(CustomRank.MEMBER_ID);
    }

    public String getDemotedRankId(String currentRankId) {
        if (currentRankId == null) return null;
        if (CustomRank.MEMBER_ID.equals(currentRankId)) return null;
        if (CustomRank.OWNER_ID.equals(currentRankId)) return getDefaultOfficerRankId();
        return CustomRank.MEMBER_ID;
    }

    public String getPromotedRankId(String currentRankId) {
        if (currentRankId == null) return null;
        if (!CustomRank.MEMBER_ID.equals(currentRankId)) return null;
        return getDefaultOfficerRankId();
    }

    public Map<UUID, String> getMembers() {
        return Collections.unmodifiableMap(members);
    }

    public String getRankId(UUID playerId) {
        return members.get(playerId);
    }

    public CustomRank getEffectiveRank(UUID playerId) {
        String rankId = members.get(playerId);
        if (rankId == null) return null;
        CustomRank rank = ranks.get(rankId);

        return rank != null ? rank : ranks.get(CustomRank.MEMBER_ID);
    }

    public void addMember(UUID playerId, String rankId) {
        members.put(playerId, rankId);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    @SuppressWarnings("unused")
    public boolean isMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    public int getMemberCount() {
        return members.size();
    }

    public List<UUID> getOrderedMembers() {
        return members.entrySet().stream()
                .sorted((a, b) -> {
                    CustomRank ra = ranks.getOrDefault(a.getValue(),
                            ranks.getOrDefault(CustomRank.MEMBER_ID, null));
                    CustomRank rb = ranks.getOrDefault(b.getValue(),
                            ranks.getOrDefault(CustomRank.MEMBER_ID, null));
                    int permA = ra != null ? ra.getPermissions().size() : 0;
                    int permB = rb != null ? rb.getPermissions().size() : 0;
                    int p = Integer.compare(permB, permA);
                    if (p != 0) return p;
                    return a.getKey().compareTo(b.getKey());
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public boolean hasPermission(UUID playerId, ClanPermission perm) {
        if (ownerId.equals(playerId)) return true;
        CustomRank rank = getEffectiveRank(playerId);
        return rank != null && rank.hasPermission(perm);
    }

    public boolean rankHasPermission(String rankId, ClanPermission perm) {
        CustomRank rank = ranks.get(rankId);
        return rank != null && rank.hasPermission(perm);
    }

    public void setRankPermission(String rankId, ClanPermission perm, boolean enabled) {
        CustomRank rank = ranks.get(rankId);
        if (rank != null) rank.setPermission(perm, enabled);
    }

    @SuppressWarnings("unused")
    public Set<ClanPermission> getRankPermissions(String rankId) {
        CustomRank rank = ranks.get(rankId);
        if (rank == null) return EnumSet.noneOf(ClanPermission.class);
        if (CustomRank.OWNER_ID.equals(rankId)) return EnumSet.allOf(ClanPermission.class);
        return rank.getPermissions();
    }

    @SuppressWarnings("unused")
    public String getDisplayNameFor(UUID playerId) {
        CustomRank rank = getEffectiveRank(playerId);
        return rank != null ? rank.getName() : "Unbekannt";
    }

    @SuppressWarnings("unused")
    public String getColoredNameFor(UUID playerId) {
        CustomRank rank = getEffectiveRank(playerId);
        return rank != null ? rank.getColoredName() : "§7Unbekannt";
    }

    @SuppressWarnings("unused")
    public String getDisplayNameFor(String rankId) {
        CustomRank rank = ranks.get(rankId);
        return rank != null ? rank.getName() : rankId;
    }

    @SuppressWarnings("unused")
    public String getColoredNameFor(String rankId) {
        CustomRank rank = ranks.get(rankId);
        return rank != null ? rank.getColoredName() : "§7" + rankId;
    }

    public int getLevel() { return level; }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public long getXp() { return xp; }

    public void setXp(long xp) {
        this.xp = Math.max(0, xp);
    }

    public void addXp(long amount, Settings settings) {
        if (amount <= 0) return;
        this.xp += amount;
        while (level < settings.maxLevel() && xp >= settings.xpForLevel(level + 1)) {
            level++;
        }
    }

    public long getXpForNextLevel(Settings settings) {
        return settings.xpForLevel(level + 1);
    }

    public long getXpForCurrentLevel(Settings settings) {
        return settings.xpForLevel(level);
    }

    @SuppressWarnings("unused")
    public double getLevelProgress(Settings settings) {
        long currentLevelXp = getXpForCurrentLevel(settings);
        long nextLevelXp = getXpForNextLevel(settings);
        long range = nextLevelXp - currentLevelXp;
        if (range <= 0) return 1.0;
        return Math.min(1.0, (double) (xp - currentLevelXp) / range);
    }

    public int getMaxMembers(Settings settings) {
        return maxMembersOverride > 0 ? maxMembersOverride : settings.maxMembers(level);
    }

    public int getMaxMembersOverride() { return maxMembersOverride; }
    public void setMaxMembersOverride(int v) { this.maxMembersOverride = v; }

    public String getBannerColor() {
        return bannerColor != null ? bannerColor : "WHITE";
    }

    public void setBannerColor(String bannerColor) {
        this.bannerColor = bannerColor != null ? bannerColor : "WHITE";
    }

    public String getTagColor() { return tagColor != null ? tagColor : "§7"; }
    public void setTagColor(String color) { this.tagColor = color != null ? color : "§7"; }

    public Location getBase() { return base; }
    public void setBase(Location base) { this.base = base; }

    public Location getBase2() { return base2; }
    public void setBase2(Location base2) { this.base2 = base2; }

    public boolean isFriendlyFire() { return friendlyFire; }
    public void setFriendlyFire(boolean friendlyFire) { this.friendlyFire = friendlyFire; }

    public List<String> getClanLog() { return Collections.unmodifiableList(clanLog); }
    public void addLog(String entry) {
        String ts = new java.text.SimpleDateFormat("dd.MM HH:mm").format(new java.util.Date());
        clanLog.addFirst("§7[" + ts + "] §r" + entry);
        if (clanLog.size() > MAX_LOG_ENTRIES) clanLog.subList(MAX_LOG_ENTRIES, clanLog.size()).clear();
    }
    public void setClanLog(List<String> entries) { clanLog.clear(); clanLog.addAll(entries); }

    public Location getMemberWarp(UUID playerId) { return memberWarps.get(playerId); }
    public void setMemberWarp(UUID playerId, Location location) { memberWarps.put(playerId, location); }
    public Map<UUID, Location> getMemberWarps() { return Collections.unmodifiableMap(memberWarps); }

    public long getLastSeen(UUID playerId) { return memberLastSeen.getOrDefault(playerId, 0L); }
    public void setLastSeen(UUID playerId, long timestamp) { memberLastSeen.put(playerId, timestamp); }
    public Map<UUID, Long> getMemberLastSeenMap() { return Collections.unmodifiableMap(memberLastSeen); }

    public List<Pattern> getBannerPatterns() {
        return Collections.unmodifiableList(bannerPatterns);
    }

    public void addBannerPattern(Pattern pattern) {
        if (bannerPatterns.size() < MAX_BANNER_SLOTS) bannerPatterns.add(pattern);
    }

    public void removeBannerPattern(int index) {
        if (index >= 0 && index < bannerPatterns.size()) bannerPatterns.remove(index);
    }

    public void clearBannerPatterns() {
        bannerPatterns.clear();
    }

    public void setBannerPatterns(List<Pattern> patterns) {
        bannerPatterns.clear();
        bannerPatterns.addAll(patterns);
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Set<String> getAllies() {
        return Collections.unmodifiableSet(allies);
    }

    public boolean isAlly(String clanName) {
        return allies.contains(clanName.toLowerCase());
    }

    public void addAlly(String clanName) {
        allies.add(clanName.toLowerCase());
    }

    public void removeAlly(String clanName) {
        allies.remove(clanName.toLowerCase());
    }

    public Set<String> getAllyRequests() {
        return Collections.unmodifiableSet(allyRequests);
    }

    public boolean hasAllyRequest(String clanName) {
        return allyRequests.contains(clanName.toLowerCase());
    }

    public void addAllyRequest(String clanName) {
        allyRequests.add(clanName.toLowerCase());
    }

    public void removeAllyRequest(String clanName) {
        allyRequests.remove(clanName.toLowerCase());
    }

    public Map<String, CustomRank> getAllRanks() {
        return Collections.unmodifiableMap(ranks);
    }

    public void loadRanks(Map<String, CustomRank> loadedRanks) {
        ranks.clear();
        ranks.putAll(loadedRanks);
    }

    public void loadMembers(Map<UUID, String> loadedMembers) {
        members.clear();
        members.putAll(loadedMembers);
    }
}

