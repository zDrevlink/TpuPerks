package com.tpu.perks.data;

import com.tpu.perks.TpuPerks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

/**
 * Contiene toda la lógica de negocio de los perks: otorgar puntos por nivel,
 * calcular el costo de subir de rango, aplicar/quitar el efecto real en el
 * jugador, y verificar periódicamente que se mantenga el umbral de nivel exigido.
 */
public class PerkManager {

    private final TpuPerks plugin;
    private final DataManager dataManager;

    /** Un modifier fijo por perk para poder identificarlo y removerlo sin duplicar. */
    private final NamespacedKey attributeKey;

    public PerkManager(TpuPerks plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.attributeKey = new NamespacedKey(plugin, "tpuperks_modifier");
    }

    // ---------------------------------------------------------------------
    // Puntos por nivel de experiencia
    // ---------------------------------------------------------------------

    /**
     * Revisa cuántos "lotes" de levels-per-point ha alcanzado el jugador y
     * le otorga los puntos nuevos que le falten, sin volver a darle los ya otorgados.
     * Se debe llamar cuando cambia el nivel de experiencia del jugador.
     */
    public void checkAndGrantPoints(Player player) {
        int levelsPerPoint = plugin.getConfig().getInt("settings.levels-per-point", 20);
        if (levelsPerPoint <= 0) {
            return;
        }

        PlayerPerkData data = dataManager.getOrLoad(player.getUniqueId());
        int currentBatches = player.getLevel() / levelsPerPoint;
        int alreadyGranted = data.getLastGrantedPointBatches();

        if (currentBatches > alreadyGranted) {
            int newPoints = currentBatches - alreadyGranted;
            data.addPoints(newPoints);
            data.setLastGrantedPointBatches(currentBatches);

            player.sendMessage(Component.text("✦ ", NamedTextColor.GOLD)
                    .append(Component.text("¡Has ganado ", NamedTextColor.YELLOW))
                    .append(Component.text(newPoints, NamedTextColor.GOLD))
                    .append(Component.text(newPoints == 1 ? " Perk Point!" : " Perk Points!", NamedTextColor.YELLOW))
                    .append(Component.text(" (Total: " + data.getAvailablePoints() + ")", NamedTextColor.GRAY)));

            dataManager.saveAsync(data);
        }
    }

    // ---------------------------------------------------------------------
    // Costos y compra
    // ---------------------------------------------------------------------

    /**
     * Costo en puntos para pasar de "currentRank" al siguiente rango.
     * Escala linealmente según config (base-cost + increment * rank), permitiendo
     * progresión infinita: cada rango es simplemente más caro que el anterior.
     */
    public int getCostForNextRank(int currentRank) {
        int base = plugin.getConfig().getInt("settings.cost.base", 1);
        int increment = plugin.getConfig().getInt("settings.cost.increment-per-rank", 1);
        return base + (increment * currentRank);
    }

    /** Rango máximo configurable; 0 o negativo significa "sin límite" (infinito). */
    public int getMaxRank() {
        return plugin.getConfig().getInt("settings.max-rank", 0);
    }

    public boolean isMaxRankReached(int currentRank) {
        int max = getMaxRank();
        return max > 0 && currentRank >= max;
    }

    /** Igual que isMaxRankReached pero respeta el tope fijo de 1 para perks CUSTOM (únicos, sin rangos). */
    public boolean isMaxRankReached(PerkType type, int currentRank) {
        if (type.getKind() == PerkType.PerkKind.CUSTOM) {
            return currentRank >= 1;
        }
        return isMaxRankReached(currentRank);
    }

    /**
     * Intenta comprar (subir de rango) un perk. Devuelve true si tuvo éxito.
     * No activa el perk automáticamente: eso lo decide la GUI/el jugador aparte,
     * aunque normalmente se activa al comprar el primer rango.
     */
    public boolean purchaseRank(Player player, PerkType type) {
        PlayerPerkData data = dataManager.getOrLoad(player.getUniqueId());
        int currentRank = data.getRank(type);

        if (isMaxRankReached(type, currentRank)) {
            return false;
        }

        if (type.getRequiredLevel() > 0 && player.getLevel() < type.getRequiredLevel()) {
            return false;
        }

        int cost = getCostForNextRank(currentRank);
        if (data.getAvailablePoints() < cost) {
            return false;
        }

        data.addPoints(-cost);
        data.incrementRank(type);

        // FIX: si el perk ya estaba activo, hay que reaplicarlo con el nuevo rango,
        // si no el jugador se queda con el amplifier/atributo del rango anterior
        // hasta que lo desactive y reactive manualmente.
        if (data.isActive(type) && meetsThreshold(player)) {
            applyPerk(player, type, data.getRank(type));
        }

        dataManager.saveAsync(data);
        return true;
    }

