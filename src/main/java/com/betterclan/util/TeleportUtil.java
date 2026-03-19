package com.betterclan.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class TeleportUtil {
    private TeleportUtil() {}

    @SuppressWarnings("unused")
    public static void startCountdown(Plugin plugin, Player player, Location dest, String label) {
        startCountdown(plugin, player, dest, label, 5);
    }

    public static void startCountdown(Plugin plugin, Player player, Location dest, String label, int seconds) {
        Location startLoc = player.getLocation().clone();
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (seconds + 1) * 20, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (seconds + 1) * 20, 2, false, false));
        player.showTitle(Title.title(
                Component.text(label, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD),
                Component.text("Bitte bewege dich nicht — Teleport in " + seconds + "s…", NamedTextColor.GRAY)));

        final int[] remaining = {seconds};
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!player.isOnline()) { task.cancel(); return; }

            Location cur = player.getLocation();
            if (Math.abs(cur.getX() - startLoc.getX()) > 0.1
                    || Math.abs(cur.getY() - startLoc.getY()) > 0.1
                    || Math.abs(cur.getZ() - startLoc.getZ()) > 0.1) {
                task.cancel();

                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2, false, false));
                player.resetTitle();
                player.sendActionBar(Component.text("Teleport abgebrochen!", NamedTextColor.RED, TextDecoration.BOLD));
                return;
            }

            if (remaining[0] > 0) {
                player.sendActionBar(
                        Component.text("Teleport in ", NamedTextColor.YELLOW)
                                .append(Component.text(remaining[0] + "s", NamedTextColor.WHITE, TextDecoration.BOLD)));
                player.getWorld().spawnParticle(Particle.SMALL_GUST,
                        player.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.0);
                remaining[0]--;
            } else {
                task.cancel();
                if (!player.isOnline()) return;

                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
                player.getWorld().spawnParticle(Particle.SMALL_GUST,
                        player.getLocation().add(0, 1, 0), 60, 0.5, 0.8, 0.5, 0.0);
                player.teleport(dest);
                player.resetTitle();
                player.removePotionEffect(PotionEffectType.BLINDNESS);
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                player.sendActionBar(Component.text("Teleportiert!", NamedTextColor.WHITE, TextDecoration.BOLD));

                Location arrivalLoc = player.getLocation();
                player.getWorld().spawnParticle(Particle.SMALL_GUST, arrivalLoc.clone().add(0, 0.5, 0), 80, 0.6, 0.4, 0.6, 0.0);
                player.getWorld().spawnParticle(Particle.SMALL_GUST, arrivalLoc.clone().add(0, 1.2, 0), 50, 0.8, 0.3, 0.8, 0.0);
                player.getWorld().spawnParticle(Particle.SMALL_GUST, arrivalLoc.clone().add(0, 2.0, 0), 30, 0.4, 0.2, 0.4, 0.0);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) return;
                    player.getWorld().spawnParticle(Particle.SMALL_GUST, player.getLocation().clone().add(0, 1, 0), 40, 1.0, 0.5, 1.0, 0.0);
                    player.playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.8f, 1.0f);
                }, 2L);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) return;
                    player.getWorld().spawnParticle(Particle.SMALL_GUST, player.getLocation().clone().add(0, 0.5, 0), 25, 1.2, 0.3, 1.2, 0.0);
                }, 5L);
            }
        }, 0L, 20L);
    }
}

