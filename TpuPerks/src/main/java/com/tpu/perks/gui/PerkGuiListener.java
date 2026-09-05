package com.tpu.perks.gui;

import com.tpu.perks.TpuPerks;
import com.tpu.perks.data.DataManager;
import com.tpu.perks.data.PerkManager;
import com.tpu.perks.data.PerkType;
import com.tpu.perks.data.PlayerPerkData;
import com.tpu.perks.recipe.TrueHeroSwordManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Maneja los clicks dentro de la GUI de TpuPerks:
 *  - Click normal sobre un perk ya comprado -> alterna activar/desactivar.
 *  - Shift+Click sobre un perk -> intenta comprar el siguiente rango.
 *  - Cualquier click se cancela para impedir sacar ítems del inventario.
 */
public class PerkGuiListener implements Listener {

    private final TpuPerks plugin;
    private final PerkManager perkManager;
    private final DataManager dataManager;
    private final PerkGuiFactory factory;
    private final TrueHeroSwordManager trueHeroSwordManager;

    public PerkGuiListener(TpuPerks plugin, PerkManager perkManager, DataManager dataManager,
                            TrueHeroSwordManager trueHeroSwordManager) {
        this.plugin = plugin;
        this.perkManager = perkManager;
        this.dataManager = dataManager;
        this.factory = new PerkGuiFactory(perkManager);
        this.trueHeroSwordManager = trueHeroSwordManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();
        if (!(holder instanceof PerkGuiHolder)) {
            return;
        }

        // Cancelar siempre: es una GUI de solo interacción, no de almacenamiento.
        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInventory)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getSlot();

        if (slot == factory.getResetButtonSlot()) {
            handleResetButton(player, event.isShiftClick());
            player.getOpenInventory().getTopInventory().setContents(factory.build(player).getContents());
            return;
        }

        PerkType clickedType = slotToPerkType(slot);
        if (clickedType == null) {
            return;
        }

        PlayerPerkData data = dataManager.getOrLoad(player.getUniqueId());

        if (event.isShiftClick()) {
            handlePurchase(player, clickedType, data);
        } else {
            handleToggle(player, clickedType, data);
        }

        // Refrescar la GUI para reflejar el nuevo estado.
        player.getOpenInventory().getTopInventory().setContents(factory.build(player).getContents());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof PerkGuiHolder) {
            event.setCancelled(true);
        }
    }

    private void handlePurchase(Player player, PerkType type, PlayerPerkData data) {
        int currentRank = data.getRank(type);
        String plainName = plainName(type);

        if (perkManager.isMaxRankReached(type, currentRank)) {
            player.sendMessage(Component.text(type.getKind() == PerkType.PerkKind.CUSTOM
                    ? "Ya tienes " + plainName + " comprado."
                    : "Ese perk ya está en su rango máximo.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (type.getRequiredLevel() > 0 && player.getLevel() < type.getRequiredLevel()) {
            player.sendMessage(Component.text("Necesitas nivel " + type.getRequiredLevel()
                    + " para comprar " + plainName + ".", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        boolean success = perkManager.purchaseRank(player, type);
        if (success) {
            player.sendMessage(Component.text("✔ ", NamedTextColor.GREEN)
                    .append(Component.text("Compraste el rango " + (currentRank + 1) + " de " + plainName,
                            NamedTextColor.GRAY)));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);

            // Si es la primera compra, se activa automáticamente para comodidad del jugador.
            if (currentRank == 0) {
                perkManager.activatePerk(player, type);
            }

            // "Legado del Héroe Verdadero": al comprarlo, se desbloquea la receta secreta
            // SOLO para este jugador y se le otorga el advancement THE TRUE HERO (toast
            // morado + broadcast global nativo de Minecraft + sonido épico adicional).
            if (type == PerkType.TRUE_HERO && currentRank == 0) {
                trueHeroSwordManager.discoverRecipeFor(player);
                trueHeroSwordManager.grantAdvancement(player);
            }
        } else {
            player.sendMessage(Component.text("No tienes suficientes Perk Points.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    private void handleToggle(Player player, PerkType type, PlayerPerkData data) {
        String plainName = plainName(type);

        if (data.getRank(type) <= 0) {
            player.sendMessage(Component.text("Todavía no has comprado ese perk. Usa Shift+Click para comprarlo.",
                    NamedTextColor.YELLOW));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (data.isActive(type)) {
            perkManager.deactivatePerk(player, type);
            player.sendMessage(Component.text("Desactivaste " + plainName + ".", NamedTextColor.YELLOW));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.8f);
        } else {
            boolean success = perkManager.activatePerk(player, type);
            if (success) {
                player.sendMessage(Component.text("✔ ", NamedTextColor.GREEN)
                        .append(Component.text("Activaste " + plainName + ".", NamedTextColor.GRAY)));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            } else {
                int required = Math.max(perkManager.getRequiredLevelThreshold(), type.getRequiredLevel());
                player.sendMessage(Component.text("No cumples el nivel mínimo (" + required
                        + ") para activar este perk.", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }

    /** Convierte el nombre legacy (con códigos §) de un perk a texto plano, para insertarlo en frases sueltas. */
    private String plainName(PerkType type) {
        Component component = LegacyComponentSerializer.legacySection().deserialize(type.getDisplayName());
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Maneja el botón "Redistribuir Perks": solo actúa con Shift+Click, como confirmación
     * para evitar resets accidentales por un click normal. Reembolsa el costo total gastado
     * como Perk Points disponibles, y pone todos los rangos comprados de vuelta en 0.
     */
    private void handleResetButton(Player player, boolean isShiftClick) {
        if (!isShiftClick) {
            player.sendMessage(Component.text("⚠ Usa Shift+Click para confirmar el reset de perks.",
                    NamedTextColor.YELLOW));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.8f);
            return;
        }

        perkManager.redistributePerks(player);
        player.sendMessage(Component.text("↺ ", NamedTextColor.YELLOW)
                .append(Component.text("Restableciste todos tus rangos de perks. Tus Perk Points fueron reembolsados"
                        + " y puedes repartirlos de nuevo.", NamedTextColor.GRAY)));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.0f);
    }

    private PerkType slotToPerkType(int slot) {
        return factory.slotToPerkType(slot);
    }

    public PerkGuiFactory getFactory() {
        return factory;
    }
}

