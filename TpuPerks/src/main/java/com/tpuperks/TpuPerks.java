// src/main/java/com/tpuperks/TpuPerks.java

package com.tpuperks;

import org.bukkit.plugin.java.JavaPlugin;

public final class TpuPerks extends JavaPlugin {

    private DataManager dataManager;
    private PerkManager perkManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        dataManager = new DataManager(this);
        perkManager = new PerkManager(this, dataManager);

        TpuCommand command = new TpuCommand(this, dataManager, perkManager);
        getCommand("tpu").setExecutor(command);
        getCommand("tpu").setTabCompleter(command);

        getServer().getPluginManager().registerEvents(
                new PerkListener(this, dataManager, perkManager),
                this
        );

        getLogger().info("TpuPerks habilitado.");
    }

    @Override
    public void onDisable() {
        dataManager.save();
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public PerkManager getPerkManager() {
        return perkManager;
    }
}