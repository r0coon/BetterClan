package com.betterclan.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.betterclan.clan.Clan;
import com.betterclan.clan.Manager;
import com.betterclan.clan.Permission;
import com.betterclan.clan.War;
import com.betterclan.gui.GuiHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerListener implements Listener {

    private static final NamespacedKey WAR_BANNER_KEY = new NamespacedKey("betterclan", "war-banner");

    private static final int ENTITY_FLAGS_INDEX = 0;
    private static final byte GLOW_BIT = 0x40;

    private static final double GLOW_RANGE_SQUARED = 128 * 128;

    private final Manager manager;

    private final Map<UUID, Scoreboard> warScoreboards = new HashMap<>();

    private final Map<Location, String> pendingBannerData = new HashMap<>();

    private final Map<UUID, Set<UUID>> viewerEnemyGlow = new ConcurrentHashMap<>();
    private final Plugin plugin;

    private Map<UUID, Set<UUID>> prevGlowMap = new HashMap<>();

    private final boolean protocolLibEnabled;

    public PlayerListener(Manager manager, Plugin plugin) {
        this.manager = manager;
        this.plugin = plugin;

        this.protocolLibEnabled = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib");

        if (protocolLibEnabled) ProtocolLibrary.getProtocolManager().addPacketListener(
            new PacketAdapter(plugin, PacketType.Play.Server.ENTITY_METADATA) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    Player viewer = event.getPlayer();
                    PacketContainer packet = event.getPacket();
                    int entityId = packet.getIntegers().read(0);

                    Player target = null;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getEntityId() == entityId) { target = p; break; }
                    }
                    if (target == null) return;

                    Set<UUID> enemies = viewerEnemyGlow.get(viewer.getUniqueId());

                    if (enemies == null) {
                        stripGlowBit(packet);
                        return;
                    }

                    boolean shouldSeeGlow = enemies.contains(target.getUniqueId());

                    if (shouldSeeGlow) {
                        ensureGlowBit(packet);
                    } else {
                        stripGlowBit(packet);
                    }
                }

                private void ensureGlowBit(PacketContainer packet) {
                    var mod = packet.getDataValueCollectionModifier();
                    List<WrappedDataValue> values = mod.read(0);
                    if (values == null || values.isEmpty()) return;
                    for (int i = 0; i < values.size(); i++) {
                        WrappedDataValue dv = values.get(i);
                        if (dv.getIndex() == ENTITY_FLAGS_INDEX && dv.getValue() instanceof Byte flags) {
                            if ((flags & GLOW_BIT) != 0) return;
                            List<WrappedDataValue> copy = new ArrayList<>(values);
                            copy.set(i, new WrappedDataValue(dv.getIndex(), dv.getSerializer(),
                                    (byte) (flags | GLOW_BIT)));
                            mod.write(0, copy);
                            return;
                        }
                    }
                }

                private void stripGlowBit(PacketContainer packet) {
                    var mod = packet.getDataValueCollectionModifier();
                    List<WrappedDataValue> values = mod.read(0);
                    if (values == null || values.isEmpty()) return;
                    for (int i = 0; i < values.size(); i++) {
                        WrappedDataValue dv = values.get(i);
                        if (dv.getIndex() == ENTITY_FLAGS_INDEX && dv.getValue() instanceof Byte flags) {
                            if ((flags & GLOW_BIT) == 0) return;
                            List<WrappedDataValue> copy = new ArrayList<>(values);
                            copy.set(i, new WrappedDataValue(dv.getIndex(), dv.getSerializer(),
                                    (byte) (flags & ~GLOW_BIT)));
                            mod.write(0, copy);
                            return;
                        }
                    }
                }
            }
        );
    }

    private void sendGlowPacket(Player viewer, Player target) {
        if (!protocolLibEnabled) return;
        byte flags = 0;
        if (target.getFireTicks() > 0) flags |= 0x01;
        if (target.isSneaking()) flags |= 0x02;
        if (target.isSprinting()) flags |= 0x08;
        if (target.isSwimming()) flags |= 0x10;
        if (target.isInvisible()) flags |= 0x20;
        if (target.isGlowing()) flags |= GLOW_BIT;
        if (target.isGliding()) flags = (byte) (flags | 0x80);

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, target.getEntityId());
        List<WrappedDataValue> values = new ArrayList<>();
        values.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), flags));
        packet.getDataValueCollectionModifier().write(0, values);
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, packet);
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Clan clan = manager.getClan(player.getUniqueId());

        viewerEnemyGlow.put(player.getUniqueId(), Collections.emptySet());
        player.setGlowing(false);

        event.joinMessage(Component.text(player.getName() + " ist dem Spiel beigetreten.", NamedTextColor.YELLOW));

        updateTabName(player, clan);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            War joinerWar = clan != null ? manager.getActiveWar(clan.getName()) : null;
            if (joinerWar != null) {
                String joinerEnemyClan = joinerWar.getOpponent(clan.getName());

                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    if (viewer == player) continue;
                    Clan viewerClan = manager.getClan(viewer.getUniqueId());
                    if (viewerClan != null && viewerClan.getName().equalsIgnoreCase(joinerEnemyClan)) {

                        if (player.getWorld() == viewer.getWorld()
                            && player.getLocation().distanceSquared(viewer.getLocation()) < GLOW_RANGE_SQUARED) {
                            if (!manager.isGlowDisabled(viewer.getUniqueId())) {

                                Set<UUID> enemies = new HashSet<>(viewerEnemyGlow.getOrDefault(viewer.getUniqueId(), Collections.emptySet()));
                                enemies.add(player.getUniqueId());
                                viewerEnemyGlow.put(viewer.getUniqueId(), Collections.unmodifiableSet(enemies));
                            }
                        }
                    }
                }

                player.setGlowing(true);
            }

            if (joinerWar != null && !manager.isGlowDisabled(player.getUniqueId())) {
                String enemyClanName = joinerWar.getOpponent(clan.getName());
                Set<UUID> enemies = new HashSet<>();
                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (target == player) continue;
                    Clan targetClan = manager.getClan(target.getUniqueId());
                    if (targetClan != null && targetClan.getName().equalsIgnoreCase(enemyClanName)) {

                        if (player.getWorld() == target.getWorld()
                            && player.getLocation().distanceSquared(target.getLocation()) < GLOW_RANGE_SQUARED) {
                            enemies.add(target.getUniqueId());

                            sendGlowPacket(player, target);
                        }
                    }
                }
                if (!enemies.isEmpty()) {
                    viewerEnemyGlow.put(player.getUniqueId(), Collections.unmodifiableSet(enemies));
                }
            }
        }, 2L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            Clan c = manager.getClan(player.getUniqueId());

            List<String> hints = new ArrayList<>();

            int clanInvites = manager.getActiveInvites(player.getUniqueId()).size();
            if (clanInvites > 0) {
                hints.add("§7- §f" + clanInvites + "x §aClan-Einladung");
            }

            if (c != null) {

                if (manager.hasPermission(c, player.getUniqueId(), Permission.WAR)) {
                    int warRequests = manager.getWarRequests(c.getOwnerId()).size();
                    if (warRequests > 0) {
                        hints.add("§7- §f" + warRequests + "x §cKriegseinladung");
                    }
                }

                if (manager.hasPermission(c, player.getUniqueId(), Permission.ALLY)) {
                    int allyRequests = c.getAllyRequests().size();
                    if (allyRequests > 0) {
                        hints.add("§7- §f" + allyRequests + "x §9Allianz-Anfrage");
                    }
                }
            }

            if (!hints.isEmpty()) {
                player.sendMessage("§6§lBetterClan §8| §eDu hast offene Einladungen:");
                for (String hint : hints) {
                    player.sendMessage(hint);
                }
            }
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID leavingUUID = player.getUniqueId();

        event.quitMessage(Component.text(player.getName() + " hat das Spiel verlassen.", NamedTextColor.YELLOW));

        Clan clan = manager.getClan(leavingUUID);
        if (clan != null) {
            manager.broadcastToClan(clan, "§c§l● §f" + player.getName() + " §7ist jetzt offline.");
            clan.setLastSeen(leavingUUID, System.currentTimeMillis());
        }

        Scoreboard warSb = warScoreboards.remove(leavingUUID);
        if (warSb != null) {

            Player leavingPlayer = Bukkit.getPlayer(leavingUUID);
            if (leavingPlayer != null) leavingPlayer.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String tn = teamName(leavingUUID);
        Team team = board.getTeam(tn);
        if (team != null) team.unregister();

        viewerEnemyGlow.remove(leavingUUID);
        for (Map.Entry<UUID, Set<UUID>> entry : viewerEnemyGlow.entrySet()) {
            if (entry.getValue().contains(leavingUUID)) {
                Set<UUID> updated = new HashSet<>(entry.getValue());
                updated.remove(leavingUUID);
                viewerEnemyGlow.put(entry.getKey(), Collections.unmodifiableSet(updated));
            }
        }

        player.setGlowing(false);
    }

    static String teamName(UUID id) {
        return "gc_" + id.toString().replace("-", "");
    }

    public static Component renderTag(String tagFormat, String clanName, boolean trailingSpace) {
        String content = "[" + clanName + "]" + (trailingSpace ? " " : "");
        return GuiHelper.tagColored(tagFormat, content);
    }

    public void updateTabName(Player player, Clan clan) {
        updateNameTag(player, clan);
        if (clan != null) {
            player.playerListName(
                    Component.text(player.getName(), NamedTextColor.WHITE)
                            .append(Component.text(" "))
                            .append(renderTag(clan.getTagColor(), clan.getName(), false)));
        } else {
            player.playerListName(
                    Component.text(player.getName(), NamedTextColor.WHITE)
                            .append(Component.text(" []", NamedTextColor.GRAY)));
        }
    }

    private void updateNameTag(Player player, Clan clan) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String tn = teamName(player.getUniqueId());
        Team team = board.getTeam(tn);
        if (team == null) team = board.registerNewTeam(tn);

        team.prefix(Component.empty());
        if (clan != null) {
            team.suffix(Component.text(" ").append(renderTag(clan.getTagColor(), clan.getName(), false)));
        } else {
            team.suffix(Component.text(" []", NamedTextColor.GRAY));
        }
        if (!team.hasEntry(player.getName())) team.addEntry(player.getName());
    }

    public void refreshClanTabNames(Clan clan) {
        for (UUID memberId : clan.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberId);
            if (p != null && p.isOnline()) updateTabName(p, clan);
        }
    }

    public void clearClanTabNames(Clan clan) {
        for (UUID memberId : clan.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberId);
            if (p != null && p.isOnline()) updateTabName(p, null);
        }
    }

    public void updateAllTabNames() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Clan clan = manager.getClan(player.getUniqueId());
            updateTabName(player, clan);
        }
    }

    public void tickWarGlow() {
        Set<UUID> inWar = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Clan vClan = manager.getClan(player.getUniqueId());
            War war = vClan != null ? manager.getActiveWar(vClan.getName()) : null;

            if (war != null) {
                inWar.add(player.getUniqueId());

                Scoreboard sb = warScoreboards.computeIfAbsent(
                        player.getUniqueId(), k -> Bukkit.getScoreboardManager().getNewScoreboard());
                if (player.getScoreboard() != sb) player.setScoreboard(sb);

                String enemyClanName = war.getOpponent(vClan.getName());

                for (Player target : Bukkit.getOnlinePlayers()) {
                    Clan tClan = manager.getClan(target.getUniqueId());
                    String tTeamName = teamName(target.getUniqueId());
                    Team tTeam = sb.getTeam(tTeamName);
                    if (tTeam == null) tTeam = sb.registerNewTeam(tTeamName);
                    if (!tTeam.hasEntry(target.getName())) tTeam.addEntry(target.getName());
                    tTeam.prefix(Component.empty());
                    tTeam.suffix(tClan != null
                            ? Component.text(" ").append(renderTag(tClan.getTagColor(), tClan.getName(), false))
                            : Component.text(" []", NamedTextColor.GRAY));
                    boolean isEnemy = tClan != null && tClan.getName().equalsIgnoreCase(enemyClanName);
                    tTeam.color(isEnemy ? NamedTextColor.RED : NamedTextColor.WHITE);
                }

            } else {

                if (warScoreboards.remove(player.getUniqueId()) != null) {
                    player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                }
            }
        }

        Map<UUID, Set<UUID>> newGlowMap = new HashMap<>();
        for (Player viewer : Bukkit.getOnlinePlayers()) {

            if (!inWar.contains(viewer.getUniqueId()) || manager.isGlowDisabled(viewer.getUniqueId())) {
                newGlowMap.put(viewer.getUniqueId(), Collections.emptySet());
                continue;
            }
            Clan vClan = manager.getClan(viewer.getUniqueId());
            War war = vClan != null ? manager.getActiveWar(vClan.getName()) : null;
            if (war == null) {
                newGlowMap.put(viewer.getUniqueId(), Collections.emptySet());
                continue;
            }
            String enemyClanName = war.getOpponent(vClan.getName());
            Set<UUID> enemies = new HashSet<>();
            for (Player target : Bukkit.getOnlinePlayers()) {
                Clan tClan = manager.getClan(target.getUniqueId());
                if (tClan != null && tClan.getName().equalsIgnoreCase(enemyClanName)) {
                    enemies.add(target.getUniqueId());
                }
            }
            newGlowMap.put(viewer.getUniqueId(), Collections.unmodifiableSet(enemies));
        }

        viewerEnemyGlow.putAll(newGlowMap);
        viewerEnemyGlow.keySet().retainAll(newGlowMap.keySet());

        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean shouldGlow = inWar.contains(player.getUniqueId());
            if (shouldGlow != player.isGlowing()) {
                player.setGlowing(shouldGlow);
            }
        }

        for (Map.Entry<UUID, Set<UUID>> entry : newGlowMap.entrySet()) {
            UUID viewerId = entry.getKey();
            Set<UUID> newEnemies = entry.getValue();
            Set<UUID> oldEnemies = prevGlowMap.getOrDefault(viewerId, Collections.emptySet());
            if (!newEnemies.equals(oldEnemies)) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer == null || !viewer.isOnline()) continue;
                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (target != viewer && inWar.contains(target.getUniqueId())) {
                        sendGlowPacket(viewer, target);
                    }
                }
            }
        }
        prevGlowMap = newGlowMap;

        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!warScoreboards.containsKey(p.getUniqueId())) {
                String tn = teamName(p.getUniqueId());
                Team t = main.getTeam(tn);
                if (t != null) t.color(NamedTextColor.WHITE);
            }
        }
    }

    @EventHandler
    public void onWarBannerPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!item.getType().name().endsWith("_BANNER")) return;
        if (!item.hasItemMeta()) return;
        String data = item.getItemMeta().getPersistentDataContainer()
                .get(WAR_BANNER_KEY, PersistentDataType.STRING);
        if (data == null) return;
        BlockState state = event.getBlock().getState();
        if (state instanceof TileState ts) {
            ts.getPersistentDataContainer().set(WAR_BANNER_KEY, PersistentDataType.STRING, data);
            ts.update();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWarBannerBreakCapture(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!block.getType().name().endsWith("_BANNER") && !block.getType().name().endsWith("_WALL_BANNER")) return;
        BlockState state = block.getState();
        if (!(state instanceof TileState ts)) return;
        String data = ts.getPersistentDataContainer().get(WAR_BANNER_KEY, PersistentDataType.STRING);
        if (data == null) return;

        pendingBannerData.put(block.getLocation(), data);

        Bukkit.getScheduler().runTaskLater(
            plugin,
            () -> pendingBannerData.remove(block.getLocation()), 100L);
    }

    @EventHandler
    public void onWarBannerDrop(BlockDropItemEvent event) {
        Location loc = event.getBlock().getLocation();
        String data = pendingBannerData.remove(loc);
        if (data == null) return;
        String[] parts = data.split("\u0001", 3);
        String loserName = parts.length > 0 && !parts[0].isEmpty() ? parts[0] : null;
        String scoreStr  = parts.length > 1 ? parts[1] : "?:?";
        String dateStr   = parts.length > 2 ? parts[2] : "?";
        for (Item droppedItem : event.getItems()) {
            ItemStack stack = droppedItem.getItemStack();
            if (!stack.getType().name().endsWith("_BANNER")) continue;
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) continue;
            com.betterclan.gui.GuiHelper.applyWarBannerLore(meta, WAR_BANNER_KEY, loserName, scoreStr, dateStr);
            stack.setItemMeta(meta);
            droppedItem.setItemStack(stack);
            break;
        }
    }
}

