package com.betterclan;

import com.betterclan.clan.Clan;
import com.betterclan.clan.ClanManager;
import com.betterclan.clan.ClanStorage;
import com.betterclan.clan.Settings;
import com.betterclan.storage.DatabaseManager;
import com.betterclan.storage.DatabaseStorage;
import com.betterclan.storage.SqlDialect;
import com.betterclan.storage.StorageProvider;
import com.betterclan.command.AdminCommand;
import com.betterclan.command.ChatCommand;
import com.betterclan.command.ClanCommand;
import com.betterclan.gui.MenuListener;
import com.betterclan.listener.ChatInputListener;
import com.betterclan.listener.ChatListener;
import com.betterclan.listener.CombatListener;
import com.betterclan.listener.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicBoolean;

public class BetterClanPlugin extends JavaPlugin {

    private ClanManager clanManager;
    private StorageProvider storage;
    private ChatInputListener chatInput;
    private Settings settings;
    private PlayerListener playerListener;
    private final AtomicBoolean savePending = new AtomicBoolean(false);

    public StorageProvider getStorage() { return storage; }

    public void saveAsync() {
        if (!settings.isDatabase()) {

            storage.save(clanManager);
            return;
        }
        if (savePending.compareAndSet(false, true)) {
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    storage.save(clanManager);
                } finally {
                    savePending.set(false);
                }
            });
        }

    }
    @SuppressWarnings("unused")
    public ClanManager getClanManager() { return clanManager; }
    @SuppressWarnings("unused")
    public Settings getSettings() { return settings; }
    public PlayerListener getPlayerListener() { return playerListener; }
    public ChatInputListener getChatInput() { return chatInput; }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.settings = Settings.load(getConfig());
        this.clanManager = new ClanManager(settings);

        if (settings.isDatabase()) {
            SqlDialect dialect = SqlDialect.fromString(settings.dbDialect());
            int port = settings.dbPort();

            if (port == 3306 && dialect == SqlDialect.POSTGRES) port = 5432;
            DatabaseManager db = new DatabaseManager(dialect, settings.dbHost(), port,
                    settings.dbDatabase(), settings.dbUsername(), settings.dbPassword(),
                    settings.dbPoolSize(), getLogger());
            this.storage = new DatabaseStorage(db, getLogger());
            getLogger().info("Storage: " + dialect.name() + " @ " + settings.dbHost());
        } else {
            this.storage = new ClanStorage(this);
            getLogger().info("Storage: JSON (clans.json)");
        }

        Clan.configure(settings.maxLogEntries(), settings.bannerMaxPatterns(),
                settings.defaultFriendlyFire(), settings.defaultMemberPermissions());

        storage.load(clanManager);
        this.chatInput = new ChatInputListener(this, settings);

        registerCommands();
        registerListeners();
        registerSchedulers();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BetterClanExpansion(this, clanManager).register();
        }

        getLogger().info("successfully loaded");
    }

    @Override
    public void onDisable() {
        if (storage != null && clanManager != null) {
            storage.save(clanManager);
            storage.shutdown();
        }
        if (chatInput != null) chatInput.shutdown();

    }

    private void registerCommands() {
        ClanCommand clanCommand = new ClanCommand(clanManager, chatInput);
        var cmd = getCommand("clan");
        if (cmd != null) {
            cmd.setExecutor(clanCommand);
            cmd.setTabCompleter(clanCommand);
        }

        var ccCmd = getCommand("chat");
        if (ccCmd != null) {
            ChatCommand chat = new ChatCommand(clanManager);
            ccCmd.setExecutor(chat);
            ccCmd.setTabCompleter(chat);
        }

        var adminCmd = getCommand("sca");
        if (adminCmd != null) {
            AdminCommand adminHandler = new AdminCommand(clanManager);
            adminCmd.setExecutor(adminHandler);
            adminCmd.setTabCompleter(adminHandler);
        }

    }

    private void registerListeners() {
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(chatInput, this);
        pm.registerEvents(new MenuListener(this, clanManager, chatInput), this);
        pm.registerEvents(new ChatListener(clanManager, chatInput, settings), this);
        playerListener = new PlayerListener(clanManager, this);
        pm.registerEvents(playerListener, this);

        clanManager.setOnTagRemove(id -> {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) playerListener.updateTabName(p, null);
        });
        pm.registerEvents(new CombatListener(clanManager), this);

        Bukkit.getScheduler().runTaskTimer(this, playerListener::updateAllTabNames, 20L * 5, 20L * 60);
        Bukkit.getScheduler().runTaskTimer(this, playerListener::tickWarGlow, 20L, 20L);
    }

    private void registerSchedulers() {

        if (settings.isDatabase()) {
            long saveInterval = 20L * 60 * settings.autoSaveMinutes();
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                if (storage != null && clanManager != null) {
                    saveAsync();
                }
            }, saveInterval, saveInterval);
        }
    }
}

