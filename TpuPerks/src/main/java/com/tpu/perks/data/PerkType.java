package com.tpu.perks.data;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.potion.PotionEffectType;

/**
 * Define todos los perks disponibles. Cada perk escala infinitamente:
 * el "rango" (nivel comprado por el jugador) determina cuánto se le suma
 * al atributo base o cuál amplifier de PotionEffect se le aplica.
 *
 * No hay límite superior de rango en el código: el límite real lo pone
 * el config.yml (max-rank) o simplemente cuántos puntos acumule el jugador.
 */
public enum PerkType {

    STRENGTH(
            "strength",
            "§cFuerza",
            "§7+%s de daño por rango (Strength)",
            Material.IRON_SWORD,
            PerkKind.POTION_EFFECT,
            PotionEffectType.STRENGTH,
            null
    ),
    SPEED(
            "speed",
            "§bVelocidad",
            "§7+%s de velocidad por rango (Speed)",
            Material.SUGAR,
            PerkKind.POTION_EFFECT,
            PotionEffectType.SPEED,
            null
    ),
    HEALTH(
            "health",
            "§4Corazón Extra",
            "§7+%s corazón(es) de vida máxima por rango",
            Material.APPLE,
            PerkKind.ATTRIBUTE,
            null,
            Attribute.MAX_HEALTH
    ),
    RESISTANCE(
            "resistance",
            "§9Resistencia",
            "§7+%s de resistencia al daño por rango",
            Material.SHIELD,
            PerkKind.POTION_EFFECT,
            PotionEffectType.RESISTANCE,
            null
    ),
    HASTE(
            "haste",
            "§eVelocidad de Minado",
            "§7+%s de velocidad de minado por rango (Haste)",
            Material.GOLDEN_PICKAXE,
            PerkKind.POTION_EFFECT,
            PotionEffectType.HASTE,
            null
    ),
    REACH(
            "reach",
            "§dAlcance",
            "§7+%s bloques de alcance por rango",
            Material.FISHING_ROD,
            PerkKind.ATTRIBUTE_DUAL,
            null,
            Attribute.BLOCK_INTERACTION_RANGE
    ),
    DOUBLE_JUMP(
            "double_jump",
            "§fDoble Salto",
            "§7Permite dar un salto extra en el aire (perk único, sin rangos)",
            Material.FEATHER,
            PerkKind.CUSTOM,
            null,
            null
    );

    /** Tipo de mecánica usada internamente para aplicar el perk. */
    public enum PerkKind {
        /** Se aplica como PotionEffect infinito con amplifier = rango - 1. */
        POTION_EFFECT,
        /** Se aplica como modifier sobre un único Attribute. */
        ATTRIBUTE,
        /** Se aplica sobre dos atributos relacionados (usado por REACH: bloque + entidad). */
        ATTRIBUTE_DUAL,
        /** Perk único (sin escalado por rango) manejado por lógica propia, ej. Doble Salto. */
        CUSTOM
    }

    private final String id;
    private final String displayName;
    private final String descriptionFormat;
    private final Material icon;
    private final PerkKind kind;
    private final PotionEffectType potionEffectType;
    private final Attribute attribute;

    PerkType(String id, String displayName, String descriptionFormat, Material icon,
             PerkKind kind, PotionEffectType potionEffectType, Attribute attribute) {
        this.id = id;
        this.displayName = displayName;
        this.descriptionFormat = descriptionFormat;
        this.icon = icon;
        this.kind = kind;
        this.potionEffectType = potionEffectType;
        this.attribute = attribute;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescriptionFormat() {
        return descriptionFormat;
    }

    public Material getIcon() {
        return icon;
    }

    public PerkKind getKind() {
        return kind;
    }

    public PotionEffectType getPotionEffectType() {
        return potionEffectType;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public static PerkType fromId(String id) {
        for (PerkType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
