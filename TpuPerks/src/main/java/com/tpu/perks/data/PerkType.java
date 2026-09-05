package com.tpu.perks.data;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.potion.PotionEffectType;

/**
 * Define todos los perks disponibles. Cada perk escala (salvo los marcados como
 * CUSTOM de rango único, o los que traen un maxRankOverride propio): el "rango"
 * comprado por el jugador determina cuánto se le suma al atributo base o cuál
 * amplifier de PotionEffect se le aplica.
 *
 * Cada perk trae tres textos distintos para la GUI:
 *  - displayName: nombre épico mostrado como título del ítem.
 *  - shortEffect: una línea corta y clara de qué hace mecánicamente el perk.
 *  - flavorLore: 1-3 líneas de lore/ambientación, con estilo narrativo.
 */
public enum PerkType {

    STRENGTH(
            "strength",
            "§c§lPuño del Titán",
            "§7+%s de daño de ataque por rango",
            "§7§oSe dice que quien lo porta puede partir\n"
                    + "§7§oescudos de hierro con un solo golpe.",
            Material.IRON_SWORD,
            PerkKind.POTION_EFFECT,
            PotionEffectType.STRENGTH,
            null,
            0,
            0
    ),
    SPEED(
            "speed",
            "§b§lPaso del Viento",
            "§7+%s de velocidad de movimiento por rango",
            "§7§oTus pies apenas rozan el suelo;\n"
                    + "§7§oel viento mismo parece abrirte camino.",
            Material.SUGAR,
            PerkKind.POTION_EFFECT,
            PotionEffectType.SPEED,
            null,
            0,
            0
    ),
    HEALTH(
            "health",
            "§4§lCorazón Inquebrantable",
            "§7+%s corazón(es) de vida máxima por rango",
            "§7§oUn corazón forjado en la adversidad late\n"
                    + "§7§ocon más fuerza que el acero mismo.",
            Material.APPLE,
            PerkKind.ATTRIBUTE,
            null,
            Attribute.MAX_HEALTH,
            0,
            0
    ),
    RESISTANCE(
            "resistance",
            "§9§lPiel de Roca",
            "§7+%s de resistencia al daño por rango",
            "§7§oCada golpe que recibes se estrella\n"
                    + "§7§ocontra una coraza invisible pero real.",
            Material.SHIELD,
            PerkKind.POTION_EFFECT,
            PotionEffectType.RESISTANCE,
            null,
            0,
            0
    ),
    HASTE(
            "haste",
            "§e§lManos de Minero Ancestral",
            "§7+%s de velocidad de minado por rango",
            "§7§oLos picos se rompen de envidia al ver\n"
                    + "§7§ocon qué facilidad partes la piedra.",
            Material.GOLDEN_PICKAXE,
            PerkKind.POTION_EFFECT,
            PotionEffectType.HASTE,
            null,
            0,
            0
    ),
    REACH(
            "reach",
            "§d§lBrazo Largo del Cazador",
            "§7+%s bloques de alcance por rango",
            "§7§oDicen que puede tocar lo que otros\n"
                    + "§7§osolo alcanzan a soñar con rozar.",
            Material.FISHING_ROD,
            PerkKind.ATTRIBUTE_DUAL,
            null,
            Attribute.BLOCK_INTERACTION_RANGE,
            0,
            0
    ),
    DOUBLE_JUMP(
            "double_jump",
            "§f§lAlas Invisibles",
            "§7Permite dar un salto extra en el aire",
            "§7§oNadie ve las alas, pero todos ven\n"
                    + "§7§ocómo desafías la caída dos veces.",
            Material.FEATHER,
            PerkKind.CUSTOM,
            null,
            null,
            0,
            0
    ),
    INCINERATOR(
            "incinerator",
            "§6§lToque del Infierno",
            "§7Tus golpes cuerpo a cuerpo prenden fuego al objetivo",
            "§7§oTus manos ya no sienten el calor:\n"
                    + "§7§olo reparten con cada golpe que das.",
            Material.FIRE_CHARGE,
            PerkKind.CUSTOM,
            null,
            null,
            0,
            0
    ),
    CRAFTSMAN_MASTERY(
            "craftsman_mastery",
            "§6§lMaestría del Artesano",
            "§7Tus herramientas, armas, tijeras, mecheros y escudos\n"
                    + "§7crafteados pueden salir ya encantados (0 a 5 encantos)",
            "§7§oLos antiguos herreros susurraban que solo un elegido\n"
                    + "§7§opodía arrancarle secretos al metal con sus propias\n"
                    + "§7§omanos, sin yunque ni mesa de encantar.",
            Material.ENCHANTED_BOOK,
            PerkKind.CUSTOM,
            null,
            null,
            100,
            0
    ),
    BLACKSMITH_MASTERY(
            "blacksmith_mastery",
            "§b§lMaestría del Herrero",
            "§7Tus armaduras crafteadas pueden salir ya encantadas\n"
                    + "§7(0 a 5 encantamientos compatibles con armadura)",
            "§7§oNo todo herrero forja acero: algunos forjan\n"
                    + "§7§odestinos, y el tuyo quedó grabado en cada placa\n"
                    + "§7§oque sale de tu yunque.",
            Material.IRON_HELMET,
            PerkKind.CUSTOM,
            null,
            null,
            150,
            0
    ),
    MAGNETISM(
            "magnetism",
            "§5§lLlamado del Abismo",
            "§7Atrae ítems y orbes de experiencia del suelo\n"
                    + "§7en un radio de 10 bloques",
            "§7§oLa tierra misma parece rendirle tributo,\n"
                    + "§7§oofreciéndole lo que otros dejan atrás.",
            Material.STONE,
            PerkKind.CUSTOM,
            null,
            null,
            0,
            0
    ),
    EXPERT_SWIMMER(
            "expert_swimmer",
            "§3§lAliento de las Profundidades",
            "§7Nadas un 35% más rápido dentro del agua",
            "§7§oLas corrientes del océano lo reconocen\n"
                    + "§7§ocomo uno de los suyos y le ceden el paso.",
            Material.KELP,
            PerkKind.CUSTOM,
            null,
            null,
            0,
            0
    ),
    FIRE_FORGED(
            "fire_forged",
            "§4§lTemple del Volcán",
            "§7Reduce el daño de fuego y lava: 5% por rango\n"
                    + "§7(rango 1 = 5%, rango 5 = 25% máximo)",
            "§7§oCuenta la leyenda que se bañó en lava\n"
                    + "§7§otantas veces que las llamas ya lo consideran familia.",
            Material.MAGMA_BLOCK,
            PerkKind.FIRE_RESISTANCE,
            null,
            null,
            0,
            5
    ),
    VAMPIRISM(
            "vampirism",
            "§4§lSed Carmesí",
            "§75% de probabilidad de robar vida al golpear\n"
                    + "§7en cuerpo a cuerpo (perk único)",
            "§7§oCada golpe que asestas despierta un hambre\n"
                    + "§7§oantigua que solo la sangre ajena calma.",
            Material.POTION,
            PerkKind.CUSTOM,
            null,
            null,
            0,
            0
    ),
    CHOSEN_EMBLEM(
            "chosen_emblem",
            "§5§l§nBlasón del Elegido",
            "§7+5 corazones de vida máxima. Al caer a 5 corazones\n"
                    + "§7o menos, activa el ÚLTIMO ALIENTO (cooldown 1h)",
            "§7§oCuando todo parece perdido, algo ancestral\n"
                    + "§7§odespierta en su pecho y se niega a dejarlo caer.\n"
                    + "§7§oSolo los elegidos por el destino conocen ese instante.",
            Material.TOTEM_OF_UNDYING,
            PerkKind.CUSTOM,
            null,
            null,
            125,
            0
    ),
    TRUE_HERO(
            "true_hero",
            "§d§l§nLegado del Héroe Verdadero",
            "§7Desbloquea la receta secreta de una espada legendaria\n"
                    + "§7de netherite, ya grabada en tu libro de recetas",
            "§7§oNo todos merecen forjar el arma de las leyendas.\n"
                    + "§7§oSolo aquel cuyo nombre quedará escrito en la\n"
                    + "§7§ohistoria puede desbloquear sus secretos.",
            Material.NETHERITE_SWORD,
            PerkKind.CUSTOM,
            null,
            null,
            200,
            0
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
        CUSTOM,
        /** Reducción porcentual de daño de fuego/lava, escala en pasos fijos hasta un tope propio. */
        FIRE_RESISTANCE
    }

