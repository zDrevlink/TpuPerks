package com.tpu.perks.gui;

import com.tpu.perks.data.PerkManager;
import com.tpu.perks.data.PerkType;
import com.tpu.perks.data.PlayerPerkData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
 * Diseño: 45 slots (5 filas).
 *  - Fila 1: panel de información (puntos disponibles, umbral de nivel).
 *  - Filas 2-4: un ítem por perk, centrados.
 *  - Fila 5: cristal decorativo de relleno.
 */
public class PerkGuiFactory {

    private static final int SIZE = 45;
    // Slots centrales de la fila 2 y 3 para distribuir los 6 perks de forma prolija.
    private static final int[] PERK_SLOTS = {11, 13, 15, 29, 31, 33};

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

        PerkType[] types = PerkType.values();
        for (int i = 0; i < types.length && i < PERK_SLOTS.length; i++) {
            inventory.setItem(PERK_SLOTS[i], buildPerkItem(player, types[i]));
        }

        return inventory;
    }

    private void fillBorder(Inventory inventory) {
        ItemStack filler = namedItem(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "));
        for (int slot : new int[]{0,1,2,3,5,6,7,8, 9,17, 18,26, 27,35, 36,37,38,39,40,41,42,43,44}) {
            inventory.setItem(slot, filler);
        }
    }

    private ItemStack buildInfoItem(Player player) {
        PlayerPerkData data = perkManager.getDataManager().getOrLoad(player.getUniqueId());
        boolean meetsThreshold = perkManager.meetsThreshold(player);

        ItemStack item = new ItemStack(Material.NETHER_STAR);
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
        boolean maxed = perkManager.isMaxRankReached(rank);
        int cost = perkManager.getCostForNextRank(rank);
        boolean canAfford = data.getAvailablePoints() >= cost;

        Material material = type.getIcon();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        Component name = Component.text(type.getDisplayName() + " ", active ? NamedTextColor.GREEN : NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text("[" + toRoman(Math.max(rank, 0)) + "]", NamedTextColor.GRAY));
        meta.displayName(name);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        double perRank = perkManager.getPerRankValueFor(type);
        String amountText = describeAmount(type, rank, perRank);
        lore.add(Component.text(String.format(type.getDescriptionFormat().replace("%s", "%s"), amountText),
                NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("Rango actual: ", NamedTextColor.GRAY)
                .append(Component.text(rank, NamedTextColor.YELLOW)));

        if (!maxed) {
            lore.add(Component.text("Costo próximo rango: ", NamedTextColor.GRAY)
                    .append(Component.text(cost + " puntos", canAfford ? NamedTextColor.GREEN : NamedTextColor.RED)));
        } else {
            lore.add(Component.text("¡Rango máximo alcanzado!", NamedTextColor.LIGHT_PURPLE));
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

    private String describeAmount(PerkType type, int rank, double perRank) {
        return switch (type) {
            case HEALTH -> String.valueOf(rank); // ya expresado en corazones directamente
            case REACH -> String.valueOf((int) (perRank * rank));
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
}
