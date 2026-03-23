package com.betterclan.clan;

import org.bukkit.configuration.file.FileConfiguration;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record Settings(
        int baseMaxMembers,
        int membersPerLevel,
        int maxLevel,
        int autoSaveMinutes,
        boolean showClanTag,
        int warTimeoutDays,
        int warCooldownMinutes,
        int warTargetKills,
        int warMinClanSize,
        int inviteExpiryHours,
        int vaultLevel1,
        int vaultLevel2,
        int vaultLevel3,
        int vaultLevel4,
        int vaultLevel5,
        int vaultLevel6,
        int teleportCountdownSeconds,
        int bannerMaxPatterns,
        int maxLogEntries,
        int maxAllies,

        int dynamicColorsMinLevel,
        int secondBaseMinLevel,

        int warXpReward,

        List<Long> levelXpThresholds,

        int rankMaxTotal,

        boolean defaultFriendlyFire,
        Set<Permission> defaultMemberPermissions,

        int chatInputTimeoutSeconds,
        int chatInputTitleFadeInMs,
        int chatInputTitleStaySeconds,
        int chatInputTitleFadeOutMs,

        String storageType,
        String dbDialect,
        String dbHost,
        int dbPort,
        String dbDatabase,
        String dbUsername,
        String dbPassword,
        int dbPoolSize
) {

    public static final int CLAN_MIN_NAME_LENGTH = 3;
    public static final int CLAN_MAX_NAME_LENGTH = 24;
    public static final int RANK_MIN_NAME_LENGTH = 2;
    public static final int RANK_MAX_NAME_LENGTH = 20;

    public static Settings load(FileConfiguration cfg) {
        List<?> rawThresholds = cfg.getList("xp.level-thresholds");
        List<Long> thresholds;
        if (rawThresholds != null && !rawThresholds.isEmpty()) {
            thresholds = rawThresholds.stream()
                    .filter(o -> o instanceof Number)
                    .map(o -> ((Number) o).longValue())
                    .collect(Collectors.toList());
        } else {
            thresholds = List.of(1L, 4L, 8L, 12L, 16L);
        }

        return new Settings(
                cfg.getInt("clan.base-max-members", 10),
                cfg.getInt("clan.members-per-level", 2),
                cfg.getInt("clan.max-level", 6),
                cfg.getInt("storage.database.auto-save-interval", 10),
                cfg.getBoolean("chat.show-tag", true),
                cfg.getInt("war.timeout-days", 7),
                cfg.getInt("war.cooldown-minutes", 60),
                cfg.getInt("war.target-kills", 20),
                cfg.getInt("war.min-clan-size", 1),
                cfg.getInt("invites.expiry-hours", 48),
                cfg.getInt("vault.level-1-size", 9),
                cfg.getInt("vault.level-2-size", 18),
                cfg.getInt("vault.level-3-size", 27),
                cfg.getInt("vault.level-4-size", 36),
                cfg.getInt("vault.level-5-size", 45),
                cfg.getInt("vault.level-6-size", 54),
                cfg.getInt("teleport.countdown-seconds", 5),
                cfg.getInt("clan.banner-max-patterns", 6),
                cfg.getInt("log.max-entries", 200),
                cfg.getInt("clan.max-allies", 5),
                cfg.getInt("features.dynamic-colors-min-level", 3),
                cfg.getInt("features.second-base-min-level", 4),
                cfg.getInt("xp.war-win-reward", 1),
                thresholds,
                cfg.getInt("ranks.max-total-ranks", 28),
                cfg.getBoolean("clan.default-friendly-fire", false),
                loadPermissionSet(cfg.getStringList("permissions.member-defaults"), EnumSet.of(
                        Permission.VAULT, Permission.VIEW_LOG, Permission.TELEPORT,
                        Permission.CHAT_ALLY, Permission.CLAN_CHAT
                )),
                cfg.getInt("chat.input.timeout-seconds", 30),
                cfg.getInt("chat.input.title-fade-in-ms", 200),
                cfg.getInt("chat.input.title-stay-seconds", 30),
                cfg.getInt("chat.input.title-fade-out-ms", 500),
                cfg.getString("storage.type", "json"),
                cfg.getString("storage.database.dialect", "mariadb"),
                cfg.getString("storage.database.host", "localhost"),
                cfg.getInt("storage.database.port", 3306),
                cfg.getString("storage.database.database", "betterclan"),
                cfg.getString("storage.database.username", "betterclan"),
                cfg.getString("storage.database.password", "changeme"),
                cfg.getInt("storage.database.pool-size", 5)
        );
    }

    private static Set<Permission> loadPermissionSet(List<String> list, Set<Permission> fallback) {
        if (list == null || list.isEmpty()) return fallback;
        Set<Permission> result = EnumSet.noneOf(Permission.class);
        for (String s : list) {
            try { result.add(Permission.valueOf(s.toUpperCase())); } catch (IllegalArgumentException ignored) {}
        }
        return result.isEmpty() ? fallback : result;
    }

    public int maxMembers(int level) {
        return baseMaxMembers + (level - 1) * membersPerLevel;
    }

    public long xpForLevel(int level) {
        if (level <= 1) return 0;
        int idx = level - 2;
        if (idx < levelXpThresholds.size()) return levelXpThresholds.get(idx);

        long last = levelXpThresholds.isEmpty() ? 0 : levelXpThresholds.getLast();
        return last + (long) (idx - levelXpThresholds.size() + 1) * 4;
    }

    @SuppressWarnings("unused")
    public long warTimeoutMs()   { return warTimeoutDays * 24L * 3_600_000; }
    public long warCooldownMs()  { return warCooldownMinutes * 60_000L; }
    public Duration inviteExpiry() { return Duration.ofHours(inviteExpiryHours); }

    public boolean isDatabase() { return "database".equalsIgnoreCase(storageType); }

    public int getVaultSize(int level) {
        return switch (level) {
            case 1 -> vaultLevel1;
            case 2 -> vaultLevel2;
            case 3 -> vaultLevel3;
            case 4 -> vaultLevel4;
            case 5 -> vaultLevel5;
            default -> vaultLevel6;
        };
    }
}

