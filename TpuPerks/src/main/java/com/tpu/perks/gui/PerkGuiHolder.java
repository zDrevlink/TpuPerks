package com.tpu.perks.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * InventoryHolder marcador para identificar de forma segura que un inventario
 * abierto es la GUI de TpuPerks (evita depender del título, que puede fallar
 * con distintos idiomas o si otro plugin usa un título parecido).
 */
public class PerkGuiHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