    // ---------------------------------------------------------------------
    // Umbral mínimo de nivel para mantener perks activos
    // ---------------------------------------------------------------------

    public int getRequiredLevelThreshold() {
        return plugin.getConfig().getInt("settings.min-level-threshold", 10);
    }

    public boolean meetsThreshold(Player player) {
        return player.getLevel() >= getRequiredLevelThreshold();
    }

    /** Umbral efectivo para activar un perk específico: el mayor entre el general y el propio del perk. */
    public boolean meetsThreshold(Player player, PerkType type) {
        int required = Math.max(getRequiredLevelThreshold(), type.getRequiredLevel());
        return player.getLevel() >= required;
    }

    /**
     * Intenta activar un perk para el jugador. Falla si no tiene rango comprado
     * o si no cumple el umbral mínimo de nivel (general o el propio del perk, ej.
     * Maestría del Artesano exige nivel 100 aunque el umbral general sea menor).
     */
    public boolean activatePerk(Player player, PerkType type) {
        PlayerPerkData data = dataManager.getOrLoad(player.getUniqueId());
        if (data.getRank(type) <= 0) {
            return false;
        }
        if (!meetsThreshold(player, type)) {
            return false;
        }

        data.setActive(type, true);
        applyPerk(player, type, data.getRank(type));
        dataManager.saveAsync(data);
        return true;
    }

    public void deactivatePerk(Player player, PerkType type) {
        PlayerPerkData data = dataManager.getOrLoad(player.getUniqueId());
        data.setActive(type, false);
        removePerk(player, type);

        if (type == PerkType.DOUBLE_JUMP && player.getGameMode() != org.bukkit.GameMode.CREATIVE
                && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }

        dataManager.saveAsync(data);
    }

    /**
     * Reset TOTAL al morir: se pierden los perks activos (efectos/atributos removidos),
     * TODOS los rangos comprados de cada perk vuelven a 0, y los Perk Points disponibles
     * vuelven a 0. El jugador empieza de cero en el sistema de perks tras cada muerte
     * (hardcore total, por decisión explícita del dueño del servidor).
     */
    public void fullResetOnDeath(Player player) {
        PlayerPerkData data = dataManager.get(player.getUniqueId());
        if (data == null) {
            return;
        }

        for (PerkType type : PerkType.values()) {
            if (data.isActive(type)) {
                removePerk(player, type);
            }
            data.setRank(type, 0);
        }
        data.deactivateAll();
        data.setAvailablePoints(0);
        // No reseteamos lastGrantedPointBatches: representa niveles de XP ya "cobrados"
        // en puntos, y el nivel de XP del jugador es independiente de este reset de perks
        // (vanilla ya penaliza la muerte con pérdida de XP por separado).

        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }

