package com.tpu.perks.gui;

import com.tpu.perks.data.PerkManager;
import com.tpu.perks.data.PerkType;
import com.tpu.perks.data.PlayerPerkData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Construye el contenido visual de la GUI de perks. Se reconstruye cada vez
 * que algo cambia (compra, activación) para reflejar el estado actualizado.
 *
 * Diseño: 54 slots (6 filas).
 *  - Fila 1: panel de información (puntos disponibles, umbral de nivel).
 *  - Filas 2-4: los 14 perks normales, 5 por fila (15 huecos, 1 libre).
 *  - Fila 5, centro: las 2 "Maestrías" especiales de alto nivel (Artesano y
 *    Herrero), lado a lado, destacadas con marco dorado.
 *  - Bordes: cristal negro decorativo.
 */
public class PerkGuiFactory {

    private static final int SIZE = 54;
    // 14 perks "normales" repartidos en 3 filas centrales (5 por fila, el último hueco vacío).
    private static final int[] PERK_SLOTS = {
            10, 11, 12, 13, 14,
            19, 20, 21, 22, 23,
            28, 29, 30, 31
    };
    // Las dos Maestrías van juntas, destacadas en su propia fila.
    private static final int CRAFTSMAN_SLOT = 39;
    private static final int BLACKSMITH_SLOT = 41;
    // Botón de reset de perks (mantiene los puntos), en la última fila, centrado.
    private static final int RESET_BUTTON_SLOT = 49;

    private final PerkManager perkManager;

    public PerkGuiFactory(PerkManager perkManager) {
        this.perkManager = perkManager;
    }

    public Inventory build(Player player) {
        PerkGuiHolder holder = new PerkGuiHolder();
        Component title = Component.text("✦ ", NamedTextColor.GOLD)
                .append(Component.text("TpuPerks", NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text(" ✦", NamedTextColor.GOLD));

        Inventory inventory = org.bukkit.Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inventory);

        fillBorder(inventory);
        inventory.setItem(4, buildInfoItem(player));

        PerkType[] normalTypes = normalPerks();
        for (int i = 0; i < normalTypes.length && i < PERK_SLOTS.length; i++) {
            inventory.setItem(PERK_SLOTS[i], buildPerkItem(player, normalTypes[i]));
        }

        inventory.setItem(CRAFTSMAN_SLOT, buildPerkItem(player, PerkType.CRAFTSMAN_MASTERY));
        inventory.setItem(BLACKSMITH_SLOT, buildPerkItem(player, PerkType.BLACKSMITH_MASTERY));

        if (isResetButtonEnabled()) {
            inventory.setItem(RESET_BUTTON_SLOT, buildResetButton(player));
        }

        return inventory;
    }

    /** Si el botón de reset de perks está habilitado en config.yml (default: activo). */
    private boolean isResetButtonEnabled() {
        return perkManager.getPlugin().getConfig().getBoolean("settings.enable-reset-button", true);
    }

