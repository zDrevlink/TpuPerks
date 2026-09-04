package com.tpu.perks.listeners;

import com.tpu.perks.TpuPerks;
import com.tpu.perks.data.DataManager;
import com.tpu.perks.data.PerkType;
import com.tpu.perks.data.PlayerPerkData;
import com.tpu.perks.util.RandomEnchantUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Implementa el perk único "Maestría del Artesano" (requiere nivel 100+):
 * cuando el jugador craftea una herramienta, arma, escudo, tijeras o mechero,
 * el ítem resultante tiene una probabilidad de salir con encantamientos
 * aleatorios ya aplicados (0 a 5), respetando siempre la compatibilidad
 * real de encantamientos entre sí y con el tipo de ítem.
 *
 * Probabilidades (config: perk-values.craftsman.weight-N): 0 encantamientos es
 * el resultado más probable, 5 el menos probable, tal como se pidió explícitamente.
 */
public class CraftsmanMasteryListener implements Listener {

    private final TpuPerks plugin;
    private final DataManager dataManager;

    public CraftsmanMasteryListener(TpuPerks plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) {
            return;
        }

        if (!isEligible(result.getType())) {
            return;
        }

        PlayerPerkData data = dataManager.get(player.getUniqueId());
        if (data == null || !data.isActive(PerkType.CRAFTSMAN_MASTERY)) {
            return;
        }

        int amountOfEnchants = RandomEnchantUtil.rollEnchantCount(plugin.getConfig(), "perk-values.craftsman");
        if (amountOfEnchants <= 0) {
            return; // 0 encantamientos: el ítem sale normal, sin gastar cómputo de más.
        }

        // El resultado del crafteo puede clonarse varias veces en el inventario (crafteo en stack);
        // aplicamos el encantamiento a la copia que realmente recibirá el jugador.
        ItemStack finalItem = result.clone();
        RandomEnchantUtil.applyRandomCompatibleEnchants(finalItem, amountOfEnchants);
        event.setCurrentItem(finalItem);

        player.sendMessage(Component.text("⚒ ", NamedTextColor.GOLD)
                .append(Component.text("¡Maestría del Artesano activada! ", NamedTextColor.YELLOW))
                .append(Component.text("Tu creación surgió con " + amountOfEnchants
                        + (amountOfEnchants == 1 ? " encantamiento." : " encantamientos."), NamedTextColor.GRAY)));
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 1.2f);
    }

    /**
     * Determina qué tipos de ítem cuentan como "herramienta" para este perk:
     * herramientas normales (picos, palas, hachas, azadas), espadas, tijeras,
     * mecheros (flint and steel) y escudos, tal como se pidió explícitamente.
     * Las armaduras NO son elegibles aquí: esas las cubre Maestría del Herrero.
     */
    private boolean isEligible(Material material) {
        String name = material.name();
        if (material == Material.SHEARS || material == Material.FLINT_AND_STEEL || material == Material.SHIELD) {
            return true;
        }
        return name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE") || name.endsWith("_SWORD");
    }
}
