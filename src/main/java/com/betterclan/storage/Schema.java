package com.betterclan.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

final class Schema {

    private Schema() {}

    static void createTables(ConnectionPool db, Logger log) {
        Dialect d = db.getDialect();
        String ai = d.autoIncrement();
        String bl = d.blobType();
        String bo = d.boolType();

        String[] ddl = {

            "CREATE TABLE IF NOT EXISTS gc_clans ("
                + "id " + ai + " PRIMARY KEY, "
                + "name VARCHAR(32) NOT NULL, "
                + "name_lower VARCHAR(32) NOT NULL UNIQUE, "
                + "owner_uuid CHAR(36) NOT NULL, "
                + "level SMALLINT DEFAULT 1, "
                + "xp BIGINT DEFAULT 0, "
                + "created_at BIGINT NOT NULL, "
                + "banner_color VARCHAR(32) DEFAULT 'WHITE', "
                + "tag_color VARCHAR(64) DEFAULT '§7', "
                + "friendly_fire " + bo + " DEFAULT " + (d == Dialect.POSTGRES ? "FALSE" : "0") + ", "
                + "max_members_override INT DEFAULT -1, "
                + "base_world VARCHAR(64), base_x DOUBLE, base_y DOUBLE, base_z DOUBLE, "
                + "base_yaw REAL, base_pitch REAL, "
                + "base2_world VARCHAR(64), base2_x DOUBLE, base2_y DOUBLE, base2_z DOUBLE, "
                + "base2_yaw REAL, base2_pitch REAL"
                + ")",

            "CREATE TABLE IF NOT EXISTS gc_members ("
                + "clan_id INT NOT NULL, "
                + "player_uuid CHAR(36) NOT NULL, "
                + "rank_id VARCHAR(64) NOT NULL, "
                + "last_seen BIGINT DEFAULT 0, "
                + "PRIMARY KEY (clan_id, player_uuid)"
                + ")",

            ("CREATE INDEX IF NOT EXISTS idx_members_uuid ON gc_members(player_uuid)"),

            "CREATE TABLE IF NOT EXISTS gc_ranks ("
                + "clan_id INT NOT NULL, "
                + "rank_id VARCHAR(64) NOT NULL, "
                + "name VARCHAR(64) NOT NULL, "
                + "color VARCHAR(16) DEFAULT '§7', "
                + "deletable " + bo + " DEFAULT " + (d == Dialect.POSTGRES ? "TRUE" : "1") + ", "
                + "PRIMARY KEY (clan_id, rank_id)"
                + ")",

            "CREATE TABLE IF NOT EXISTS gc_rank_permissions ("
                + "clan_id INT NOT NULL, "
                + "rank_id VARCHAR(64) NOT NULL, "
                + "permission VARCHAR(64) NOT NULL, "
                + "PRIMARY KEY (clan_id, rank_id, permission)"
                + ")",

            "CREATE TABLE IF NOT EXISTS gc_allies ("
                + "clan_id INT NOT NULL, "
                + "ally_clan_id INT NOT NULL, "
                + "PRIMARY KEY (clan_id, ally_clan_id)"
                + ")",

            "CREATE TABLE IF NOT EXISTS gc_ally_requests ("
                + "clan_id INT NOT NULL, "
                + "from_clan VARCHAR(64) NOT NULL, "
                + "PRIMARY KEY (clan_id, from_clan)"
                + ")",

            "CREATE TABLE IF NOT EXISTS gc_member_warps ("
                + "clan_id INT NOT NULL, "
                + "player_uuid CHAR(36) NOT NULL, "
                + "world VARCHAR(64), x DOUBLE, y DOUBLE, z DOUBLE, "
                + "yaw REAL, pitch REAL, "
                + "PRIMARY KEY (clan_id, player_uuid)"
                + ")",

            "CREATE TABLE IF NOT EXISTS gc_banner_patterns ("
                + "clan_id INT NOT NULL, "
                + "pos SMALLINT NOT NULL, "
                + "pattern_type VARCHAR(64) NOT NULL, "
                + "color VARCHAR(32) NOT NULL, "
                + "PRIMARY KEY (clan_id, pos)"
                + ")",

            "CREATE TABLE IF NOT EXISTS gc_clan_log ("
                + "id " + ai + " PRIMARY KEY, "
                + "clan_id INT NOT NULL, "
                + "message TEXT NOT NULL, "
                + "created_at BIGINT NOT NULL"
                + ")",

            ("CREATE INDEX IF NOT EXISTS idx_log_clan ON gc_clan_log(clan_id, created_at DESC)"),

            "CREATE TABLE IF NOT EXISTS gc_wars ("
                + "id " + ai + " PRIMARY KEY, "
                + "clan1_name VARCHAR(32) NOT NULL, "
                + "clan2_name VARCHAR(32) NOT NULL, "
                + "clan1_kills INT DEFAULT 0, "
                + "clan2_kills INT DEFAULT 0, "
                + "target_kills INT NOT NULL, "
                + "started_at BIGINT NOT NULL, "
                + "ended_at BIGINT DEFAULT 0, "
                + "timeout_ms BIGINT NOT NULL, "
                + "forced_winner VARCHAR(32) DEFAULT NULL, "
                + "active " + bo + " DEFAULT " + (d == Dialect.POSTGRES ? "TRUE" : "1")
                + ")",

            "CREATE TABLE IF NOT EXISTS gc_war_stats ("
                + "war_id INT NOT NULL, "
                + "player_uuid CHAR(36) NOT NULL, "
                + "clan_name VARCHAR(32), "
                + "kills INT DEFAULT 0, "
                + "deaths INT DEFAULT 0, "
                + "PRIMARY KEY (war_id, player_uuid)"
                + ")",

            "CREATE TABLE IF NOT EXISTS gc_war_requests ("
                + "id " + ai + " PRIMARY KEY, "
                + "target_owner_uuid CHAR(36) NOT NULL, "
                + "clan1_name VARCHAR(32) NOT NULL, "
                + "clan2_name VARCHAR(32) NOT NULL, "
                + "target_kills INT NOT NULL, "
                + "started_at BIGINT NOT NULL, "
                + "timeout_ms BIGINT NOT NULL"
                + ")",

            "CREATE TABLE IF NOT EXISTS gc_vaults ("
                + "clan_name VARCHAR(32) NOT NULL, "
                + "slot SMALLINT NOT NULL, "
                + "item_data " + bl + ", "
                + "PRIMARY KEY (clan_name, slot)"
                + ")",

            "CREATE TABLE IF NOT EXISTS gc_player_settings ("
                + "player_uuid CHAR(36) PRIMARY KEY, "
                + "invite_blocked " + bo + " DEFAULT " + (d == Dialect.POSTGRES ? "FALSE" : "0") + ", "
                + "glow_disabled " + bo + " DEFAULT " + (d == Dialect.POSTGRES ? "FALSE" : "0") + ", "
                + "ban_expires_at BIGINT DEFAULT 0"
                + ")"
        };

        try (Connection conn = db.getConnection()) {
            var stmt = conn.createStatement();
            for (String sql : ddl) {
                stmt.execute(sql);
            }

            runSafe(stmt, "ALTER TABLE gc_wars ADD COLUMN forced_winner VARCHAR(32) DEFAULT NULL");
            runSafe(stmt, "ALTER TABLE gc_player_settings ADD COLUMN glow_disabled " + bo + " DEFAULT " + (d == Dialect.POSTGRES ? "FALSE" : "0"));
            log.info("DB schema created / verified.");
        } catch (SQLException e) {
            log.severe("Schema-Erstellung fehlgeschlagen: " + e.getMessage());
        }
    }

    private static void runSafe(java.sql.Statement stmt, String sql) {
        try { stmt.execute(sql); } catch (SQLException ignored) {}
    }
}

