package com.tpu.perks.listeners;

import com.tpu.perks.TpuPerks;
import com.tpu.perks.data.DataManager;
import com.tpu.perks.data.PerkManager;
import com.tpu.perks.data.PerkType;
import com.tpu.perks.data.PlayerPerkData;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

/**
 * Implementa el perk único "Doble Salto".
 *
 * Mecánica: mientras el jugador tenga el perk activo (y no esté ya en modo
 * vuelo por creative/spectator/elytra), se le habilita "allowFlight" para que
 * el cliente le deje pulsar espacio en el aire. Cuando lo hace, Bukkit dispara
 * PlayerToggleFlightEvent con isFlying()=true; en ese momento cancelamos el
 * vuelo real y en su lugar le damos un impulso vertical (el "segundo salto"),
 * consumiendo la carga hasta que vuelva a tocar el suelo.
 *
 * Esto evita depender de PlayerAnimationEvent (poco fiable para detectar salto)
 * y no interfiere con Elytra ni con el vuelo real de creative/spectator.
 */
public class DoubleJumpListener implements Listener {

    private final TpuPerks plugin;
    private final PerkManager perkManager;
    private final DataManager dataManager;

    /** Fuerza del impulso vertical del segundo salto. Ajustable si se quiere más/menos altura. */
    private static final double JUMP_VELOCITY = 0.62;

    public DoubleJumpListener(TpuPerks plugin, PerkManager perkManager, DataManager dataManager) {
        this.plugin = plugin;
        this.perkManager = perkManager;
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Sincroniza el estado de allowFlight apenas entra, por si tenía el perk activo guardado.
        updateFlightPermission(event.getPlayer());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Solo nos importa cuando aterriza, para "recargar" la habilidad de doble salto.
        if (player.isOnGround() && !player.getAllowFlight() && hasDoubleJumpActive(player)
                && !isRealFlightMode(player)) {
            player.setAllowFlight(true);
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        if (!hasDoubleJumpActive(player) || isRealFlightMode(player)) {
            return;
        }

        // El jugador activó "vuelo" en el aire pulsando espacio dos veces: esto es el gatillo del doble salto.
        if (event.isFlying() && !player.isOnGround()) {
            event.setCancelled(true);
            player.setAllowFlight(false);
            player.setFlying(false);

            Vector velocity = player.getVelocity();
            velocity.setY(JUMP_VELOCITY);
            player.setVelocity(velocity);

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.5f, 1.3f);
        }
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

    /** Activa/desactiva allowFlight según si el jugador tiene el perk activo (llamado al activar/desactivar/morir). */
    public void updateFlightPermission(Player player) {
        if (isRealFlightMode(player)) {
            return;
        }
        boolean shouldHaveDoubleJump = hasDoubleJumpActive(player);
        if (!shouldHaveDoubleJump && player.getAllowFlight()) {
            player.setAllowFlight(false);
            player.setFlying(false);
        } else if (shouldHaveDoubleJump && player.isOnGround()) {
            player.setAllowFlight(true);
        }
    }
}
