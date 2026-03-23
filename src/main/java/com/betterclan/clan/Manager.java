package com.betterclan.clan;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.betterclan.gui.GuiHelper;

import java.time.Instant;
import java.util.*;

public class Manager {

    private final Settings settings;

    private java.util.function.Consumer<UUID> onTagRemove;

    private final Map<String, Clan> clansByName = new HashMap<>();
    private final Map<UUID, Clan> clanByPlayer = new HashMap<>();
    private final Map<UUID, List<ClanInvite>> invitesByPlayer = new HashMap<>();
    private final Set<UUID> inviteBlocked = new HashSet<>();

    private final Map<UUID, Long> clanBanned = new HashMap<>();

    private final List<War> activeWars = new ArrayList<>();
    private final List<War> warHistory = new ArrayList<>();
    private final Map<UUID, List<War>> warRequests = new HashMap<>();
    private final Set<UUID> glowDisabled = new HashSet<>();
    private final Map<String, ItemStack[]> clanVaults = new HashMap<>();
    private final Map<String, Long> warCooldowns = new HashMap<>();
    private final Map<UUID, ItemStack[]> vaultSnapshots = new HashMap<>();

    public record ClanInvite(String clanName, UUID inviter, Instant expiresAt) {}

    public Manager(Settings settings) {
        this.settings = settings;
    }

    public void setOnTagRemove(java.util.function.Consumer<UUID> callback) {
        this.onTagRemove = callback;
    }

    public Settings getSettings() {
        return settings;
    }

    public boolean isGlowDisabled(UUID playerId) { return glowDisabled.contains(playerId); }
    public void toggleGlow(UUID playerId) {
        if (!glowDisabled.remove(playerId)) glowDisabled.add(playerId);
    }
    public Set<UUID> getGlowDisabled() {
        return Collections.unmodifiableSet(glowDisabled);
    }
    public void setGlowDisabled(Set<UUID> disabled) {
        glowDisabled.clear();
        glowDisabled.addAll(disabled);
    }

    public boolean isInviteBlocked(UUID playerId) {
        return inviteBlocked.contains(playerId);
    }

    public void toggleInviteBlock(UUID playerId) {
        if (!inviteBlocked.remove(playerId)) {
            inviteBlocked.add(playerId);
        }
    }

    public Set<UUID> getInviteBlocked() {
        return Collections.unmodifiableSet(inviteBlocked);
    }

    public void setInviteBlocked(Set<UUID> blocked) {
        inviteBlocked.clear();
        inviteBlocked.addAll(blocked);
    }

    public boolean isClanBanned(UUID playerId) {
        Long expiry = clanBanned.get(playerId);
        if (expiry == null) return false;
        if (expiry == -1L) return true;
        if (System.currentTimeMillis() < expiry) return true;
        clanBanned.remove(playerId);
        return false;
    }

    public long getClanBanExpiry(UUID playerId) {
        return clanBanned.getOrDefault(playerId, 0L);
    }

    public void setClanBan(UUID playerId, long expiryMs) {
        clanBanned.put(playerId, expiryMs);
    }

    public void removeClanBan(UUID playerId) {
        clanBanned.remove(playerId);
    }

    public Map<UUID, Long> getClanBans() {
        return Collections.unmodifiableMap(clanBanned);
    }

    public void setClanBans(Map<UUID, Long> bans) {
        clanBanned.clear();
        clanBanned.putAll(bans);
    }

    public Clan getClan(UUID playerId) {
        return clanByPlayer.get(playerId);
    }

    public Clan createClan(Player owner, String clanName) {
        UUID ownerId = owner.getUniqueId();
        if (clanByPlayer.containsKey(ownerId)) return null;
        if (isClanBanned(ownerId)) return null;
        if (clansByName.containsKey(clanName.toLowerCase())) return null;
        Clan clan = new Clan(clanName, ownerId);
        clansByName.put(clanName.toLowerCase(), clan);
        clanByPlayer.put(ownerId, clan);
        clan.addLog("§7 Der Clan wurde gegründet von §f" + owner.getName() + "§7.");
        return clan;
    }

