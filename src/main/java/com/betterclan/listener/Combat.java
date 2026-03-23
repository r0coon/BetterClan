package com.betterclan.listener;

import com.betterclan.clan.Clan;
import com.betterclan.clan.Manager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class Combat implements Listener {

    private final Manager manager;

    public Combat(Manager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        Clan victimClan = manager.getClan(victim.getUniqueId());
        if (victimClan == null) return;
        Clan attackerClan = manager.getClan(attacker.getUniqueId());
        if (attackerClan == null) return;
        if (!attackerClan.getName().equals(victimClan.getName())) return;

        if (!attackerClan.isFriendlyFire()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {

        Component original = event.deathMessage();
        if (original != null) {
            String plain = PlainTextComponentSerializer.plainText().serialize(original);

            plain = plain.replaceAll(" \\[[^\\]]*\\]", "").trim().replaceAll("  +", " ");
            event.deathMessage(Component.text(plain, NamedTextColor.GRAY));
        }

        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        Clan killerClan = manager.getClan(killer.getUniqueId());
        if (killerClan == null) return;

        Clan victimClan = manager.getClan(victim.getUniqueId());
        if (victimClan == killerClan) return;
        if (victimClan != null && manager.areAllies(killerClan, victimClan)) return;

        if (victimClan != null) {
            manager.registerWarKill(killerClan.getName(), victimClan.getName(), killer.getUniqueId(), victim.getUniqueId());
        }
    }
}

