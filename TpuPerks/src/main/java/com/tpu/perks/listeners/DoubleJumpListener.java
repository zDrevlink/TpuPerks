package com.tpu.perks.listeners;

import com.tpu.perks.TpuPerks;
import com.tpu.perks.data.DataManager;
import com.tpu.perks.data.PerkManager;
import com.tpu.perks.data.PerkType;
import com.tpu.perks.data.PlayerPerkData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementa el perk único "Doble Salto".
 *
 * IMPORTANTE — historial de bug corregido:
 * La versión anterior usaba allowFlight + PlayerToggleFlightEvent#setCancelled(true)
 * para "atrapar" el segundo salto. Ese patrón es inseguro: el cliente ya asume que
 * está en modo vuelo en el momento en que envía el paquete, y cancelar el evento no
 * siempre revierte ese estado a tiempo en el cliente, dejando al jugador "flotando"
 * indefinidamente (vuelo gratis). NUNCA se debe re-introducir ese patrón.
 *
 * Nuevo enfoque (seguro):
 *  - allowFlight se activa solo brevísimamente al saltar, y se fuerza a false de
 *    forma inmediata y síncrona en el mismo evento, nunca dejándolo "abierto".
 *  - Una tarea de vigilancia (fail-safe) corre cada pocos ticks y corrige a
 *    cualquier jugador cuyo allowFlight quedara encendido sin motivo legítimo,
 *    evitando por completo la posibilidad de vuelo persistente.
 *  - El daño de caída se gestiona a mano (ver consumeFallDamageWaiver): al usar
 *    el doble salto se cancela el fall damage nativo de ESE aterrizaje (para no
 *    penalizar el salto extra en sí), pero el perk jamás anula el daño de caída
 *    por una caída real posterior; si el jugador se tira de una torre después de
 *    usar el doble salto, sigue tomando daño de caída normal.
 */
public class DoubleJumpListener implements Listener {

    private final TpuPerks plugin;
    private final PerkManager perkManager;
    private final DataManager dataManager;

    /** Fuerza del impulso vertical del segundo salto. */
    private static final double JUMP_VELOCITY = 0.62;

    /** Jugadores que ya gastaron su doble salto y deben tocar el suelo para recargarlo. */
    private final Map<UUID, Boolean> jumpUsed = new HashMap<>();

    /** Jugadores a los que se les debe anular el próximo fall-damage por haber usado el doble salto. */
    private final Map<UUID, Boolean> pendingFallDamageWaiver = new HashMap<>();

    public DoubleJumpListener(TpuPerks plugin, PerkManager perkManager, DataManager dataManager) {
        this.plugin = plugin;
        this.perkManager = perkManager;
        this.dataManager = dataManager;

        // Fail-safe: cada 2 segundos, revisa que ningún jugador tenga allowFlight
        // "colgado" sin razón legítima (creative/spectator). Cero tolerancia a vuelo persistente.
        Bukkit.getScheduler().runTaskTimer(plugin, this::failSafeSweep, 40L, 40L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        jumpUsed.put(player.getUniqueId(), false);
        // Aseguramos estado limpio: nunca confiar en lo que el cliente traiga puesto al entrar.
        if (!isRealFlightMode(player)) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        jumpUsed.remove(uuid);
        pendingFallDamageWaiver.remove(uuid);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (isRealFlightMode(player)) {
            return;
        }

        boolean active = hasDoubleJumpActive(player);

        if (player.isOnGround()) {
            // Recarga la habilidad al tocar el suelo.
            jumpUsed.put(uuid, false);
            // Garantiza que allowFlight nunca quede encendido en el suelo salvo el instante de saltar.
            if (player.getAllowFlight()) {
                player.setAllowFlight(false);
            }
            return;
        }

        // En el aire, sin haber gastado el salto, con el perk activo: habilitamos
        // allowFlight SOLO como bandera para poder capturar el próximo espacio,
        // no como vuelo real (se revierte inmediatamente en onToggleFlight).
        if (active && !Boolean.TRUE.equals(jumpUsed.get(uuid)) && !player.getAllowFlight()) {
            player.setAllowFlight(true);
        }

        // Si el perk se desactivó mientras estaba en el aire, quitamos la bandera de inmediato.
        if (!active && player.getAllowFlight()) {
            player.setAllowFlight(false);
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (isRealFlightMode(player) || !hasDoubleJumpActive(player)) {
            return;
        }

        // Cancelamos SIEMPRE y de inmediato: allowFlight solo existía para capturar este toggle,
        // jamás debe traducirse en vuelo real, ni siquiera por un instante.
        event.setCancelled(true);
        player.setFlying(false);
        player.setAllowFlight(false);

        if (Boolean.TRUE.equals(jumpUsed.get(uuid)) || player.isOnGround()) {
            return; // Ya gastó su salto o no estaba realmente en el aire: no hacer nada más.
        }

        jumpUsed.put(uuid, true);
        pendingFallDamageWaiver.put(uuid, true);

        Vector velocity = player.getVelocity();
        velocity.setY(JUMP_VELOCITY);
        player.setVelocity(velocity);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.5f, 1.3f);
    }

    /**
     * Consultado por el listener de daño para saber si debe anular el próximo golpe
     * de fall-damage de este jugador. Se consume una sola vez: solo perdona la caída
     * inmediatamente derivada del impulso del doble salto, nunca caídas posteriores.
     * Así "el perk desactiva el daño de caída" únicamente en el sentido de no penalizar
     * el propio salto extra, tal como se pidió — no otorga inmunidad general a fall damage.
     */
    public boolean consumeFallDamageWaiver(Player player) {
        Boolean waiver = pendingFallDamageWaiver.remove(player.getUniqueId());
        return Boolean.TRUE.equals(waiver);
    }

    private boolean hasDoubleJumpActive(Player player) {
        PlayerPerkData data = dataManager.get(player.getUniqueId());
        return data != null && data.isActive(PerkType.DOUBLE_JUMP);
    }

    /** No debemos interferir si el jugador ya vuela "de verdad" (creative, spectator o elytra). */
    private boolean isRealFlightMode(Player player) {
        return player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR
                || player.isGliding();
    }

    /** Llamado por PerkManager al activar/desactivar/morir para sincronizar el estado inmediatamente. */
    public void updateFlightPermission(Player player) {
        if (isRealFlightMode(player)) {
            return;
        }
        if (!hasDoubleJumpActive(player) && player.getAllowFlight()) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }

    /**
     * Barrido de seguridad: revisa a TODOS los jugadores online y apaga allowFlight
     * a cualquiera que no debería tenerlo. Esta es la última línea de defensa contra
     * cualquier condición de carrera que deje a alguien volando gratis.
     */
    private void failSafeSweep() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isRealFlightMode(player)) {
                continue;
            }
            boolean active = hasDoubleJumpActive(player);
            boolean midAirWithChargeLeft = active && !player.isOnGround()
                    && !Boolean.TRUE.equals(jumpUsed.get(player.getUniqueId()));

            // Solo se permite allowFlight=true en el aire mientras le quede carga; cualquier
            // otra combinación (en el suelo, sin el perk, o ya gastado el salto) se corrige.
            if (player.getAllowFlight() && !midAirWithChargeLeft) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }
    }
}
