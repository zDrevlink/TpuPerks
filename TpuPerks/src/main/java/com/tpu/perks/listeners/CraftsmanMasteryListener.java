package com.tpu.perks.listeners;

import com.tpu.perks.TpuPerks;
import com.tpu.perks.data.DataManager;
import com.tpu.perks.data.PerkType;
import com.tpu.perks.data.PlayerPerkData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.ThreadLocalRandom;

/**
 * Implementa el perk único "Maestría del Artesano" (requiere nivel 100+):
 * cuando el jugador craftea una herramienta, arma, escudo, tijeras o mechero,
 * el ítem resultante tiene una probabilidad de salir con encantamientos
 * aleatorios ya aplicados (0 a 5), respetando siempre la compatibilidad
 * real de encantamientos entre sí y con el tipo de ítem (Bukkit se encarga
 * de esa validación mediante Enchantment#canEnchantItem/conflictsWith).
 *
 * Probabilidades (config: perk-values.craftsman.chances):
 *   5 encantamientos = la MENOS probable
 *   0 encantamientos = la MÁS probable
 * tal como se pidió explícitamente.
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

        int amountOfEnchants = rollEnchantCount();
        if (amountOfEnchants <= 0) {
            return; // 0 encantamientos: el ítem sale normal, sin gastar cómputo de más.
        }

        // El resultado del crafteo puede clonarse varias veces en el inventario (crafteo en stack);
        // aplicamos el encantamiento a la copia que realmente recibirá el jugador.
        ItemStack finalItem = result.clone();
        applyRandomCompatibleEnchants(finalItem, amountOfEnchants);
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
     */
    private boolean isEligible(Material material) {
        String name = material.name();
        if (material == Material.SHEARS || material == Material.FLINT_AND_STEEL || material == Material.SHIELD) {
            return true;
        }
        return name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE") || name.endsWith("_SWORD");
    }

    /**
     * Sortea cuántos encantamientos tendrá el ítem (0 a 5), donde 0 es el resultado
     * más probable y 5 el menos probable, según las probabilidades configuradas.
     */
    private int rollEnchantCount() {
        int[] weights = new int[6];
        for (int i = 0; i <= 5; i++) {
            weights[i] = plugin.getConfig().getInt("perk-values.craftsman.weight-" + i, defaultWeight(i));
        }

        int total = 0;
        for (int w : weights) total += Math.max(0, w);
        if (total <= 0) {
            return 0;
        }

        int roll = ThreadLocalRandom.current().nextInt(total);
        int cumulative = 0;
        for (int i = 0; i <= 5; i++) {
            cumulative += Math.max(0, weights[i]);
            if (roll < cumulative) {
                return i;
            }
        }
        return 0;
    }

    /** Pesos por defecto: decrecen fuertemente para que 5 sea muchísimo menos probable que 0. */
    private int defaultWeight(int enchantCount) {
        return switch (enchantCount) {
            case 0 -> 45;
            case 1 -> 25;
            case 2 -> 15;
            case 3 -> 9;
            case 4 -> 4;
            case 5 -> 2;
            default -> 0;
        };
    }

    /**
     * Aplica hasta `amount` encantamientos aleatorios al ítem, respetando SIEMPRE:
     *  - que el encantamiento pueda aplicarse a ese tipo de ítem (canEnchantItem)
     *  - que no entre en conflicto con encantamientos ya aplicados (conflictsWith)
     * Si tras varios intentos no se pueden añadir más encantamientos compatibles
     * (p. ej. se agotaron las opciones libres de conflicto), simplemente se detiene
     * con los que ya logró aplicar, sin forzar nada incompatible.
     */
    private void applyRandomCompatibleEnchants(ItemStack item, int amount) {
        List<Enchantment> candidates = new ArrayList<>();
        for (Enchantment enchantment : Enchantment.values()) {
            if (enchantment.canEnchantItem(item)) {
                candidates.add(enchantment);
            }
        }

        List<Enchantment> applied = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = candidates.size() * 4; // margen generoso para no quedar en loop infinito

        while (applied.size() < amount && !candidates.isEmpty() && attempts < maxAttempts) {
            attempts++;
            Enchantment pick = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));

            boolean conflicts = applied.stream().anyMatch(e -> e.conflictsWith(pick) || pick.conflictsWith(e));
            if (conflicts) {
                candidates.remove(pick);
                continue;
            }

            int level = randomLevelFor(pick);
            item.addUnsafeEnchantment(pick, level);
            applied.add(pick);
            candidates.remove(pick);
        }
    }

    /** Nivel aleatorio entre 1 y el máximo natural del encantamiento (nunca por encima de lo vanilla-legal). */
    private int randomLevelFor(Enchantment enchantment) {
        int max = enchantment.getMaxLevel();
        if (max <= 1) {
            return 1;
        }
        return ThreadLocalRandom.current().nextInt(max) + 1;
    }
}
