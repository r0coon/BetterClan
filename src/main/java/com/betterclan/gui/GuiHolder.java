package com.betterclan.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class GuiHolder implements InventoryHolder {

    private final MenuType type;
    private final Map<String, Object> data = new HashMap<>();

    public GuiHolder(MenuType type) {
        this.type = type;
    }

    public MenuType getType() {
        return type;
    }

    public GuiHolder set(String key, Object value) {
        data.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    public <T> T get(String key, T defaultValue) {
        T val = get(key);
        return val != null ? val : defaultValue;
    }

    @Override
    public @NotNull Inventory getInventory() {
        throw new UnsupportedOperationException("GuiHolder does not hold a direct inventory reference.");
    }
}

