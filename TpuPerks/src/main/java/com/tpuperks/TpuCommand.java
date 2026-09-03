// src/main/java/com/tpuperks/TpuCommand.java

package com.tpuperks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class TpuCommand
        implements CommandExecutor, TabCompleter {

    private final TpuPerks plugin;
    private final DataManager data;
    private final PerkManager perkManager;

    public TpuCommand(
            TpuPerks plugin,
            DataManager data,
            PerkManager perkManager
    ) {
        this.plugin = plugin;
        this.data = data;
        this.perkManager = perkManager;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(
                        "§cEste comando debe ejecutarse desde el juego."
                );
                return true;
            }

            openGUI(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("perks")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(
                        "§cEste comando debe ejecutarse desde el juego."
                );
                return true;
            }

            openGUI(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("levels")) {

            if (!sender.isOp()) {
                sender.sendMessage(
                        "§cNecesitas ser OP para utilizar este comando."
                );
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(
                        "§cUso: /tpu levels give <jugador> <cantidad>"
                );
                sender.sendMessage(
                        "§cUso: /tpu levels get <jugador> <cantidad>"
                );
                return true;
            }

            String action = args[1];

            Player target = Bukkit.getPlayerExact(args[2]);

            if (target == null) {
                sender.sendMessage(
                        "§cEl jugador no está conectado."
                );
                return true;
            }

            if (action.equalsIgnoreCase("give")) {
                if (args.length < 4) {
                    sender.sendMessage(
                            "§cUso: /tpu levels give <jugador> <cantidad>"
                    );
                    return true;
                }

                Integer amount = parseAmount(args[3]);

                if (amount == null || amount <= 0) {
                    sender.sendMessage(
                            "§cLa cantidad debe ser un número positivo."
                    );
                    return true;
                }

                data.addPoints(
                        target.getUniqueId(),
                        amount
                );

                data.save();

                sender.sendMessage(
                        "§aHas dado §e" + amount +
                                " §aPerk Point(s) a §e" +
                                target.getName() + "§a."
                );

                target.sendMessage(
                        "§aHas recibido §e" + amount +
                                " §aPerk Point(s)."
                );

                return true;
            }

            if (action.equalsIgnoreCase("get")) {
                sender.sendMessage(
                        "§e" + target.getName() +
                                " §7tiene §f" +
                                data.getPoints(target.getUniqueId()) +
                                " §7Perk Point(s)."
                );

                sender.sendMessage(
                        "§7Nivel actual: §f" +
                                target.getLevel()
                );

                sender.sendMessage(
                        "§7Perks activos: §f" +
                                data.getActivePerks(
                                        target.getUniqueId()
                                ).size()
                );

                return true;
            }

            sender.sendMessage(
                    "§cUso: /tpu levels give <jugador> <cantidad>"
            );

            sender.sendMessage(
                    "§cUso: /tpu levels get <jugador> <cantidad>"
            );

            return true;
        }

        sender.sendMessage(
                "§e/tpu perks §7- Abrir menú de perks"
        );

        sender.sendMessage(
                "§e/tpu levels give <jugador> <cantidad>"
        );

        sender.sendMessage(
                "§e/tpu levels get <jugador> <cantidad>"
        );

        return true;
    }

    private void openGUI(Player player) {
        new PerkGUI(
                plugin,
                data,
                perkManager
        ).open(player);
    }

    private Integer parseAmount(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        List<String> result = new ArrayList<>();

        if (args.length == 1) {
            result.add("perks");
            result.add("levels");
            return filter(result, args[0]);
        }

        if (args.length == 2 &&
                args[0].equalsIgnoreCase("levels")) {

            result.add("give");
            result.add("get");

            return filter(result, args[1]);
        }

        if (args.length == 3 &&
                args[0].equalsIgnoreCase("levels")) {

            for (Player player :
                    Bukkit.getOnlinePlayers()) {

                result.add(player.getName());
            }

            return filter(result, args[2]);
        }

        return result;
    }

    private List<String> filter(
            List<String> values,
            String input
    ) {
        return values.stream()
                .filter(value ->
                        value.toLowerCase()
                                .startsWith(input.toLowerCase()))
                .toList();
    }
}