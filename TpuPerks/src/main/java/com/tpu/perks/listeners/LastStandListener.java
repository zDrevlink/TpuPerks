package com.tpu.perks.listeners;

import com.tpu.perks.TpuPerks;
import com.tpu.perks.data.DataManager;
import com.tpu.perks.data.PerkType;
import com.tpu.perks.data.PlayerPerkData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementa el "Último Aliento" (Last Stand) del perk "Blasón del Elegido":
 * cuando el jugador con el perk activo queda a 5 corazones (10 puntos) de vida
 * o menos tras recibir daño, se dispara un aviso épico global (sonido + mensaje
 * en el chat de todo el servidor), con un cooldown de 1 hora por jugador.
 *
 * IMPORTANTE: esto es un aviso/momento dramático, NO otorga invulnerabilidad,
 * curación, ni ningún efecto de combate — tal como se pidió, el perk solo
 * "anuncia" el momento con un sonido y mensaje épico. Si en el futuro se quiere
 * que además cure o proteja al jugador, eso debe pedirse explícitamente aparte,
 * ya que cambiaría el balance del perk de forma significativa.
 */
public class LastStandListener implements Listener {

    private final TpuPerks plugin;
    private final DataManager dataManager;

    /** Cooldown de 1 hora en milisegundos, tal como se pidió explícitamente. */
    private static final long COOLDOWN_MILLIS = 60L * 60L * 1000L;

    /** Umbral de vida (en puntos, no corazones): 5 corazones = 10.0 de vida. */
    private static final double HEALTH_THRESHOLD = 10.0;

    private final Map<UUID, Long> lastTriggered = new HashMap<>();

    public LastStandListener(TpuPerks plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PlayerPerkData data = dataManager.get(player.getUniqueId());
        if (data == null || !data.isActive(PerkType.CHOSEN_EMBLEM)) {
            return;
        }

        double healthAfterDamage = player.getHealth() - event.getFinalDamage();
        if (healthAfterDamage > HEALTH_THRESHOLD || healthAfterDamage <= 0) {
            return; // No entra en el rango del Last Stand (o ya murió: PlayerListener maneja la muerte).
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastTriggered.get(uuid);
        if (last != null && (now - last) < COOLDOWN_MILLIS) {
            return; // Todavía en cooldown: no repetir el aviso.
        }

        lastTriggered.put(uuid, now);
        triggerLastStand(player);
    }

    private void triggerLastStand(Player player) {
        Component message = Component.text("⚔ ", NamedTextColor.DARK_RED, TextDecoration.BOLD)
                .append(Component.text("¡ÚLTIMO ALIENTO! ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .append(Component.text(player.getName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" se niega a caer, aferrado al filo de la vida por voluntad del ",
                        NamedTextColor.GRAY))
                .append(Component.text("Blasón del Elegido", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .append(Component.text(".", NamedTextColor.GRAY));

        // Broadcast global: todo el servidor ve el momento, tal como se pidió.
        Bukkit.broadcast(message);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.playSound(online.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.4f);
            online.playSound(online.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.3f, 1.8f);
        }
    }

    /** Milisegundos restantes de cooldown para este jugador (0 si ya está disponible). */
    public long getRemainingCooldownMillis(UUID uuid) {
        Long last = lastTriggered.get(uuid);
        if (last == null) {
            return 0L;
        }
        long elapsed = System.currentTimeMillis() - last;
        return Math.max(0L, COOLDOWN_MILLIS - elapsed);
    }
}
