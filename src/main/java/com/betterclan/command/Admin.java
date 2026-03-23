package com.betterclan.command;

import com.betterclan.BetterClan;
import com.betterclan.clan.Clan;
import com.betterclan.clan.Manager;
import com.betterclan.gui.admin.AdminClanList;
import com.betterclan.gui.admin.ClanMenu;
import com.betterclan.gui.misc.PlayerMenu;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.stream.Stream;

public class Admin implements CommandExecutor, TabCompleter {

    private final BetterClan plugin;
    private final Manager manager;

    public Admin(BetterClan plugin, Manager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur für Spieler.");
            return true;
        }
        if (!player.hasPermission("betterclan.admin")) {
            player.sendMessage("§cKeine Berechtigung. Benötigt: betterclan.admin");
            return true;
        }

        if (args.length == 0) {
            AdminClanList.open(manager, player, 1);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> { plugin.reloadPlugin(player); return true; }
            case "menu" -> AdminClanList.open(manager, player, 1);
            case "user" -> {
                if (args.length < 2) { player.sendMessage("§eVerwendung: §f/sclan user <Spielername>"); return true; }
                String targetName = args[1].trim();
                org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
                if (op.getName() == null && !op.hasPlayedBefore()) {
                    player.sendMessage("§cSpieler §e" + targetName + " §cwurde nicht gefunden.");
                    return true;
                }
                PlayerMenu.open(manager, player, op.getUniqueId());
            }
            case "clan", "name" -> {
                if (args.length < 2) { player.sendMessage("§eVerwendung: §f/sclan clan <clan>"); return true; }
                String clanName = args[1].trim();
                Clan clan = manager.getClanByName(clanName);
                if (clan == null) {
                    player.sendMessage("§cClan §e" + clanName + " §cwurde nicht gefunden.");
                    return true;
                }
                ClanMenu.open(manager, player, clan.getName());
            }
            default -> sendUsage(player);
        }
        return true;
    }

    private static void sendUsage(Player player) {
        player.sendMessage("§c§lAdmin-Panel §8— §7Verwendung:");
        player.sendMessage("§e/sclan               §7— Alle Clans verwalten");
        player.sendMessage("§e/sclan menu          §7— Alle Clans verwalten");
        player.sendMessage("§e/sclan user <name>   §7— Spieler direkt öffnen");
        player.sendMessage("§e/sclan clan <clan>   §7— Clan direkt öffnen");
        player.sendMessage("§e/sclan reload        §7— Config neu einlesen");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String @NonNull [] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Stream.of("menu", "user", "clan", "reload")
                    .filter(s -> s.startsWith(prefix))
                    .toList();
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String prefix = args[1].toLowerCase();
            if (sub.equals("user")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(prefix))
                        .toList();
            }
            if (sub.equals("clan") || sub.equals("name")) {
                return manager.getAllClans().stream()
                        .map(Clan::getName)
                        .filter(n -> n.toLowerCase().startsWith(prefix))
                        .toList();
            }
        }
        return List.of();
    }
}

