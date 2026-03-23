package com.betterclan.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("deprecation")
public final class ItemBuilder {

    private final ItemStack stack;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.stack = new ItemStack(material);
        this.meta = stack.getItemMeta();
    }

    @SuppressWarnings("unused")
    public ItemBuilder(Material material, int amount) {
        this.stack = new ItemStack(material, amount);
        this.meta = stack.getItemMeta();
    }

    public ItemBuilder(ItemStack source) {
        this.stack = source.clone();
        this.meta = stack.getItemMeta();
    }

    public ItemBuilder name(String displayName) {
        meta.setDisplayName(displayName);
        return this;
    }

    public ItemBuilder lore(String... lines) {
        meta.setLore(Arrays.asList(lines));
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        meta.setLore(lines);
        return this;
    }

    public ItemBuilder glow() {
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder skullOwner(org.bukkit.OfflinePlayer player) {
        if (meta instanceof SkullMeta skull) {
            String name = player.getName();
            if (name != null && !name.isBlank()) {
                try {
                    var profile = Bukkit.getServer().createProfile(player.getUniqueId(), name);
                    skull.setPlayerProfile(profile);
                } catch (Throwable ignored) {
                    skull.setOwningPlayer(player);
                }
            } else {
                skull.setOwningPlayer(player);
            }
        }
        return this;
    }

    public ItemBuilder persistData(NamespacedKey key, String value) {
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        return this;
    }

    @SuppressWarnings("unused")
    public ItemBuilder customModelData(int data) {
        meta.setCustomModelData(data);
        return this;
    }

    public ItemBuilder bannerPatterns(java.util.List<org.bukkit.block.banner.Pattern> patterns) {
        if (meta instanceof BannerMeta bannerMeta) {
            for (var p : patterns) bannerMeta.addPattern(p);
        }
        return this;
    }

    public ItemStack build() {
        stack.setItemMeta(meta);
        return stack;
    }
}

