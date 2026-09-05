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
 * Implementa el perk único "Maestría del Herrero" (requiere nivel 150+):
 * misma mecánica que Maestría del Artesano, pero exclusiva para armaduras
 * (cascos, petos, pantalones, botas, incluyendo variantes de turtle/elytra
 * no aplica al no ser crafteable como armadura estándar). El ítem resultante
 * puede salir con 0 a 5 encantamientos ya aplicados, respetando siempre la
 * compatibilidad real de encantamientos con armadura.
 */
public class BlacksmithMasteryListener implements Listener {

    private final TpuPerks plugin;
    private final DataManager dataManager;

    public BlacksmithMasteryListener(TpuPerks plugin, DataManager dataManager) {
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

        if (!isArmor(result.getType())) {
            return;
        }

        PlayerPerkData data = dataManager.get(player.getUniqueId());
        if (data == null || !data.isActive(PerkType.BLACKSMITH_MASTERY)) {
            return;
        }

        int amountOfEnchants = RandomEnchantUtil.rollEnchantCount(plugin.getConfig(), "perk-values.blacksmith");
        if (amountOfEnchants <= 0) {
            return;
        }

        ItemStack finalItem = result.clone();
        RandomEnchantUtil.applyRandomCompatibleEnchants(finalItem, amountOfEnchants);
        event.setCurrentItem(finalItem);

        player.sendMessage(Component.text("🛡 ", NamedTextColor.AQUA)
                .append(Component.text("¡Maestría del Herrero activada! ", NamedTextColor.YELLOW))
                .append(Component.text("Tu armadura surgió con " + amountOfEnchants
                        + (amountOfEnchants == 1 ? " encantamiento." : " encantamientos."), NamedTextColor.GRAY)));
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 0.8f);
    }

    /** Cascos, petos, pantalones y botas de cualquier material (incluye turtle helmet). */
    private boolean isArmor(Material material) {
        String name = material.name();
        if (material == Material.TURTLE_HELMET) {
            return true;
        }
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }
}
