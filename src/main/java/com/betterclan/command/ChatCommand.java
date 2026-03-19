package com.betterclan.command;

import com.betterclan.clan.Clan;
import com.betterclan.clan.ClanManager;
import com.betterclan.clan.ClanPermission;
import com.betterclan.gui.GuiHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import java.util.stream.Stream;

public class ChatCommand implements CommandExecutor, TabCompleter {

    private final ClanManager manager;

    public ChatCommand(ClanManager manager) {
        this.manager = manager;
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

        if (args.length < 2) {
            player.sendMessage("§cVerwendung: /chat clan <Nachricht> | /chat ally <Nachricht>");
            return true;
        }

        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cDu bist in keinem Clan.");
            return true;
        }

        String sub = args[0].toLowerCase();
        String text = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        switch (sub) {
            case "clan" -> {
                if (!manager.hasPermission(clan, player.getUniqueId(), ClanPermission.CLAN_CHAT)) {
                    player.sendMessage("§cDu hast keine Berechtigung, im Clan-Chat zu schreiben.");
                    return true;
                }
                Component msg = GuiHelper.tagColored(clan.getTagColor(), "[Clan-Chat] " + player.getName())
                        .append(Component.text(": ", NamedTextColor.GRAY))
                        .append(Component.text(text, NamedTextColor.WHITE));
                manager.broadcastToClanChat(clan, msg);
            }
            case "ally" -> {
                if (!manager.hasPermission(clan, player.getUniqueId(), ClanPermission.CHAT_ALLY)) {
                    player.sendMessage("§cDu hast keine Berechtigung, im Allianzen-Chat zu schreiben.");
                    return true;
                }
                if (clan.getAllies().isEmpty()) {
                    player.sendMessage("§cEuer Clan hat keine Allianzen.");
                    return true;
                }
                Component msg = GuiHelper.tagColored(clan.getTagColor(),
                                "[Allianz-Chat] [" + clan.getName() + "] " + player.getName())
                        .append(Component.text(": ", NamedTextColor.GRAY))
                        .append(Component.text(text, NamedTextColor.WHITE));
                manager.broadcastToAllianceChat(clan, msg);
            }
            default -> player.sendMessage("§cUnbekannte Chat-Art. Verwende: clan oder ally");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String @NonNull [] args
    ) {
        if (args.length == 1) {
            return Stream.of("clan", "ally")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }

}

