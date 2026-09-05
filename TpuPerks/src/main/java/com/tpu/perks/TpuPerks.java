package com.tpu.perks;

import com.tpu.perks.commands.TpuCommand;
import com.tpu.perks.data.DataManager;
import com.tpu.perks.data.PerkManager;
import com.tpu.perks.gui.PerkGuiListener;
import com.tpu.perks.listeners.BlacksmithMasteryListener;
import com.tpu.perks.listeners.CraftsmanMasteryListener;
import com.tpu.perks.listeners.DoubleJumpListener;
import com.tpu.perks.listeners.ExpertSwimmerListener;
import com.tpu.perks.listeners.LastStandListener;
import com.tpu.perks.listeners.MagnetismTask;
import com.tpu.perks.listeners.PlayerListener;
import com.tpu.perks.recipe.TrueHeroSwordManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * TpuPerks - Plugin ligero de perks escalables basados en niveles de experiencia.
 *
 * Flujo general:
 *  - El jugador gana "Perk Points" cada X niveles de XP (configurable).
 *  - Puede gastar esos puntos en una GUI para subir el rango de cada perk.
 *  - Cada perk exige mantener un nivel mínimo de XP para seguir activo (se revisa periódicamente).
 *  - Al morir: RESET TOTAL. Se pierden perks activos, todos los rangos comprados y los
 *    Perk Points disponibles. El jugador empieza de cero en el sistema de perks.
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

        // Registra la receta secreta de la espada legendaria y su advancement asociado.
        TrueHeroSwordManager trueHeroSwordManager = new TrueHeroSwordManager(this);
        trueHeroSwordManager.registerAll();

        // Comandos
        TpuCommand tpuCommand = new TpuCommand(this, perkManager, dataManager);
        getCommand("tpu").setExecutor(tpuCommand);
        getCommand("tpu").setTabCompleter(tpuCommand);

        // Listeners. DoubleJumpListener se crea primero porque PlayerListener necesita
        // su referencia para consultar el "waiver" de daño de caída del doble salto.
        DoubleJumpListener doubleJumpListener = new DoubleJumpListener(this, perkManager, dataManager);
        getServer().getPluginManager().registerEvents(doubleJumpListener, this);
        getServer().getPluginManager().registerEvents(
                new PlayerListener(this, perkManager, dataManager, doubleJumpListener), this);
        getServer().getPluginManager().registerEvents(
                new PerkGuiListener(this, perkManager, dataManager, trueHeroSwordManager), this);
        getServer().getPluginManager().registerEvents(new CraftsmanMasteryListener(this, dataManager), this);
        getServer().getPluginManager().registerEvents(new BlacksmithMasteryListener(this, dataManager), this);

        ExpertSwimmerListener expertSwimmerListener = new ExpertSwimmerListener(this, perkManager, dataManager);
        getServer().getPluginManager().registerEvents(expertSwimmerListener, this);

        // Tarea periódica ligera (cada 4 ticks) que sincroniza el modifier de velocidad de
        // natación: no existe un evento nativo de "entrar/salir del agua" en Paper.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                expertSwimmerListener.tickCheck(player);
            }
        }, 20L, 4L);

        // Tarea de magnetismo: atrae ítems/orbes de XP para jugadores con el perk activo.
        new MagnetismTask(this, dataManager).start();

        // Listener del Último Aliento (Last Stand) del Blasón del Elegido.
        getServer().getPluginManager().registerEvents(new LastStandListener(this, dataManager), this);

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
