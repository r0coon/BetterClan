package com.betterclan.clan;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ClanWar {

    public static final long DEFAULT_TIMEOUT_MS = TimeUnit.DAYS.toMillis(7);

    private final String clan1Name;
    private final String clan2Name;
    private int clan1Kills;
    private int clan2Kills;
    private final int targetKills;
    private final long startedAt;
    private long endedAt;
    private boolean active;
    private long timeoutMs;
    private final Map<UUID, Integer> killsByPlayer = new HashMap<>();
    private final Map<UUID, Integer> deathsByPlayer = new HashMap<>();

    private final Map<UUID, String> playerClans = new HashMap<>();

    private String forcedWinner = null;

    public ClanWar(String clan1Name, String clan2Name, int targetKills) {
        this(clan1Name, clan2Name, targetKills, DEFAULT_TIMEOUT_MS);
    }

    public ClanWar(String clan1Name, String clan2Name, int targetKills, long timeoutMs) {
        this.clan1Name = clan1Name;
        this.clan2Name = clan2Name;
        this.clan1Kills = 0;
        this.clan2Kills = 0;
        this.targetKills = Math.max(1, targetKills);
        this.startedAt = System.currentTimeMillis();
        this.endedAt = 0;
        this.active = true;
        this.timeoutMs = timeoutMs;
    }

    public String getClan1Name() { return clan1Name; }
    public String getClan2Name() { return clan2Name; }
    public int getClan1Kills() { return clan1Kills; }
    public int getClan2Kills() { return clan2Kills; }
    public int getTargetKills() { return targetKills; }
    public long getStartedAt() { return startedAt; }
    public long getEndedAt() { return endedAt; }
    public boolean isActive() { return active; }
    public long getTimeoutMs() { return timeoutMs; }
    public String getForcedWinner() { return forcedWinner; }
    public void setForcedWinner(String winner) { this.forcedWinner = winner; }

    @SuppressWarnings("unused")
    public void addKill(String clanName) {
        addKill(clanName, null);
    }

    public void addKill(String clanName, UUID killer) {
        if (!active) return;
        if (clanName.equalsIgnoreCase(clan1Name)) {
            clan1Kills++;
        } else if (clanName.equalsIgnoreCase(clan2Name)) {
            clan2Kills++;
        } else return;
        if (killer != null) {
            killsByPlayer.merge(killer, 1, Integer::sum);
            playerClans.putIfAbsent(killer, clanName);
        }
    }

    public void addDeath(String victimClanName, UUID victim) {
        if (victim == null) return;
        deathsByPlayer.merge(victim, 1, Integer::sum);
        playerClans.putIfAbsent(victim, victimClanName);
    }

    public Map<UUID, Integer> getKillsByPlayer() { return killsByPlayer; }
    public Map<UUID, Integer> getDeathsByPlayer() { return deathsByPlayer; }
    public Map<UUID, String> getPlayerClans() { return playerClans; }

    public boolean isFinished() {
        return clan1Kills >= targetKills || clan2Kills >= targetKills;
    }

    public boolean isTimedOut() {
        if (!active) return false;
        return System.currentTimeMillis() - startedAt > timeoutMs;
    }

    public boolean isDraw() {
        if (forcedWinner != null) return false;

        if (clan1Kills >= targetKills && clan2Kills >= targetKills) return true;

        return !active && clan1Kills == clan2Kills;
    }

    public String getWinnerName() {
        if (forcedWinner != null) return forcedWinner;
        if (isDraw()) return null;

        if (clan1Kills >= targetKills && clan2Kills < targetKills) return clan1Name;
        if (clan2Kills >= targetKills && clan1Kills < targetKills) return clan2Name;

        if (!active || isTimedOut()) {
            if (clan1Kills > clan2Kills) return clan1Name;
            if (clan2Kills > clan1Kills) return clan2Name;
        }

        return null;
    }

    public String getLoserName() {
        String winner = getWinnerName();
        if (winner == null) return null;
        return winner.equalsIgnoreCase(clan1Name) ? clan2Name : clan1Name;
    }

    public int getKillsFor(String clanName) {
        if (clanName.equalsIgnoreCase(clan1Name)) return clan1Kills;
        if (clanName.equalsIgnoreCase(clan2Name)) return clan2Kills;
        return 0;
    }

    public boolean involves(String clanName) {
        return clanName.equalsIgnoreCase(clan1Name) || clanName.equalsIgnoreCase(clan2Name);
    }

    public String getOpponent(String clanName) {
        if (clanName.equalsIgnoreCase(clan1Name)) return clan2Name;
        if (clanName.equalsIgnoreCase(clan2Name)) return clan1Name;
        return null;
    }

    public void end() {
        this.active = false;
        this.endedAt = System.currentTimeMillis();
    }

    public long getRemainingTimeMs() {
        if (!active) return 0;
        long elapsed = System.currentTimeMillis() - startedAt;
        return Math.max(0, timeoutMs - elapsed);
    }

    public String getRemainingTimeFormatted() {
        long remaining = getRemainingTimeMs();
        if (remaining <= 0) return "Abgelaufen";

        long days = TimeUnit.MILLISECONDS.toDays(remaining);
        remaining -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(remaining);
        remaining -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining);

        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    public void setClan1Kills(int k) { this.clan1Kills = Math.max(0, k); }
    public void setClan2Kills(int k) { this.clan2Kills = Math.max(0, k); }
    @SuppressWarnings("unused") public void setActive(boolean a) { this.active = a; }
    public void setEndedAt(long endedAt) { this.endedAt = endedAt; }
    @SuppressWarnings("unused") public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
    public void setKillsByPlayer(Map<UUID, Integer> map) { killsByPlayer.clear(); if (map != null) killsByPlayer.putAll(map); }
    public void setDeathsByPlayer(Map<UUID, Integer> map) { deathsByPlayer.clear(); if (map != null) deathsByPlayer.putAll(map); }
    public void setPlayerClans(Map<UUID, String> map) { playerClans.clear(); if (map != null) playerClans.putAll(map); }
}