    public void renameClan(Clan clan, String newName) {
        if (clan == null || newName == null || newName.isBlank()) return;
        String oldName = clan.getName();
        clansByName.remove(oldName.toLowerCase());

        for (Clan other : clansByName.values()) {
            if (other.isAlly(oldName)) {
                other.removeAlly(oldName);
                other.addAlly(newName);
            }
            if (other.hasAllyRequest(oldName)) {
                other.removeAllyRequest(oldName);
                other.addAllyRequest(newName);
            }
        }

        for (List<ClanInvite> list : invitesByPlayer.values()) {
            ListIterator<ClanInvite> it = list.listIterator();
            while (it.hasNext()) {
                ClanInvite inv = it.next();
                if (inv.clanName().equalsIgnoreCase(oldName)) {
                    it.set(new ClanInvite(newName, inv.inviter(), inv.expiresAt()));
                }
            }
        }

        ItemStack[] vault = clanVaults.remove(oldName.toLowerCase());
        if (vault != null) clanVaults.put(newName.toLowerCase(), vault);

        clan.setName(newName);
        clansByName.put(newName.toLowerCase(), clan);
    }

    public void disbandClan(Clan clan) {
        if (clan == null) return;
        Set<UUID> memberIds = new HashSet<>(clan.getMembers().keySet());
        String clanName = clan.getName();

        for (Clan other : clansByName.values()) {
            other.removeAlly(clanName);
            other.removeAllyRequest(clanName);
        }
        activeWars.removeIf(w -> w.involves(clanName));

        warRequests.values().forEach(list -> list.removeIf(w -> w.involves(clanName)));
        warRequests.values().removeIf(List::isEmpty);

        invitesByPlayer.values().forEach(list -> list.removeIf(inv -> inv.clanName().equalsIgnoreCase(clanName)));
        invitesByPlayer.values().removeIf(List::isEmpty);

        clanVaults.remove(clanName.toLowerCase());
        clansByName.values().removeIf(c -> c == clan);
        clanByPlayer.values().removeIf(c -> c == clan);

        for (UUID memberId : memberIds) {
            clearTagFor(memberId);
        }
    }

    public void addMember(Clan clan, UUID playerId, String rankId) {
        clan.addMember(playerId, rankId);
        clanByPlayer.put(playerId, clan);

        String name = Optional.ofNullable(Bukkit.getOfflinePlayer(playerId).getName()).orElse("Spieler");
        broadcastToClan(clan, "§a✦ §f" + name + " §aist dem Clan beigetreten.");
    }

    public void removeMember(Clan clan, UUID playerId) {
        clan.removeMember(playerId);
        clanByPlayer.remove(playerId);
        String name = Optional.ofNullable(Bukkit.getOfflinePlayer(playerId).getName()).orElse("Spieler");
        broadcastToClan(clan, "§c✦ §f" + name + " §chat den Clan verlassen.");
        clearTagFor(playerId);
    }

    public void kickMember(Clan clan, UUID playerId) {
        clan.removeMember(playerId);
        clanByPlayer.remove(playerId);
        String name = Optional.ofNullable(Bukkit.getOfflinePlayer(playerId).getName()).orElse("Spieler");
        broadcastToClan(clan, "§c✦ §f" + name + " §cwurde aus dem Clan geschmissen.");
        clearTagFor(playerId);
    }

    private void clearTagFor(UUID playerId) {

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        String tn = "gc_" + playerId.toString().replace("-", "");
        Team team = board.getTeam(tn);
        if (team != null) team.prefix(Component.empty());

        if (onTagRemove != null) onTagRemove.accept(playerId);

        Player p = Bukkit.getPlayer(playerId);
        if (p != null && p.isOnline()) {
            p.playerListName(Component.text(p.getName()));
        }
    }

    public void leaveClan(Player player) {
        UUID id = player.getUniqueId();
        Clan clan = clanByPlayer.get(id);
        if (clan == null || clan.getOwnerId().equals(id)) return;
        removeMember(clan, id);
    }

    public boolean canKick(Clan clan, UUID actor, UUID target) {
        if (actor.equals(target)) return false;
        Rank actorRank = clan.getEffectiveRank(actor);
        Rank targetRank = clan.getEffectiveRank(target);
        if (actorRank == null || targetRank == null) return false;
        if (Rank.OWNER_ID.equals(targetRank.getId())) return false;
        if (Rank.OWNER_ID.equals(actorRank.getId())) return true;
        return actorRank.getPermissions().size() > targetRank.getPermissions().size();
    }

    public boolean canManage(Clan clan, UUID playerId) {
        if (clan.getOwnerId().equals(playerId)) return true;
        Rank rank = clan.getEffectiveRank(playerId);
        return rank != null && rank.hasPermission(Permission.INVITE);
    }

    public boolean hasPermission(Clan clan, UUID playerId, Permission perm) {
        if (clan.getOwnerId().equals(playerId)) return true;
        return clan.hasPermission(playerId, perm);
    }

    public boolean isInClan(UUID playerId) {
        return clanByPlayer.containsKey(playerId);
    }

