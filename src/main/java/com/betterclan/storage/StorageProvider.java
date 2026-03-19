package com.betterclan.storage;

import com.betterclan.clan.ClanManager;

public interface StorageProvider {

    void load(ClanManager manager);

    void save(ClanManager manager);

    void shutdown();
}

