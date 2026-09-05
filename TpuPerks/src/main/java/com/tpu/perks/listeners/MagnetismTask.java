package com.tpu.perks.listeners;

import com.tpu.perks.TpuPerks;
import com.tpu.perks.data.DataManager;
import com.tpu.perks.data.PerkType;
import com.tpu.perks.data.PlayerPerkData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Implementa el perk único "Llamado del Abismo" (Atracción Mental): atrae ítems
 * dropeados y orbes de experiencia del suelo hacia el jugador en un radio de 10 bloques.
 *
 * Diseño pensado para rendimiento: en vez de escuchar movimiento o usar un evento por
 * entidad, corre como UNA sola tarea periódica que recorre solo a los jugadores con el
 * perk activo (normalmente pocos) y usa getNearbyEntities acotado al radio configurado,
 * evitando escanear el mundo completo o crear listeners pesados por jugador.
 */
public class MagnetismTask implements Runnable {

    private final TpuPerks plugin;
    private final DataManager dataManager;

    /** Radio de atracción en bloques, tal como se pidió explícitamente. */
    private static final double RADIUS = 10.0;

    /** Velocidad con la que se atraen los ítems/orbes hacia el jugador cada tick de la tarea. */
    private static final double PULL_STRENGTH = 0.35;

    public MagnetismTask(TpuPerks plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    /** Registra esta tarea para correr cada 4 ticks (5 veces por segundo): suficiente para verse fluido. */
    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this, 20L, 4L);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerPerkData data = dataManager.get(player.getUniqueId());
            if (data == null || !data.isActive(PerkType.MAGNETISM)) {
                continue;
            }

            Location center = player.getLocation();
            for (Entity entity : player.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
                if (entity instanceof Item item) {
                    if (item.getPickupDelay() <= 0) {
                        pullTowards(entity, center);
                    }
                } else if (entity instanceof ExperienceOrb) {
                    pullTowards(entity, center);
                }
            }
        }
    }

    private void pullTowards(Entity entity, Location target) {
        Vector direction = target.toVector().subtract(entity.getLocation().toVector());
        double distanceSquared = direction.lengthSquared();

        // Ya está prácticamente encima del jugador: dejar que el pickup vanilla lo recoja.
        if (distanceSquared < 0.6) {
            return;
        }

        Vector pull = direction.normalize().multiply(PULL_STRENGTH);
        // Un ligero componente vertical para que no se queden "arrastrando" el suelo si hay desnivel.
        pull.setY(Math.max(pull.getY(), 0.05));
        entity.setVelocity(entity.getVelocity().add(pull));
    }
}
