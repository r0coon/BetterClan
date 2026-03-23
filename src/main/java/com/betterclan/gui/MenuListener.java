package com.betterclan.gui;

import com.betterclan.BetterClan;
import com.betterclan.clan.*;
import com.betterclan.gui.admin.*;
import com.betterclan.gui.clan.*;
import com.betterclan.gui.invite.*;
import com.betterclan.gui.member.*;
import com.betterclan.gui.misc.*;
import com.betterclan.gui.rank.*;
import com.betterclan.gui.tag.*;
import com.betterclan.gui.war.*;
import com.betterclan.gui.warlog.*;
import com.betterclan.listener.ChatInput;
import com.betterclan.util.Teleport;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class MenuListener implements Listener {

    private final BetterClan plugin;
    private final Manager manager;
    private final ChatInput chatInput;

    public MenuListener(BetterClan plugin, Manager manager, ChatInput chatInput) {
        this.plugin = plugin;
        this.manager = manager;
        this.chatInput = chatInput;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof GuiHolder) event.setCancelled(true);
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getView().getTitle().startsWith("§6§lClan-Vault")) return;
        if (!(event.getInventory().getHolder() instanceof GuiHolder holder)) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (isCloseButton(clicked)) { player.closeInventory(); return; }

        switch (holder.getType()) {
            case MAIN_NO_CLAN -> handleMainNoClan(player, clicked, event.getClick());
            case MAIN_IN_CLAN -> handleMainInClan(player, clicked, event.getClick());
            case MEMBERS -> handleMembers(player, clicked);
            case MEMBER_ACTIONS -> handleMemberActions(player, clicked, holder);
            case INVITE_LIST -> handleInviteList(player, clicked, event.getClick());
            case INVITE_PLAYERS -> handleInvitePlayers(player, clicked, holder);
            case SETTINGS -> handleSettings(player, clicked, event.getClick());
            case CLAN_LIST -> handleClanList(player, clicked, holder);
            case CLAN_LIST_INFO -> handleClanListInfo(player, clicked, holder);
            case ALLIES -> handleAllies(player, clicked, event.getClick());
            case CONFIRM -> handleConfirm(player, clicked, holder);
            case BANNER_COLOR -> handleBannerColor(player, clicked);
            case BANNER_PATTERN_LIST -> handleBannerPatternList(player, clicked);
            case BANNER_PATTERN_TYPE -> handleBannerPatternType(player, clicked, holder);
            case BANNER_PATTERN_COLOR -> handleBannerPatternColor(player, clicked, holder);
            case WAR_MENU -> handleWarMenu(player, clicked, event.getClick());
            case WAR_LEADERBOARD -> {
                String _wlName = clicked.hasItemMeta() ? clicked.getItemMeta().getDisplayName() : "";
                if (clicked.getType() == Material.ARROW) { Overview.open(manager, player); return; }
                if (clicked.getType() == Material.LIGHT_GRAY_DYE) {
                    int page = holder.get("page", 1);
                    if (_wlName.contains("«")) Leaderboard.open(manager, player, page - 1);
                    if (_wlName.contains("»")) Leaderboard.open(manager, player, page + 1);
                }
            }
            case WAR_HISTORY -> {
                if (clicked.getType() == Material.ARROW || isCloseButton(clicked) || clicked.getType() == Material.BARRIER) {
                    String vcn = holder.get("viewClanName");
                    if (vcn != null) {
                        com.betterclan.clan.Clan vc = manager.getClanByName(vcn);
                        if (vc != null) openClanListInfo(player, vc);
                        else ClanList.open(manager, player, 1);
                    } else {
                        Overview.open(manager, player);
                    }
                    return;
                }

                if (clicked.getType() == Material.LIGHT_GRAY_DYE) {
                    int page = holder.get("page", 1);
                    String vcn = holder.get("viewClanName");
                    String itemName = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                            ? clicked.getItemMeta().getDisplayName() : "";
                    if (itemName.contains("«")) {
                        if (vcn != null) History.open(manager, player, vcn, page - 1);
                        else History.open(manager, player, page - 1);
                    }
                    if (itemName.contains("»")) {
                        if (vcn != null) History.open(manager, player, vcn, page + 1);
                        else History.open(manager, player, page + 1);
                    }
                    return;
                }
                if (clicked.getType() == Material.BOOK || clicked.getType() == Material.WRITTEN_BOOK || clicked.getType() == Material.KNOWLEDGE_BOOK) {
                    com.betterclan.clan.War war = holder.get("war_" + event.getRawSlot());
                    if (war != null) PartySelect.open(manager, player, war, holder.get("viewClanName"));
                }
            }
            case WAR_HISTORY_DETAIL -> {
                if (clicked.getType() == Material.ARROW || isCloseButton(clicked) || clicked.getType() == Material.BARRIER) {
                    String vcn2 = holder.get("viewClanName");
                    if (vcn2 != null) History.open(manager, player, vcn2);
                    else { com.betterclan.clan.Clan hClan = manager.getClan(player.getUniqueId()); if (hClan != null) History.open(manager, player); }
                    return;
                }
                com.betterclan.clan.War war = holder.get("war");
                if (war == null) return;
                if (!clicked.hasItemMeta()) return;
                String displayName = clicked.getItemMeta().getDisplayName();
                String chosen = displayName.replaceAll("§.", "").trim();
                if (chosen.equalsIgnoreCase(war.getClan1Name()) || chosen.equalsIgnoreCase(war.getClan2Name())) {
                    PastLeaderboard.open(player, war, chosen, holder.get("viewClanName"));
                }
            }
            case WAR_PAST_LEADERBOARD -> {
                if (clicked.getType() == Material.ARROW || isCloseButton(clicked) || clicked.getType() == Material.BARRIER) {
                    com.betterclan.clan.War war = holder.get("war");
                    if (war != null) PartySelect.open(manager, player, war, holder.get("viewClanName"));
                    else { com.betterclan.clan.Clan bClan = manager.getClan(player.getUniqueId()); if (bClan != null) History.open(manager, player); }
                }
            }
            case WAR_REQUESTS -> handleWarRequests(player, clicked, event.getClick());
            case TAG_COLOR -> handleTagColor(player, clicked);
            case TAG_COLOR_STATIC -> handleTagColorStatic(player, clicked);
            case TAG_COLOR_DYNAMIC -> handleTagColorDynamic(player, clicked);
            case ROLE_PERMISSIONS -> handleRolePermissions(player, clicked);
            case ROLE_PERM_EDIT -> handleRolePermEdit(player, clicked, holder);
            case RANK_MANAGEMENT -> handleRankManagement(player, clicked, holder, event.getClick());
            case RANK_ACTION -> handleRankAction(player, clicked, holder);
            case ALLY_LIST -> handleAllyList(player, clicked, holder, event.getClick());
            case ALLY_SHIELDS -> handleAllyShields(player, clicked, holder, event.getClick());
            case ALLY_REQUESTS -> handleAllyRequests(player, clicked, holder, event.getClick());
            case WARP_LIST -> handleWarpList(player, clicked, holder, event.getRawSlot(), event.getClick());
            case CLAN_LOG -> handleClanLog(player, clicked, holder);
            case MEMBER_RANK_PICKER -> handleMemberRankPicker(player, clicked, holder);
            case ADMIN_CLAN_LIST -> handleAdminClanList(player, clicked, holder, event.getClick());
            case ADMIN_CLAN_MENU -> handleAdminClanMenu(player, clicked, holder, event.getClick());
            case ADMIN_PLAYER_MENU -> handleAdminPlayerMenu(player, clicked, holder);
            case ADMIN_ALLY_REMOVE -> handleAdminAllyRemove(player, clicked, holder, event.getClick());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {

        if (event.getInventory().getHolder() instanceof VaultHolder(String clanName)) {
            ItemStack[] newContents = event.getInventory().getContents();
            manager.saveVault(clanName, newContents);

            if (event.getPlayer() instanceof Player player) {
                ItemStack[] snapshot = manager.takeVaultSnapshot(player.getUniqueId());
                if (snapshot != null) {
                    Clan clan = manager.getClanByName(clanName);
                    if (clan != null) {
                        String removed = buildVaultDiff(snapshot, newContents, true);
                        String added   = buildVaultDiff(newContents, snapshot, false);
                        if (!removed.isEmpty())
                            clan.addLog("§6[Vault] §e" + player.getName() + " §centnommen: §f" + removed);
                        if (!added.isEmpty())
                            clan.addLog("§6[Vault] §e" + player.getName() + " §aeingelagert: §f" + added);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private String buildVaultDiff(ItemStack[] before, ItemStack[] after, boolean removed) {
        Map<String, Integer> delta = new LinkedHashMap<>();
        int len = Math.max(before.length, after.length);
        for (int i = 0; i < len; i++) {
            ItemStack b = i < before.length ? before[i] : null;
            ItemStack a = i < after.length  ? after[i]  : null;
            int qBefore = (b != null) ? b.getAmount() : 0;
            int qAfter  = (a != null) ? a.getAmount() : 0;
            int diff = qBefore - qAfter;
            if (diff <= 0) continue;
            ItemStack ref = (b != null) ? b : a;
            String label = itemLabel(ref);
            delta.merge(label, diff, Integer::sum);
        }
        if (delta.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        delta.forEach((label, qty) -> {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(qty).append("x ").append(label);
        });
        return sb.toString();
    }

    private static String legacyDisplayName(ItemMeta meta) {
        Component c = meta.displayName();
        return c != null ? LegacyComponentSerializer.legacySection().serialize(c) : "";
    }

    private String itemLabel(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return legacyDisplayName(item.getItemMeta());
        }
        String raw = item.getType().name().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    @SuppressWarnings("deprecation")
    private boolean isCloseButton(ItemStack item) {
        if (item.getType() != Material.BARRIER || !item.hasItemMeta()) return false;
        String name = item.getItemMeta().getDisplayName();
        return name.contains("Schließen");
    }

    private void handleClanLog(Player player, ItemStack clicked, GuiHolder holder) {
        int page = holder.get("page", 1);
        String adminClanName = holder.get("adminClanName");
        if (clicked.getType() == Material.ARROW) {
            if (adminClanName != null) ClanMenu.open(manager, player, adminClanName);
            else SettingsMenu.open(manager, chatInput, player);
            return;
        }
        if (clicked.getType() == Material.LIGHT_GRAY_DYE) {
            String name = clicked.hasItemMeta() ? legacyDisplayName(clicked.getItemMeta()) : "";
            if (name.contains("Vorherige") || name.contains("«")) { openLog(player, adminClanName, page - 1); return; }
            if (name.contains("Nächste") || name.contains("»")) { openLog(player, adminClanName, page + 1); }
        }
    }

    private void openLog(Player player, String adminClanName, int page) {
        if (adminClanName != null) Log.openForAdmin(manager, player, adminClanName, page);
        else Log.open(manager, player, page);
    }

    private void startCountdownTeleport(Player player, Location dest, String label) {
        Teleport.startCountdown(plugin, player, dest, label, manager.getSettings().teleportCountdownSeconds());
    }

    private void handleWarpList(Player player, ItemStack clicked, GuiHolder holder, int slot, ClickType click) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) return;

        int page = holder.get("page", 1);

        if (slot == 49 && clicked.getType() == Material.ARROW) { Main.open(manager, player); return; }

        if (slot == 47) { WarpList.open(plugin, manager, player, page - 1); return; }
        if (slot == 51) { WarpList.open(plugin, manager, player, page + 1); return; }

        if (clicked.getType() == Material.IRON_NAUTILUS_ARMOR) {
            boolean isBase2 = (slot == 15);
            if (isBase2 && clan.getLevel() < manager.getSettings().secondBaseMinLevel()) {
                player.sendMessage("§cClan-Base 2 wird ab Level " + manager.getSettings().secondBaseMinLevel() + " freigeschaltet.");
                return;
            }
            if (click.isRightClick()) {

                if (!manager.hasPermission(clan, player.getUniqueId(), Permission.SET_BASE)) {
                    player.sendMessage("§cDu hast keine Berechtigung, die Clan-Base zu setzen.");
                    return;
                }
                Location existingBase = isBase2 ? clan.getBase2() : clan.getBase();
                if (existingBase != null) {
                    Confirm.open(player,
                            "§c§lBase überschreiben?",
                            "Überschreiben", "setBase",
                            "isBase2", String.valueOf(isBase2));
                } else {
                    if (isBase2) {
                        clan.setBase2(player.getLocation());
                    } else {
                        clan.setBase(player.getLocation());
                    }
                    plugin.saveAsync();
                    player.sendMessage("§a Clan-Base " + (isBase2 ? "2 " : "") + "wurde gesetzt!");
                    WarpList.open(plugin, manager, player, page);
                }
            } else {

                if (!manager.hasPermission(clan, player.getUniqueId(), Permission.TELEPORT)) {
                    player.sendMessage("§cDu hast keine Berechtigung zum Teleportieren.");
                    return;
                }
                Location dest = isBase2 ? clan.getBase2() : clan.getBase();
                if (dest == null) {
                    player.sendMessage("§cDiese Clan-Base wurde noch nicht gesetzt.");
                    return;
                }
                player.closeInventory();
                startCountdownTeleport(player, dest, "Clan-Base" + (isBase2 ? " 2" : ""));
            }
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD && clicked.hasItemMeta()) {
            String uuidStr = clicked.getItemMeta().getPersistentDataContainer()
                    .get(WarpList.WARP_MEMBER_KEY, PersistentDataType.STRING);
            if (uuidStr == null) return;
            UUID targetId;
            try { targetId = UUID.fromString(uuidStr); } catch (IllegalArgumentException e) { return; }

            if (targetId.equals(player.getUniqueId())) {
                Location existingWarp = clan.getMemberWarp(player.getUniqueId());
                if (click.isRightClick()) {

                    if (existingWarp != null) {
                        Confirm.open(player,
                                "§e§lWarp überschreiben?",
                                "Überschreiben", "setWarp");
                    } else {
                        clan.setMemberWarp(player.getUniqueId(), player.getLocation());
                        plugin.saveAsync();
                        player.sendMessage("§a Dein Warp-Punkt wurde gesetzt!");
                        WarpList.open(plugin, manager, player, page);
                    }
                } else {

                    if (existingWarp != null) {
                        if (!manager.hasPermission(clan, player.getUniqueId(), Permission.TELEPORT)) {
                            player.sendMessage("§cDu hast keine Berechtigung zum Teleportieren.");
                            return;
                        }
                        player.closeInventory();
                        startCountdownTeleport(player, existingWarp, "dein Warp");
                    } else {
                        clan.setMemberWarp(player.getUniqueId(), player.getLocation());
                        plugin.saveAsync();
                        player.sendMessage("§a Dein Warp-Punkt wurde gesetzt!");
                        WarpList.open(plugin, manager, player, page);
                    }
                }
                return;
            }

            if (!manager.hasPermission(clan, player.getUniqueId(), Permission.TELEPORT)) {
                player.sendMessage("§cDu hast keine Berechtigung zum Teleportieren.");
                return;
            }
            Location warp = clan.getMemberWarp(targetId);
            if (warp == null) {
                String targetName = Bukkit.getOfflinePlayer(targetId).getName();
                player.sendMessage("§c" + (targetName != null ? targetName : "Dieser Spieler")
                        + " §chat noch keinen Warp-Punkt gesetzt.");
                return;
            }
            player.closeInventory();
            String label = Bukkit.getOfflinePlayer(targetId).getName();
            startCountdownTeleport(player, warp, label != null ? label + "'s Warp" : "Warp");
        }
    }

    private void handleMainNoClan(Player player, ItemStack clicked, ClickType click) {
        Settings settings = manager.getSettings();
        switch (clicked.getType()) {
            case BARRIER -> player.closeInventory();
            case NETHER_STAR -> {
                player.closeInventory();
                chatInput.awaitInput(player, name -> {
                    if (manager.isInClan(player.getUniqueId())) {
                        player.sendMessage("§cDu bist bereits in einem Clan.");
                        return;
                    }
                    if (manager.isClanBanned(player.getUniqueId())) {
                        long expiry = manager.getClanBanExpiry(player.getUniqueId());
                        if (expiry == -1L) {
                            player.sendMessage("§cDu wurdest dauerhaft gesperrt und kannst daher keinen Clan gründen. Wende dich an einen Admin.");
                        } else {
                            String until = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date(expiry));
                            player.sendMessage("§cDu bist bis zum §e" + until + "§c gesperrt und kannst daher keinen Clan gründen.");
                        }
                        return;
                    }
                    if (name.length() < Settings.CLAN_MIN_NAME_LENGTH) {
                        player.sendMessage("§cDer Name muss mindestens " + Settings.CLAN_MIN_NAME_LENGTH + " Zeichen lang sein.");
                        return;
                    }
                    if (name.length() > Settings.CLAN_MAX_NAME_LENGTH) {
                        player.sendMessage("§cDer Name darf maximal " + Settings.CLAN_MAX_NAME_LENGTH + " Zeichen lang sein.");
                        return;
                    }
                    if (manager.getClanByName(name) != null) {
                        player.sendMessage("§cEin Clan mit diesem Namen existiert bereits.");
                        return;
                    }
                    Clan created = manager.createClan(player, name);
                    if (created == null) {
                        player.sendMessage("§cDer Clan konnte nicht erstellt werden.");
                        return;
                    }
                    player.sendMessage("§a§l §aClan §f\"" + created.getName() + "\" §awurde gegründet!");
                    plugin.getPlayerListener().updateTabName(player, created);
                    Main.open(manager, player);
                });
            }
            case WRITABLE_BOOK -> {
                if (click == ClickType.RIGHT) {
                    manager.toggleInviteBlock(player.getUniqueId());
                    boolean blocked = manager.isInviteBlocked(player.getUniqueId());
                    player.sendMessage(blocked
                            ? "§c§l✉ §cEinladungen werden jetzt blockiert."
                            : "§a§l✉ §aEinladungen werden jetzt akzeptiert.");
                    Main.open(manager, player);
                } else {
                    InviteList.open(manager, player);
                }
            }
            case SHIELD -> ClanList.open(manager, player, 1);
            default -> {}
        }
    }

    @SuppressWarnings("unused")
    private void handleMainInClan(Player player, ItemStack clicked, ClickType click) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) { player.closeInventory(); return; }

        switch (clicked.getType()) {
            case BARRIER -> player.closeInventory();
            case PLAYER_HEAD -> Members.open(plugin, manager, player, 1);
            case WRITABLE_BOOK -> {
                if (!manager.canManage(clan, player.getUniqueId())) {
                    player.sendMessage("§cDu hast keine Berechtigung, Spieler einzuladen.");
                    return;
                }
                Players.open(plugin, manager, player, 1);
            }
            case SHIELD -> Ally.openList(manager, player, 1);
            case BUNDLE -> {
                if (!manager.hasPermission(clan, player.getUniqueId(), Permission.VAULT)) {
                    player.sendMessage("§cDu hast keine Berechtigung, den Clan-Vault zu öffnen.");
                    return;
                }
                player.openInventory(manager.openVault(player, clan));
            }
            case ANVIL -> SettingsMenu.open(manager, chatInput, player);
            case SPYGLASS -> ClanList.open(manager, player, 1);
            case IRON_SPEAR -> Overview.open(manager, player);
            case COMPASS -> WarpList.open(plugin, manager, player, 1);
            default -> {}
        }
    }

    @SuppressWarnings("deprecation")
    private void handleMembers(Player player, ItemStack clicked) {
        GuiHolder holder = (GuiHolder) player.getOpenInventory().getTopInventory().getHolder();
        String adminClanName = holder != null ? holder.get("adminClanName") : null;
        boolean isAdmin = adminClanName != null;

        if (clicked.getType() == Material.ARROW) {
            if (isAdmin) ClanMenu.open(manager, player, adminClanName);
            else Main.open(manager, player);
            return;
        }
        if (clicked.getType() == Material.LIGHT_GRAY_DYE) {
            if (holder == null) return;
            String name = clicked.hasItemMeta() ? clicked.getItemMeta().getDisplayName() : "";
            int page = holder.get("page", 1);
            if (name.contains("«")) {
                if (isAdmin) Members.openForAdmin(plugin, manager, player, adminClanName, page - 1);
                else Members.open(plugin, manager, player, page - 1);
            }
            if (name.contains("»")) {
                if (isAdmin) Members.openForAdmin(plugin, manager, player, adminClanName, page + 1);
                else Members.open(plugin, manager, player, page + 1);
            }
            return;
        }

        UUID targetId = Members.extractMemberId(plugin, clicked);
        if (targetId == null) return;

        if (isAdmin) {
            MemberActions.openForAdmin(manager, player, targetId, adminClanName);
        } else {
            if (targetId.equals(player.getUniqueId())) return;
            Clan clan = manager.getClan(player.getUniqueId());
            if (clan == null || !manager.canManage(clan, player.getUniqueId())) {
                player.sendMessage("§cDu hast keine Berechtigung, Mitglieder zu verwalten.");
                return;
            }
            MemberActions.open(manager, player, targetId);
        }
    }

    private void handleMemberActions(Player player, ItemStack clicked, GuiHolder holder) {
        String targetIdStr = holder.get("targetId");
        String targetName = holder.get("targetName");
        String adminClanName = holder.get("adminClanName");
        boolean isAdmin = adminClanName != null;
        if (targetIdStr == null) return;
        UUID targetId;
        try { targetId = UUID.fromString(targetIdStr); } catch (IllegalArgumentException e) { return; }
        Clan clan = isAdmin ? manager.getClanByName(adminClanName) : manager.getClan(player.getUniqueId());
        if (clan == null) { player.closeInventory(); return; }

        switch (clicked.getType()) {
            case ARROW -> {
                if (isAdmin) Members.openForAdmin(plugin, manager, player, adminClanName, 1);
                else Members.open(plugin, manager, player, 1);
            }
            case COMPASS -> {
                org.bukkit.Location warp = clan.getMemberWarp(targetId);
                if (warp == null) {
                    player.sendMessage("§c" + targetName + " hat keinen Heim-Warp gesetzt.");
                } else {
                    player.closeInventory();
                    startCountdownTeleport(player, warp, targetName + "'s Heim");
                    return;
                }
                if (isAdmin) MemberActions.openForAdmin(manager, player, targetId, adminClanName);
                else MemberActions.open(manager, player, targetId);
            }
            case NAME_TAG -> {
                if (isAdmin) RankPicker.openForAdmin(manager, player, targetId, adminClanName);
                else RankPicker.open(manager, player, targetId);
            }
            case BARRIER -> Confirm.open(player,
                    "§c§lSpieler kicken?",
                    "Kicken", "kick",
                    "targetId", targetIdStr, "targetName", targetName,
                    isAdmin ? "adminClanName" : "_dummy", isAdmin ? adminClanName : "");
            case GOLDEN_HELMET -> Confirm.open(player,
                    "§6§lFührung übertragen?",
                    "Übertragen", "transfer", "targetId", targetIdStr, "targetName", targetName);
            default -> {}
        }
    }

    @SuppressWarnings("deprecation")
    private void handleMemberRankPicker(Player player, ItemStack clicked, GuiHolder holder) {
        String targetIdStr = holder.get("targetId");
        String targetName = holder.get("targetName");
        String adminClanName = holder.get("adminClanName");
        boolean isAdmin = adminClanName != null;
        if (targetIdStr == null) return;
        UUID targetId;
        try { targetId = UUID.fromString(targetIdStr); } catch (IllegalArgumentException e) { return; }
        Clan clan = isAdmin ? manager.getClanByName(adminClanName) : manager.getClan(player.getUniqueId());
        if (clan == null) { player.closeInventory(); return; }

        if (clicked.getType() == Material.ARROW) {
            if (isAdmin) MemberActions.openForAdmin(manager, player, targetId, adminClanName);
            else MemberActions.open(manager, player, targetId);
            return;
        }

        if ((clicked.getType() == Material.LIME_DYE || clicked.getType() == Material.GRAY_DYE)
                && clicked.hasItemMeta()) {
            if (!isAdmin && !manager.hasPermission(clan, player.getUniqueId(), Permission.PROMOTE)) {
                player.sendMessage("§cDu hast keine Berechtigung, Ränge zu setzen.");
                MemberActions.open(manager, player, targetId);
                return;
            }
            String displayName = clicked.getItemMeta().getDisplayName();
            String stripped = displayName.replaceAll("§.", "").replace("(aktuell)", "").trim();
            for (Rank rank : clan.getOrderedRanks()) {
                if (rank.getName().equals(stripped)) {
                    clan.addMember(targetId, rank.getId());
                    clan.addLog((isAdmin ? "§7(Admin) §f" : "§f") + player.getName() + " §7hat §f" + targetName + " §7auf §f" + rank.getName() + " §7gesetzt.");
                    plugin.saveAsync();
                    plugin.getPlayerListener().refreshClanTabNames(clan);
                    String roleName = rank.getColoredName();
                    player.sendMessage("§6§l §f" + targetName + " §awurde auf " + roleName + " §aGesetzt.");
                    Player target = Bukkit.getPlayer(targetId);
                    if (target != null) target.sendMessage("§6§l✔ §aDein Rang wurde auf " + roleName + " §agesetzt.");
                    if (isAdmin) MemberActions.openForAdmin(manager, player, targetId, adminClanName);
                    else MemberActions.open(manager, player, targetId);
                    return;
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void handleInviteList(Player player, ItemStack clicked, ClickType click) {
        if (clicked.getType() == Material.ARROW) { Main.open(manager, player); return; }
        if (clicked.getType() != Material.PAPER || !clicked.hasItemMeta()) return;

        String displayName = clicked.getItemMeta().getDisplayName();
        if (displayName.isBlank()) return;
        String clanName = displayName.replaceAll("§.", "").trim();
        if (clanName.isEmpty()) return;

        if (click == ClickType.RIGHT) {
            manager.declineInvite(player.getUniqueId(), clanName);
            player.sendMessage("§c Einladung von §f" + clanName + " §cabgelehnt.");
            InviteList.open(manager, player);
        } else {
            Clan invitingClan = manager.getClanByName(clanName);
            if (invitingClan != null && manager.isAtWar(invitingClan.getName())) {
                player.sendMessage("§cDer Clan §f" + clanName + " §cbefindet sich aktuell im Krieg. Warte ab bis er sich wieder im Frieden befindet.");
                return;
            }
            boolean ok = manager.acceptInvite(player, clanName);
            if (ok) {
                Clan joined = manager.getClan(player.getUniqueId());
                if (joined != null) joined.addLog("§f" + player.getName() + " §7ist dem Clan beigetreten.");
                plugin.saveAsync();
                plugin.getPlayerListener().updateTabName(player, joined);
                player.closeInventory();
                Main.open(manager, player);
            } else {

                UUID pid = player.getUniqueId();
                if (manager.isClanBanned(pid)) {
                    long expiry = manager.getClanBanExpiry(pid);
                    if (expiry == -1L) {
                        player.sendMessage("§cDu wurdest dauerhaft gesperrt und kannst daher keinem Clan beitreten. Wende dich an einen Admin.");
                    } else {
                        String until = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date(expiry));
                        player.sendMessage("§cDu bist bis zum §e" + until + "§c gesperrt und kannst daher keinem Clan beitreten.");
                    }
                } else {
                    player.sendMessage("§cDie Einladung ist nicht mehr gültig oder der Clan ist voll.");
                }
                InviteList.open(manager, player);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void handleInvitePlayers(Player player, ItemStack clicked, GuiHolder holder) {
        if (clicked.getType() == Material.ARROW) {
            Main.open(manager, player); return;
        }
        if (clicked.getType() == Material.LIGHT_GRAY_DYE) {
            int page = holder.get("page", 1);
            String sq = holder.get("searchQuery");
            String name = clicked.hasItemMeta() ? clicked.getItemMeta().getDisplayName() : "";
            if (name.contains("«")) Players.open(plugin, manager, player, page - 1, sq);
            if (name.contains("»")) Players.open(plugin, manager, player, page + 1, sq);
            return;
        }

        if (clicked.getType() == Material.NAME_TAG && clicked.hasItemMeta()) {
            String name = clicked.getItemMeta().getDisplayName();
            if (name.contains("Spieler suchen")) {
                player.closeInventory();
                chatInput.awaitInput(player, query -> {
                    String q = (query == null || query.isBlank()) ? null : query.trim();
                    Players.open(plugin, manager, player, 1, q);
                });
                return;
            }
        }

        UUID targetId = Players.extractTargetId(plugin, clicked);
        if (targetId == null) return;
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) return;

        Player target = Bukkit.getPlayer(targetId);
        String targetName = target != null ? target.getName() : "Unbekannt";

        if (manager.isInviteBlocked(targetId)) {
            player.sendMessage("§c§l✉ §c" + targetName + " akzeptiert keine Clan-Einladungen.");
        } else if (manager.isAtWar(clan.getName())) {
            player.sendMessage("§cDein Clan ist im Krieg und kann keine neuen Mitglieder einladen.");
        } else if (manager.getClan(targetId) != null) {
            player.sendMessage("§c" + targetName + " ist bereits in einem Clan.");
        } else {
            boolean sent = manager.sendInvite(clan, player.getUniqueId(), targetId);
            player.sendMessage(sent
                    ? "§a§l✉ §aEinladung an §f" + targetName + " §aversendet!"
                    : "§c" + targetName + " wurde von deinem Clan bereits eingeladen.");
        }
        int page = holder.get("page", 1);
        Players.open(plugin, manager, player, page, holder.get("searchQuery"));
    }

    private void handleSettings(Player player, ItemStack clicked, ClickType click) {
        Settings settings = manager.getSettings();
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) { player.closeInventory(); return; }
        boolean isOwner = clan.getOwnerId().equals(player.getUniqueId());

        switch (clicked.getType()) {
            case ARROW -> Main.open(manager, player);
            case NAME_TAG -> {
                if (!manager.hasPermission(clan, player.getUniqueId(), Permission.RENAME)) {
                    player.sendMessage("§cDu hast keine Berechtigung, den Clan umzubenennen.");
                    return;
                }
                if (manager.getActiveWar(clan.getName()) != null) {
                    player.sendMessage("§cDer Clan befindet sich gerade im Krieg!");
                    return;
                }
                player.closeInventory();
                chatInput.awaitInput(player, newName -> {
                    if (newName.length() < Settings.CLAN_MIN_NAME_LENGTH) {
                        player.sendMessage("§cDer Name muss mindestens " + Settings.CLAN_MIN_NAME_LENGTH + " Zeichen lang sein.");
                        return;
                    }
                    if (newName.length() > Settings.CLAN_MAX_NAME_LENGTH) {
                        player.sendMessage("§cDer Name darf maximal " + Settings.CLAN_MAX_NAME_LENGTH + " Zeichen lang sein.");
                        return;
                    }
                    Clan c = manager.getClan(player.getUniqueId());
                    if (c == null || !manager.hasPermission(c, player.getUniqueId(), Permission.RENAME)) {
                        player.sendMessage("§cDu hast keine Berechtigung, den Clan umzubenennen.");
                        return;
                    }
                    if (manager.getClanByName(newName) != null && !newName.equalsIgnoreCase(c.getName())) {
                        player.sendMessage("§cEin Clan mit diesem Namen existiert bereits.");
                        return;
                    }
                    manager.renameClan(c, newName);
                    c.addLog("§f" + player.getName() + " §7hat den Clan umbenannt zu §f" + newName);
                    plugin.saveAsync();
                    manager.broadcastToClan(c, "§eDer Clan wurde umbenannt zu §f" + newName + "§e!");
                    plugin.getPlayerListener().refreshClanTabNames(c);
                });
            }
            case TRIAL_KEY -> {
                if (!manager.hasPermission(clan, player.getUniqueId(), Permission.MANAGE_RANKS)) {
                    player.sendMessage("§cDu hast keine Berechtigung, Ränge zu verwalten.");
                    return;
                }
                Management.open(manager, player);
            }
            case FIRE_CHARGE, SNOWBALL -> {
                if (!manager.hasPermission(clan, player.getUniqueId(), Permission.FRIENDLY_FIRE)) {
                    player.sendMessage("§cDu hast keine Berechtigung, Friendly Fire zu ändern.");
                    return;
                }
                if (click != ClickType.DOUBLE_CLICK) return;
                boolean newFf = !clan.isFriendlyFire();
                clan.setFriendlyFire(newFf);
                clan.addLog("§f" + player.getName() + " §7hat Friendly Fire " + (newFf ? "§aaktiviert" : "§cdeaktiviert") + "§7.");
                plugin.saveAsync();
                SettingsMenu.open(manager, chatInput, player);
            }
            case TNT -> {
                if (isOwner) {
                    if (manager.getActiveWar(clan.getName()) != null) {
                        player.sendMessage("§cDer Clan kann nicht aufgelöst werden, während ein Krieg läuft!");
                        return;
                    }
                    Confirm.open(player,
                        "§c§lClan auflösen?",
                        "Auflösen", "disband");
                }
            }
            case DARK_OAK_DOOR -> {
                if (!isOwner) Confirm.open(player,
                        "§c§lClan verlassen?",
                        "Verlassen", "leave");
            }
            case CLOCK -> {
                if (!manager.hasPermission(clan, player.getUniqueId(), Permission.SET_TAG)) {
                    player.sendMessage("§cDu hast keine Berechtigung, die Tag-Farbe zu ändern.");
                    return;
                }
                if (manager.getActiveWar(clan.getName()) != null) {
                    player.sendMessage("§cDer Clan befindet sich gerade im Krieg!");
                    return;
                }
                Picker.open(manager, player);
            }
            case BOOK -> {
                if (!manager.hasPermission(clan, player.getUniqueId(), Permission.VIEW_LOG)) {
                    player.sendMessage("§cDu hast keine Berechtigung, das Clan-Log einzusehen.");
                    return;
                }
                Log.open(manager, player);
            }
            case ENDER_EYE, ENDER_PEARL -> {

                manager.toggleGlow(player.getUniqueId());
                plugin.saveAsync();
                SettingsMenu.open(manager, chatInput, player);
            }
            default -> {
                if (clicked.getType().name().endsWith("_BANNER")) {
                    if (!manager.hasPermission(clan, player.getUniqueId(), Permission.SETTINGS)) {
                        player.sendMessage("§cDu hast keine Berechtigung, Einstellungen zu ändern.");
                        return;
                    }
                    BannerColor.open(manager, player);
                }
            }
        }
    }

    private void handleTagColor(Player player, ItemStack clicked) {
        GuiHolder holder = (GuiHolder) player.getOpenInventory().getTopInventory().getHolder();
        String adminClanName = holder != null ? holder.get("adminClanName") : null;
        boolean isAdmin = adminClanName != null;
        if (clicked.getType() == Material.ARROW) {
            if (isAdmin) ClanMenu.open(manager, player, adminClanName);
            else SettingsMenu.open(manager, chatInput, player);
            return;
        }
        Clan clan = isAdmin ? manager.getClanByName(adminClanName) : manager.getClan(player.getUniqueId());
        if (clan == null) return;
        if (!isAdmin) {
            if (!manager.hasPermission(clan, player.getUniqueId(), Permission.SET_TAG)) return;
            if (manager.getActiveWar(clan.getName()) != null) {
                player.sendMessage("§cDer Clan befindet sich gerade im Krieg!");
                player.closeInventory();
                return;
            }
        }
        switch (clicked.getType()) {
            case WHITE_DYE   -> Static.open(manager, player, adminClanName);
            case NETHER_STAR -> Dynamic.open(manager, player, adminClanName);
            default -> {}
        }
    }

    @SuppressWarnings("deprecation")
    private void handleTagColorStatic(Player player, ItemStack clicked) {
        GuiHolder holder = (GuiHolder) player.getOpenInventory().getTopInventory().getHolder();
        String adminClanName = holder != null ? holder.get("adminClanName") : null;
        boolean isAdmin = adminClanName != null;
        if (clicked.getType() == Material.ARROW) { Picker.open(manager, player, adminClanName); return; }
        Clan clan = isAdmin ? manager.getClanByName(adminClanName) : manager.getClan(player.getUniqueId());
        if (clan == null) return;
        if (!isAdmin) {
            if (!manager.hasPermission(clan, player.getUniqueId(), Permission.SET_TAG)) return;
            if (manager.getActiveWar(clan.getName()) != null) {
                player.sendMessage("§cDer Clan befindet sich gerade im Krieg!");
                player.closeInventory();
                return;
            }
        }
        if (clicked.getType().name().endsWith("_DYE") && clicked.hasItemMeta()) {
            String displayName = clicked.getItemMeta().getDisplayName();
            if (displayName.length() < 2) return;
            String colorCode = displayName.substring(0, 2);
            if (colorCode.charAt(0) != '§') return;
            char c = Character.toLowerCase(colorCode.charAt(1));
            if ("0123456789abcdef".indexOf(c) < 0) return;
            applyTagColor(clan, "§" + c, player, false, adminClanName);
        }
    }

    @SuppressWarnings("deprecation")
    private void handleTagColorDynamic(Player player, ItemStack clicked) {
        GuiHolder holder = (GuiHolder) player.getOpenInventory().getTopInventory().getHolder();
        String adminClanName = holder != null ? holder.get("adminClanName") : null;
        boolean isAdmin = adminClanName != null;
        if (clicked.getType() == Material.ARROW) {
            Picker.open(manager, player, adminClanName); return;
        }
        if (clicked.getType() == Material.LIGHT_GRAY_DYE) {
            String dyeName = clicked.hasItemMeta() ? clicked.getItemMeta().getDisplayName() : "";
            int page = holder != null ? holder.get("page", 1) : 1;
            if (dyeName.contains("«")) { Dynamic.open(manager, player, adminClanName, Math.max(1, page - 1)); return; }
            if (dyeName.contains("»")) { Dynamic.open(manager, player, adminClanName, page + 1); return; }
            return;
        }
        Clan clan = isAdmin ? manager.getClanByName(adminClanName) : manager.getClan(player.getUniqueId());
        if (clan == null) return;
        if (!isAdmin) {
            if (!manager.hasPermission(clan, player.getUniqueId(), Permission.SET_TAG)) return;
            if (manager.getActiveWar(clan.getName()) != null) {
                player.sendMessage("§cDer Clan befindet sich gerade im Krieg!");
                player.closeInventory();
                return;
            }
            if (clan.getLevel() < manager.getSettings().dynamicColorsMinLevel()) {
                player.sendMessage("§cDynamische Farben sind erst ab Clan Lvl. " + manager.getSettings().dynamicColorsMinLevel() + " verfügbar.");
                return;
            }
        }
        if (clicked.getType().name().endsWith("_STAINED_GLASS_PANE") && clicked.hasItemMeta()) {
            String format = clicked.getItemMeta().getPersistentDataContainer()
                    .get(Picker.TAG_FORMAT_KEY, PersistentDataType.STRING);
            if (format == null) return;
            applyTagColor(clan, format, player, true, adminClanName);
        }
    }

    private void applyTagColor(Clan clan, String format, Player player, boolean dynamic, String adminClanName) {
        String oldColor = clan.getTagColor();
        clan.setTagColor(format);
        clan.addLog("§f" + player.getName() + " §7hat die Tag-Farbe geändert: " + oldColor + clan.getName() + " §8→ " + format + clan.getName() + "§7.");
        plugin.saveAsync();
        plugin.getPlayerListener().refreshClanTabNames(clan);
        if (dynamic) {
            GuiHolder h = (GuiHolder) player.getOpenInventory().getTopInventory().getHolder();
            int page = h != null ? h.get("page", 1) : 1;
            Dynamic.open(manager, player, adminClanName, page);
        } else {
            Static.open(manager, player, adminClanName);
        }
    }

    private void handleBannerColor(Player player, ItemStack clicked) {
        GuiHolder holder = (GuiHolder) player.getOpenInventory().getTopInventory().getHolder();
        String adminClanName = holder != null ? holder.get("adminClanName") : null;
        boolean isAdmin = adminClanName != null;
        if (clicked.getType() == Material.ARROW) {
            if (isAdmin) ClanMenu.open(manager, player, adminClanName);
            else SettingsMenu.open(manager, chatInput, player);
            return;
        }
        if (clicked.getType() == Material.LOOM) {
            BannerPattern.openPatternList(manager, player, adminClanName);
            return;
        }
        if (clicked.getType().name().endsWith("_BANNER")) {
            Clan clan = isAdmin ? manager.getClanByName(adminClanName) : manager.getClan(player.getUniqueId());
            if (clan == null) return;
            String newColor = clicked.getType().name().replace("_BANNER", "");
            clan.setBannerColor(newColor);
            clan.addLog("§f" + player.getName() + " §7hat die Banner-Farbe geändert.");
            plugin.saveAsync();
            BannerColor.open(manager, player, adminClanName);
        }
    }

    private void handleBannerPatternList(Player player, ItemStack clicked) {
        GuiHolder holder = (GuiHolder) player.getOpenInventory().getTopInventory().getHolder();
        String adminClanName = holder != null ? holder.get("adminClanName") : null;
        boolean isAdmin = adminClanName != null;
        Clan clan = isAdmin ? manager.getClanByName(adminClanName) : manager.getClan(player.getUniqueId());
        if (clan == null) return;
        switch (clicked.getType()) {
            case ARROW -> BannerColor.open(manager, player, adminClanName);
            case LIME_DYE -> BannerPattern.openPatternTypePicker(manager, player, 1, adminClanName);
            case RED_DYE -> Confirm.open(player,
                    "§c§lAlle Muster entfernen?",
                    "Entfernen", "clearBannerPatterns",
                    isAdmin ? "adminClanName" : "_dummy", isAdmin ? adminClanName : "");
            default -> {
                if (clicked.getType().name().endsWith("_BANNER") && clicked.hasItemMeta()) {
                    for (int i = 0; i < 7; i++) {
                        ItemStack item = player.getOpenInventory().getTopInventory().getItem(10 + i);
                        if (item != null && item.equals(clicked) && i < clan.getBannerPatterns().size()) {
                            Confirm.open(player,
                                    "§c§lMuster entfernen?",
                                    "Entfernen", "removeBannerPattern",
                                    "patternIndex", String.valueOf(i),
                                    isAdmin ? "adminClanName" : "_dummy", isAdmin ? adminClanName : "");
                            break;
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void handleBannerPatternType(Player player, ItemStack clicked, GuiHolder holder) {
        String adminClanName = holder.get("adminClanName");
        if (clicked.getType() == Material.ARROW) {
            BannerPattern.openPatternList(manager, player, adminClanName); return;
        }
        if (clicked.getType() == Material.LIGHT_GRAY_DYE) {
            String name = clicked.hasItemMeta() ? clicked.getItemMeta().getDisplayName() : "";
            int page = holder.get("page", 1);
            if (name.contains("«")) { BannerPattern.openPatternTypePicker(manager, player, page - 1, adminClanName); return; }
            if (name.contains("»")) { BannerPattern.openPatternTypePicker(manager, player, page + 1, adminClanName); return; }
            return;
        }
        if (clicked.getType().name().endsWith("_BANNER") && clicked.hasItemMeta()) {
            String displayName = clicked.getItemMeta().getDisplayName();
            String typeName = displayName.replaceAll("§.", "").trim();
            for (var pt : org.bukkit.Registry.BANNER_PATTERN) {
                if (GuiHelper.patternTypeName(pt).equals(typeName)) {
                    BannerPattern.openPatternColorPicker(manager, player, GuiHelper.patternTypeKeyValue(pt), adminClanName);
                    return;
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void handleBannerPatternColor(Player player, ItemStack clicked, GuiHolder holder) {
        String adminClanName = holder.get("adminClanName");
        Clan clan = adminClanName != null ? manager.getClanByName(adminClanName) : manager.getClan(player.getUniqueId());
        if (clan == null) return;
        if (clicked.getType() == Material.ARROW) {
            BannerPattern.openPatternTypePicker(manager, player, 1, adminClanName); return;
        }
        if (!clicked.getType().name().endsWith("_BANNER") || !clicked.hasItemMeta()) return;
        String patternKey = holder.get("patternKey");
        if (patternKey == null) return;
        patternKey = patternKey.toLowerCase(java.util.Locale.ROOT);
        String displayName = clicked.getItemMeta().getDisplayName();
        String colorName = displayName.replaceAll("§.", "").trim();

        org.bukkit.block.banner.PatternType pt;
        try {
            pt = org.bukkit.Registry.BANNER_PATTERN.get(org.bukkit.NamespacedKey.minecraft(patternKey));
        } catch (IllegalArgumentException ex) {
            return;
        }
        if (pt == null) return;
        var dc = GuiHelper.dyeColorFromName(GuiHelper.colorNameToKey(colorName));
        clan.addBannerPattern(new org.bukkit.block.banner.Pattern(dc, pt));
        BannerPattern.openPatternList(manager, player, adminClanName);
    }

    @SuppressWarnings("unused")
    private void handleWarMenu(Player player, ItemStack clicked, ClickType click) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) { player.closeInventory(); return; }
        boolean canWar = manager.hasPermission(clan, player.getUniqueId(), Permission.WAR);

        switch (clicked.getType()) {
            case PLAYER_HEAD -> Leaderboard.open(manager, player);
            case ARROW -> Main.open(manager, player);
            case BOOK -> History.open(manager, player);
            case NETHERITE_SWORD -> {
                if (!canWar) return;
                if (manager.getActiveWar(clan.getName()) != null) return;
                if (clan.getMemberCount() < manager.getSettings().warMinClanSize()) {
                    player.sendMessage("§cDer Clan muss mindestens " + manager.getSettings().warMinClanSize() + " Mitglieder haben.");
                    return;
                }
                ClanList.openForWar(manager, player, 1);
            }
            case WRITABLE_BOOK, WRITTEN_BOOK -> Requests.open(manager, player);
            case WHITE_BANNER -> {
                if (canWar) Confirm.open(player,
                        "§c§lAufgeben?",
                        "Aufgeben", "surrender");
            }
            default -> {}
        }
    }

    @SuppressWarnings("deprecation")
    private void handleWarRequests(Player player, ItemStack clicked, ClickType click) {
        if (clicked.getType() == Material.ARROW || isCloseButton(clicked) || clicked.getType() == Material.BARRIER) {
            Overview.open(manager, player);
            return;
        }
        if (clicked.getType() != Material.WRITTEN_BOOK) return;
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) return;
        if (!manager.hasPermission(clan, player.getUniqueId(), Permission.WAR)) return;
        if (!clicked.hasItemMeta()) return;
        String displayName = clicked.getItemMeta().getDisplayName();
        String fromName = displayName.replaceAll("§.", "").replace("Herausforderung von ", "").trim();
        if (fromName.isEmpty()) return;
        if (click == ClickType.LEFT) {
            if (manager.isAtWar(clan.getName())) {
                player.sendMessage("§cDein Clan befindet sich aktuell im Krieg. Warte ab bis er sich wieder im Frieden befindet.");
                return;
            }
            if (clan.getMemberCount() < manager.getSettings().warMinClanSize()) {
                player.sendMessage("§cDer Clan muss insgesamt " + manager.getSettings().warMinClanSize() + " Mitglieder für Kriegsanfragen benötigt.");
                Requests.open(manager, player);
                return;
            }
            Clan fromClan = manager.getClanByName(fromName);
            if (fromClan != null && fromClan.getMemberCount() < manager.getSettings().warMinClanSize()) {
                player.sendMessage("§cDer anfordernde Clan muss insgesamt " + manager.getSettings().warMinClanSize() + " Mitglieder für Kriegsanfragen benötigt.");
                Requests.open(manager, player);
                return;
            }
            boolean ok = manager.acceptWar(clan, fromName);
            if (!ok) player.sendMessage("§cKrieg konnte nicht gestartet werden.");
            else { Overview.open(manager, player); return; }
        } else if (click == ClickType.RIGHT) {
            manager.declineWar(clan, fromName);
            player.sendMessage("§7 Kriegsanfrage von §f" + fromName + " §7abgelehnt.");
        }
        Requests.open(manager, player);
    }

    private void handleRolePermissions(Player player, ItemStack clicked) {
        GuiHolder holder = (GuiHolder) player.getOpenInventory().getTopInventory().getHolder();
        if (clicked.getType() == Material.ARROW) {
            String rankId = holder != null ? holder.get("rankId") : null;
            if (rankId != null) RankActions.open(manager, player, rankId);
            else Management.open(manager, player);
        }
    }

    @SuppressWarnings("deprecation")
    private void handleRolePermEdit(Player player, ItemStack clicked, GuiHolder holder) {
        if (clicked.getType() == Material.ARROW) {
            String rankId = holder.get("rankId");
            if (rankId != null) RankActions.open(manager, player, rankId);
            else Management.open(manager, player);
            return;
        }
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null || !clan.getOwnerId().equals(player.getUniqueId())) return;
        String rankId = holder.get("rankId");
        if (rankId == null) return;

        if (clicked.getType() == Material.LIME_DYE || clicked.getType() == Material.GRAY_DYE) {
            if (!clicked.hasItemMeta()) return;
            String displayName = clicked.getItemMeta().getDisplayName();
            String stripped = displayName.replaceAll("§.", "").trim();
            for (Permission perm : Permission.values()) {
                if (perm.getDisplayName().equals(stripped)) {
                    clan.setRankPermission(rankId, perm, !clan.rankHasPermission(rankId, perm));
                    plugin.saveAsync();
                    Permissions.openPermissions(manager, player, rankId);
                    return;
                }
            }
        }
    }

    @SuppressWarnings({"deprecation", "unused"})
    private void handleRankManagement(Player player, ItemStack clicked, GuiHolder holder, ClickType click) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null || !clan.getOwnerId().equals(player.getUniqueId())) {
            player.sendMessage("§cNur der Clan-Besitzer kann Ränge verwalten.");
            return;
        }
        if (clicked.getType() == Material.ARROW) { SettingsMenu.open(manager, chatInput, player); return; }

        if (clicked.getType() == Material.LIME_DYE && clicked.hasItemMeta()) {
            String name = clicked.getItemMeta().getDisplayName();
            if (name.contains("Neuer Rang")) {
                player.closeInventory();
                chatInput.awaitInput(player, rankName -> {
                    int maxRanks = Math.min(manager.getSettings().rankMaxTotal(), GuiHelper.CONTENT_SLOTS_28.length);
                    if (clan.getOrderedRanks().size() >= maxRanks) {
                        player.sendMessage("§cMaximal " + maxRanks + " Ränge erlaubt.");
                        Management.open(manager, player);
                        return;
                    }
                    if (rankName.length() < Settings.RANK_MIN_NAME_LENGTH || rankName.length() > Settings.RANK_MAX_NAME_LENGTH) {
                        player.sendMessage("§cDer Rang-Name muss zwischen " + Settings.RANK_MIN_NAME_LENGTH + " und " + Settings.RANK_MAX_NAME_LENGTH + " Zeichen lang sein.");
                        Management.open(manager, player);
                        return;
                    }
                    Clan c = manager.getClan(player.getUniqueId());
                    if (c == null) return;
                    String newId = "custom_" + UUID.randomUUID().toString().substring(0, 8);
                    Rank newRank = new Rank(newId, rankName, "§6", true);
                    newRank.setPermissions(java.util.EnumSet.noneOf(Permission.class));
                    c.addCustomRank(newRank);
                    plugin.saveAsync();
                    player.sendMessage("§a Rang §f\"" + rankName + "\"§a erstellt!");
                    Management.open(manager, player);
                });
                return;
            }
        }

        if (clicked.hasItemMeta()) {
            String displayName = clicked.getItemMeta().getDisplayName();
            String stripped = displayName.replaceAll("§.", "").trim();
            for (Rank rank : clan.getOrderedRanks()) {
                if (rank.getName().equals(stripped)) {
                    RankActions.open(manager, player, rank.getId());
                    return;
                }
            }
        }
    }

    private void handleRankAction(Player player, ItemStack clicked, GuiHolder holder) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null || !clan.getOwnerId().equals(player.getUniqueId())) return;
        String rankId = holder.get("rankId");
        if (rankId == null) return;
        Rank rank = clan.getRank(rankId);
        if (rank == null) { Management.open(manager, player); return; }

        if (clicked.getType() == Material.ARROW) { Management.open(manager, player); return; }

        switch (clicked.getType()) {
            case NAME_TAG -> {
                player.closeInventory();
                chatInput.awaitInput(player, newName -> {
                    if (newName.length() < Settings.RANK_MIN_NAME_LENGTH || newName.length() > Settings.RANK_MAX_NAME_LENGTH) {
                        player.sendMessage("§cDer Rang-Name muss zwischen " + Settings.RANK_MIN_NAME_LENGTH + " und " + Settings.RANK_MAX_NAME_LENGTH + " Zeichen lang sein.");
                        RankActions.open(manager, player, rankId);
                        return;
                    }
                    Clan c = manager.getClan(player.getUniqueId());
                    if (c == null) return;
                    Rank r = c.getRank(rankId);
                    if (r == null) return;
                    r.setName(newName);
                    c.addLog("§f" + player.getName() + " §7hat einen Rang in §f\"" + newName + "\" §7umbenannt.");
                    plugin.saveAsync();
                    plugin.getPlayerListener().refreshClanTabNames(c);
                    Management.open(manager, player);
                });
            }
            case COMMAND_BLOCK -> {
                if (!Rank.OWNER_ID.equals(rank.getId())) Permissions.openPermissions(manager, player, rankId);
            }
            case BARRIER -> {
                if (rank.isDeletable()) {
                    Confirm.open(player,
                            "§c§lRang löschen?",
                            "Löschen", "deleteRank", "rankId", rankId);
                }
            }
            default -> {}
        }
    }

    @SuppressWarnings("deprecation")
    private void handleClanList(Player player, ItemStack clicked, GuiHolder holder) {
        boolean warMode = "true".equals(holder.get("warMode"));

        if (clicked.getType() == Material.ARROW) {
            if (warMode) Overview.open(manager, player);
            else Main.open(manager, player);
            return;
        }
        if (clicked.getType() == Material.LIGHT_GRAY_DYE) {
            String name = clicked.hasItemMeta() ? clicked.getItemMeta().getDisplayName() : "";
            int page = holder.get("page", 1);
            if (name.contains("«")) {
                if (warMode) ClanList.openForWar(manager, player, page - 1);
                else ClanList.open(manager, player, page - 1);
                return;
            }
            if (name.contains("»")) {
                if (warMode) ClanList.openForWar(manager, player, page + 1);
                else ClanList.open(manager, player, page + 1);
            }
            return;
        }

        if (clicked.getType() == Material.NAME_TAG && !warMode) {
            player.closeInventory();
            chatInput.awaitInput(player, query -> {
                var results = manager.searchClans(query);
                if (results.isEmpty()) {
                    player.sendMessage("§cKeine Clans mit dem Suchbegriff \"" + query + "\" gefunden.");
                    ClanList.open(manager, player, 1);
                } else {
                    ClanList.openSearch(manager, player, results, query);
                }
            });
            return;
        }

        if (clicked.getType().name().endsWith("_BANNER") && clicked.hasItemMeta()) {
            String displayName = clicked.getItemMeta().getDisplayName();
            String clanName = displayName.replaceAll("§.", "").trim();
            int bracketIdx = clanName.indexOf('[');
            if (bracketIdx > 0) clanName = clanName.substring(0, bracketIdx).trim();
            Clan targetClan = manager.getClanByName(clanName);
            if (targetClan != null) openClanListInfo(player, targetClan);
        }
    }

    private void openClanListInfo(Player player, Clan targetClan) {
        ClanList.openInfo(manager, player, targetClan, manager.getSettings().warTargetKills());
    }

    private void handleClanListInfo(Player player, ItemStack clicked, GuiHolder holder) {
        if (clicked.getType() == Material.ARROW) {
            ClanList.open(manager, player, 1);
            return;
        }
        if (clicked.getType() == Material.BARRIER) { player.closeInventory(); return; }

        String clanName = holder.get("clanName");
        if (clanName == null) return;
        Clan playerClan = manager.getClan(player.getUniqueId());
        Clan targetClan = manager.getClanByName(clanName);
        if (targetClan == null) { player.closeInventory(); return; }

        if (clicked.getType() == Material.ENDER_EYE && playerClan != null) {
            Confirm.open(player,
                    "§a§lBündnis anfragen?",
                    "Anfrage senden", "requestAlly",
                    "targetClanName", clanName);
        } else if (clicked.getType() == Material.NETHERITE_SWORD && playerClan != null) {
            if (playerClan.getMemberCount() < manager.getSettings().warMinClanSize()) {
                player.sendMessage("§cDer Clan muss insgesamt " + manager.getSettings().warMinClanSize() + " Mitglieder für Kriegsanfragen benötigt.");
                return;
            }
            if (targetClan.getMemberCount() < manager.getSettings().warMinClanSize()) {
                player.sendMessage("§cDer Ziel-Clan muss insgesamt " + manager.getSettings().warMinClanSize() + " Mitglieder für Kriegsanfragen benötigt.");
                return;
            }
            int kills = manager.getSettings().warTargetKills();
            Confirm.open(player,
                    "§c§lKrieg erklären?",
                    "Krieg erklären", "requestWar",
                    "targetClanName", clanName,
                    "targetKills", String.valueOf(kills));
        } else if (clicked.getType() == Material.BOOK) {
            History.open(manager, player, targetClan.getName());
        }
    }

    @SuppressWarnings("unused")
    private void handleAllies(Player player, ItemStack clicked, ClickType click) {
        if (!clicked.hasItemMeta()) return;
        switch (clicked.getType()) {
            case SHIELD       -> Ally.openList(manager, player, 1);
            case WRITABLE_BOOK, WRITTEN_BOOK -> Ally.openRequests(manager, player, 1);
            case ENDER_EYE    -> ClanList.open(manager, player, 1);
            case ARROW        -> Main.open(manager, player);
            default           -> {}
        }
    }

    @SuppressWarnings("unused")
    private void handleAllyList(Player player, ItemStack clicked, GuiHolder holder, ClickType click) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) { player.closeInventory(); return; }

        if (isCloseButton(clicked)) { player.closeInventory(); return; }
        if (clicked.getType() == Material.ARROW) { Main.open(manager, player); return; }
        if (clicked.getType() == Material.BARRIER) { player.closeInventory(); return; }
        if (clicked.getType() == Material.WRITABLE_BOOK || clicked.getType() == Material.WRITTEN_BOOK) { Ally.openRequests(manager, player, 1); return; }
        if (clicked.getType() == Material.ENDER_EYE) { ClanList.open(manager, player, 1); return; }
        if (clicked.getType() == Material.PAPER) { Ally.openShields(manager, player);
        }
    }

    @SuppressWarnings({"deprecation", "unused"})
    private void handleAllyShields(Player player, ItemStack clicked, GuiHolder holder, ClickType click) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) { player.closeInventory(); return; }

        if (isCloseButton(clicked)) { player.closeInventory(); return; }
        if (clicked.getType() == Material.BARRIER) { player.closeInventory(); return; }
        if (clicked.getType() == Material.ARROW) { Ally.openList(manager, player, 1); return; }

        if (clicked.getType() == Material.SHIELD && click == ClickType.RIGHT && clicked.hasItemMeta()) {
            if (!manager.hasPermission(clan, player.getUniqueId(), Permission.ALLY)) { player.sendMessage("§cDu hast keine Berechtigung, Bündnisse zu verwalten."); return; }
            String displayName = clicked.getItemMeta().getDisplayName();
            String allyName = displayName.replaceAll("§.", "").trim();
            Clan allyClan = manager.getClanByName(allyName);
            if (allyClan != null) {
                Confirm.open(player,
                        "§c§lBündnis auflösen?",
                        "Auflösen", "dissolveAlly", "allyName", allyClan.getName());
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void handleAllyRequests(Player player, ItemStack clicked, GuiHolder holder, ClickType click) {
        Clan clan = manager.getClan(player.getUniqueId());
        if (clan == null) { player.closeInventory(); return; }

        int page = holder.get("page", 1);

        if (clicked.getType() == Material.ARROW) {
            Main.open(manager, player); return;
        }
        if (clicked.getType() == Material.LIGHT_GRAY_DYE && clicked.hasItemMeta()) {
            String name = clicked.getItemMeta().getDisplayName();
            if (name.contains("«")) { Ally.openRequests(manager, player, page - 1); return; }
            if (name.contains("»")) { Ally.openRequests(manager, player, page + 1); return; }
            return;
        }

        if (clicked.getType() == Material.BARRIER) { player.closeInventory(); return; }

        if ((clicked.getType() == Material.WRITABLE_BOOK || clicked.getType() == Material.WRITTEN_BOOK) && clicked.hasItemMeta()) {
            if (!manager.hasPermission(clan, player.getUniqueId(), Permission.ALLY)) { player.sendMessage("§cDu hast keine Berechtigung, Bündnisse zu verwalten."); return; }
            String displayName = clicked.getItemMeta().getDisplayName();
            String reqName = displayName.replaceAll("§.", "").trim();
            if (reqName.startsWith("Anfrage von ")) {
                reqName = reqName.substring("Anfrage von ".length()).trim();
            }
            if (click == ClickType.LEFT) {
                if (manager.isAtWar(clan.getName())) {
                    player.sendMessage("§cDein Clan befindet sich aktuell im Krieg. Warte ab bis er sich wieder im Frieden befindet.");
                    return;
                }
                manager.acceptAlly(clan, reqName);
                clan.addLog("§f" + player.getName() + " §7hat die Bündnis-Anfrage von §f" + reqName + " §7angenommen.");
                Clan reqClan = manager.getClanByName(reqName);
                if (reqClan != null) reqClan.addLog("§7Bündnis-Anfrage an §f" + clan.getName() + " §7wurde angenommen.");
                plugin.saveAsync();
            } else if (click == ClickType.RIGHT) {
                manager.declineAllyRequest(clan, reqName);
            }
            Ally.openRequests(manager, player, page);
        }
    }

    private void handleConfirm(Player player, ItemStack clicked, GuiHolder holder) {
        String action = holder.get("action");
        if (action == null) { player.closeInventory(); return; }
        if (clicked.getType() == Material.RED_CONCRETE) {
            String cancelAdminClanName = holder.get("adminClanName");
            player.closeInventory();
            if (cancelAdminClanName != null) ClanMenu.open(manager, player, cancelAdminClanName);
            else Main.open(manager, player);
            return;
        }
        if (clicked.getType() != Material.LIME_CONCRETE) return;

        Clan clan = manager.getClan(player.getUniqueId());

        switch (action) {
            case "setWarp" -> {
                if (clan != null) {
                    clan.setMemberWarp(player.getUniqueId(), player.getLocation());
                    plugin.saveAsync();
                    player.sendMessage("§a Dein Warp-Punkt wurde überschrieben!");
                }
                WarpList.open(plugin, manager, player, 1);
            }
            case "clearBannerPatterns" -> {
                String adminBannerClanName = holder.get("adminClanName");
                Clan bannerClan = adminBannerClanName != null ? manager.getClanByName(adminBannerClanName) : clan;
                if (bannerClan != null) bannerClan.clearBannerPatterns();
                plugin.saveAsync();
                BannerPattern.openPatternList(manager, player, adminBannerClanName);
            }
            case "removeBannerPattern" -> {
                String adminBannerClanName = holder.get("adminClanName");
                Clan bannerClan = adminBannerClanName != null ? manager.getClanByName(adminBannerClanName) : clan;
                String idxStr = holder.get("patternIndex");
                if (bannerClan != null && idxStr != null) {
                    try {
                        int idx = Integer.parseInt(idxStr);
                        if (idx >= 0 && idx < bannerClan.getBannerPatterns().size()) {
                            bannerClan.removeBannerPattern(idx);
                            plugin.saveAsync();
                        }
                    } catch (NumberFormatException ignored) {}
                }
                BannerPattern.openPatternList(manager, player, adminBannerClanName);
            }
            case "setBase" -> {
                if (clan != null) {
                    boolean isBase2 = "true".equals(holder.get("isBase2"));
                    if (isBase2) {
                        clan.setBase2(player.getLocation());
                    } else {
                        clan.setBase(player.getLocation());
                    }
                    plugin.saveAsync();
                    player.sendMessage("§a Clan-Base " + (isBase2 ? "2 " : "") + "wurde überschrieben!");
                }
                WarpList.open(plugin, manager, player, 1);
            }
            case "leave" -> {
                if (clan != null && !clan.getOwnerId().equals(player.getUniqueId())) {
                    clan.addLog("§f" + player.getName() + " §7hat den Clan verlassen.");
                    manager.leaveClan(player);
                    plugin.getPlayerListener().updateTabName(player, null);
                    plugin.saveAsync();
                } else {
                    player.sendMessage("§cDu kannst als Anführer den Clan nicht verlassen.");
                }
                player.closeInventory();
            }
            case "disband" -> {
                if (clan != null && clan.getOwnerId().equals(player.getUniqueId())) {

                    if (manager.getActiveWar(clan.getName()) != null) {
                        player.sendMessage("§cDer Clan kann nicht aufgelöst werden, während ein Krieg läuft!");
                        player.closeInventory();
                        return;
                    }
                    String name = clan.getName();

                    java.util.List<org.bukkit.entity.Player> onlineMembers = clan.getMembers().keySet().stream()
                            .map(org.bukkit.Bukkit::getPlayer)
                            .filter(p -> p != null && p.isOnline())
                            .toList();
                    plugin.getPlayerListener().clearClanTabNames(clan);
                    manager.disbandClan(clan);
                    plugin.saveAsync();
                    for (org.bukkit.entity.Player member : onlineMembers) {
                        member.sendMessage("§c§l §cDer Clan §f" + name + " §cwurde aufgelöst.");
                        member.closeInventory();
                    }
                }
                player.closeInventory();
            }
            case "kick" -> {
                String targetIdStr = holder.get("targetId");
                String kickAdminClanName = holder.get("adminClanName");
                boolean kickIsAdmin = kickAdminClanName != null;
                Clan kickClan = kickIsAdmin ? manager.getClanByName(kickAdminClanName) : clan;
                if (targetIdStr != null && kickClan != null) {
                    UUID targetId = UUID.fromString(targetIdStr);
                    if (kickIsAdmin || manager.canKick(kickClan, player.getUniqueId(), targetId)) {
                        String kickedName = holder.get("targetName");
                        manager.kickMember(kickClan, targetId);
                        kickClan.addLog((kickIsAdmin ? "§7(Admin) §f" : "§f") + player.getName() + " §7hat §f" + (kickedName != null ? kickedName : targetIdStr) + " §7aus dem Clan geschmissen.");
                        plugin.saveAsync();
                        Player target = Bukkit.getPlayer(targetId);
                        if (target != null) {
                            target.sendMessage("§c§l §cDu wurdest aus dem Clan §f" + kickClan.getName() + " §cgeschmissen.");
                        }
                    } else {
                        player.sendMessage("§cDu kannst diesen Spieler nicht kicken.");
                    }
                }
                player.closeInventory();
                if (kickIsAdmin) Members.openForAdmin(plugin, manager, player, kickAdminClanName, 1);
                else Members.open(plugin, manager, player, 1);
            }
            case "transfer" -> {
                String targetIdStr = holder.get("targetId");
                String targetName = holder.get("targetName");
                if (targetIdStr != null && clan != null) {
                    UUID targetId = UUID.fromString(targetIdStr);
                    boolean ok = manager.transferOwnership(clan, targetId);
                    if (ok) {
                        clan.addLog("§f" + player.getName() + " §7hat die Clan-Führung an §f" + targetName + " §7übertragen.");
                        plugin.saveAsync();
                        player.sendMessage("§6§l §eDu hast die Führung an §f" + targetName + " §eübertragen.");
                        Player target = Bukkit.getPlayer(targetId);
                        if (target != null) target.sendMessage("§6§l §eDu bist jetzt Anführer des Clans §f" + clan.getName() + "§e!");

                        plugin.getPlayerListener().refreshClanTabNames(clan);
                    } else {
                        player.sendMessage("§cÜbertragung fehlgeschlagen.");
                    }
                }
                player.closeInventory();
            }
            case "adminTransfer" -> {
                String aTargetIdStr = holder.get("targetId");
                String aTargetName = holder.get("targetName");
                String aClanName = holder.get("adminClanName");
                if (aTargetIdStr != null && aClanName != null) {
                    Clan aClan = manager.getClanByName(aClanName);
                    if (aClan != null) {
                        UUID aTargetId = UUID.fromString(aTargetIdStr);
                        manager.transferOwnership(aClan, aTargetId);
                        plugin.saveAsync();
                        plugin.getPlayerListener().refreshClanTabNames(aClan);
                        player.sendMessage("§aClan §e" + aClanName + " §awurde an §e" + aTargetName + " §aübertragen.");
                    }
                }
                player.closeInventory();
                if (aClanName != null) ClanMenu.open(manager, player, aClanName);
            }
            case "adminDisband" -> {
                String dClanName = holder.get("adminClanName");
                if (dClanName != null) {
                    Clan dClan = manager.getClanByName(dClanName);
                    if (dClan != null) {
                        java.util.List<org.bukkit.entity.Player> onlineMembers2 = dClan.getMembers().keySet().stream()
                                .map(org.bukkit.Bukkit::getPlayer)
                                .filter(p -> p != null && p.isOnline())
                                .toList();
                        manager.disbandClan(dClan);
                        plugin.saveAsync();
                        player.sendMessage("§cClan §e" + dClanName + " §cwurde aufgelöst.");
                        for (org.bukkit.entity.Player member : onlineMembers2) {
                            if (!member.equals(player)) {
                                member.sendMessage("§c§l §cDer Clan §e" + dClanName + " §cwurde aufgelöst.");
                                member.closeInventory();
                            }
                        }
                    }
                }
                player.closeInventory();
                ClanList.open(manager, player, 1);
            }
            case "surrender" -> {
                if (clan != null) manager.surrenderWar(clan);
                player.closeInventory();
            }
            case "deleteRank" -> {
                String rankId = holder.get("rankId");
                if (rankId != null && clan != null) {
                    Rank rank = clan.getRank(rankId);
                    String rankName = rank != null ? rank.getName() : rankId;
                    boolean deleted = clan.deleteRank(rankId);
                    if (deleted) {
                        clan.addLog("§f" + player.getName() + " §7hat den Rang §f" + rankName + " §7gelöscht.");
                        plugin.saveAsync();
                    } else {
                        player.sendMessage("§cDieser Rang kann nicht gelöscht werden.");
                    }
                }
                Management.open(manager, player);
            }
            case "adminRemoveAlly" -> {
                if (!player.hasPermission("betterclan.admin")) { player.closeInventory(); return; }
                String adminClanName = holder.get("clanName");
                String adminAllyName = holder.get("allyName");
                int adminPage = holder.get("page", 1);
                if (adminClanName != null && adminAllyName != null) {
                    com.betterclan.clan.Clan adminClan = manager.getClanByName(adminClanName);
                    com.betterclan.clan.Clan adminAlly = manager.getClanByName(adminAllyName);
                    if (adminClan != null) {
                        adminClan.removeAlly(adminAllyName);
                        if (adminAlly != null) adminAlly.removeAlly(adminClanName);
                        plugin.saveAsync();
                        player.sendMessage("§aBündnis zwischen §e" + adminClanName + " §aund §e" + adminAllyName + " §aaufgelöst.");
                    }
                    AllyRemove.open(manager, player, adminClanName, adminPage);
                } else {
                    player.closeInventory();
                }
            }
            case "dissolveAlly" -> {
                String allyName = holder.get("allyName");
                if (allyName != null && clan != null) {
                    manager.removeAlly(clan, allyName);
                    clan.addLog("§f" + player.getName() + " §7hat die Allianz mit §f" + allyName + " §7aufgelöst.");
                    Clan allyClan = manager.getClanByName(allyName);
                    if (allyClan != null) allyClan.addLog("§7Allianz mit §f" + clan.getName() + " §7wurde aufgelöst.");
                    plugin.saveAsync();
                }
                Ally.openList(manager, player, 1);
            }
            case "requestAlly" -> {
                String targetClanName = holder.get("targetClanName");
                if (targetClanName != null && clan != null) {
                    Clan targetClan = manager.getClanByName(targetClanName);
                    if (targetClan != null) {
                        boolean sent = manager.requestAlly(clan, targetClan);
                        player.sendMessage(sent
                                ? "§a§l⚔ §aAllianz-Anfrage an §f" + targetClan.getName() + " §agesendet!"
                                : "§cAnfrage konnte nicht gesendet werden (bereits verbündet/angefragt).");
                    }
                }
                ClanList.open(manager, player, 1);
            }
            case "requestWar" -> {
                String targetClanName = holder.get("targetClanName");
                String killsStr = holder.get("targetKills");
                if (targetClanName != null && clan != null) {
                    if (clan.getMemberCount() < manager.getSettings().warMinClanSize()) {
                        player.sendMessage("§cDer Clan muss insgesamt " + manager.getSettings().warMinClanSize() + " Mitglieder für Kriegsanfragen benötigt.");
                        player.closeInventory();
                        return;
                    }
                    Clan targetClan = manager.getClanByName(targetClanName);
                    if (targetClan != null && targetClan.getMemberCount() < manager.getSettings().warMinClanSize()) {
                        player.sendMessage("§cDer Ziel-Clan muss insgesamt " + manager.getSettings().warMinClanSize() + " Mitglieder für Kriegsanfragen benötigt.");
                        player.closeInventory();
                        return;
                    }
                    int kills = killsStr != null ? Integer.parseInt(killsStr) : manager.getSettings().warTargetKills();
                    if (targetClan != null) {
                        long myCooldown = manager.getWarCooldownRemaining(clan.getName());
                        long theirCooldown = manager.getWarCooldownRemaining(targetClan.getName());
                        boolean alreadyRequested = manager.getWarRequests(targetClan.getOwnerId())
                                .stream().anyMatch(w -> w.getClan1Name().equalsIgnoreCase(clan.getName()));
                        boolean sent = manager.requestWar(clan, targetClan, kills);
                        if (sent) {
                            player.sendMessage("§c§l §cKriegsanfrage an §f" + targetClan.getName() + " §cgesendet! (Ziel: " + kills + " Kills)");
                        } else if (alreadyRequested) {
                            player.sendMessage("§cDu hast bereits eine offene Kriegsanfrage an §f" + targetClan.getName() + "§c.");
                        } else if (manager.isAtWar(clan.getName())) {
                            player.sendMessage("§cDein Clan befindet sich bereits im Krieg.");
                        } else if (manager.isAtWar(targetClan.getName())) {
                            player.sendMessage("§cDer Clan §f" + targetClan.getName() + " §cbefindet sich bereits im Krieg.");
                        } else if (myCooldown > 0) {
                            long mins = myCooldown / 60000;
                            long secs = (myCooldown % 60000) / 1000;
                            player.sendMessage("§cDein Clan hat noch Krieg-Cooldown: §e" + mins + "m " + secs + "s");
                        } else if (theirCooldown > 0) {
                            long mins = theirCooldown / 60000;
                            long secs = (theirCooldown % 60000) / 1000;
                            player.sendMessage("§cDer Ziel-Clan hat noch Krieg-Cooldown: §e" + mins + "m " + secs + "s");
                        } else {
                            player.sendMessage("§cAnfrage konnte nicht gesendet werden (bereits im Krieg oder verbündet).");
                        }
                    }
                }
                ClanList.open(manager, player, 1);
            }
            default -> player.closeInventory();
        }
    }

    @SuppressWarnings("deprecation")
    private void handleAdminClanList(Player player, ItemStack clicked, GuiHolder holder, ClickType click) {
        if (!player.hasPermission("betterclan.admin")) return;
        if (!clicked.hasItemMeta()) return;
        String name = clicked.getItemMeta().getDisplayName();

        if (clicked.getType() == Material.COMPASS) {
            chatInput.awaitInput(player, query -> {
                java.util.List<com.betterclan.clan.Clan> results = manager.searchClans(query);
                ClanList.openSearch(manager, player, results, query);
            });
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD && name.contains("Spieler verwalten")) {
            chatInput.awaitInput(player, input -> {
                String trimmed = input.trim();
                org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(trimmed);
                if (op.getName() == null && !op.hasPlayedBefore()) {
                    player.sendMessage("§cSpieler §e" + trimmed + " §cwurde nicht gefunden.");
                    ClanList.open(manager, player, 1);
                    return;
                }
                PlayerMenu.open(manager, player, op.getUniqueId());
            });
            return;
        }

        if (clicked.getType() == Material.LIGHT_GRAY_DYE) {
            int page = holder.get("page", 1);
            if (name.contains("«")) ClanList.open(manager, player, page - 1);
            if (name.contains("»")) ClanList.open(manager, player, page + 1);
            return;
        }

        if (isCloseButton(clicked)) { player.closeInventory(); return; }

        boolean isBanner = clicked.getType().name().endsWith("_BANNER");
        if (clicked.getType() == Material.PLAYER_HEAD || isBanner) {
            String rawName = name.replaceAll("§.", "").trim();
            com.betterclan.clan.Clan clan = manager.getClanByName(rawName);
            if (clan == null) return;
            if (click == ClickType.RIGHT) {

                player.openInventory(manager.openVault(player, clan));
            } else {
                ClanMenu.open(manager, player, clan.getName());
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void handleAdminClanMenu(Player player, ItemStack clicked, GuiHolder holder, ClickType click) {
        if (!player.hasPermission("betterclan.admin")) return;
        String clanName = holder.get("clanName");
        if (clanName == null) return;
        com.betterclan.clan.Clan clan = manager.getClanByName(clanName);
        if (clan == null) { player.sendMessage("§cClan nicht mehr vorhanden."); return; }

        if (!clicked.hasItemMeta()) return;
        Material mat = clicked.getType();

        if (mat == Material.ARROW) {
            ClanList.open(manager, player, 1);
            return;
        }

        switch (mat) {

            case CHEST -> player.openInventory(manager.openVault(player, clan));

            case EXPERIENCE_BOTTLE -> {
                if (click == ClickType.SHIFT_RIGHT) {
                    int lvl = Math.max(1, clan.getLevel() - 1);
                    clan.setLevel(lvl);
                    plugin.saveAsync();
                    ClanMenu.open(manager, player, clan.getName());
                } else if (click == ClickType.DOUBLE_CLICK) {
                    int lvl = Math.min(manager.getSettings().maxLevel(), clan.getLevel() + 1);
                    clan.setLevel(lvl);
                    plugin.saveAsync();
                    ClanMenu.open(manager, player, clan.getName());
                }
            }

            case PLAYER_HEAD -> {
                String headName = clicked.getItemMeta().getDisplayName();
                if (headName.contains("Max. Mitglieder")) {
                    if (click == ClickType.RIGHT) {
                        clan.setMaxMembersOverride(-1);
                        plugin.saveAsync();
                        ClanMenu.open(manager, player, clan.getName());
                    } else {
                        chatInput.awaitInput(player, input -> {
                            try {
                                int max = Integer.parseInt(input.trim());
                                max = Math.max(1, max);
                                clan.setMaxMembersOverride(max);
                                plugin.saveAsync();
                                ClanMenu.open(manager, player, clan.getName());
                            } catch (NumberFormatException e) {
                                ClanMenu.open(manager, player, clan.getName());
                            }
                        });
                    }
                } else if (headName.contains("Mitglieder verwalten")) {
                    Members.openForAdmin(plugin, manager, player, clan.getName(), 1);
                }
            }

            case NAME_TAG -> chatInput.awaitInput(player, input -> {
                String newName = input.trim();
                if (newName.length() < Settings.CLAN_MIN_NAME_LENGTH || newName.length() > Settings.CLAN_MAX_NAME_LENGTH) {
                    player.sendMessage("§cName muss " + Settings.CLAN_MIN_NAME_LENGTH + "–" + Settings.CLAN_MAX_NAME_LENGTH + " Zeichen lang sein.");
                    ClanMenu.open(manager, player, clan.getName());
                    return;
                }
                if (manager.getClanByName(newName) != null && !newName.equalsIgnoreCase(clan.getName())) {
                    player.sendMessage("§cEin Clan mit diesem Namen existiert bereits.");
                    ClanMenu.open(manager, player, clan.getName());
                    return;
                }
                String oldName = clan.getName();
                manager.renameClan(clan, newName);
                plugin.saveAsync();
                player.sendMessage("§aClan §e" + oldName + " §awurde umbenannt zu §e" + newName + "§a.");
                ClanMenu.open(manager, player, newName);
            });

            case FIRE_CHARGE, SNOWBALL -> {
                if (click == ClickType.DOUBLE_CLICK) {
                    clan.setFriendlyFire(!clan.isFriendlyFire());
                    plugin.saveAsync();
                    ClanMenu.open(manager, player, clan.getName());
                }
            }

            case RED_BANNER -> {
                com.betterclan.clan.War war = manager.getActiveWar(clan.getName());
                if (war != null) {
                    manager.endWar(war);
                    plugin.saveAsync();
                    player.sendMessage("§aKrieg von §e" + clan.getName() + " §abeendet.");
                }
                ClanMenu.open(manager, player, clan.getName());
            }

            case BOOKSHELF -> Management.openForAdmin(manager, clan, player);

            case GOLDEN_HELMET -> chatInput.awaitInput(player, input -> {
                String targetName = input.trim();
                org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                UUID targetId = target.getUniqueId();
                if (!clan.getMembers().containsKey(targetId)) {
                    player.sendMessage("§c" + targetName + " ist kein Mitglied von §e" + clan.getName() + "§c.");
                    ClanMenu.open(manager, player, clan.getName());
                    return;
                }
                if (clan.getOwnerId().equals(targetId)) {
                    player.sendMessage("§7" + targetName + " §7ist bereits der Anführer.");
                    ClanMenu.open(manager, player, clan.getName());
                    return;
                }
                Confirm.open(player, "§6Clan übertragen?",
                        "An " + targetName + " übertragen", "adminTransfer",
                        "adminClanName", clan.getName(),
                        "targetId", targetId.toString(),
                        "targetName", targetName);
            });

            case IRON_SWORD -> {
                if (!clan.getAllies().isEmpty()) {
                    AllyRemove.open(manager, player, clan.getName());
                } else {
                    player.sendMessage("§7" + clan.getName() + " §7hat keine Bündnisse.");
                }
            }

            case BOOK -> Log.openForAdmin(manager, player, clan.getName());

            case PAINTING -> {
                if (click == ClickType.RIGHT) {
                    BannerColor.open(manager, player, clan.getName());
                } else {
                    Picker.open(manager, player, clan.getName());
                }
            }

            case TNT -> Confirm.open(player, "§4Clan auflösen?",
                    clan.getName() + " auflösen", "adminDisband",
                    "adminClanName", clan.getName());

            default -> {  }
        }
    }

    @SuppressWarnings({"deprecation", "unused"})
    private void handleAdminAllyRemove(Player player, ItemStack clicked, GuiHolder holder, ClickType click) {
        if (!player.hasPermission("betterclan.admin")) return;
        String clanName = holder.get("clanName");
        if (clanName == null) return;
        int page = holder.get("page", 1);

        Material mat = clicked.getType();

        if (mat == Material.ARROW) {
            ClanMenu.open(manager, player, clanName); return;
        }
        if (mat == Material.LIGHT_GRAY_DYE) {
            String nm = clicked.hasItemMeta() ? clicked.getItemMeta().getDisplayName() : "";
            if (nm.contains("«")) { AllyRemove.open(manager, player, clanName, page - 1); return; }
            if (nm.contains("»")) { AllyRemove.open(manager, player, clanName, page + 1); return; }
        }

        if (clicked.hasItemMeta()) {
            String allyName = clicked.getItemMeta().getPersistentDataContainer()
                    .get(new org.bukkit.NamespacedKey("betterclan", "ally-name"),
                            org.bukkit.persistence.PersistentDataType.STRING);
            if (allyName != null) {
                Confirm.open(player,
                        "§c§lBündnis auflösen?",
                        "Auflösen", "adminRemoveAlly",
                        "clanName", clanName,
                        "allyName", allyName,
                        "adminClanName", clanName,
                        "page", String.valueOf(page));
            }
        }
    }

    private void handleAdminPlayerMenu(Player player, ItemStack clicked, GuiHolder holder) {
        if (!player.hasPermission("betterclan.admin")) return;
        String targetIdStr = holder.get("targetId");
        if (targetIdStr == null) return;
        java.util.UUID targetId;
        try { targetId = java.util.UUID.fromString(targetIdStr); }
        catch (IllegalArgumentException e) { return; }

        if (!clicked.hasItemMeta()) return;
        Material mat = clicked.getType();

        if (mat == Material.ARROW) {
            ClanList.open(manager, player, 1);
            return;
        }

        switch (mat) {

            case BARRIER, IRON_BARS -> {
                if (manager.isClanBanned(targetId)) {
                    manager.removeClanBan(targetId);
                    plugin.saveAsync();
                    String n = java.util.Optional.ofNullable(org.bukkit.Bukkit.getOfflinePlayer(targetId).getName()).orElse(targetIdStr);
                    player.sendMessage("§aClan-Bann für §e" + n + " §aaufgehoben.");
                } else {
                    manager.setClanBan(targetId, -1L);
                    plugin.saveAsync();
                    String n = java.util.Optional.ofNullable(org.bukkit.Bukkit.getOfflinePlayer(targetId).getName()).orElse(targetIdStr);
                    player.sendMessage("§cClan-Bann §7(permanent) für §e" + n + " §cgesetzt.");
                }
                PlayerMenu.open(manager, player, targetId);
            }

            case CLOCK -> chatInput.awaitInput(player, input -> {
                try {
                    long hours = Long.parseLong(input.trim());
                    if (hours <= 0) throw new NumberFormatException();
                    long expiry = System.currentTimeMillis() + hours * 3600_000L;
                    manager.setClanBan(targetId, expiry);
                    plugin.saveAsync();
                    String n = java.util.Optional.ofNullable(org.bukkit.Bukkit.getOfflinePlayer(targetId).getName()).orElse(targetIdStr);
                    player.sendMessage("§aClan-Bann für §e" + n + " §afür §f" + hours + " §aStunden gesetzt.");
                    PlayerMenu.open(manager, player, targetId);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cUngültige Zahl (Stunden als ganze Zahl eingeben).");
                    PlayerMenu.open(manager, player, targetId);
                }
            });

            case LIME_DYE, RED_DYE -> {
                manager.toggleInviteBlock(targetId);
                plugin.saveAsync();
                PlayerMenu.open(manager, player, targetId);
            }

            default -> { }
        }
    }
}

