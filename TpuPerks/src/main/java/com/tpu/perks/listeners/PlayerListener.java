package com.tpu.perks.listeners;

import com.tpu.perks.TpuPerks;
import com.tpu.perks.data.DataManager;
import com.tpu.perks.data.PerkManager;
import com.tpu.perks.data.PerkType;
import com.tpu.perks.data.PlayerPerkData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Escucha los eventos base del ciclo de vida del jugador relacionados con perks:
 *  - Join: carga datos y reaplica perks activos.
 *  - Quit: guarda y libera memoria.
 *  - LevelChange: verifica si corresponde otorgar nuevos Perk Points.
 *  - Death: limpia todos los perks activos y REINICIA los Perk Points a 0 (reset total).
 *  - EntityDamage: anula el fall-damage puntual generado por el impulso del doble salto.
 *  - EntityDamageByEntity: aplica el perk "Incinerador" (quema al golpear).
 */
public class PlayerListener implements Listener {

    private final TpuPerks plugin;
    private final PerkManager perkManager;
    private final DataManager dataManager;
    private final DoubleJumpListener doubleJumpListener;

    public PlayerListener(TpuPerks plugin, PerkManager perkManager, DataManager dataManager,
                           DoubleJumpListener doubleJumpListener) {
        this.plugin = plugin;
        this.perkManager = perkManager;
        this.dataManager = dataManager;
        this.doubleJumpListener = doubleJumpListener;
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
        perkManager.fullResetOnDeath(player);
        player.sendMessage(Component.text("☠ ", NamedTextColor.DARK_RED)
                .append(Component.text("Has muerto. TODO tu progreso de perks se reinició: ", NamedTextColor.RED))
                .append(Component.text("perks activos, rangos comprados y Perk Points, todo a 0.", NamedTextColor.GRAY)));
    }

    /**
     * Anula el fall-damage de un aterrizaje puntual si ese aterrizaje viene justo
     * después de usar el doble salto. No afecta ninguna otra caída.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (doubleJumpListener.consumeFallDamageWaiver(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Aplica el perk "Temple del Volcán" (Forjado en Fuego): reduce un % del daño
     * de fuego, quemaduras residuales y lava según el rango comprado y activo.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFireDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        boolean isFireRelated = switch (event.getCause()) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR -> true;
            default -> false;
        };
        if (!isFireRelated) {
            return;
        }

        PlayerPerkData data = dataManager.get(player.getUniqueId());
        if (data == null || !data.isActive(PerkType.FIRE_FORGED)) {
            return;
        }

        int rank = data.getRank(PerkType.FIRE_FORGED);
        double reduction = perkManager.getFireResistancePercent(rank);
        if (reduction <= 0) {
            return;
        }

        event.setDamage(event.getDamage() * (1.0 - reduction));
    }

    /** Aplica el perk "Incinerador": al golpear a una entidad viva, la prende fuego. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIncinerator(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        PlayerPerkData data = dataManager.get(player.getUniqueId());
        if (data == null || !data.isActive(PerkType.INCINERATOR)) {
            return;
        }

        int fireSeconds = plugin.getConfig().getInt("perk-values.incinerator-fire-seconds", 4);
        target.setFireTicks(Math.max(target.getFireTicks(), fireSeconds * 20));
    }
}

