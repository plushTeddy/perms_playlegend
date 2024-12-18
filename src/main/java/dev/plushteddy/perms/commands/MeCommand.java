package dev.plushteddy.perms.commands;

import dev.plushteddy.perms.Main;
import dev.plushteddy.perms.Utilities.MySQL;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.util.concurrent.CompletableFuture;

public class MeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            String playerName = args[0];
            String permission_check = "perms.me_player";
            if (!sender.hasPermission(permission_check)) {
                sender.sendMessage(Main.getMain().getMessage("noPermission", "permission", permission_check));
                return false;
            }

            CompletableFuture.runAsync(() -> {
                try {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                    if (!offlinePlayer.hasPlayedBefore()) {
                        sender.sendMessage(Main.getMain().getMessage("me.playerNotFound"));
                        return;
                    }

                    String uuid = offlinePlayer.getUniqueId().toString();
                    showPermissions(sender, uuid);
                } catch (Exception e) {
                    sender.sendMessage(Main.getMain().getMessage("error"));
                    e.printStackTrace();
                }
            });
        } else if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Main.getMain().getMessage("me.shouldBePlayer"));
                return false;
            }
            showPermissions(sender, player.getUniqueId().toString());
        } else {
            sender.sendMessage(Main.getMain().getMessage("me.usage"));
            return false;
        }

        return true;
    }

    private void showPermissions(CommandSender sender, String uuid) {
        MySQL mySQL = Main.getMain().getMySQL();

        try {
            String permissionsQuery = "SELECT * FROM permissions WHERE uuid = ?";
            ResultSet rsPermissions = mySQL.query(permissionsQuery, uuid);

            boolean hasPermissions = false;
            while (rsPermissions != null && rsPermissions.next()) {
                if (!hasPermissions) {
                    sender.sendMessage(Main.getMain().getMessage("me.permissionsListTitle-"));
                    hasPermissions = true;
                }
                String permission = rsPermissions.getString("permission");
                long expiryTime = rsPermissions.getLong("expiry_time");
                String expiryMessage = (expiryTime > 30000000000000L) ? Main.getMain().getMessage("me.permanent-") : formatExpiryTime(expiryTime);
                sender.sendMessage(Main.getMain().getMessage("me.list-", "thing", permission, "expiry", expiryMessage));
            }

            String groupsQuery = "SELECT * FROM player_groups WHERE uuid = ?";
            ResultSet rsGroups = mySQL.query(groupsQuery, uuid);

            boolean hasGroups = false;
            while (rsGroups != null && rsGroups.next()) {
                if (!hasGroups) {
                    sender.sendMessage(Main.getMain().getMessage("me.groupsListTitle-"));
                    hasGroups = true;
                }
                String groupName = rsGroups.getString("group_name");
                long groupExpiryTime = rsGroups.getLong("expiry_time");
                String groupExpiryMessage = (groupExpiryTime > 30000000000000L) ? Main.getMain().getMessage("me.permanent-") : formatExpiryTime(groupExpiryTime);
                sender.sendMessage(Main.getMain().getMessage("me.list-", "thing", groupName, "expiry", groupExpiryMessage));
            }

            if (!hasPermissions && !hasGroups) {
                sender.sendMessage(Main.getMain().getMessage("me.noPermsOrGroups"));
            }

        } catch (Exception e) {
            sender.sendMessage(Main.getMain().getMessage("error"));
            e.printStackTrace();
        }
    }


    private String formatExpiryTime(long expiryTime) {
        if (expiryTime == Long.MAX_VALUE) {
            return Main.getMain().getMessage("me.permanent-");
        }
        long remainingTime = expiryTime - System.currentTimeMillis();
        if (remainingTime <= 0) {
            return Main.getMain().getMessage("me.expired-");
        }

        long days = remainingTime / (1000 * 60 * 60 * 24);
        long hours = (remainingTime % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (remainingTime % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (remainingTime % (1000 * 60)) / 1000;

        return Main.getMain().getMessage("me.timeLeft-", "days", days, "hours", hours, "minutes", minutes, "seconds", seconds);
    }


}

