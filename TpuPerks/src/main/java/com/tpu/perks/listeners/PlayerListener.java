package com.tpu.perks.listeners;

import com.tpu.perks.TpuPerks;
import com.tpu.perks.data.DataManager;
import com.tpu.perks.data.PerkManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Escucha los eventos base del ciclo de vida del jugador relacionados con perks:
 *  - Join: carga datos y reaplica perks activos.
 *  - Quit: guarda y libera memoria.
 *  - LevelChange: verifica si corresponde otorgar nuevos Perk Points.
 *  - Death: limpia todos los perks activos (deben volver a equiparse).
 */
public class PlayerListener implements Listener {

    private final TpuPerks plugin;
    private final PerkManager perkManager;
    private final DataManager dataManager;

    public PlayerListener(TpuPerks plugin, PerkManager perkManager, DataManager dataManager) {
        this.plugin = plugin;
        this.perkManager = perkManager;
        this.dataManager = dataManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Cargar datos de forma asíncrona sería ideal, pero Bukkit necesita el objeto
        // Player ya disponible; la carga desde YAML es rápida (archivo pequeño por jugador),
        // así que se hace de forma síncrona pero ligera. Para servidores muy grandes,
        // considerar pre-cachear en AsyncPlayerPreLoginEvent.
        dataManager.getOrLoad(player.getUniqueId());
        perkManager.reapplyActivePerksOnJoin(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        dataManager.unload(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onLevelChange(PlayerLevelChangeEvent event) {
        perkManager.checkAndGrantPoints(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        perkManager.clearActivePerksOnDeath(player);
        player.sendMessage(Component.text("☠ ", NamedTextColor.DARK_RED)
                .append(Component.text("Has muerto. Todos tus perks activos se desactivaron.", NamedTextColor.RED))
                .append(Component.text(" Usa /tpu perks para volver a equiparlos.", NamedTextColor.GRAY)));
    }
}
