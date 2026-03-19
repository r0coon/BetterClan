package com.betterclan;

import com.betterclan.clan.Clan;
import com.betterclan.clan.ClanManager;
import com.betterclan.clan.ClanWar;
import com.betterclan.clan.CustomRank;
import com.betterclan.listener.PlayerListener;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BetterClanExpansion extends PlaceholderExpansion {

    private final BetterClanPlugin plugin;
    private final ClanManager manager;

    public BetterClanExpansion(BetterClanPlugin plugin, ClanManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override public @NotNull String getIdentifier() { return "betterclan"; }
    @Override public @NotNull String getAuthor()     { return "BetterClan"; }
    @Override public @NotNull String getVersion()    { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public @Nullable String onPlaceholderRequest(@Nullable Player player, @NotNull String params) {
        Clan clan = player != null ? manager.getClan(player.getUniqueId()) : null;

        return switch (params) {
            case "has_clan" -> clan != null ? "ja" : "nein";

            case "name" -> clan != null ? clan.getName() : "-";

            case "tag" -> {
                if (clan == null) yield "§7[]";
                yield LegacyComponentSerializer.legacySection()
                        .serialize(PlayerListener.renderTag(clan.getTagColor(), clan.getName(), false));
            }

            case "tag_mini" -> {
                if (clan == null) yield "§7[]";
                String fmt = clan.getTagColor();
                yield (fmt != null ? fmt : "§7") + "[" + clan.getName() + "]";
            }

            case "tag_format" -> clan != null ? clan.getTagColor() : "";

            case "level"   -> clan != null ? String.valueOf(clan.getLevel())              : "0";
            case "xp"      -> clan != null ? String.valueOf(clan.getXp())                : "0";
            case "members" -> clan != null ? String.valueOf(clan.getMemberCount())        : "0";
            case "online"  -> clan != null ? String.valueOf(manager.getOnlineMemberCount(clan)) : "0";
            case "allies"  -> clan != null ? String.valueOf(clan.getAllies().size())      : "0";

            case "rank" -> {
                if (clan == null) yield "-";
                CustomRank rank = clan.getEffectiveRank(player.getUniqueId());
                yield rank != null ? rank.getColoredName() : "-";
            }

            case "rank_plain" -> {
                if (clan == null) yield "-";
                CustomRank rank = clan.getEffectiveRank(player.getUniqueId());
                yield rank != null ? rank.getName() : "-";
            }

            case "in_war" -> {
                if (clan == null) yield "nein";
                yield manager.getActiveWar(clan.getName()) != null ? "ja" : "nein";
            }

            case "war_opponent" -> {
                if (clan == null) yield "-";
                ClanWar war = manager.getActiveWar(clan.getName());
                yield war != null ? war.getOpponent(clan.getName()) : "-";
            }

            case "war_kills" -> {
                if (clan == null) yield "0";
                ClanWar war = manager.getActiveWar(clan.getName());
                yield war != null ? String.valueOf(war.getKillsFor(clan.getName())) : "0";
            }

            case "base_world" -> {
                if (clan == null || clan.getBase() == null) yield "-";
                yield clan.getBase().getWorld().getName();
            }
            case "base_x" -> {
                if (clan == null || clan.getBase() == null) yield "-";
                yield String.valueOf((int) clan.getBase().getX());
            }
            case "base_y" -> {
                if (clan == null || clan.getBase() == null) yield "-";
                yield String.valueOf((int) clan.getBase().getY());
            }
            case "base_z" -> {
                if (clan == null || clan.getBase() == null) yield "-";
                yield String.valueOf((int) clan.getBase().getZ());
            }

            default -> null;
        };
    }
}

