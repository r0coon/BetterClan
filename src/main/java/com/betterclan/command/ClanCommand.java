package com.betterclan.command;

import com.betterclan.clan.Clan;
import com.betterclan.clan.Manager;
import com.betterclan.clan.Permission;
import com.betterclan.gui.invite.Ally;
import com.betterclan.gui.invite.InviteList;
import com.betterclan.gui.clan.Main;
import com.betterclan.gui.war.Requests;
import com.betterclan.listener.ChatInput;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;

public class ClanCommand implements CommandExecutor, TabCompleter {

    private final Manager manager;
    private final ChatInput chatInput;

    public ClanCommand(Manager manager, ChatInput chatInput) {
        this.manager = manager;
        this.chatInput = chatInput;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NonNull [] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("einladungen")) {
            InviteList.open(manager, player);
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("kriegsanfragen")) {
            Clan warClan = manager.getClan(player.getUniqueId());
            if (warClan != null && manager.hasPermission(warClan, player.getUniqueId(), Permission.WAR)) {
                Requests.open(manager, player);
            } else {
                player.sendMessage("§cDu hast keine Berechtigung dafür.");
            }
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("allianzanfragen")) {
            Clan allyClan = manager.getClan(player.getUniqueId());
            if (allyClan != null && manager.hasPermission(allyClan, player.getUniqueId(), Permission.ALLY)) {
                Ally.openRequests(manager, player, 1);
            } else {
                player.sendMessage("§cDu hast keine Berechtigung dafür.");
            }
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("cancel")) {
            if (chatInput.hasPendingInput(player.getUniqueId())) {
                chatInput.cancelInput(player);
            } else {
                player.sendMessage("§7Du hast keine aktive Eingabe.");
            }
            return true;
        }

        Main.open(manager, player);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String @NonNull [] args
    ) {
        return Collections.emptyList();
    }
}

