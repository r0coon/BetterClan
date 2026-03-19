package com.betterclan.command;

import com.betterclan.clan.Clan;
import com.betterclan.clan.ClanManager;
import com.betterclan.gui.AdminClanListMenu;
import com.betterclan.gui.AdminClanMenu;
import com.betterclan.gui.AdminPlayerMenu;
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

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final ClanManager manager;

    public AdminCommand(ClanManager manager) {
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
            AdminClanListMenu.open(manager, player, 1);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "menu" -> AdminClanListMenu.open(manager, player, 1);
            case "user" -> {
                if (args.length < 2) { player.sendMessage("§eVerwendung: §f/sca user <Spielername>"); return true; }
                String targetName = args[1].trim();
                org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
                if (op.getName() == null && !op.hasPlayedBefore()) {
                    player.sendMessage("§cSpieler §e" + targetName + " §cwurde nicht gefunden.");
                    return true;
                }
                AdminPlayerMenu.open(manager, player, op.getUniqueId());
            }
            case "clan", "name" -> {
                if (args.length < 2) { player.sendMessage("§eVerwendung: §f/sca clan <clan>"); return true; }
                String clanName = args[1].trim();
                Clan clan = manager.getClanByName(clanName);
                if (clan == null) {
                    player.sendMessage("§cClan §e" + clanName + " §cwurde nicht gefunden.");
                    return true;
                }
                AdminClanMenu.open(manager, player, clan.getName());
            }
            default -> sendUsage(player);
        }
        return true;
    }

    private static void sendUsage(Player player) {
        player.sendMessage("§c§lAdmin-Panel §8— §7Verwendung:");
        player.sendMessage("§e/sca               §7— Alle Clans verwalten");
        player.sendMessage("§e/sca menu          §7— Alle Clans verwalten");
        player.sendMessage("§e/sca user <name>   §7— Spieler direkt öffnen");
        player.sendMessage("§e/sca clan <clan>   §7— Clan direkt öffnen");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String @NonNull [] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Stream.of("menu", "user", "clan")
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

