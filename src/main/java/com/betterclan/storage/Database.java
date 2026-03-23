package com.betterclan.storage;

import com.betterclan.clan.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.DyeColor;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public final class Database implements Provider {

    private final ConnectionPool db;
    private final Logger log;

    public Database(ConnectionPool db, Logger log) {
        this.db = db;
        this.log = log;
        Schema.createTables(db, log);
    }

    @Override
    public void load(Manager manager) {
        manager.clearAllClans();
        try (Connection conn = db.getConnection()) {
            loadClans(conn, manager);
            loadWars(conn, manager);
            loadWarRequests(conn, manager);
            loadPlayerSettings(conn, manager);
            loadVaults(conn, manager);
            log.info("Data loaded from DB.");
        } catch (SQLException e) {
            log.severe("Fehler beim Laden: " + e.getMessage());
        }
    }

    private void loadClans(Connection conn, Manager manager) throws SQLException {

        Map<Integer, Clan> clansById = new LinkedHashMap<>();

        try (var rs = conn.createStatement().executeQuery("SELECT * FROM gc_clans")) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                UUID ownerId = UUID.fromString(rs.getString("owner_uuid"));
                Clan clan = new Clan(name, ownerId);
                clan.setLevel(rs.getInt("level"));
                clan.setXp(rs.getLong("xp"));
                clan.setCreatedAt(rs.getLong("created_at"));
                clan.setBannerColor(rs.getString("banner_color"));
                clan.setTagColor(rs.getString("tag_color"));
                clan.setFriendlyFire(rs.getBoolean("friendly_fire"));
                clan.setMaxMembersOverride(rs.getInt("max_members_override"));

                Location base = readLoc(rs, "base_");
                if (base != null) clan.setBase(base);
                Location base2 = readLoc(rs, "base2_");
                if (base2 != null) clan.setBase2(base2);

                clansById.put(id, clan);
            }
        }

        if (clansById.isEmpty()) return;

        try (var rs = conn.createStatement().executeQuery("SELECT * FROM gc_ranks")) {
            while (rs.next()) {
                int cid = rs.getInt("clan_id");
                Clan clan = clansById.get(cid);
                if (clan == null) continue;

                String rid = rs.getString("rank_id");
                String rname = rs.getString("name");
                String rcolor = rs.getString("color");
                boolean del = rs.getBoolean("deletable");
                Rank rank = new Rank(rid, rname, rcolor != null ? rcolor : "§7", del);
                clan.addCustomRank(rank);
            }
        }

        try (var rs = conn.createStatement().executeQuery("SELECT * FROM gc_rank_permissions")) {
            while (rs.next()) {
                int cid = rs.getInt("clan_id");
                Clan clan = clansById.get(cid);
                if (clan == null) continue;
                String rid = rs.getString("rank_id");
                Rank rank = clan.getRank(rid);
                if (rank == null) continue;
                try {
                    Permission perm = Permission.valueOf(rs.getString("permission"));
                    rank.setPermission(perm, true);
                } catch (IllegalArgumentException e) {
                    log.warning("Unbekannte Berechtigung in DB ignoriert: " + e.getMessage());
                }
            }
        }

        try (var rs = conn.createStatement().executeQuery("SELECT * FROM gc_members")) {
            while (rs.next()) {
                int cid = rs.getInt("clan_id");
                Clan clan = clansById.get(cid);
                if (clan == null) continue;
                UUID pid = UUID.fromString(rs.getString("player_uuid"));
                clan.addMember(pid, rs.getString("rank_id"));
                long ls = rs.getLong("last_seen");
                if (ls > 0) clan.setLastSeen(pid, ls);
            }
        }

        try (var rs = conn.createStatement().executeQuery("SELECT * FROM gc_member_warps")) {
            while (rs.next()) {
                int cid = rs.getInt("clan_id");
                Clan clan = clansById.get(cid);
                if (clan == null) continue;
                UUID pid = UUID.fromString(rs.getString("player_uuid"));
                Location loc = readLocDirect(rs);
                if (loc != null) clan.setMemberWarp(pid, loc);
            }
        }

        try (var rs = conn.createStatement().executeQuery(
                "SELECT a.clan_id, c.name_lower FROM gc_allies a JOIN gc_clans c ON c.id = a.ally_clan_id")) {
            while (rs.next()) {
                int cid = rs.getInt("clan_id");
                Clan clan = clansById.get(cid);
                if (clan == null) continue;
                clan.addAlly(rs.getString("name_lower"));
            }
        }

        try (var rs = conn.createStatement().executeQuery("SELECT * FROM gc_ally_requests")) {
            while (rs.next()) {
                int cid = rs.getInt("clan_id");
                Clan clan = clansById.get(cid);
                if (clan == null) continue;
                clan.addAllyRequest(rs.getString("from_clan"));
            }
        }

        try (var rs = conn.createStatement().executeQuery("SELECT * FROM gc_banner_patterns ORDER BY pos")) {
            while (rs.next()) {
                int cid = rs.getInt("clan_id");
                Clan clan = clansById.get(cid);
                if (clan == null) continue;
                try {
                    PatternType pt = RegistryAccess.registryAccess()
                            .getRegistry(RegistryKey.BANNER_PATTERN)
                            .get(NamespacedKey.minecraft(rs.getString("pattern_type").toLowerCase(java.util.Locale.ROOT)));
                    DyeColor dc = DyeColor.valueOf(rs.getString("color"));
                    if (pt != null) clan.addBannerPattern(new Pattern(dc, pt));
                } catch (Exception e) {
                    log.warning("Banner-Pattern konnte nicht geladen werden: " + e.getMessage());
                }
            }
        }

        try (var rs = conn.createStatement().executeQuery(
                "SELECT * FROM gc_clan_log ORDER BY created_at DESC")) {

            Map<Integer, List<String>> logMap = new HashMap<>();
            while (rs.next()) {
                int cid = rs.getInt("clan_id");
                logMap.computeIfAbsent(cid, k -> new ArrayList<>()).add(rs.getString("message"));
            }
            for (var entry : logMap.entrySet()) {
                Clan clan = clansById.get(entry.getKey());
                if (clan != null) clan.setClanLog(entry.getValue());
            }
        }

        for (Clan clan : clansById.values()) {
            manager.registerClan(clan);
        }
    }

    private void loadWars(Connection conn, Manager manager) throws SQLException {
        List<War> wars = new ArrayList<>();
        Map<Integer, War> warsById = new HashMap<>();

        try (var rs = conn.createStatement().executeQuery("SELECT * FROM gc_wars")) {
            while (rs.next()) {
                int warId = rs.getInt("id");
                long timeout = rs.getLong("timeout_ms");
                if (timeout <= 0) timeout = War.DEFAULT_TIMEOUT_MS;
                War war = new War(rs.getString("clan1_name"), rs.getString("clan2_name"),
                        rs.getInt("target_kills"), timeout);
                war.setClan1Kills(rs.getInt("clan1_kills"));
                war.setClan2Kills(rs.getInt("clan2_kills"));
                long endedAt = rs.getLong("ended_at");
                if (endedAt > 0) war.setEndedAt(endedAt);
                if (!rs.getBoolean("active")) war.end();
                String fw = rs.getString("forced_winner");
                if (fw != null && !fw.isEmpty()) war.setForcedWinner(fw);
                warsById.put(warId, war);
                wars.add(war);
            }
        }

        try (var rs = conn.createStatement().executeQuery("SELECT * FROM gc_war_stats")) {
            while (rs.next()) {
                War war = warsById.get(rs.getInt("war_id"));
                if (war == null) continue;
                UUID pid = UUID.fromString(rs.getString("player_uuid"));
                int kills = rs.getInt("kills");
                int deaths = rs.getInt("deaths");
                String cname = rs.getString("clan_name");
                if (kills > 0) {
                    Map<UUID, Integer> kbp = new HashMap<>(war.getKillsByPlayer());
                    kbp.put(pid, kills);
                    war.setKillsByPlayer(kbp);
                }
                if (deaths > 0) {
                    Map<UUID, Integer> dbp = new HashMap<>(war.getDeathsByPlayer());
                    dbp.put(pid, deaths);
                    war.setDeathsByPlayer(dbp);
                }
                if (cname != null) {
                    Map<UUID, String> pc = new HashMap<>(war.getPlayerClans());
                    pc.put(pid, cname);
                    war.setPlayerClans(pc);
                }
            }
        }

        manager.setActiveWars(wars);
    }

    private void loadWarRequests(Connection conn, Manager manager) throws SQLException {
        Map<UUID, List<War>> requests = new HashMap<>();
        try (var rs = conn.createStatement().executeQuery("SELECT * FROM gc_war_requests")) {
            while (rs.next()) {
                UUID targetOwner = UUID.fromString(rs.getString("target_owner_uuid"));
                long timeout = rs.getLong("timeout_ms");
                if (timeout <= 0) timeout = War.DEFAULT_TIMEOUT_MS;
                War war = new War(rs.getString("clan1_name"), rs.getString("clan2_name"),
                        rs.getInt("target_kills"), timeout);
                requests.computeIfAbsent(targetOwner, k -> new ArrayList<>()).add(war);
            }
        }
        manager.setAllWarRequests(requests);
    }

    private void loadPlayerSettings(Connection conn, Manager manager) throws SQLException {
        Set<UUID> blocked = new HashSet<>();
        Set<UUID> glowOff = new HashSet<>();
        Map<UUID, Long> bans = new HashMap<>();

        try (var rs = conn.createStatement().executeQuery("SELECT * FROM gc_player_settings")) {
            while (rs.next()) {
                UUID pid = UUID.fromString(rs.getString("player_uuid"));
                if (rs.getBoolean("invite_blocked")) blocked.add(pid);
                try { if (rs.getBoolean("glow_disabled")) glowOff.add(pid); } catch (SQLException ignored) {}
                long ban = rs.getLong("ban_expires_at");
                if (ban != 0) bans.put(pid, ban);
            }
        }
        manager.setInviteBlocked(blocked);
        manager.setGlowDisabled(glowOff);
        manager.setClanBans(bans);
    }

    private void loadVaults(Connection conn, Manager manager) throws SQLException {

        Map<String, ItemStack[]> vaults = new HashMap<>();
        try (var rs = conn.createStatement().executeQuery("SELECT * FROM gc_vaults WHERE item_data IS NOT NULL")) {
            while (rs.next()) {
                String cn = rs.getString("clan_name").toLowerCase();
                int slot = rs.getShort("slot");
                byte[] data = rs.getBytes("item_data");
                if (data == null || data.length == 0) continue;
                vaults.computeIfAbsent(cn, k -> new ItemStack[54]);
                try {
                    vaults.get(cn)[slot] = ItemStack.deserializeBytes(data);
                } catch (Exception e) {
                    log.warning("Vault-Item Slot " + slot + " für '" + cn + "' konnte nicht deserialisiert werden: " + e.getMessage());
                }
            }
        }
        for (var entry : vaults.entrySet()) {
            manager.setVaultContents(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void save(Manager manager) {
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                saveClans(conn, manager);
                saveWars(conn, manager);
                saveWarRequests(conn, manager);
                savePlayerSettings(conn, manager);
                saveVaults(conn, manager);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.severe("DB-Speichern fehlgeschlagen: " + e.getMessage());
        }
    }

    private void saveClans(Connection conn, Manager manager) throws SQLException {

        exec(conn, "DELETE FROM gc_clan_log");
        exec(conn, "DELETE FROM gc_banner_patterns");
        exec(conn, "DELETE FROM gc_member_warps");
        exec(conn, "DELETE FROM gc_ally_requests");
        exec(conn, "DELETE FROM gc_allies");
        exec(conn, "DELETE FROM gc_rank_permissions");
        exec(conn, "DELETE FROM gc_ranks");
        exec(conn, "DELETE FROM gc_members");
        exec(conn, "DELETE FROM gc_clans");

        for (Clan clan : manager.getAllClans()) {

            int clanId;
            try (var ps = conn.prepareStatement(
                    "INSERT INTO gc_clans (name, name_lower, owner_uuid, level, xp, created_at, "
                    + "banner_color, tag_color, friendly_fire, max_members_override, "
                    + "base_world, base_x, base_y, base_z, base_yaw, base_pitch, "
                    + "base2_world, base2_x, base2_y, base2_z, base2_yaw, base2_pitch) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, clan.getName());
                ps.setString(2, clan.getName().toLowerCase());
                ps.setString(3, clan.getOwnerId().toString());
                ps.setInt(4, clan.getLevel());
                ps.setLong(5, clan.getXp());
                ps.setLong(6, clan.getCreatedAt());
                ps.setString(7, clan.getBannerColor());
                ps.setString(8, clan.getTagColor());
                ps.setBoolean(9, clan.isFriendlyFire());
                ps.setInt(10, clan.getMaxMembersOverride());
                writeLoc(ps, 11, clan.getBase());
                writeLoc(ps, 17, clan.getBase2());
                ps.executeUpdate();

                try (var keys = ps.getGeneratedKeys()) {
                    keys.next();
                    clanId = keys.getInt(1);
                }
            }

            try (var ps = conn.prepareStatement(
                    "INSERT INTO gc_members (clan_id, player_uuid, rank_id, last_seen) VALUES (?,?,?,?)")) {
                for (var entry : clan.getMembers().entrySet()) {
                    ps.setInt(1, clanId);
                    ps.setString(2, entry.getKey().toString());
                    ps.setString(3, entry.getValue());
                    ps.setLong(4, clan.getLastSeen(entry.getKey()));
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (var ps = conn.prepareStatement(
                    "INSERT INTO gc_ranks (clan_id, rank_id, name, color, deletable) VALUES (?,?,?,?,?)")) {
                for (Rank rank : clan.getAllRanks().values()) {
                    ps.setInt(1, clanId);
                    ps.setString(2, rank.getId());
                    ps.setString(3, rank.getName());
                    ps.setString(4, rank.getColor());
                    ps.setBoolean(5, rank.isDeletable());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (var ps = conn.prepareStatement(
                    "INSERT INTO gc_rank_permissions (clan_id, rank_id, permission) VALUES (?,?,?)")) {
                for (Rank rank : clan.getAllRanks().values()) {
                    for (Permission perm : rank.getPermissions()) {
                        ps.setInt(1, clanId);
                        ps.setString(2, rank.getId());
                        ps.setString(3, perm.name());
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }

            try (var ps = conn.prepareStatement(
                    "INSERT INTO gc_allies (clan_id, ally_clan_id) "
                    + "SELECT ?, id FROM gc_clans WHERE name_lower = ?")) {
                for (String allyName : clan.getAllies()) {
                    ps.setInt(1, clanId);
                    ps.setString(2, allyName.toLowerCase());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (var ps = conn.prepareStatement(
                    "INSERT INTO gc_ally_requests (clan_id, from_clan) VALUES (?,?)")) {
                for (String req : clan.getAllyRequests()) {
                    ps.setInt(1, clanId);
                    ps.setString(2, req);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (var ps = conn.prepareStatement(
                    "INSERT INTO gc_member_warps (clan_id, player_uuid, world, x, y, z, yaw, pitch) "
                    + "VALUES (?,?,?,?,?,?,?,?)")) {
                for (var entry : clan.getMemberWarps().entrySet()) {
                    ps.setInt(1, clanId);
                    ps.setString(2, entry.getKey().toString());
                    Location loc = entry.getValue();
                    ps.setString(3, loc.getWorld() != null ? loc.getWorld().getName() : "world");
                    ps.setDouble(4, loc.getX());
                    ps.setDouble(5, loc.getY());
                    ps.setDouble(6, loc.getZ());
                    ps.setFloat(7, loc.getYaw());
                    ps.setFloat(8, loc.getPitch());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (var ps = conn.prepareStatement(
                    "INSERT INTO gc_banner_patterns (clan_id, pos, pattern_type, color) VALUES (?,?,?,?)")) {
                List<Pattern> patterns = clan.getBannerPatterns();
                for (int i = 0; i < patterns.size(); i++) {
                    Pattern pattern = patterns.get(i);

                    ps.setInt(1, clanId);
                    ps.setInt(2, i);

                    NamespacedKey nk = RegistryAccess.registryAccess()
                            .getRegistry(RegistryKey.BANNER_PATTERN).getKey(pattern.getPattern());

                    ps.setString(3, nk != null ? nk.getKey() : "base");
                    ps.setString(4, pattern.getColor().name());

                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (var ps = conn.prepareStatement(
                    "INSERT INTO gc_clan_log (clan_id, message, created_at) VALUES (?,?,?)")) {
                List<String> logEntries = clan.getClanLog();
                long now = System.currentTimeMillis();
                for (int i = 0; i < logEntries.size(); i++) {
                    ps.setInt(1, clanId);
                    ps.setString(2, logEntries.get(i));

                    ps.setLong(3, now - i);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    private void saveWars(Connection conn, Manager manager) throws SQLException {
        exec(conn, "DELETE FROM gc_war_stats");
        exec(conn, "DELETE FROM gc_wars");

        for (War war : manager.getAllWarsForSave()) {
            int warId;
            try (var ps = conn.prepareStatement(
                    "INSERT INTO gc_wars (clan1_name, clan2_name, clan1_kills, clan2_kills, "
                    + "target_kills, started_at, ended_at, timeout_ms, forced_winner, active) VALUES (?,?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, war.getClan1Name());
                ps.setString(2, war.getClan2Name());
                ps.setInt(3, war.getClan1Kills());
                ps.setInt(4, war.getClan2Kills());
                ps.setInt(5, war.getTargetKills());
                ps.setLong(6, war.getStartedAt());
                ps.setLong(7, war.getEndedAt());
                ps.setLong(8, war.getTimeoutMs());
                ps.setString(9, war.getForcedWinner());
                ps.setBoolean(10, war.isActive());
                ps.executeUpdate();
                try (var keys = ps.getGeneratedKeys()) {
                    keys.next();
                    warId = keys.getInt(1);
                }
            }

            Set<UUID> allPlayers = new HashSet<>();
            allPlayers.addAll(war.getKillsByPlayer().keySet());
            allPlayers.addAll(war.getDeathsByPlayer().keySet());
            allPlayers.addAll(war.getPlayerClans().keySet());

            if (!allPlayers.isEmpty()) {
                try (var ps = conn.prepareStatement(
                        "INSERT INTO gc_war_stats (war_id, player_uuid, clan_name, kills, deaths) VALUES (?,?,?,?,?)")) {
                    for (UUID pid : allPlayers) {
                        ps.setInt(1, warId);
                        ps.setString(2, pid.toString());
                        ps.setString(3, war.getPlayerClans().getOrDefault(pid, null));
                        ps.setInt(4, war.getKillsByPlayer().getOrDefault(pid, 0));
                        ps.setInt(5, war.getDeathsByPlayer().getOrDefault(pid, 0));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }
        }
    }

    private void saveWarRequests(Connection conn, Manager manager) throws SQLException {
        exec(conn, "DELETE FROM gc_war_requests");
        try (var ps = conn.prepareStatement(
                "INSERT INTO gc_war_requests (target_owner_uuid, clan1_name, clan2_name, target_kills, started_at, timeout_ms) "
                + "VALUES (?,?,?,?,?,?)")) {
            for (var entry : manager.getAllWarRequests().entrySet()) {
                for (War war : entry.getValue()) {
                    ps.setString(1, entry.getKey().toString());
                    ps.setString(2, war.getClan1Name());
                    ps.setString(3, war.getClan2Name());
                    ps.setInt(4, war.getTargetKills());
                    ps.setLong(5, war.getStartedAt());
                    ps.setLong(6, war.getTimeoutMs());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    private void savePlayerSettings(Connection conn, Manager manager) throws SQLException {
        exec(conn, "DELETE FROM gc_player_settings");

        Set<UUID> allPlayers = new HashSet<>(manager.getInviteBlocked());
        allPlayers.addAll(manager.getClanBans().keySet());
        allPlayers.addAll(manager.getGlowDisabled());

        if (allPlayers.isEmpty()) return;

        try (var ps = conn.prepareStatement(
                "INSERT INTO gc_player_settings (player_uuid, invite_blocked, glow_disabled, ban_expires_at) VALUES (?,?,?,?)")) {
            for (UUID pid : allPlayers) {
                ps.setString(1, pid.toString());
                ps.setBoolean(2, manager.getInviteBlocked().contains(pid));
                ps.setBoolean(3, manager.getGlowDisabled().contains(pid));
                ps.setLong(4, manager.getClanBans().getOrDefault(pid, 0L));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void saveVaults(Connection conn, Manager manager) throws SQLException {
        exec(conn, "DELETE FROM gc_vaults");

        try (var ps = conn.prepareStatement(
                "INSERT INTO gc_vaults (clan_name, slot, item_data) VALUES (?,?,?)")) {
            for (var entry : manager.getAllVaults().entrySet()) {
                ItemStack[] items = entry.getValue();
                for (int i = 0; i < items.length; i++) {
                    if (items[i] != null && items[i].getType() != Material.AIR) {
                        ps.setString(1, entry.getKey());
                        ps.setInt(2, i);
                        try {
                            ps.setBytes(3, items[i].serializeAsBytes());
                        } catch (Exception e) {
                            continue;
                        }
                        ps.addBatch();
                    }
                }
            }
            ps.executeBatch();
        }
    }

    @Override
    public void shutdown() {
        db.shutdown();
    }

    private static void exec(Connection conn, String sql) throws SQLException {
        try (var stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private static Location readLoc(ResultSet rs, String prefix) throws SQLException {
        String wn = rs.getString(prefix + "world");
        if (wn == null) return null;
        World w = Bukkit.getWorld(wn);
        if (w == null) return null;
        return new Location(w, rs.getDouble(prefix + "x"), rs.getDouble(prefix + "y"),
                rs.getDouble(prefix + "z"), rs.getFloat(prefix + "yaw"), rs.getFloat(prefix + "pitch"));
    }

    private static Location readLocDirect(ResultSet rs) throws SQLException {
        String wn = rs.getString("world");
        if (wn == null) return null;
        World w = Bukkit.getWorld(wn);
        if (w == null) return null;
        return new Location(w, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                rs.getFloat("yaw"), rs.getFloat("pitch"));
    }

    private static void writeLoc(PreparedStatement ps, int startIdx, Location loc) throws SQLException {
        if (loc == null) {
            for (int i = 0; i < 6; i++) ps.setNull(startIdx + i, Types.NULL);
            return;
        }
        ps.setString(startIdx, loc.getWorld() != null ? loc.getWorld().getName() : "world");
        ps.setDouble(startIdx + 1, loc.getX());
        ps.setDouble(startIdx + 2, loc.getY());
        ps.setDouble(startIdx + 3, loc.getZ());
        ps.setFloat(startIdx + 4, loc.getYaw());
        ps.setFloat(startIdx + 5, loc.getPitch());
    }
}

