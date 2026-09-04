package com.tpu.perks.commands;

import com.tpu.perks.TpuPerks;
import com.tpu.perks.data.DataManager;
import com.tpu.perks.data.PerkManager;
import com.tpu.perks.data.PlayerPerkData;
import com.tpu.perks.gui.PerkGuiFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Comando raíz: /tpu
 *   /tpu perks                              -> abre la GUI (cualquier jugador)
 *   /tpu levels give <jugador> <cantidad>   -> OP: añade niveles de XP al jugador
 *   /tpu levels get <jugador>               -> OP: consulta niveles y puntos del jugador
 */
public class TpuCommand implements CommandExecutor, TabCompleter {

    private final TpuPerks plugin;
    private final PerkManager perkManager;
    private final DataManager dataManager;
    private final PerkGuiFactory guiFactory;

    public TpuCommand(TpuPerks plugin, PerkManager perkManager, DataManager dataManager) {
        this.plugin = plugin;
        this.perkManager = perkManager;
        this.dataManager = dataManager;
        this.guiFactory = new PerkGuiFactory(perkManager);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "perks" -> handlePerks(sender);
            case "levels" -> handleLevels(sender, args);
            default -> sendUsage(sender);
        }
        return true;
    }

    // ---------------------------------------------------------------------
    // /tpu perks
    // ---------------------------------------------------------------------

    private void handlePerks(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede abrir esta GUI.", NamedTextColor.RED));
            return;
        }
        player.openInventory(guiFactory.build(player));
    }

    // ---------------------------------------------------------------------
    // /tpu levels ...
    // ---------------------------------------------------------------------

    private void handleLevels(CommandSender sender, String[] args) {
        if (!sender.hasPermission("tpuperks.admin.levels")) {
            sender.sendMessage(Component.text("No tienes permiso para usar este comando.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /tpu levels <give|get> <jugador> [cantidad]", NamedTextColor.RED));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "give" -> handleLevelsGive(sender, args);
            case "get" -> handleLevelsGet(sender, args);
            default -> sender.sendMessage(Component.text("Uso: /tpu levels <give|get> <jugador> [cantidad]", NamedTextColor.RED));
        }
    }

    private void handleLevelsGive(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Uso: /tpu levels give <jugador> <cantidad>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(Component.text("El jugador debe estar online para añadir niveles.", NamedTextColor.RED));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("La cantidad debe ser un número entero.", NamedTextColor.RED));
            return;
        }

        if (amount == 0) {
            sender.sendMessage(Component.text("La cantidad no puede ser 0.", NamedTextColor.RED));
            return;
        }

        int newLevel = Math.max(0, target.getLevel() + amount);
        target.setLevel(newLevel);
        // setLevel no siempre dispara PlayerLevelChangeEvent de forma fiable en todas las versiones,
        // así que forzamos la revisión de puntos manualmente para no depender de eso.
        perkManager.checkAndGrantPoints(target);

        sender.sendMessage(Component.text("✔ ", NamedTextColor.GREEN)
                .append(Component.text("Le diste " + amount + " niveles a " + target.getName()
                        + ". Ahora tiene " + newLevel + " niveles.", NamedTextColor.GRAY)));
        target.sendMessage(Component.text("✦ ", NamedTextColor.GOLD)
                .append(Component.text("Un administrador te dio " + amount + " niveles de experiencia.", NamedTextColor.YELLOW)));
    }

    private void handleLevelsGet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /tpu levels get <jugador>", NamedTextColor.RED));
            return;
        }

        String targetName = args[2];
        Player onlineTarget = Bukkit.getPlayerExact(targetName);

        int level;
        UUID uuid;
        String displayName;

        if (onlineTarget != null) {
            level = onlineTarget.getLevel();
            uuid = onlineTarget.getUniqueId();
            displayName = onlineTarget.getName();
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
            if (offline == null || !offline.hasPlayedBefore()) {
                sender.sendMessage(Component.text("Ese jugador no existe o nunca se ha conectado.", NamedTextColor.RED));
                return;
            }
            level = -1; // No se puede leer el nivel de XP de un jugador offline sin NBT externo.
            uuid = offline.getUniqueId();
            displayName = offline.getName();
        }

        PlayerPerkData data = dataManager.getOrLoadOffline(uuid);

        sender.sendMessage(Component.text("── ", NamedTextColor.DARK_GRAY)
                .append(Component.text("Datos de " + displayName, NamedTextColor.AQUA))
                .append(Component.text(" ──", NamedTextColor.DARK_GRAY)));
        if (level >= 0) {
            sender.sendMessage(Component.text("Nivel de XP: ", NamedTextColor.GRAY)
                    .append(Component.text(level, NamedTextColor.GREEN)));
        } else {
            sender.sendMessage(Component.text("Nivel de XP: ", NamedTextColor.GRAY)
                    .append(Component.text("desconocido (jugador offline)", NamedTextColor.DARK_GRAY)));
        }
        sender.sendMessage(Component.text("Perk Points disponibles: ", NamedTextColor.GRAY)
                .append(Component.text(data.getAvailablePoints(), NamedTextColor.GOLD)));

        for (var type : com.tpu.perks.data.PerkType.values()) {
            int rank = data.getRank(type);
            if (rank > 0) {
                Component name = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacySection().deserialize(type.getDisplayName());
                sender.sendMessage(Component.text("  • ", NamedTextColor.GRAY)
                        .append(name)
                        .append(Component.text(": ", NamedTextColor.GRAY))
                        .append(Component.text("rango " + rank, NamedTextColor.YELLOW))
                        .append(Component.text(data.isActive(type) ? " (activo)" : " (inactivo)",
                                data.isActive(type) ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY)));
            }
        }
    }

    // ---------------------------------------------------------------------
    // Ayuda / uso
    // ---------------------------------------------------------------------

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("── ", NamedTextColor.DARK_GRAY)
                .append(Component.text("TpuPerks", NamedTextColor.AQUA))
                .append(Component.text(" ──", NamedTextColor.DARK_GRAY)));
        sender.sendMessage(Component.text("/tpu perks", NamedTextColor.YELLOW)
                .append(Component.text(" - Abre la GUI de perks", NamedTextColor.GRAY)));
        if (sender.hasPermission("tpuperks.admin.levels")) {
            sender.sendMessage(Component.text("/tpu levels give <jugador> <cantidad>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Añade niveles", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("/tpu levels get <jugador>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Consulta niveles y puntos", NamedTextColor.GRAY)));
        }
    }

    // ---------------------------------------------------------------------
    // Tab complete
    // ---------------------------------------------------------------------

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                  @NotNull String alias, String[] args) {
        List<String> result = new ArrayList<>();

        if (args.length == 1) {
            result.add("perks");
            if (sender.hasPermission("tpuperks.admin.levels")) {
                result.add("levels");
            }
            return filter(result, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("levels") && sender.hasPermission("tpuperks.admin.levels")) {
            result.add("give");
            result.add("get");
            return filter(result, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("levels") && sender.hasPermission("tpuperks.admin.levels")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("levels") && args[1].equalsIgnoreCase("give")
                && sender.hasPermission("tpuperks.admin.levels")) {
            return filter(List.of("1", "5", "10", "20"), args[3]);
        }

        return result;
    }

    private List<String> filter(List<String> options, String current) {
        String lower = current.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
