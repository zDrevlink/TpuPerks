// src/main/java/com/tpuperks/DataManager.java

package com.tpuperks;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class DataManager {

    private final TpuPerks plugin;
    private final File file;
    private final YamlConfiguration data;

    public DataManager(TpuPerks plugin) {
        this.plugin = plugin;

        file = new File(plugin.getDataFolder(), "data.yml");

        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("No se pudo crear data.yml");
            }
        }

        data = YamlConfiguration.loadConfiguration(file);
    }

    private String path(UUID uuid) {
        return "players." + uuid;
    }

    public int getPoints(UUID uuid) {
        return data.getInt(path(uuid) + ".points", 0);
    }

    public void setPoints(UUID uuid, int points) {
        data.set(path(uuid) + ".points", Math.max(0, points));
    }

    public void addPoints(UUID uuid, int amount) {
        setPoints(uuid, getPoints(uuid) + amount);
    }

    public int getLastAwardedLevel(UUID uuid) {
        return data.getInt(path(uuid) + ".last-awarded-level", 0);
    }

    public void setLastAwardedLevel(UUID uuid, int level) {
        data.set(path(uuid) + ".last-awarded-level", Math.max(0, level));
    }

    public Set<String> getActivePerks(UUID uuid) {
        return new HashSet<>(
                data.getStringList(path(uuid) + ".active-perks")
        );
    }

    public void setActivePerks(UUID uuid, Set<String> perks) {
        data.set(
                path(uuid) + ".active-perks",
                new ArrayList<>(perks)
        );
    }

    public boolean hasActivePerk(UUID uuid, String perk) {
        return getActivePerks(uuid).contains(perk);
    }

    public void addActivePerk(UUID uuid, String perk) {
        Set<String> perks = getActivePerks(uuid);
        perks.add(perk);
        setActivePerks(uuid, perks);
    }

    public void removeActivePerk(UUID uuid, String perk) {
        Set<String> perks = getActivePerks(uuid);
        perks.remove(perk);
        setActivePerks(uuid, perks);
    }

    public void clearActivePerks(UUID uuid) {
        setActivePerks(uuid, new HashSet<>());
    }

    public void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar data.yml");
        }
    }
}