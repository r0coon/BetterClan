package com.betterclan.listener;

import com.betterclan.clan.Settings;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatInput implements Listener {

    private final Plugin plugin;
    private final Settings settings;
    private final Map<UUID, Consumer<String>> pendingInputs = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> pendingTaskIds = new ConcurrentHashMap<>();

    public ChatInput(Plugin plugin, Settings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public boolean hasPendingInput(UUID playerId) {
        return pendingInputs.containsKey(playerId);
    }

    public void cancelInput(Player player) {
        UUID id = player.getUniqueId();
        if (pendingInputs.remove(id) != null) {
            Integer taskId = pendingTaskIds.remove(id);
            if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
            player.resetTitle();
            player.sendMessage(Component.text("Eingabe abgebrochen.", NamedTextColor.GRAY));
        }
    }

    public void awaitInput(Player player, Consumer<String> callback) {
        UUID id = player.getUniqueId();

        Integer oldTask = pendingTaskIds.remove(id);
        if (oldTask != null) Bukkit.getScheduler().cancelTask(oldTask);
        pendingInputs.put(id, callback);

        player.closeInventory();

        player.showTitle(Title.title(
                Component.text("✎ Eingabe", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text("Schreibe deinen Text im Chat", NamedTextColor.GRAY),
            Title.Times.times(
                Duration.ofMillis(Math.max(0, settings.chatInputTitleFadeInMs())),
                Duration.ofSeconds(Math.max(1, settings.chatInputTitleStaySeconds())),
                Duration.ofMillis(Math.max(0, settings.chatInputTitleFadeOutMs()))
            )
        ));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text()
                .append(Component.text("  ▶ ", NamedTextColor.GOLD))
                .append(Component.text("Gib deinen Text im Chat ein.", NamedTextColor.WHITE))
                .build());
        player.sendMessage(Component.text()
                .append(Component.text("  ✗ ", NamedTextColor.RED))
                .append(Component.text("Klicke um ", NamedTextColor.GRAY))
                .append(Component.text("'Abzubrechen'", NamedTextColor.RED, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.runCommand("/clan cancel")))
                .build());
        player.sendMessage(Component.empty());

        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingInputs.remove(id) != null) {
                pendingTaskIds.remove(id);
                player.resetTitle();
                player.sendMessage(Component.text("Eingabe abgebrochen (Zeitüberschreitung).", NamedTextColor.GRAY));
            }
        }, 20L * Math.max(1, settings.chatInputTimeoutSeconds())).getTaskId();
        pendingTaskIds.put(id, taskId);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Consumer<String> callback = pendingInputs.remove(id);
        if (callback == null) return;

        Integer taskId = pendingTaskIds.remove(id);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);

        event.setCancelled(true);
        Player player = event.getPlayer();
        String input = PlainTextComponentSerializer.plainText()
                .serialize(event.message()).trim();

        if (input.isEmpty() || input.equalsIgnoreCase("abbrechen") || input.equalsIgnoreCase("abzubrechen")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.resetTitle();
                player.sendMessage(Component.text("Eingabe abgebrochen.", NamedTextColor.GRAY));
            });
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            player.resetTitle();
            callback.accept(input);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        pendingInputs.remove(id);
        Integer taskId = pendingTaskIds.remove(id);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
    }

    public void shutdown() {
        pendingInputs.clear();
    }
}

