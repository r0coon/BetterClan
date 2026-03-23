package com.betterclan.command;

import com.betterclan.BetterClan;
import com.betterclan.clan.Clan;
import com.betterclan.clan.Manager;
import com.betterclan.clan.War;
import com.betterclan.clan.Rank;
import com.betterclan.gui.admin.*;
import com.betterclan.gui.clan.*;
import com.betterclan.gui.invite.*;
import com.betterclan.gui.member.*;
import com.betterclan.gui.misc.*;
import com.betterclan.gui.rank.*;
import com.betterclan.gui.tag.*;
import com.betterclan.gui.war.*;
import com.betterclan.gui.warlog.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class Dev {

    public static final List<String> MENUS = List.of(
            "main",
            "members",
            "invite-list",
            "invite-players",
            "settings",
            "clan-list",
            "clan-list-war",
            "clan-info",
            "allies",
            "ally-list",
            "ally-shields",
            "ally-requests",
            "banner-color",
            "banner-pattern-list",
            "banner-pattern-type",
            "banner-pattern-color",
            "war-menu",
            "war-requests",
            "war-leaderboard",
            "war-history",
            "war-detail",
            "war-past-leaderboard",
            "tag-color",
            "tag-color-static",
            "tag-color-dynamic",
            "rank-management",
            "rank-action",
            "role-permissions",
            "member-action",
            "member-rank-picker",
            "warp-list",
            "clan-log",
            "admin-clan-list",
            "admin-clan-menu",
            "admin-player-menu",
            "admin-ally-remove",
            "confirm"
    );

    private Dev() {
    }

    public static boolean openMenuForPlayer(BetterClan plugin, Player player, String rawName) {
        String name = rawName.toLowerCase(Locale.ROOT);
        Manager manager = plugin.getClanManager();

        Clan playerClan = manager.getClan(player.getUniqueId());
        Clan anyClan = anyClan(manager, playerClan);
        Clan targetClan = otherClan(manager, playerClan, anyClan);
        UUID memberTarget = memberTarget(playerClan);
        Rank rankTarget = rankTarget(playerClan);
        War warTarget = warTarget(manager, playerClan, anyClan);
        UUID adminPlayerTarget = adminPlayerTarget(targetClan, player);

        switch (name) {
            case "main" -> Main.open(manager, player);
            case "members" -> {
                if (playerClan == null) return false;
                Members.open(plugin, manager, player, 1);
            }
            case "invite-list" -> InviteList.open(manager, player);
            case "invite-players" -> {
                if (playerClan == null) return false;
                Players.open(plugin, manager, player, 1);
            }
            case "settings" -> {
                if (playerClan == null) return false;
                SettingsMenu.open(manager, plugin.getChatInput(), player);
            }
            case "clan-list" -> ClanList.open(manager, player, 1);
            case "clan-list-war" -> ClanList.openForWar(manager, player, 1);
            case "clan-info" -> {
                if (targetClan == null) return false;
                ClanList.openInfo(manager, player, targetClan, manager.getSettings().warTargetKills());
            }
            case "allies" -> {
                if (playerClan == null) return false;
                Ally.open(manager, player);
            }
            case "ally-list" -> {
                if (playerClan == null) return false;
                Ally.openList(manager, player, 1);
            }
            case "ally-shields" -> {
                if (playerClan == null) return false;
                Ally.openShields(manager, player);
            }
            case "ally-requests" -> {
                if (playerClan == null) return false;
                Ally.openRequests(manager, player, 1);
            }
            case "banner-color" -> {
                if (playerClan == null) return false;
                BannerColor.open(manager, player);
            }
            case "banner-pattern-list" -> {
                if (playerClan == null) return false;
                BannerPattern.openPatternList(manager, player);
            }
            case "banner-pattern-type" -> {
                if (playerClan == null) return false;
                BannerPattern.openPatternTypePicker(manager, player, 1);
            }
            case "banner-pattern-color" -> {
                if (playerClan == null) return false;
                BannerPattern.openPatternColorPicker(manager, player, "border");
            }
            case "war-menu" -> {
                if (playerClan == null) return false;
                Overview.open(manager, player);
            }
            case "war-requests" -> {
                if (playerClan == null) return false;
                Requests.open(manager, player);
            }
            case "war-leaderboard" -> {
                if (playerClan == null) return false;
                Leaderboard.open(manager, player, 1);
            }
            case "war-history" -> {
                if (playerClan == null) return false;
                History.open(manager, player, 1);
            }
            case "war-detail" -> {
                if (warTarget == null) return false;
                PartySelect.open(manager, player, warTarget);
            }
            case "war-past-leaderboard" -> {
                if (warTarget == null) return false;
                String clanName = warTarget.getClan1Name();
                PastLeaderboard.open(player, warTarget, clanName);
            }
            case "tag-color" -> {
                if (playerClan == null) return false;
                Picker.open(manager, player);
            }
            case "tag-color-static" -> {
                if (playerClan == null) return false;
                Static.open(manager, player);
            }
            case "tag-color-dynamic" -> {
                if (playerClan == null) return false;
                Dynamic.open(manager, player);
            }
            case "rank-management" -> {
                if (playerClan == null) return false;
                Management.openForAdmin(manager, playerClan, player);
            }
            case "rank-action" -> {
                if (playerClan == null || rankTarget == null) return false;
                RankActions.open(manager, player, rankTarget.getId());
            }
            case "role-permissions" -> {
                if (playerClan == null || rankTarget == null) return false;
                Permissions.openPermissions(manager, player, rankTarget.getId());
            }
            case "member-action" -> {
                if (playerClan == null || memberTarget == null) return false;
                MemberActions.open(manager, player, memberTarget);
            }
            case "member-rank-picker" -> {
                if (playerClan == null || memberTarget == null) return false;
                RankPicker.open(manager, player, memberTarget);
            }
            case "warp-list" -> {
                if (playerClan == null) return false;
                WarpList.open(plugin, manager, player, 1);
            }
            case "clan-log" -> {
                if (playerClan == null) return false;
                Log.open(manager, player);
            }
            case "admin-clan-list" -> AdminClanList.open(manager, player, 1);
            case "admin-clan-menu" -> {
                if (anyClan == null) return false;
                ClanMenu.open(manager, player, anyClan.getName());
            }
            case "admin-player-menu" -> {
                if (adminPlayerTarget == null) return false;
                PlayerMenu.open(manager, player, adminPlayerTarget);
            }
            case "admin-ally-remove" -> {
                if (anyClan == null) return false;
                AllyRemove.open(manager, player, anyClan.getName());
            }
            case "confirm" -> Confirm.open(player, "§6§lSqDev Test", "Bestätigen", "sqdev-test");
            default -> {
                return false;
            }
        }
        return true;
    }

    private static Clan anyClan(Manager manager, Clan preferred) {
        if (preferred != null) return preferred;
        return manager.getAllClans().stream().min(Comparator.comparing(Clan::getName, String.CASE_INSENSITIVE_ORDER))
                .orElse(null);
    }

    private static Clan otherClan(Manager manager, Clan playerClan, Clan fallback) {
        return manager.getAllClans().stream()
                .filter(clan -> playerClan == null || !clan.getName().equalsIgnoreCase(playerClan.getName()))
                .sorted(Comparator.comparing(Clan::getName, String.CASE_INSENSITIVE_ORDER))
                .findFirst().orElse(fallback);
    }

    private static UUID memberTarget(Clan clan) {
        if (clan == null) return null;
        return clan.getOrderedMembers().stream()
                .filter(id -> !id.equals(clan.getOwnerId()))
                .findFirst()
                .orElse(clan.getOwnerId());
    }

    private static Rank rankTarget(Clan clan) {
        if (clan == null) return null;
        return clan.getOrderedRanks().stream().findFirst().orElse(null);
    }

    private static War warTarget(Manager manager, Clan playerClan, Clan anyClan) {
        if (playerClan != null) {
            War active = manager.getActiveWar(playerClan.getName());
            if (active != null) return active;
            List<War> history = manager.getWarHistory(playerClan.getName());
            if (!history.isEmpty()) return history.getFirst();
        }
        if (anyClan != null) {
            War active = manager.getActiveWar(anyClan.getName());
            if (active != null) return active;
            List<War> history = manager.getWarHistory(anyClan.getName());
            if (!history.isEmpty()) return history.getFirst();
        }
        List<War> activeWars = manager.getActiveWars();
        return activeWars.isEmpty() ? null : activeWars.getFirst();
    }

    private static UUID adminPlayerTarget(Clan clan, Player fallback) {
        if (clan != null) return clan.getOwnerId();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(fallback.getUniqueId())) return online.getUniqueId();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(fallback.getUniqueId());
        return offline.getUniqueId();
    }
}
