// src/main/java/com/tpuperks/PerkManager.java

package com.tpuperks;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public final class PerkManager {

    private static final String MODIFIER_PREFIX = "tpuperks_";

    private final TpuPerks plugin;
    private final DataManager data;

    public PerkManager(TpuPerks plugin, DataManager data) {
        this.plugin = plugin;
        this.data = data;
    }

    public void checkLevelReward(Player player) {
        int level = player.getLevel();
        int lastAwarded = data.getLastAwardedLevel(player.getUniqueId());

        int currentMilestone = (level / plugin.getConfig().getInt(
                "levels.levels-per-point", 20
        )) * plugin.getConfig().getInt(
                "levels.levels-per-point", 20
        );

        if (currentMilestone <= lastAwarded) {
            return;
        }

        int levelStep = plugin.getConfig().getInt(
                "levels.levels-per-point", 20
        );

        int newPoints = (currentMilestone - lastAwarded) / levelStep;

        if (newPoints > 0) {
            data.addPoints(player.getUniqueId(), newPoints);
            data.setLastAwardedLevel(player.getUniqueId(), currentMilestone);
            data.save();

            player.sendMessage(
                    "§aHas obtenido §e" + newPoints +
                    " §aPerk Point" + (newPoints == 1 ? "" : "s") + "."
            );
        }
    }

    public boolean canUse(Player player, Perk perk) {
        int minimumLevel = plugin.getConfig().getInt(
                "perks." + perk.getId() + ".minimum-level",
                perk.getMinimumLevel()
        );

        return player.getLevel() >= minimumLevel;
    }

    public boolean activate(Player player, Perk perk) {
        UUID uuid = player.getUniqueId();

        if (data.hasActivePerk(uuid, perk.getId())) {
            return false;
        }

        if (!canUse(player, perk)) {
            return false;
        }

        if (data.getPoints(uuid) <= 0) {
            return false;
        }

        data.addPoints(uuid, -1);
        data.addActivePerk(uuid, perk.getId());

        applyPerk(player, perk);
        data.save();

        return true;
    }

    public void refresh(Player player) {
        removeAllEffectsAndModifiers(player);

        for (String id : data.getActivePerks(player.getUniqueId())) {
            Perk perk = Perk.fromId(id);

            if (perk == null) {
                continue;
            }

            if (canUse(player, perk)) {
                applyPerk(player, perk);
            } else {
                data.removeActivePerk(
                        player.getUniqueId(),
                        perk.getId()
                );
            }
        }

        data.save();
    }

    public void removeAllEffectsAndModifiers(Player player) {
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.HASTE);

        removeModifier(
                player,
                Attribute.MAX_HEALTH,
                "health"
        );

        removeModifier(
                player,
                Attribute.MOVEMENT_SPEED,
                "speed"
        );

        removeModifier(
                player,
                Attribute.BLOCK_INTERACTION_RANGE,
                "reach-block"
        );

        removeModifier(
                player,
                Attribute.ENTITY_INTERACTION_RANGE,
                "reach-entity"
        );

        // Evita que el jugador conserve una vida máxima inválida.
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private void applyPerk(Player player, Perk perk) {
        switch (perk) {

            case STRENGTH -> player.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.STRENGTH,
                            Integer.MAX_VALUE,
                            0,
                            false,
                            false,
                            true
                    )
            );

            case SPEED -> {
                AttributeInstance attribute =
                        player.getAttribute(Attribute.MOVEMENT_SPEED);

                if (attribute != null) {
                    addModifier(
                            attribute,
                            "speed",
                            plugin.getConfig().getDouble(
                                    "perks.speed.amount", 1.0
                            ),
                            AttributeModifier.Operation.ADD_NUMBER
                    );
                }
            }

            case HEALTH -> {
                AttributeInstance attribute =
                        player.getAttribute(Attribute.MAX_HEALTH);

                if (attribute != null) {
                    addModifier(
                            attribute,
                            "health",
                            plugin.getConfig().getDouble(
                                    "perks.health.amount", 2.0
                            ),
                            AttributeModifier.Operation.ADD_NUMBER
                    );
                }
            }

            case RESISTANCE -> player.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.RESISTANCE,
                            Integer.MAX_VALUE,
                            0,
                            false,
                            false,
                            true
                    )
            );

            case HASTE -> player.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.HASTE,
                            Integer.MAX_VALUE,
                            0,
                            false,
                            false,
                            true
                    )
            );

            case REACH -> {
                AttributeInstance block =
                        player.getAttribute(
                                Attribute.BLOCK_INTERACTION_RANGE
                        );

                AttributeInstance entity =
                        player.getAttribute(
                                Attribute.ENTITY_INTERACTION_RANGE
                        );

                double amount = plugin.getConfig().getDouble(
                        "perks.reach.amount", 1.0
                );

                if (block != null) {
                    addModifier(
                            block,
                            "reach-block",
                            amount,
                            AttributeModifier.Operation.ADD_NUMBER
                    );
                }

                if (entity != null) {
                    addModifier(
                            entity,
                            "reach-entity",
                            amount,
                            AttributeModifier.Operation.ADD_NUMBER
                    );
                }
            }
        }
    }

    private void addModifier(
            AttributeInstance attribute,
            String id,
            double amount,
            AttributeModifier.Operation operation
    ) {
        removeModifier(attribute, id);

        AttributeModifier modifier = new AttributeModifier(
                UUID.nameUUIDFromBytes(
                        (MODIFIER_PREFIX + id).getBytes()
                ),
                MODIFIER_PREFIX + id,
                amount,
                operation
        );

        attribute.addModifier(modifier);
    }

    private void removeModifier(
            Player player,
            Attribute attribute,
            String id
    ) {
        AttributeInstance instance = player.getAttribute(attribute);

        if (instance != null) {
            removeModifier(instance, id);
        }
    }

    private void removeModifier(
            AttributeInstance attribute,
            String id
    ) {
        UUID uuid = UUID.nameUUIDFromBytes(
                (MODIFIER_PREFIX + id).getBytes()
        );

        AttributeModifier modifier = attribute.getModifier(uuid);

        if (modifier != null) {
            attribute.removeModifier(modifier);
        }
    }
}