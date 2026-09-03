// src/main/java/com/tpuperks/PerkListener.java

package com.tpuperks;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;

public final class PerkListener implements Listener {

    private final TpuPerks plugin;
    private final DataManager data;
    private final PerkManager perkManager;
    private final PerkGUI gui;

    public PerkListener(
            TpuPerks plugin,
            DataManager data,
            PerkManager perkManager
    ) {
        this.plugin = plugin;
        this.data = data;
        this.perkManager = perkManager;
        this.gui = new PerkGUI(
                plugin,
                data,
                perkManager
        );
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        perkManager.checkLevelReward(player);
        perkManager.refresh(player);
    }

    @EventHandler
    public void onLevelChange(PlayerLevelChangeEvent event) {
        Player player = event.getPlayer();

        plugin.getServer().getScheduler().runTask(
                plugin,
                () -> {
                    perkManager.checkLevelReward(player);
                    perkManager.refresh(player);
                }
        );
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        perkManager.removeAllEffectsAndModifiers(player);
        data.clearActivePerks(player.getUniqueId());
        data.save();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        gui.handleClick(event);
    }
}