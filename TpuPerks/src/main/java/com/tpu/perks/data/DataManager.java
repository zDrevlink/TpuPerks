package com.tpu.perks.data;

import com.tpu.perks.TpuPerks;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona la persistencia de los datos de perks de cada jugador.
 *
 * Diseño pensado para rendimiento:
 *  - Todo se mantiene en un ConcurrentHashMap en memoria mientras el jugador está online
 *    (y también en cache tras desconectarse, hasta el siguiente guardado global).
 *  - La carga individual (login) y el guardado global (disable / autosave) usan I/O de disco,
 *    que se ejecuta de forma asíncrona salvo en el apagado del servidor (onDisable),
 *    donde Bukkit no garantiza async seguro y se guarda de forma síncrona rápida.
 */
public class DataManager {

    private final TpuPerks plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerPerkData> cache = new ConcurrentHashMap<>();

    public DataManager(TpuPerks plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /**
     * Devuelve los datos de un jugador desde cache, cargándolos de disco de forma
     * síncrona si es la primera vez que se piden en esta sesión. Pensado para
     * llamarse en el evento de login/join.
     */
    public PlayerPerkData getOrLoad(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadFromDisk);
    }

    /** Devuelve los datos ya cacheados sin tocar disco (usar cuando se sabe que ya están cargados). */
    public PlayerPerkData get(UUID uuid) {
        return cache.get(uuid);
    }

    public void unload(UUID uuid) {
        PlayerPerkData data = cache.remove(uuid);
        if (data != null) {
            saveToDisk(data);
        }
    }

    private PlayerPerkData loadFromDisk(UUID uuid) {
        PlayerPerkData data = new PlayerPerkData(uuid);
        File file = new File(dataFolder, uuid.toString() + ".yml");
        if (!file.exists()) {
            return data;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        data.setAvailablePoints(yaml.getInt("points", 0));
        data.setLastGrantedPointBatches(yaml.getInt("last-granted-batches", 0));

        for (PerkType type : PerkType.values()) {
            String path = "perks." + type.getId();
            data.setRank(type, yaml.getInt(path + ".rank", 0));
            data.setActive(type, yaml.getBoolean(path + ".active", false));
        }

        return data;
    }

    /** Guarda un jugador en disco. Se ejecuta de forma asíncrona. */
    public void saveAsync(PlayerPerkData data) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveToDisk(data));
    }

    private void saveToDisk(PlayerPerkData data) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("points", data.getAvailablePoints());
        yaml.set("last-granted-batches", data.getLastGrantedPointBatches());

        for (PerkType type : PerkType.values()) {
            String path = "perks." + type.getId();
            yaml.set(path + ".rank", data.getRank(type));
            yaml.set(path + ".active", data.isActive(type));
        }

        File file = new File(dataFolder, data.getUuid().toString() + ".yml");
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar los datos de " + data.getUuid() + ": " + e.getMessage());
        }
    }

    /** Guarda todos los jugadores en cache. Usado en onDisable (síncrono, es rápido y puntual). */
    public void saveAll() {
        for (PlayerPerkData data : cache.values()) {
            saveToDisk(data);
        }
    }

    /**
     * Busca un jugador por UUID conocido; si no está cacheado, lo carga de disco
     * temporalmente (usado por comandos OP sobre jugadores offline).
     */
    public PlayerPerkData getOrLoadOffline(UUID uuid) {
        PlayerPerkData cached = cache.get(uuid);
        if (cached != null) {
            return cached;
        }
        return loadFromDisk(uuid);
    }

    /** Guarda datos de un jugador que puede estar offline (comandos OP). */
    public void saveOffline(PlayerPerkData data) {
        if (cache.containsKey(data.getUuid())) {
            saveAsync(data);
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveToDisk(data));
        }
    }
}