    public Clan getClanByName(String name) {
        if (name == null) return null;
        return clansByName.get(name.toLowerCase());
    }

    public boolean transferOwnership(Clan clan, UUID newOwnerId) {
        if (clan == null || newOwnerId == null) return false;
        if (!clan.getMembers().containsKey(newOwnerId)) return false;
        UUID oldOwner = clan.getOwnerId();
        if (oldOwner.equals(newOwnerId)) return false;
        clan.addMember(oldOwner, clan.getDefaultOfficerRankId());
        clan.addMember(newOwnerId, Rank.OWNER_ID);
        clan.setOwnerId(newOwnerId);
        return true;
    }

    @SuppressWarnings("unused")
    public boolean promotePlayer(Clan clan, UUID actor, UUID target) {
        Rank actorRank = clan.getEffectiveRank(actor);
        Rank targetRank = clan.getEffectiveRank(target);
        if (actorRank == null || targetRank == null) return false;
        if (actorRank.getPermissions().size() <= targetRank.getPermissions().size()) return false;
        String nextRankId = clan.getPromotedRankId(clan.getRankId(target));
        if (nextRankId == null) return false;
        Rank nextRank = clan.getRank(nextRankId);
        if (nextRank == null || nextRank.getPermissions().size() >= actorRank.getPermissions().size()) return false;
        clan.addMember(target, nextRankId);
        return true;
    }

    @SuppressWarnings("unused")
    public boolean demotePlayer(Clan clan, UUID actor, UUID target) {
        Rank actorRank = clan.getEffectiveRank(actor);
        Rank targetRank = clan.getEffectiveRank(target);
        if (actorRank == null || targetRank == null) return false;
        if (actorRank.getPermissions().size() <= targetRank.getPermissions().size()) return false;
        String lowerRankId = clan.getDemotedRankId(clan.getRankId(target));
        if (lowerRankId == null) return false;
        clan.addMember(target, lowerRankId);
        return true;
    }

    public boolean sendInvite(Clan clan, UUID inviterId, UUID targetId) {
        if (clanByPlayer.containsKey(targetId)) return false;
        if (inviteBlocked.contains(targetId)) return false;
        if (isAtWar(clan.getName())) return false;

        List<ClanInvite> invites = invitesByPlayer.computeIfAbsent(targetId, k -> new ArrayList<>());
        Instant now = Instant.now();
        invites.removeIf(inv -> inv.expiresAt().isBefore(now));

        for (ClanInvite inv : invites) {
            if (inv.clanName().equalsIgnoreCase(clan.getName())) return false;
        }

        invites.add(new ClanInvite(clan.getName(), inviterId, now.plus(settings.inviteExpiry())));

        Player target = Bukkit.getPlayer(targetId);
        if (target != null) {
            String inviterName = Optional.ofNullable(Bukkit.getPlayer(inviterId))
                    .map(Player::getName).orElse("Jemand");

            target.sendMessage(Component.text()
                    .append(Component.text("✉ ", NamedTextColor.GOLD))
                    .append(Component.text(inviterName, NamedTextColor.WHITE))
                    .append(Component.text(" hat dich in den Clan ", NamedTextColor.GREEN))
                    .append(Component.text(clan.getName(), NamedTextColor.GOLD))
                    .append(Component.text(" eingeladen! ", NamedTextColor.GREEN))
                    .append(Component.text("[Öffnen]", NamedTextColor.YELLOW, TextDecoration.BOLD)
                            .clickEvent(ClickEvent.runCommand("/clan einladungen"))
                            .hoverEvent(HoverEvent.showText(
                                    Component.text("Klicke, um deine Einladungen zu sehen.", NamedTextColor.GRAY))))
                    .build());
        }
        return true;
    }

    public List<ClanInvite> getActiveInvites(UUID playerId) {
        List<ClanInvite> invites = invitesByPlayer.get(playerId);
        if (invites == null) return Collections.emptyList();
        Instant now = Instant.now();
        invites.removeIf(inv -> inv.expiresAt().isBefore(now));
        if (invites.isEmpty()) {
            invitesByPlayer.remove(playerId);
            return Collections.emptyList();
        }
        return new ArrayList<>(invites);
    }

