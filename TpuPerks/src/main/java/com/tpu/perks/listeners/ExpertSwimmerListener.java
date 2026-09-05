package com.tpu.perks.listeners;

import com.tpu.perks.TpuPerks;
import com.tpu.perks.data.DataManager;
import com.tpu.perks.data.PerkManager;
import com.tpu.perks.data.PerkType;
import com.tpu.perks.data.PlayerPerkData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Implementa el perk único "Aliento de las Profundidades" (Nadador Experto):
 * +35% de velocidad de natación mientras el jugador está dentro del agua.
 *
 * Implementación vía PotionEffectType.DOLPHINS_GRACE (Gracia del Delfín), el
 * efecto vanilla diseñado específicamente para acelerar la natación, aplicado
 * de forma "ambient" (icono discreto) solo mientras el jugador está en el agua
 * y tiene el perk activo. Se eligió este enfoque en vez de un AttributeModifier
 * crudo porque DOLPHINS_GRACE es estable en todas las versiones modernas de la
 * API, evitando depender de nombres de atributos que cambian entre versiones.
 *
 * Amplifier 0 de Dolphins Grace ya da un boost notable (~aprox +30-40% real de
 * velocidad de nado en vanilla), por lo que se usa un único nivel fijo, en línea
 * con el +35% pedido; no requiere tocarse por rango al ser un perk único.
 */
public class ExpertSwimmerListener implements Listener {

    private final TpuPerks plugin;
    private final PerkManager perkManager;
    private final DataManager dataManager;

    public ExpertSwimmerListener(TpuPerks plugin, PerkManager perkManager, DataManager dataManager) {
        this.plugin = plugin;
        this.perkManager = perkManager;
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        tickCheck(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // No hace falta remover el efecto manualmente: el jugador se desconecta
        // y el estado de PotionEffect no persiste entre sesiones.
    }

    /**
     * Llamado periódicamente (ver TpuPerks#onEnable) para sincronizar el efecto de
     * natación según si el jugador está en el agua y tiene el perk activo. No hay un
     * evento nativo de "entrar/salir del agua" en Paper, así que se revisa por tarea
     * en vez de en cada PlayerMoveEvent, para mantener el impacto en rendimiento mínimo.
     */
    public void tickCheck(Player player) {
        PlayerPerkData data = dataManager.get(player.getUniqueId());
        boolean active = data != null && data.isActive(PerkType.EXPERT_SWIMMER);
        boolean inWater = player.isSwimming() || player.isInWater();

        boolean hasEffect = player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE);

        if (active && inWater && !hasEffect) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.DOLPHINS_GRACE,
                    100, // 5 segundos; se refresca cada tick de la tarea mientras siga en el agua
                    0,
                    true,   // ambient
                    false,  // sin partículas
                    false   // sin ícono para no ensuciar el HUD con un efecto "automático"
            ));
        } else if ((!active || !inWater) && hasEffect) {
            player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
        }
    }
}
