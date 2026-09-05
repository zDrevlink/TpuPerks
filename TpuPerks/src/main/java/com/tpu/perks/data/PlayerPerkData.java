package com.tpu.perks.data;

import java.util.ArrayDeque;
import java.util.Deque;
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

    /**
     * Historial cronológico de qué perk se compró/subió de rango con cada punto gastado,
     * en orden (el último elemento es la compra más reciente). Se usa para "deshacer" una
     * compra cuando el jugador gasta XP y su nivel cae por debajo del lote que le había
     * dado ese punto (ver PerkManager#checkAndRevokePoints). Un tope de tamaño evita que
     * esta pila crezca indefinidamente en sesiones muy largas.
     */
    private final Deque<PerkType> purchaseHistory = new ArrayDeque<>();
    private static final int MAX_HISTORY_SIZE = 500;

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

    /** Registra que se gastó un punto en subir el rango de este perk (para poder deshacerlo luego). */
    public void recordPurchase(PerkType type) {
        purchaseHistory.addLast(type);
        while (purchaseHistory.size() > MAX_HISTORY_SIZE) {
            purchaseHistory.removeFirst();
        }
    }

    /** Saca y devuelve el último perk comprado (o null si no hay historial), para deshacerlo. */
    public PerkType popLastPurchase() {
        return purchaseHistory.pollLast();
    }

    /** Borra todo el historial de compras (usado en el reset total de muerte). */
    public void clearPurchaseHistory() {
        purchaseHistory.clear();
    }
}