    private ItemStack buildResetButton(Player player) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("↺ Redistribuir Perks", NamedTextColor.YELLOW, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Devuelve TODOS los rangos comprados a 0 y", NamedTextColor.GRAY));
        lore.add(Component.text("te reembolsa cada punto gastado en ellos.", NamedTextColor.GRAY));
        lore.add(Component.text("Tus Perk Points disponibles NO se pierden:", NamedTextColor.GRAY));
        lore.add(Component.text("podrás repartirlos donde quieras después.", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("Esto NO borra el desbloqueo de recetas/logros", NamedTextColor.DARK_GRAY));
        lore.add(Component.text("permanentes (ej. Legado del Héroe Verdadero).", NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("▶ Shift+Click para confirmar el reset", NamedTextColor.RED));

        setLore(meta, lore);
        item.setItemMeta(meta);
        return item;
    }

    /** Todos los perks excepto las dos Maestrías, que se muestran aparte por ser especiales. */
    private PerkType[] normalPerks() {
        List<PerkType> list = new ArrayList<>();
        for (PerkType type : PerkType.values()) {
            if (type != PerkType.CRAFTSMAN_MASTERY && type != PerkType.BLACKSMITH_MASTERY) {
                list.add(type);
            }
        }
        return list.toArray(new PerkType[0]);
    }

    private void fillBorder(Inventory inventory) {
        ItemStack filler = namedItem(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "));
        for (int slot : new int[]{
                0,1,2,3,5,6,7,8,
                9,17, 18,26, 27,35, 36,44,
                45,46,47,48,50,51,52,53
        }) {
            inventory.setItem(slot, filler);
        }
        // Marco decorativo dorado alrededor de las dos Maestrías para destacarlas.
        for (int slot : new int[]{37, 38, 40, 42, 43}) {
            inventory.setItem(slot, namedItem(Material.YELLOW_STAINED_GLASS_PANE, Component.text(" ")));
        }
    }

    private ItemStack buildInfoItem(Player player) {
        PlayerPerkData data = perkManager.getDataManager().getOrLoad(player.getUniqueId());
        boolean meetsThreshold = perkManager.meetsThreshold(player);

        ItemStack item = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Tus Perk Points", NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Puntos disponibles: ", NamedTextColor.GRAY)
                .append(Component.text(data.getAvailablePoints(), NamedTextColor.AQUA)));
        lore.add(Component.text("Tu nivel actual: ", NamedTextColor.GRAY)
                .append(Component.text(player.getLevel(), NamedTextColor.GREEN)));
        lore.add(Component.text("Nivel mínimo para mantener perks: ", NamedTextColor.GRAY)
                .append(Component.text(perkManager.getRequiredLevelThreshold(),
                        meetsThreshold ? NamedTextColor.GREEN : NamedTextColor.RED)));
        lore.add(Component.empty());
        lore.add(meetsThreshold
                ? Component.text("✔ Cumples el umbral requerido", NamedTextColor.GREEN)
                : Component.text("✘ No cumples el umbral: tus perks no se activarán", NamedTextColor.RED));
        lore.add(Component.empty());
        lore.add(Component.text("Ganas 1 punto cada " + getLevelsPerPoint() + " niveles.", NamedTextColor.DARK_GRAY));
        lore.add(Component.text("⚠ Morir reinicia TODO tu progreso de perks.", NamedTextColor.RED));

        setLore(meta, lore);
        item.setItemMeta(meta);
        return item;
    }

    private int getLevelsPerPoint() {
        return perkManager.getPlugin().getConfig().getInt("settings.levels-per-point", 20);
    }

    private ItemStack buildPerkItem(Player player, PerkType type) {
        PlayerPerkData data = perkManager.getDataManager().getOrLoad(player.getUniqueId());
        int rank = data.getRank(type);
        boolean active = data.isActive(type);
        boolean maxed = perkManager.isMaxRankReached(type, rank);
        int cost = perkManager.getCostForNextRank(rank);
        boolean canAfford = data.getAvailablePoints() >= cost;
        boolean isUnique = type.getKind() == PerkType.PerkKind.CUSTOM;

        Material material = type.getIcon();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Vampirismo usa una poción como ícono: se le fuerza color rojo sangre para que
        // se vea como una "poción roja" tal como se pidió, sin depender de un PotionType
        // vanilla específico (evita que el cliente le ponga su tinte por defecto azulado).
        if (type == PerkType.VAMPIRISM && meta instanceof org.bukkit.inventory.meta.PotionMeta potionMeta) {
            potionMeta.setColor(org.bukkit.Color.fromRGB(153, 0, 0));
            meta = potionMeta;
        }

        // --- Nombre épico ---
        Component typeName = LegacyComponentSerializer.legacySection().deserialize(type.getDisplayName());
        Component name = typeName.decoration(TextDecoration.ITALIC, false);
        if (!isUnique) {
            name = name.append(Component.text(" [" + toRoman(Math.max(rank, 0)) + "]", NamedTextColor.GRAY));
        }
        meta.displayName(name);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        // --- Texto corto: qué hace el perk mecánicamente ---
        lore.add(Component.text("▸ Efecto", NamedTextColor.WHITE, TextDecoration.BOLD));
        String effectText = isUnique
                ? type.getShortEffect()
                : type.getShortEffect().replace("%s", describeAmount(type, rank));
        for (String line : effectText.split("\n")) {
            lore.add(LegacyComponentSerializer.legacySection().deserialize(line));
        }
        lore.add(Component.empty());

        // --- Lore narrativo corto ---
        for (String line : type.getFlavorLore().split("\n")) {
            lore.add(LegacyComponentSerializer.legacySection().deserialize(line));
        }
        lore.add(Component.empty());

        if (type.getRequiredLevel() > 0) {
            boolean meetsSpecial = player.getLevel() >= type.getRequiredLevel();
            lore.add(Component.text("Requiere nivel: ", NamedTextColor.GRAY)
                    .append(Component.text(type.getRequiredLevel(), meetsSpecial ? NamedTextColor.GREEN : NamedTextColor.RED)));
            lore.add(Component.empty());
        }

        if (!isUnique) {
            lore.add(Component.text("Rango actual: ", NamedTextColor.GRAY)
                    .append(Component.text(rank, NamedTextColor.YELLOW))
                    .append(maxLabelSuffix(type)));
        }

        if (!maxed) {
            lore.add(Component.text(isUnique ? "Costo: " : "Costo próximo rango: ", NamedTextColor.GRAY)
                    .append(Component.text(cost + " puntos", canAfford ? NamedTextColor.GREEN : NamedTextColor.RED)));
        } else {
            lore.add(Component.text(isUnique ? "¡Ya comprado!" : "¡Rango máximo alcanzado!", NamedTextColor.LIGHT_PURPLE));
        }

        lore.add(Component.empty());
        if (active) {
            lore.add(Component.text("Estado: ", NamedTextColor.GRAY)
                    .append(Component.text("ACTIVO ✔", NamedTextColor.GREEN)));
            lore.add(Component.text("▶ Click para desactivar", NamedTextColor.RED));
        } else if (rank > 0) {
            lore.add(Component.text("Estado: ", NamedTextColor.GRAY)
                    .append(Component.text("INACTIVO", NamedTextColor.YELLOW)));
            lore.add(Component.text("▶ Click para activar", NamedTextColor.GREEN));
        } else {
            lore.add(Component.text("Estado: ", NamedTextColor.GRAY)
                    .append(Component.text("SIN COMPRAR", NamedTextColor.DARK_GRAY)));
        }

        if (!maxed) {
            lore.add(Component.text("▶ Shift+Click para comprar rango", canAfford ? NamedTextColor.AQUA : NamedTextColor.DARK_GRAY));
        }

        setLore(meta, lore);
        if (active) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    /** Sufijo "/ N" junto al rango actual cuando el perk tiene un tope propio (ej. Forjado en Fuego: X / 5). */
    private Component maxLabelSuffix(PerkType type) {
        if (type.getMaxRankOverride() > 0) {
            return Component.text(" / " + type.getMaxRankOverride(), NamedTextColor.DARK_GRAY);
        }
        return Component.empty();
    }

    private String describeAmount(PerkType type, int rank) {
        double perRank = perkManager.getPerRankValueFor(type);
        return switch (type) {
            case HEALTH -> String.valueOf(rank); // ya expresado en corazones directamente
            case REACH -> String.valueOf((int) (perRank * rank));
            case FIRE_FORGED -> String.valueOf((int) Math.round(perkManager.getFireResistancePercent(rank) * 100));
            default -> String.valueOf(rank); // amplifier / nivel de efecto
        };
    }

    private String toRoman(int number) {
        if (number <= 0) return "0";
        String[] thousands = {"", "M", "MM", "MMM"};
        String[] hundreds = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        String[] tens = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        String[] ones = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        if (number >= 4000) {
            return String.valueOf(number); // fallback para rangos absurdamente altos
        }
        return thousands[number / 1000] + hundreds[(number % 1000) / 100] + tens[(number % 100) / 10] + ones[number % 10];
    }

    private void setLore(ItemMeta meta, List<Component> lore) {
        List<Component> italicFree = new ArrayList<>();
        for (Component line : lore) {
            italicFree.add(line.decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(italicFree);
    }

    private ItemStack namedItem(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    public int[] getPerkSlots() {
        return PERK_SLOTS;
    }

    public int getResetButtonSlot() {
        return RESET_BUTTON_SLOT;
    }

    /** Traduce un slot clickeado de vuelta al PerkType que representa, o null si no es un slot de perk. */
    public PerkType slotToPerkType(int slot) {
        if (slot == CRAFTSMAN_SLOT) {
            return PerkType.CRAFTSMAN_MASTERY;
        }
        if (slot == BLACKSMITH_SLOT) {
            return PerkType.BLACKSMITH_MASTERY;
        }
        PerkType[] normal = normalPerks();
        for (int i = 0; i < PERK_SLOTS.length && i < normal.length; i++) {
            if (PERK_SLOTS[i] == slot) {
                return normal[i];
            }
        }
        return null;
    }
}
