package com.tpu.perks.gui;

import com.tpu.perks.TpuPerks;
import com.tpu.perks.data.DataManager;
import com.tpu.perks.data.PerkManager;
import com.tpu.perks.data.PerkType;
import com.tpu.perks.data.PlayerPerkData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    public PerkGuiListener(TpuPerks plugin, PerkManager perkManager, DataManager dataManager) {
        this.plugin = plugin;
        this.perkManager = perkManager;
        this.dataManager = dataManager;
        this.factory = new PerkGuiFactory(perkManager);
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

        if (perkManager.isMaxRankReached(type, currentRank)) {
            player.sendMessage(Component.text(type == PerkType.DOUBLE_JUMP
                    ? "Ya tienes el Doble Salto comprado."
                    : "Ese perk ya está en su rango máximo.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        boolean success = perkManager.purchaseRank(player, type);
        if (success) {
            player.sendMessage(Component.text("✔ ", NamedTextColor.GREEN)
                    .append(Component.text("Compraste el rango " + (currentRank + 1) + " de " + type.getDisplayName(),
                            NamedTextColor.GRAY)));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);

            // Si es la primera compra, se activa automáticamente para comodidad del jugador.
            if (currentRank == 0) {
                perkManager.activatePerk(player, type);
            }
        } else {
            player.sendMessage(Component.text("No tienes suficientes Perk Points.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    private void handleToggle(Player player, PerkType type, PlayerPerkData data) {
        if (data.getRank(type) <= 0) {
            player.sendMessage(Component.text("Todavía no has comprado ese perk. Usa Shift+Click para comprarlo.",
                    NamedTextColor.YELLOW));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (data.isActive(type)) {
            perkManager.deactivatePerk(player, type);
            player.sendMessage(Component.text("Desactivaste " + type.getDisplayName() + ".", NamedTextColor.YELLOW));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.8f);
        } else {
            boolean success = perkManager.activatePerk(player, type);
            if (success) {
                player.sendMessage(Component.text("✔ ", NamedTextColor.GREEN)
                        .append(Component.text("Activaste " + type.getDisplayName() + ".", NamedTextColor.GRAY)));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            } else {
                player.sendMessage(Component.text("No cumples el nivel mínimo (" + perkManager.getRequiredLevelThreshold()
                        + ") para activar este perk.", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }

    private PerkType slotToPerkType(int slot) {
        int[] slots = factory.getPerkSlots();
        PerkType[] types = PerkType.values();
        for (int i = 0; i < slots.length && i < types.length; i++) {
            if (slots[i] == slot) {
                return types[i];
            }
        }
        return null;
    }

    public PerkGuiFactory getFactory() {
        return factory;
    }
}
