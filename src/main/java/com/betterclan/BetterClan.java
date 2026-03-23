package com.betterclan;

import com.betterclan.clan.Clan;
import com.betterclan.clan.Manager;
import com.betterclan.clan.Storage;
import com.betterclan.clan.Settings;
import com.betterclan.storage.ConnectionPool;
import com.betterclan.storage.Database;
import com.betterclan.storage.Dialect;
import com.betterclan.storage.Provider;
import com.betterclan.command.Admin;
import com.betterclan.command.ClanChat;
import com.betterclan.command.ClanCommand;
import com.betterclan.gui.MenuListener;
import com.betterclan.listener.ChatInput;
import com.betterclan.listener.Chat;
import com.betterclan.listener.Combat;
import com.betterclan.listener.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicBoolean;

public class BetterClan extends JavaPlugin {

    private Manager clanManager;
    private Provider storage;
    private ChatInput chatInput;
    private Settings settings;
    private PlayerListener playerListener;
    private final AtomicBoolean savePending = new AtomicBoolean(false);

    public Provider getStorage() { return storage; }

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
    public Manager getClanManager() { return clanManager; }
    @SuppressWarnings("unused")
    public Settings getSettings() { return settings; }
    public PlayerListener getPlayerListener() { return playerListener; }
    public ChatInput getChatInput() { return chatInput; }

    public void reloadPlugin(org.bukkit.command.CommandSender sender) {
        reloadConfig();
        this.settings = Settings.load(getConfig());
        Clan.configure(settings.maxLogEntries(), settings.bannerMaxPatterns(),
                settings.defaultFriendlyFire(), settings.defaultMemberPermissions());
        sender.sendMessage("§a[BetterClan] §7Config neu geladen.");
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.settings = Settings.load(getConfig());
        this.clanManager = new Manager(settings);

        if (settings.isDatabase()) {
            Dialect dialect = Dialect.fromString(settings.dbDialect());
            int port = settings.dbPort();

            if (port == 3306 && dialect == Dialect.POSTGRES) port = 5432;
            ConnectionPool db = new ConnectionPool(dialect, settings.dbHost(), port,
                    settings.dbDatabase(), settings.dbUsername(), settings.dbPassword(),
                    settings.dbPoolSize(), getLogger());
            this.storage = new Database(db, getLogger());
            getLogger().info("Storage: " + dialect.name() + " @ " + settings.dbHost());
        } else {
            this.storage = new Storage(this);
            getLogger().info("Storage: JSON (clans.json)");
        }

        Clan.configure(settings.maxLogEntries(), settings.bannerMaxPatterns(),
                settings.defaultFriendlyFire(), settings.defaultMemberPermissions());

        storage.load(clanManager);
        this.chatInput = new ChatInput(this, settings);

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
            ClanChat chat = new ClanChat(clanManager);
            ccCmd.setExecutor(chat);
            ccCmd.setTabCompleter(chat);
        }

        var adminCmd = getCommand("sclan");
        if (adminCmd != null) {
            Admin adminHandler = new Admin(this, clanManager);
            adminCmd.setExecutor(adminHandler);
            adminCmd.setTabCompleter(adminHandler);
        }

    }

    private void registerListeners() {
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(chatInput, this);
        pm.registerEvents(new MenuListener(this, clanManager, chatInput), this);
        pm.registerEvents(new Chat(clanManager, chatInput, settings), this);
        playerListener = new PlayerListener(clanManager, this);
        pm.registerEvents(playerListener, this);

        clanManager.setOnTagRemove(id -> {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) playerListener.updateTabName(p, null);
        });
        pm.registerEvents(new Combat(clanManager), this);

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