    private final String id;
    private final String displayName;
    private final String shortEffect;
    private final String flavorLore;
    private final Material icon;
    private final PerkKind kind;
    private final PotionEffectType potionEffectType;
    private final Attribute attribute;
    /** Nivel de XP extra requerido para ACTIVAR este perk, además del umbral general. 0 = ninguno. */
    private final int requiredLevel;
    /** Límite de rango propio de este perk (0 = usa settings.max-rank global, o infinito si ese también es 0). */
    private final int maxRankOverride;

    PerkType(String id, String displayName, String shortEffect, String flavorLore, Material icon,
             PerkKind kind, PotionEffectType potionEffectType, Attribute attribute,
             int requiredLevel, int maxRankOverride) {
        this.id = id;
        this.displayName = displayName;
        this.shortEffect = shortEffect;
        this.flavorLore = flavorLore;
        this.icon = icon;
        this.kind = kind;
        this.potionEffectType = potionEffectType;
        this.attribute = attribute;
        this.requiredLevel = requiredLevel;
        this.maxRankOverride = maxRankOverride;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Texto corto y directo de qué hace el perk mecánicamente. Puede contener "%s" para el monto por rango. */
    public String getShortEffect() {
        return shortEffect;
    }

    /** Lore narrativo/ambientación, sin datos mecánicos. */
    public String getFlavorLore() {
        return flavorLore;
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

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public int getMaxRankOverride() {
        return maxRankOverride;
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
