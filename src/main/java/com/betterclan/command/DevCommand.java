package com.betterclan.command;

import com.betterclan.BetterClanPlugin;
import com.betterclan.clan.Clan;
import com.betterclan.clan.ClanManager;
import com.betterclan.clan.ClanWar;
import com.betterclan.clan.CustomRank;
import com.betterclan.gui.AdminAllyRemoveMenu;
import com.betterclan.gui.AdminClanListMenu;
import com.betterclan.gui.AdminClanMenu;
import com.betterclan.gui.AdminPlayerMenu;
import com.betterclan.gui.AllyMenu;
import com.betterclan.gui.BannerColorMenu;
import com.betterclan.gui.BannerPatternMenu;
import com.betterclan.gui.ClanListMenu;
import com.betterclan.gui.ClanLogMenu;
import com.betterclan.gui.ClanWarMenu;
import com.betterclan.gui.ConfirmMenu;
import com.betterclan.gui.DynamicTagColorMenu;
import com.betterclan.gui.InviteListMenu;
import com.betterclan.gui.InvitePlayersMenu;
import com.betterclan.gui.MainMenu;
import com.betterclan.gui.MemberActionMenu;
import com.betterclan.gui.MemberRankPickerMenu;
import com.betterclan.gui.MembersMenu;
import com.betterclan.gui.RankActionMenu;
import com.betterclan.gui.RankManagementMenu;
import com.betterclan.gui.RolePermissionMenu;
import com.betterclan.gui.SettingsMenu;
import com.betterclan.gui.StaticTagColorMenu;
import com.betterclan.gui.TagColorMenu;
import com.betterclan.gui.WarHistoryMenu;
import com.betterclan.gui.WarLeaderboardMenu;
import com.betterclan.gui.WarPartySelectMenu;
import com.betterclan.gui.WarPastLeaderboardMenu;
import com.betterclan.gui.WarRequestsMenu;
import com.betterclan.gui.WarpListMenu;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class DevCommand {

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

    private DevCommand() {
    }

    public static boolean openMenuForPlayer(BetterClanPlugin plugin, Player player, String rawName) {
        String name = rawName.toLowerCase(Locale.ROOT);
        ClanManager manager = plugin.getClanManager();

        Clan playerClan = manager.getClan(player.getUniqueId());
        Clan anyClan = anyClan(manager, playerClan);
        Clan targetClan = otherClan(manager, playerClan, anyClan);
        UUID memberTarget = memberTarget(playerClan);
        CustomRank rankTarget = rankTarget(playerClan);
        ClanWar warTarget = warTarget(manager, playerClan, anyClan);
        UUID adminPlayerTarget = adminPlayerTarget(targetClan, player);

        switch (name) {
            case "main" -> MainMenu.open(manager, player);
            case "members" -> {
                if (playerClan == null) return false;
                MembersMenu.open(plugin, manager, player, 1);
            }
            case "invite-list" -> InviteListMenu.open(manager, player);
            case "invite-players" -> {
                if (playerClan == null) return false;
                InvitePlayersMenu.open(plugin, manager, player, 1);
            }
            case "settings" -> {
                if (playerClan == null) return false;
                SettingsMenu.open(manager, plugin.getChatInput(), player);
            }
            case "clan-list" -> ClanListMenu.open(manager, player, 1);
            case "clan-list-war" -> ClanListMenu.openForWar(manager, player, 1);
            case "clan-info" -> {
                if (targetClan == null) return false;
                ClanListMenu.openInfo(manager, player, targetClan, manager.getSettings().warTargetKills());
            }
            case "allies" -> {
                if (playerClan == null) return false;
                AllyMenu.open(manager, player);
            }
            case "ally-list" -> {
                if (playerClan == null) return false;
                AllyMenu.openList(manager, player, 1);
            }
            case "ally-shields" -> {
                if (playerClan == null) return false;
                AllyMenu.openShields(manager, player);
            }
            case "ally-requests" -> {
                if (playerClan == null) return false;
                AllyMenu.openRequests(manager, player, 1);
            }
            case "banner-color" -> {
                if (playerClan == null) return false;
                BannerColorMenu.open(manager, player);
            }
            case "banner-pattern-list" -> {
                if (playerClan == null) return false;
                BannerPatternMenu.openPatternList(manager, player);
            }
            case "banner-pattern-type" -> {
                if (playerClan == null) return false;
                BannerPatternMenu.openPatternTypePicker(manager, player, 1);
            }
            case "banner-pattern-color" -> {
                if (playerClan == null) return false;
                BannerPatternMenu.openPatternColorPicker(manager, player, "border");
            }
            case "war-menu" -> {
                if (playerClan == null) return false;
                ClanWarMenu.open(manager, player);
            }
            case "war-requests" -> {
                if (playerClan == null) return false;
                WarRequestsMenu.open(manager, player);
            }
            case "war-leaderboard" -> {
                if (playerClan == null) return false;
                WarLeaderboardMenu.open(manager, player, 1);
            }
            case "war-history" -> {
                if (playerClan == null) return false;
                WarHistoryMenu.open(manager, player, 1);
            }
            case "war-detail" -> {
                if (warTarget == null) return false;
                WarPartySelectMenu.open(manager, player, warTarget);
            }
            case "war-past-leaderboard" -> {
                if (warTarget == null) return false;
                String clanName = warTarget.getClan1Name();
                WarPastLeaderboardMenu.open(player, warTarget, clanName);
            }
            case "tag-color" -> {
                if (playerClan == null) return false;
                TagColorMenu.open(manager, player);
            }
            case "tag-color-static" -> {
                if (playerClan == null) return false;
                StaticTagColorMenu.open(manager, player);
            }
            case "tag-color-dynamic" -> {
                if (playerClan == null) return false;
                DynamicTagColorMenu.open(manager, player);
            }
            case "rank-management" -> {
                if (playerClan == null) return false;
                RankManagementMenu.openForAdmin(manager, playerClan, player);
            }
            case "rank-action" -> {
                if (playerClan == null || rankTarget == null) return false;
                RankActionMenu.open(manager, player, rankTarget.getId());
            }
            case "role-permissions" -> {
                if (playerClan == null || rankTarget == null) return false;
                RolePermissionMenu.openPermissions(manager, player, rankTarget.getId());
            }
            case "member-action" -> {
                if (playerClan == null || memberTarget == null) return false;
                MemberActionMenu.open(manager, player, memberTarget);
            }
            case "member-rank-picker" -> {
                if (playerClan == null || memberTarget == null) return false;
                MemberRankPickerMenu.open(manager, player, memberTarget);
            }
            case "warp-list" -> {
                if (playerClan == null) return false;
                WarpListMenu.open(plugin, manager, player, 1);
            }
            case "clan-log" -> {
                if (playerClan == null) return false;
                ClanLogMenu.open(manager, player);
            }
            case "admin-clan-list" -> AdminClanListMenu.open(manager, player, 1);
            case "admin-clan-menu" -> {
                if (anyClan == null) return false;
                AdminClanMenu.open(manager, player, anyClan.getName());
            }
            case "admin-player-menu" -> {
                if (adminPlayerTarget == null) return false;
                AdminPlayerMenu.open(manager, player, adminPlayerTarget);
            }
            case "admin-ally-remove" -> {
                if (anyClan == null) return false;
                AdminAllyRemoveMenu.open(manager, player, anyClan.getName());
            }
            case "confirm" -> ConfirmMenu.open(player, "§6§lSqDev Test", "Bestätigen", "sqdev-test");
            default -> {
                return false;
            }
        }
        return true;
    }

    private static Clan anyClan(ClanManager manager, Clan preferred) {
        if (preferred != null) return preferred;
        return manager.getAllClans().stream().min(Comparator.comparing(Clan::getName, String.CASE_INSENSITIVE_ORDER))
                .orElse(null);
    }

    private static Clan otherClan(ClanManager manager, Clan playerClan, Clan fallback) {
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

    private static CustomRank rankTarget(Clan clan) {
        if (clan == null) return null;
        return clan.getOrderedRanks().stream().findFirst().orElse(null);
    }

    private static ClanWar warTarget(ClanManager manager, Clan playerClan, Clan anyClan) {
        if (playerClan != null) {
            ClanWar active = manager.getActiveWar(playerClan.getName());
            if (active != null) return active;
            List<ClanWar> history = manager.getWarHistory(playerClan.getName());
            if (!history.isEmpty()) return history.getFirst();
        }
        if (anyClan != null) {
            ClanWar active = manager.getActiveWar(anyClan.getName());
            if (active != null) return active;
            List<ClanWar> history = manager.getWarHistory(anyClan.getName());
            if (!history.isEmpty()) return history.getFirst();
        }
        List<ClanWar> activeWars = manager.getActiveWars();
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
