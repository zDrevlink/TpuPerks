// src/main/java/com/tpuperks/Perk.java

package com.tpuperks;

public enum Perk {

    STRENGTH(
            "strength",
            "§c+1 Fuerza",
            "§7Obtienes Fuerza I.",
            20
    ),

    SPEED(
            "speed",
            "§b+1 Velocidad",
            "§7Aumenta tu velocidad de movimiento.",
            40
    ),

    HEALTH(
            "health",
            "§c+1 Corazón",
            "§7Obtienes +1 corazón de vida.",
            60
    ),

    RESISTANCE(
            "resistance",
            "§8+1 Resistencia",
            "§7Obtienes Resistencia I.",
            80
    ),

    HASTE(
            "haste",
            "§e+1 Velocidad de Minado",
            "§7Obtienes Haste I.",
            100
    ),

    REACH(
            "reach",
            "§d+1 Alcance",
            "§7Obtienes +1 de alcance.",
            120
    );

    private final String id;
    private final String displayName;
    private final String description;
    private final int minimumLevel;

    Perk(
            String id,
            String displayName,
            String description,
            int minimumLevel
    ) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.minimumLevel = minimumLevel;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getMinimumLevel() {
        return minimumLevel;
    }

    public static Perk fromId(String id) {
        for (Perk perk : values()) {
            if (perk.id.equalsIgnoreCase(id)) {
                return perk;
            }
        }

        return null;
    }
}