package dev.plushteddy.perms.commands;

import dev.plushteddy.perms.Main;
import dev.plushteddy.perms.Utilities.MySQL;
import dev.plushteddy.perms.Utilities.Time;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PermissionsCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, String label, String[] args) {
        if (!label.equalsIgnoreCase("permissions") || args.length < 2) {
            sender.sendMessage(Main.getMain().getMessage("permissions.usage"));
            return false;
        }

        String action = args[0];
        switch (action.toLowerCase()) {
            case "add":
                handleAddPermission(sender, args);
                break;
            case "remove":
                handleRemovePermission(sender, args);
                break;
            default:
                sender.sendMessage(Main.getMain().getMessage("permissions.unknownAction"));
        }
        return true;
    }

    private void handleAddPermission(CommandSender sender, String[] args) {
        String permission_check = "perms.perms_add";
        if (!sender.hasPermission(permission_check)) {
            sender.sendMessage(Main.getMain().getMessage("noPermission", "permission", permission_check));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(Main.getMain().getMessage("permissions.add.usage"));
            return;
        }

        String playerName = args[1];
        String permission = args[2];
        String time = args.length == 4 ? args[3] : "*";

        long expiry;
        if (time.equals("*") || time.isEmpty()) {
            expiry = new Time().getFutureYearExpiry();
        } else {

            if (time.matches("\\d+[dhms]")) {
                long duration = new Time().parseDuration(time);
                if (duration == -1) {
                    sender.sendMessage(Main.getMain().getMessage("permissions.add.invalidTime"));
                    return;
                }
                expiry = System.currentTimeMillis() + duration;
            } else {
                sender.sendMessage(Main.getMain().getMessage("permissions.add.invalidTime"));
                return;
            }
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage(Main.getMain().getMessage("permissions.playerNotFound"));
            return;
        }

        CompletableFuture.runAsync(() -> {
            MySQL mySQL = Main.getMain().getMySQL();
            try {
                String permissionQuery = "SELECT * FROM permissions WHERE uuid = ? AND permission = ?";
                ResultSet rsPermission = mySQL.query(permissionQuery, offlinePlayer.getUniqueId().toString(), permission);
                if (rsPermission != null && rsPermission.next()) {
                    sender.sendMessage(Main.getMain().getMessage("permissions.add.alreadyHasPermission"));
                    return;
                }

                mySQL.update("INSERT INTO permissions (uuid, permission, expiry_time) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE expiry_time = ?",
                        offlinePlayer.getUniqueId().toString(), permission, expiry, expiry);

                if (offlinePlayer.isOnline()) {
                    Player player = offlinePlayer.getPlayer();
                    assert player != null;
                    player.addAttachment(Main.getMain(), permission, true);
                    player.recalculatePermissions();
                }

                sender.sendMessage(Main.getMain().getMessage("permissions.add.success"));

            } catch (Exception e) {
                sender.sendMessage(Main.getMain().getMessage("error"));
                e.printStackTrace();
            }
        });
    }


    private void handleRemovePermission(CommandSender sender, String[] args) {
        String permission_check = "perms.perms_remove";
        if (!sender.hasPermission(permission_check)) {
            sender.sendMessage(Main.getMain().getMessage("noPermission", "permission", permission_check));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(Main.getMain().getMessage("permissions.remove.usage"));
            return;
        }
        String playerName = args[1];
        String permission = args[2];

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage(Main.getMain().getMessage("permissions.playerNotFound"));
            return;
        }

        CompletableFuture.runAsync(() -> {
            MySQL mySQL = Main.getMain().getMySQL();
            try {
                String permissionQuery = "SELECT * FROM permissions WHERE uuid = ? AND permission = ?";
                ResultSet rsPermission = mySQL.query(permissionQuery, offlinePlayer.getUniqueId().toString(), permission);
                if (rsPermission == null || !rsPermission.next()) {
                    sender.sendMessage(Main.getMain().getMessage("permissions.remove.notFound"));
                    return;
                }

                mySQL.update("DELETE FROM permissions WHERE uuid = ? AND permission = ?",
                        offlinePlayer.getUniqueId().toString(), permission);

                if (offlinePlayer.isOnline()) {
                    Player player = offlinePlayer.getPlayer();
                    assert player != null;
                    player.addAttachment(Main.getMain(), permission, false);
                    player.recalculatePermissions();
                    sender.sendMessage(Main.getMain().getMessage("permissions.remove.success"));
                } else {
                    sender.sendMessage(Main.getMain().getMessage("permissions.remove.success"));
                }

            } catch (Exception e) {
                sender.sendMessage(Main.getMain().getMessage("error"));
                e.printStackTrace();
            }
        });
    }



    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("add");
            suggestions.add("remove");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    suggestions.add(player.getName());
                }
            }
        }
        return suggestions;
    }
}