    public boolean acceptInvite(Player player, String clanName) {
        UUID playerId = player.getUniqueId();
        if (clanByPlayer.containsKey(playerId)) return false;
        if (isClanBanned(playerId)) return false;
        List<ClanInvite> invites = invitesByPlayer.get(playerId);
        if (invites == null) return false;

        Instant now = Instant.now();
        invites.removeIf(inv -> inv.expiresAt().isBefore(now));

        ClanInvite found = invites.stream()
                .filter(inv -> inv.clanName().equalsIgnoreCase(clanName))
                .findFirst().orElse(null);

        if (found == null) {
            if (invites.isEmpty()) invitesByPlayer.remove(playerId);
            return false;
        }

        Clan clan = getClanByName(found.clanName());
        if (clan == null) { invites.remove(found); return false; }
        if (clan.getMemberCount() >= clan.getMaxMembers(settings)) {
            player.sendMessage("§cDer Clan ist voll (" + clan.getMemberCount() + "/" + clan.getMaxMembers(settings) + ").");
            return false;
        }

        addMember(clan, playerId, Rank.MEMBER_ID);
        invites.remove(found);
        if (invites.isEmpty()) invitesByPlayer.remove(playerId);
        return true;
    }

    public void declineInvite(UUID playerId, String clanName) {
        List<ClanInvite> invites = invitesByPlayer.get(playerId);
        if (invites == null) return;
        invites.removeIf(inv -> inv.clanName().equalsIgnoreCase(clanName));
        if (invites.isEmpty()) invitesByPlayer.remove(playerId);
    }

