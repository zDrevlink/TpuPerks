package com.tpu.perks;

import com.tpu.perks.commands.TpuCommand;
import com.tpu.perks.data.DataManager;
import com.tpu.perks.data.PerkManager;
import com.tpu.perks.gui.PerkGuiListener;
import com.tpu.perks.listeners.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * TpuPerks - Plugin ligero de perks escalables basados en niveles de experiencia.
 *
 * Flujo general:
 *  - El jugador gana "Perk Points" cada X niveles de XP (configurable).
 *  - Puede gastar esos puntos en una GUI para subir el rango de cada perk.
 *  - Cada perk exige mantener un nivel mínimo de XP para seguir activo (se revisa periódicamente).
 *  - Al morir, todos los perks activos se desactivan y deben volver a equiparse manualmente.
 */
public final class TpuPerks extends JavaPlugin {

    private static TpuPerks instance;

    private DataManager dataManager;
    private PerkManager perkManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.dataManager = new DataManager(this);
        this.perkManager = new PerkManager(this, dataManager);

        // Comandos
        TpuCommand tpuCommand = new TpuCommand(this, perkManager, dataManager);
        getCommand("tpu").setExecutor(tpuCommand);
        getCommand("tpu").setTabCompleter(tpuCommand);

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this, perkManager, dataManager), this);
        getServer().getPluginManager().registerEvents(new PerkGuiListener(this, perkManager, dataManager), this);

        // Tarea periódica: revisa que los jugadores online sigan cumpliendo el umbral de nivel.
        long checkInterval = getConfig().getLong("settings.threshold-check-interval-ticks", 100L);
        perkManager.startThresholdCheckTask(checkInterval);

        getLogger().info("TpuPerks habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
        getLogger().info("TpuPerks deshabilitado. Datos guardados.");
    }

    public static TpuPerks getInstance() {
        return instance;
    }

    public PerkManager getPerkManager() {
        return perkManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }
}
