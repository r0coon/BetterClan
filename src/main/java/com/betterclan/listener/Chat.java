package com.betterclan.listener;

import com.betterclan.clan.Clan;
import com.betterclan.clan.Manager;
import com.betterclan.clan.Settings;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import com.betterclan.listener.PlayerListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public class Chat implements Listener {

    private final Manager manager;
    private final ChatInput chatInput;
    private final Settings settings;

    public Chat(Manager manager, ChatInput chatInput, Settings settings) {
        this.manager = manager;
        this.chatInput = chatInput;
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!settings.showClanTag()) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (chatInput.hasPendingInput(playerId)) return;

        Clan clan = manager.getClan(playerId);
        Component prefix = clan != null
                ? PlayerListener.renderTag(clan.getTagColor(), clan.getName(), true)
                : Component.text("[] ", net.kyori.adventure.text.format.NamedTextColor.GRAY);

        event.renderer((source, sourceDisplayName, message, viewer) ->
                Component.text()
                        .append(prefix)
                        .append(sourceDisplayName)
                        .append(Component.text(": "))
                        .append(message)
                        .build()
        );
    }
}

