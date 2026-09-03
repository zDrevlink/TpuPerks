// src/main/java/com/tpuperks/PerkGUI.java

package com.tpuperks;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class PerkGUI {

    private static final String TITLE = "§8✦ TpuPerks";

    private final TpuPerks plugin;
    private final DataManager data;
    private final PerkManager perkManager;

    public PerkGUI(
            TpuPerks plugin,
            DataManager data,
            PerkManager perkManager
    ) {
        this.plugin = plugin;
        this.data = data;
        this.perkManager = perkManager;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(
                null,
                27,
                TITLE
        );

        inventory.setItem(
                4,
                createItem(
                        Material.EXPERIENCE_BOTTLE,
                        "§ePerk Points",
                        List.of(
                                "§7Disponibles: §f" +
                                        data.getPoints(player.getUniqueId()),
                                "",
                                "§7Nivel actual: §f" +
                                        player.getLevel()
                        )
                )
        );

        Perk[] perks = Perk.values();

        int[] slots = {
                10, 11, 12, 14, 15, 16
        };

        for (int i = 0; i < perks.length; i++) {
            Perk perk = perks[i];

            boolean active = data.hasActivePerk(
                    player.getUniqueId(),
                    perk.getId()
            );

            boolean levelOk = perkManager.canUse(
                    player,
                    perk
            );

            Material material;

            if (active) {
                material = Material.LIME_DYE;
            } else if (!levelOk) {
                material = Material.GRAY_DYE;
            } else {
                material = Material.YELLOW_DYE;
            }

            List<String> lore = new ArrayList<>();

            lore.add(perk.getDescription());
            lore.add("");

            lore.add(
                    "§7Nivel requerido: §f" +
                            plugin.getConfig().getInt(
                                    "perks." + perk.getId() +
                                            ".minimum-level",
                                    perk.getMinimumLevel()
                            )
            );

            lore.add("");

            if (active) {
                lore.add("§a✔ PERK ACTIVO");
            } else if (!levelOk) {
                lore.add("§c✘ Nivel insuficiente");
            } else if (data.getPoints(
                    player.getUniqueId()
            ) <= 0) {
                lore.add("§c✘ No tienes Perk Points");
            } else {
                lore.add("§eClick para activar");
                lore.add("§7Costo: §f1 Perk Point");
            }

            inventory.setItem(
                    slots[i],
                    createItem(
                            material,
                            perk.getDisplayName(),
                            lore
                    )
            );
        }

        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();

        int[] slots = {
                10, 11, 12, 14, 15, 16
        };

        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != slot) {
                continue;
            }

            Perk perk = Perk.values()[i];

            if (data.hasActivePerk(
                    player.getUniqueId(),
                    perk.getId()
            )) {
                player.sendMessage(
                        "§cEste perk ya está activo."
                );
                return;
            }

            if (!perkManager.canUse(player, perk)) {
                player.sendMessage(
                        "§cNecesitas más niveles para utilizar este perk."
                );
                return;
            }

            if (data.getPoints(player.getUniqueId()) <= 0) {
                player.sendMessage(
                        "§cNo tienes Perk Points disponibles."
                );
                return;
            }

            if (perkManager.activate(player, perk)) {
                player.sendMessage(
                        "§aHas activado " +
                                perk.getDisplayName() +
                                "§a."
                );

                open(player);
            }

            return;
        }
    }

    private ItemStack createItem(
            Material material,
            String name,
            List<String> lore
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(name);
        meta.setLore(lore);

        item.setItemMeta(meta);

        return item;
    }
}