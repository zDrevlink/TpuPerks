package com.tpu.perks.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Lógica compartida de sorteo y aplicación de encantamientos aleatorios compatibles,
 * usada tanto por "Maestría del Artesano" (herramientas/armas) como por
 * "Maestría del Herrero" (armaduras). Centralizada aquí para no duplicar código
 * entre ambos listeners.
 */
public final class RandomEnchantUtil {

    private RandomEnchantUtil() {
    }

    /**
     * Sortea cuántos encantamientos (0 a 5) le tocan a un ítem, según los pesos
     * configurados bajo el prefijo dado (ej. "perk-values.craftsman" o
     * "perk-values.blacksmith"). 0 es el resultado más probable por defecto,
     * 5 el menos probable, tal como se pidió explícitamente.
     */
    public static int rollEnchantCount(FileConfiguration config, String weightPrefix) {
        int[] weights = new int[6];
        for (int i = 0; i <= 5; i++) {
            weights[i] = config.getInt(weightPrefix + ".weight-" + i, defaultWeight(i));
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
    private static int defaultWeight(int enchantCount) {
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
     * Si tras varios intentos no se pueden añadir más encantamientos compatibles,
     * simplemente se detiene con los que ya logró aplicar, sin forzar nada incompatible.
     */
    public static void applyRandomCompatibleEnchants(ItemStack item, int amount) {
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
    private static int randomLevelFor(Enchantment enchantment) {
        int max = enchantment.getMaxLevel();
        if (max <= 1) {
            return 1;
        }
        return ThreadLocalRandom.current().nextInt(max) + 1;
    }
}
