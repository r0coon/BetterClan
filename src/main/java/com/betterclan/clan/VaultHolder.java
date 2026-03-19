package com.betterclan.clan;

import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public record VaultHolder(String clanName) implements InventoryHolder {

    @Override
    public @NotNull org.bukkit.inventory.Inventory getInventory() {
        throw new UnsupportedOperationException("VaultHolder does not hold a direct inventory reference.");
    }
}

