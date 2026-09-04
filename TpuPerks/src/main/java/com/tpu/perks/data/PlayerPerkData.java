package com.tpu.perks.data;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Datos de un jugador en memoria: puntos sin gastar, rango comprado de cada perk,
 * y si el perk está actualmente "activo" (equipado) o no.
 *
 * Un perk puede tener rango > 0 (comprado) pero no estar activo (fue desactivado
 * al morir o el jugador lo desactivó manualmente). Solo los perks activos se
 * traducen en atributos/efectos reales aplicados al jugador.
 */
public class PlayerPerkData {

    private final UUID uuid;
    private int availablePoints;
    private final Map<PerkType, Integer> ranks = new EnumMap<>(PerkType.class);
    private final Map<PerkType, Boolean> active = new EnumMap<>(PerkType.class);

    /** Último total de "perk points" ya otorgados por nivel, para no volver a otorgar los mismos. */
    private int lastGrantedPointBatches;

    public PlayerPerkData(UUID uuid) {
        this.uuid = uuid;
        for (PerkType type : PerkType.values()) {
            ranks.put(type, 0);
            active.put(type, false);
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getAvailablePoints() {
        return availablePoints;
    }

    public void setAvailablePoints(int availablePoints) {
        this.availablePoints = Math.max(0, availablePoints);
    }

    public void addPoints(int amount) {
        this.availablePoints = Math.max(0, this.availablePoints + amount);
    }

    public int getRank(PerkType type) {
        return ranks.getOrDefault(type, 0);
    }

    public void setRank(PerkType type, int rank) {
        ranks.put(type, Math.max(0, rank));
    }

    public void incrementRank(PerkType type) {
        setRank(type, getRank(type) + 1);
    }

    public boolean isActive(PerkType type) {
        return active.getOrDefault(type, false) && getRank(type) > 0;
    }

    public void setActive(PerkType type, boolean value) {
        active.put(type, value);
    }

    public void deactivateAll() {
        for (PerkType type : PerkType.values()) {
            active.put(type, false);
        }
    }

    public int getLastGrantedPointBatches() {
        return lastGrantedPointBatches;
    }

    public void setLastGrantedPointBatches(int value) {
        this.lastGrantedPointBatches = value;
    }

    public Map<PerkType, Integer> getRanksView() {
        return ranks;
    }

    public Map<PerkType, Boolean> getActiveView() {
        return active;
    }
}