    public boolean requestAlly(Clan from, Clan to) {
        if (from == to) return false;
        if (from.isAlly(to.getName())) return false;
        if (to.hasAllyRequest(from.getName())) return false;
        if (isAtWar(from.getName()) || isAtWar(to.getName())) return false;
        if (from.getAllies().size() >= settings.maxAllies()) return false;

        to.addAllyRequest(from.getName());

        broadcastToClan(to, Component.text()
                .append(Component.text("⚔ ", NamedTextColor.GOLD))
                .append(Component.text("Der Clan ", NamedTextColor.GRAY))
                .append(Component.text(from.getName(), NamedTextColor.WHITE))
                .append(Component.text(" möchte eine Allianz! ", NamedTextColor.GRAY))
                .append(Component.text("[Anzeigen]", NamedTextColor.YELLOW, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/clan allianzanfragen"))
                        .hoverEvent(HoverEvent.showText(
                                Component.text("Klicke, um die Allianzanfrage zu sehen.", NamedTextColor.GRAY))))
                .build());
        return true;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean acceptAlly(Clan acceptor, String fromClanName) {
        if (!acceptor.hasAllyRequest(fromClanName)) return false;
        Clan from = getClanByName(fromClanName);
        if (from == null) {
            acceptor.removeAllyRequest(fromClanName);
            return false;
        }

        acceptor.removeAllyRequest(fromClanName);
        if (acceptor.getAllies().size() >= settings.maxAllies()) return false;
        if (from.getAllies().size() >= settings.maxAllies()) return false;
        acceptor.addAlly(from.getName());
        from.addAlly(acceptor.getName());

        broadcastToClan(acceptor, "§a⚔ §7Allianz mit §f" + from.getName() + " §7geschlossen!");
        broadcastToClan(from, "§a⚔ §7Allianz mit §f" + acceptor.getName() + " §7geschlossen!");
        return true;
    }

    public void declineAllyRequest(Clan clan, String fromClanName) {
        clan.removeAllyRequest(fromClanName);
    }

    public void removeAlly(Clan clan, String allyClanName) {
        clan.removeAlly(allyClanName);
        Clan other = getClanByName(allyClanName);
        if (other != null) {
            other.removeAlly(clan.getName());
        }
    }

    public boolean areAllies(Clan a, Clan b) {
        if (a == null || b == null) return false;
        return a.isAlly(b.getName());
    }

    public War getActiveWar(String clanName) {
        checkWarTimeouts();
        for (War war : activeWars) {
            if (war.isActive() && war.involves(clanName)) return war;
        }
        return null;
    }

    public void checkWarTimeouts() {
        List<War> timedOut = activeWars.stream()
                .filter(w -> w.isActive() && w.isTimedOut())
                .toList();
        timedOut.forEach(this::endWarByTimeout);
    }

    private void endWarByTimeout(War war) {
        finishWar(war, false);
        postWarEnd(war, "KRIEG ABGELAUFEN! ", "abgelaufen");
    }

    public boolean isAtWar(String clanName) {
        return getActiveWar(clanName) != null;
    }

    public long getWarCooldownRemaining(String clanName) {
        Long expiry = warCooldowns.get(clanName.toLowerCase());
        if (expiry == null) return 0;
        long remaining = expiry - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public boolean requestWar(Clan from, Clan to, int targetKills) {
        if (from == to) return false;
        if (isAtWar(from.getName()) || isAtWar(to.getName())) return false;
        if (getWarCooldownRemaining(from.getName()) > 0 || getWarCooldownRemaining(to.getName()) > 0) return false;

        List<War> existing = warRequests.getOrDefault(to.getOwnerId(), Collections.emptyList());
        for (War w : existing) {
            if (w.getClan1Name().equalsIgnoreCase(from.getName())) return false;
        }

        if (from.isAlly(to.getName())) {
            removeAlly(from, to.getName());
        }

        War war = new War(from.getName(), to.getName(), targetKills);
        warRequests.computeIfAbsent(to.getOwnerId(), k -> new ArrayList<>()).add(war);

        broadcastToClan(to, Component.text()
                .append(Component.text("⚔ ", NamedTextColor.DARK_RED))
                .append(Component.text("Der Clan ", NamedTextColor.GRAY))
                .append(Component.text(from.getName(), NamedTextColor.RED))
                .append(Component.text(" fordert euch zum Krieg! ", NamedTextColor.GRAY))
                .append(Component.text("(Ziel: " + targetKills + " Kills) ", NamedTextColor.GOLD))
                .append(Component.text("[Anzeigen]", NamedTextColor.YELLOW, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/clan kriegsanfragen"))
                        .hoverEvent(HoverEvent.showText(
                                Component.text("Klicke, um die Kriegsanfrage zu sehen.", NamedTextColor.GRAY))))
                .build());
        return true;
    }

    public List<War> getWarRequests(UUID ownerId) {
        return warRequests.getOrDefault(ownerId, Collections.emptyList());
    }

    public boolean hasSentWarRequestTo(Clan from, Clan to) {
        List<War> requests = warRequests.getOrDefault(to.getOwnerId(), Collections.emptyList());
        return requests.stream().anyMatch(w -> w.getClan1Name().equalsIgnoreCase(from.getName()));
    }

    public boolean acceptWar(Clan acceptor, String fromClanName) {
        List<War> requests = warRequests.get(acceptor.getOwnerId());
        if (requests == null) return false;

        War found = requests.stream()
                .filter(w -> w.getClan1Name().equalsIgnoreCase(fromClanName))
                .findFirst().orElse(null);
        if (found == null) return false;

        requests.remove(found);
        if (requests.isEmpty()) warRequests.remove(acceptor.getOwnerId());
        if (isAtWar(acceptor.getName()) || isAtWar(fromClanName)) return false;

        activeWars.add(found);

        Clan from = getClanByName(fromClanName);
        warRequests.values().forEach(list -> list.removeIf(w -> w.getClan1Name().equalsIgnoreCase(fromClanName)));
        warRequests.values().removeIf(List::isEmpty);

        warRequests.values().forEach(list -> list.removeIf(w -> w.getClan1Name().equalsIgnoreCase(acceptor.getName())));
        warRequests.values().removeIf(List::isEmpty);

        acceptor.addLog("§c⚔ §7Krieg gegen §f" + fromClanName + " §7begonnen! Ziel: §f" + found.getTargetKills() + " §7Kills.");
        if (from != null) from.addLog("§c⚔ §7Krieg gegen §f" + acceptor.getName() + " §7begonnen! Ziel: §f" + found.getTargetKills() + " §7Kills.");

        for (Clan c : new Clan[]{acceptor, from}) {
            if (c == null) continue;
            for (UUID id : c.getMembers().keySet()) {
                Player m = Bukkit.getPlayer(id);
                if (m != null) m.playSound(m.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_2, 1.0f, 1.0f);
            }
        }

        Component warMsg = Component.text()
                .append(Component.text("⚔ KRIEG! ", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.text(found.getClan1Name(), NamedTextColor.RED))
                .append(Component.text(" vs ", NamedTextColor.GRAY))
                .append(Component.text(found.getClan2Name(), NamedTextColor.RED))
                .append(Component.text(" — Ziel: " + found.getTargetKills() + " Kills", NamedTextColor.GOLD))
                .build();
        broadcastToClan(acceptor, warMsg);
        if (from != null) broadcastToClan(from, warMsg);
        return true;
    }

    public void declineWar(Clan acceptor, String fromClanName) {
        List<War> requests = warRequests.get(acceptor.getOwnerId());
        if (requests == null) return;
        requests.removeIf(w -> w.getClan1Name().equalsIgnoreCase(fromClanName));
        if (requests.isEmpty()) warRequests.remove(acceptor.getOwnerId());
    }

    public void registerWarKill(String killerClanName, String victimClanName, UUID killer, UUID victim) {
        for (War war : activeWars) {
            if (!war.isActive() || !war.involves(killerClanName) || !war.involves(victimClanName)) continue;
            war.addKill(killerClanName, killer);
            war.addDeath(victimClanName, victim);

            Component scoreMsg = Component.text()
                    .append(Component.text("⚔ ", NamedTextColor.DARK_RED))
                    .append(Component.text(war.getClan1Name() + " ", NamedTextColor.RED))
                    .append(Component.text(String.valueOf(war.getClan1Kills()), NamedTextColor.WHITE, TextDecoration.BOLD))
                    .append(Component.text(" : ", NamedTextColor.GRAY))
                    .append(Component.text(String.valueOf(war.getClan2Kills()), NamedTextColor.WHITE, TextDecoration.BOLD))
                    .append(Component.text(" " + war.getClan2Name(), NamedTextColor.RED))
                    .append(Component.text(" (Ziel: " + war.getTargetKills() + ")", NamedTextColor.GOLD))
                    .build();

            broadcastWarEnd(war, scoreMsg);
            if (war.isFinished()) endWar(war);
            return;
        }
    }

    private void finishWar(War war, boolean applyCooldown) {
        war.end();
        activeWars.remove(war);
        warHistory.add(war);
        if (applyCooldown) {
            long expiry = System.currentTimeMillis() + settings.warCooldownMs();
            warCooldowns.put(war.getClan1Name().toLowerCase(), expiry);
            warCooldowns.put(war.getClan2Name().toLowerCase(), expiry);
        }
    }

    public void endWar(War war) {
        finishWar(war, true);
        postWarEnd(war, "KRIEG BEENDET! ", "beendet");
    }

    private void postWarEnd(War war, String header, String verb) {
        String winnerName = war.getWinnerName();
        Clan winner = winnerName != null ? getClanByName(winnerName) : null;

        Component endMsg;
        if (war.isDraw()) {
            endMsg = Component.text()
                    .append(Component.text("⚔ " + header, NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text("Unentschieden! ", NamedTextColor.GRAY))
                    .append(Component.text("(" + war.getClan1Kills() + ":" + war.getClan2Kills() + ")", NamedTextColor.GRAY))
                    .build();
        } else if (winnerName != null) {
            endMsg = Component.text()
                    .append(Component.text("⚔ " + header, NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text("Der Clan ", NamedTextColor.GRAY))
                    .append(Component.text(winnerName, NamedTextColor.GREEN, TextDecoration.BOLD))
                    .append(Component.text(" hat den Krieg ", NamedTextColor.GRAY))
                    .append(Component.text(war.getClan1Kills() + ":" + war.getClan2Kills(), NamedTextColor.WHITE, TextDecoration.BOLD))
                    .append(Component.text(" gewonnen.", NamedTextColor.GRAY))
                    .build();
            if (winner != null) {
                addClanXp(winner, settings.warXpReward());
                giveWarBannerToOnlineMembers(winner, war);
            }
        } else {
            endMsg = Component.text()
                    .append(Component.text("⚔ " + header, NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text("Keine Entscheidung.", NamedTextColor.GRAY))
                    .build();
        }

        Clan c1 = getClanByName(war.getClan1Name());
        Clan c2 = getClanByName(war.getClan2Name());
        String resultLog;
        if (war.isDraw()) resultLog = "§7Unentschieden §8(" + war.getClan1Kills() + ":" + war.getClan2Kills() + ")";
        else if (winnerName != null) resultLog = "§f" + winnerName + " §7hat gewonnen §8(" + war.getClan1Kills() + ":" + war.getClan2Kills() + ")";
        else resultLog = "§7Keine Entscheidung";
        if (c1 != null) c1.addLog("§6⚔ §7Krieg vs §f" + war.getClan2Name() + " §7" + verb + ": " + resultLog + "§7.");
        if (c2 != null) c2.addLog("§6⚔ §7Krieg vs §f" + war.getClan1Name() + " §7" + verb + ": " + resultLog + "§7.");

        String loserName = winnerName != null ? war.getOpponent(winnerName) : null;
        Clan loser = loserName != null ? getClanByName(loserName) : null;
        if (winner != null) playSoundToClan(winner, Sound.ITEM_GOAT_HORN_SOUND_0);
        if (loser != null) playSoundToClan(loser, Sound.ITEM_GOAT_HORN_SOUND_7);
        broadcastWarEnd(war, endMsg);
    }

    private void playSoundToClan(Clan clan, Sound sound) {
        for (UUID id : clan.getMembers().keySet()) {
            Player m = Bukkit.getPlayer(id);
            if (m != null && m.isOnline()) m.playSound(m.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    private void giveWarBannerToOnlineMembers(Clan winner, War war) {
        ItemStack banner = GuiHelper.createClanBanner(winner);
        ItemMeta meta = banner.getItemMeta();
        if (meta != null) {

            Component displayName = MiniMessage.miniMessage().deserialize(
                "<gradient:#FFD700:#FFA500:#FFD700><bold>Triumphbanner von <winner></bold></gradient>",
                Placeholder.unparsed("winner", winner.getName()));
            meta.displayName(displayName);

            String loserName = war != null ? war.getLoserName() : null;
            String scoreStr  = war != null
                ? winner.getName().equalsIgnoreCase(war.getClan1Name())
                    ? war.getClan1Kills() + ":" + war.getClan2Kills()
                    : war.getClan2Kills() + ":" + war.getClan1Kills()
                : "?:?";
            String dateStr = new java.text.SimpleDateFormat("dd.MM.yyyy 'um' HH:mm").format(new java.util.Date());

            GuiHelper.applyWarBannerLore(meta,
                    new org.bukkit.NamespacedKey("betterclan", "war-banner"),
                    loserName, scoreStr, dateStr);
            banner.setItemMeta(meta);
        }
        for (UUID id : winner.getMembers().keySet()) {
            Player m = Bukkit.getPlayer(id);
            if (m != null && m.isOnline()) m.getInventory().addItem(banner.clone());
        }
    }

    private void broadcastWarEnd(War war, Component msg) {
        Clan c1 = getClanByName(war.getClan1Name());
        Clan c2 = getClanByName(war.getClan2Name());
        if (c1 != null) broadcastToClan(c1, msg);
        if (c2 != null) broadcastToClan(c2, msg);
    }

    public void surrenderWar(Clan clan) {
        War war = getActiveWar(clan.getName());
        if (war == null) return;

        String opponentName = war.getOpponent(clan.getName());
        if (opponentName != null) war.setForcedWinner(opponentName);
        finishWar(war, true);
        Clan opponent = opponentName != null ? getClanByName(opponentName) : null;
        clan.addLog("§c⚔ §7Kapituliert im Krieg gegen §f" + (opponentName != null ? opponentName : "?") + "§7.");
        if (opponent != null) opponent.addLog("§a⚔ §f" + clan.getName() + " §7hat kapituliert. Krieg gewonnen!");

        Component msg = Component.text()
                .append(Component.text("⚔ KRIEG BEENDET! ", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Der Clan ", NamedTextColor.GRAY))
                .append(Component.text(clan.getName(), NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" hat kapituliert.", NamedTextColor.GRAY))
                .build();

        playSoundToClan(clan, Sound.ITEM_GOAT_HORN_SOUND_7);
        broadcastToClan(clan, msg);
        if (opponent != null) {
            broadcastToClan(opponent, msg);
            addClanXp(opponent, settings.warXpReward());
            giveWarBannerToOnlineMembers(opponent, war);
            playSoundToClan(opponent, Sound.ITEM_GOAT_HORN_SOUND_0);
        }
    }

    @SuppressWarnings("unused")
    public List<War> getActiveWars() {
        return Collections.unmodifiableList(activeWars);
    }

    public List<War> getAllWarsForSave() {
        List<War> all = new ArrayList<>(activeWars);
        all.addAll(warHistory);
        return Collections.unmodifiableList(all);
    }

    public List<War> getWarHistory(String clanName) {
        List<War> result = new ArrayList<>();
        for (War w : warHistory) {
            if (w.involves(clanName)) result.add(w);
        }
        result.sort(Comparator.comparingLong(War::getEndedAt).reversed());
        return Collections.unmodifiableList(result);
    }

    public void setActiveWars(List<War> wars) {
        activeWars.clear();
        warHistory.clear();
        for (War w : wars) {
            if (w.isActive()) activeWars.add(w);
            else warHistory.add(w);
        }
    }

    public Map<UUID, List<War>> getAllWarRequests() {
        return Collections.unmodifiableMap(warRequests);
    }

    public void setAllWarRequests(Map<UUID, List<War>> requests) {
        warRequests.clear();
        requests.forEach((key, value) -> warRequests.put(key, new ArrayList<>(value)));
    }

    @SuppressWarnings("deprecation")
    public Inventory openVault(Player player, Clan clan) {
        String key = clan.getName().toLowerCase();
        ItemStack[] contents = clanVaults.get(key);

        int vaultSize = settings.getVaultSize(clan.getLevel());
        Inventory vault = Bukkit.createInventory(new VaultHolder(clan.getName()), vaultSize, "§6§lClan-Vault");
        ItemStack[] snapshot = new ItemStack[vaultSize];
        if (contents != null) {
            for (int i = 0; i < Math.min(contents.length, vaultSize); i++) {
                if (contents[i] != null) {
                    snapshot[i] = contents[i].clone();
                    vault.setItem(i, contents[i].clone());
                }
            }
        }
        vaultSnapshots.put(player.getUniqueId(), snapshot);
        return vault;
    }

    public ItemStack[] takeVaultSnapshot(UUID playerId) {
        return vaultSnapshots.remove(playerId);
    }

    public void saveVault(String clanName, ItemStack[] contents) {

        ItemStack[] saved = new ItemStack[54];
        for (int i = 0; i < Math.min(contents.length, 54); i++) {
            saved[i] = contents[i] != null ? contents[i].clone() : null;
        }
        clanVaults.put(clanName.toLowerCase(), saved);
    }

    public Map<String, ItemStack[]> getAllVaults() {
        return Collections.unmodifiableMap(clanVaults);
    }

    public void setVaultContents(String clanName, ItemStack[] contents) {
        clanVaults.put(clanName.toLowerCase(), contents);
    }

    public List<Clan> searchClans(String query) {
        if (query == null || query.isBlank()) return new ArrayList<>(clansByName.values());
        String lower = query.toLowerCase();
        return clansByName.values().stream()
                .filter(clan -> clan.getName().toLowerCase().contains(lower))
                .toList();
    }

    public void addClanXp(Clan clan, long amount) {
        if (clan == null || amount <= 0) return;
        int oldLevel = clan.getLevel();
        clan.addXp(amount, settings);
        int newLevel = clan.getLevel();
        if (newLevel > oldLevel) {
            broadcastToClan(clan, "§6§l★ §eDer Clan hat Level §f" + newLevel + " §eerreicht!");
            clan.addLog("§6★ §7Level-Up! §f" + oldLevel + " §7→ §f" + newLevel + "§7.");
        }
    }

    public Collection<Clan> getAllClans() {
        return Collections.unmodifiableCollection(clansByName.values());
    }

    public void clearAllClans() {
        clansByName.clear();
        clanByPlayer.clear();
    }

    public void registerClan(Clan clan) {
        clansByName.put(clan.getName().toLowerCase(), clan);
        for (UUID id : clan.getMembers().keySet()) {
            clanByPlayer.put(id, clan);
        }
    }

    public void broadcastToClan(Clan clan, String message) {
        for (UUID id : clan.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(id);
            if (member != null) member.sendMessage(message);
        }
    }

    public void broadcastToClan(Clan clan, Component message) {
        for (UUID id : clan.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(id);
            if (member != null) member.sendMessage(message);
        }
    }

    @SuppressWarnings("unused")
    public void broadcastToAlliance(Clan senderClan, Component message) {
        broadcastToClan(senderClan, message);
        for (String allyName : senderClan.getAllies()) {
            Clan ally = getClanByName(allyName);
            if (ally != null) broadcastToClan(ally, message);
        }
    }

    public void broadcastToClanChat(Clan clan, Component message) {
        for (UUID id : clan.getMembers().keySet()) {
            if (!hasPermission(clan, id, Permission.CLAN_CHAT)) continue;
            Player member = Bukkit.getPlayer(id);
            if (member != null) member.sendMessage(message);
        }
    }

    public void broadcastToAllianceChat(Clan senderClan, Component message) {
        for (UUID id : senderClan.getMembers().keySet()) {
            if (!hasPermission(senderClan, id, Permission.CHAT_ALLY)) continue;
            Player member = Bukkit.getPlayer(id);
            if (member != null) member.sendMessage(message);
        }
        for (String allyName : senderClan.getAllies()) {
            Clan ally = getClanByName(allyName);
            if (ally == null) continue;
            for (UUID id : ally.getMembers().keySet()) {
                if (!hasPermission(ally, id, Permission.CHAT_ALLY)) continue;
                Player member = Bukkit.getPlayer(id);
                if (member != null) member.sendMessage(message);
            }
        }
    }

    @SuppressWarnings("unused")
    public List<Player> getOnlineMembers(Clan clan) {
        List<Player> online = new ArrayList<>();
        for (UUID id : clan.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) online.add(p);
        }
        return online;
    }

    public int getOnlineMemberCount(Clan clan) {
        int count = 0;
        for (UUID id : clan.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) count++;
        }
        return count;
    }
}

