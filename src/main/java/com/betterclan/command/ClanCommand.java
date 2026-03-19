package com.betterclan.command;

import com.betterclan.clan.Clan;
import com.betterclan.clan.ClanManager;
import com.betterclan.clan.ClanPermission;
import com.betterclan.gui.AllyMenu;
import com.betterclan.gui.InviteListMenu;
import com.betterclan.gui.MainMenu;
import com.betterclan.gui.WarRequestsMenu;
import com.betterclan.listener.ChatInputListener;
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

    private final ClanManager manager;
    private final ChatInputListener chatInput;

    public ClanCommand(ClanManager manager, ChatInputListener chatInput) {
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
            InviteListMenu.open(manager, player);
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("kriegsanfragen")) {
            Clan warClan = manager.getClan(player.getUniqueId());
            if (warClan != null && manager.hasPermission(warClan, player.getUniqueId(), ClanPermission.WAR)) {
                WarRequestsMenu.open(manager, player);
            } else {
                player.sendMessage("§cDu hast keine Berechtigung dafür.");
            }
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("allianzanfragen")) {
            Clan allyClan = manager.getClan(player.getUniqueId());
            if (allyClan != null && manager.hasPermission(allyClan, player.getUniqueId(), ClanPermission.ALLY)) {
                AllyMenu.openRequests(manager, player, 1);
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

        MainMenu.open(manager, player);
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

