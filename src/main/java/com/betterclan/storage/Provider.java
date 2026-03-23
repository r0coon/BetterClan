package com.betterclan.storage;

import com.betterclan.clan.Manager;

public interface Provider {

    void load(Manager manager);

    void save(Manager manager);

    void shutdown();
}