        dataManager.saveAsync(data);
    }

    /**
     * Aplica todos los perks marcados como activos de un jugador. Pensado para
     * llamarse en el join, reaplicando el estado guardado (siempre que cumpla el umbral).
     */
    public void reapplyActivePerksOnJoin(Player player) {
        PlayerPerkData data = dataManager.getOrLoad(player.getUniqueId());

        for (PerkType type : PerkType.values()) {
            if (data.isActive(type)) {
                if (meetsThreshold(player, type)) {
                    applyPerk(player, type, data.getRank(type));
                } else {
                    // Ya no cumple el umbral (general o específico del perk): se desactiva silenciosamente.
                    data.setActive(type, false);
                }
            }
        }
        dataManager.saveAsync(data);
    }

    // ---------------------------------------------------------------------
    // Aplicación real de efectos/atributos
    // ---------------------------------------------------------------------

    private void applyPerk(Player player, PerkType type, int rank) {
        switch (type.getKind()) {
            case POTION_EFFECT -> applyPotionEffect(player, type.getPotionEffectType(), rank);
            case ATTRIBUTE -> applyAttribute(player, type.getAttribute(), type, rank);
            case ATTRIBUTE_DUAL -> {
                applyAttribute(player, Attribute.BLOCK_INTERACTION_RANGE, type, rank);
                applyAttribute(player, Attribute.ENTITY_INTERACTION_RANGE, type, rank);
            }
            case CUSTOM -> {
                // El Doble Salto no toca atributos ni pociones: DoubleJumpListener consulta
                // directamente PlayerPerkData#isActive(DOUBLE_JUMP), así que aquí no hace falta nada.
            }
        }
    }

    private void removePerk(Player player, PerkType type) {
        switch (type.getKind()) {
            case POTION_EFFECT -> player.removePotionEffect(type.getPotionEffectType());
            case ATTRIBUTE -> removeAttributeModifier(player, type.getAttribute());
            case ATTRIBUTE_DUAL -> {
                removeAttributeModifier(player, Attribute.BLOCK_INTERACTION_RANGE);
                removeAttributeModifier(player, Attribute.ENTITY_INTERACTION_RANGE);
            }
            case CUSTOM -> {
                // Nada que limpiar: el listener revisa el flag activo en cada intento de salto.
            }
        }
    }

    private void applyPotionEffect(Player player, PotionEffectType effectType, int rank) {
        // amplifier = rank - 1 porque el nivel I (rank 1) equivale a amplifier 0.
        int amplifier = Math.max(0, rank - 1);
        PotionEffect effect = new PotionEffect(
                effectType,
                PotionEffect.INFINITE_DURATION,
                amplifier,
                true,   // ambient (visual más discreto)
                false,  // sin partículas para no molestar el rendimiento visual/cliente
                true    // mostrar icono en el HUD
        );
        player.addPotionEffect(effect);
    }

    private void applyAttribute(Player player, Attribute attribute, PerkType type, int rank) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        removeAttributeModifierFromInstance(instance);

        double perRank = getPerRankValueFor(type);
        double totalValue = perRank * rank;

        AttributeModifier modifier = new AttributeModifier(
                attributeKey,
                totalValue,
                AttributeModifier.Operation.ADD_NUMBER
        );
        instance.addModifier(modifier);

        // Si es MAX_HEALTH, curamos la diferencia para que el jugador sienta el corazón nuevo lleno.
        if (attribute == Attribute.MAX_HEALTH) {
            double newMax = instance.getValue();
            if (player.getHealth() < newMax) {
                // No forzamos curación completa, solo evitamos que quede "invisible" el corazón nuevo
                // dejando la salud actual intacta; Paper ya clampa automáticamente.
            }
        }
    }

    private void removeAttributeModifier(Player player, Attribute attribute) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        removeAttributeModifierFromInstance(instance);
    }

    private void removeAttributeModifierFromInstance(AttributeInstance instance) {
        instance.getModifiers().stream()
                .filter(mod -> mod.getKey().equals(attributeKey))
                .findFirst()
                .ifPresent(instance::removeModifier);
    }

    /** Valor que aporta CADA rango de un perk. Configurable en config.yml. */
    public double getPerRankValueFor(PerkType type) {
        return plugin.getConfig().getDouble("perk-values." + type.getId(), defaultPerRank(type));
    }

    private double defaultPerRank(PerkType type) {
        return switch (type) {
            case HEALTH -> 2.0;      // +2.0 = +1 corazón (cada corazón = 2 puntos de salud)
            case REACH -> 1.0;       // +1 bloque de alcance
            default -> 0.0;          // Los PotionEffect no usan este valor (usan amplifier directo)
        };
    }

    // ---------------------------------------------------------------------
    // Tarea periódica de verificación de umbral
    // ---------------------------------------------------------------------

    /**
     * Inicia una tarea repetitiva que revisa a todos los jugadores online:
     * si algún perk activo ya no cumple el umbral mínimo de nivel, se desactiva
     * automáticamente y se le avisa al jugador.
     */
    public void startThresholdCheckTask(long intervalTicks) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                PlayerPerkData data = dataManager.get(uuid);
                if (data == null) {
                    continue;
                }

                boolean anyChanged = false;
                for (PerkType type : PerkType.values()) {
                    if (data.isActive(type) && !meetsThreshold(player, type)) {
                        removePerk(player, type);
                        data.setActive(type, false);
                        anyChanged = true;

                        int required = Math.max(getRequiredLevelThreshold(), type.getRequiredLevel());
                        Component displayName = LegacyComponentSerializer.legacySection().deserialize(type.getDisplayName());
                        player.sendMessage(Component.text("⚠ ", NamedTextColor.RED)
                                .append(displayName)
                                .append(Component.text(" se desactivó: necesitas nivel ", NamedTextColor.YELLOW))
                                .append(Component.text(required, NamedTextColor.GOLD))
                                .append(Component.text(".", NamedTextColor.YELLOW)));
                    }
                }
                if (anyChanged) {
                    dataManager.saveAsync(data);
                }
            }
        }, intervalTicks, intervalTicks);
    }

    public TpuPerks getPlugin() {
        return plugin;
    }

    public DataManager getDataManager() {
        return dataManager;
    }
}
