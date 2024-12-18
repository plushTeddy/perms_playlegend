package dev.plushteddy.perms.events;

import dev.plushteddy.perms.Main;
import dev.plushteddy.perms.Utilities.Time;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ConnectEvents implements Listener {




    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();


        CompletableFuture.runAsync(() -> {
            try {
                removeExpiredPermissions(playerUUID);
                checkAndAddMainGroup(player);
                addGroupPermissionsToPlayer(player);
                setPlayerPrefixAndName(player);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        Bukkit.getScheduler().runTask(Main.getMain(), () -> {
            String playerPrefix;
            try {
                playerPrefix = getLowestWeightPrefix(player.getUniqueId().toString());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            String playerNameWithPrefix = playerPrefix + " " + player.getName();
            player.sendMessage(Main.getMain().getMessage("welcome", "player", playerNameWithPrefix));
        });

    }

    public void setPlayerPrefixAndName(Player player) throws SQLException {
        String uuid = player.getUniqueId().toString();

        List<String> groupPrefixes = getSortedGroupPrefixes(uuid);

        if (groupPrefixes.isEmpty()) {
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
            return;
        }

        String prefix = groupPrefixes.getFirst();

        String playerName = prefix + " " + player.getName();
        player.setDisplayName(playerName);

        player.setPlayerListName(playerName);
    }

    private List<String> getSortedGroupPrefixes(String uuid) throws SQLException {
        List<String> prefixes = new ArrayList<>();

        String query = "SELECT g.prefix FROM player_groups pg " +
                "JOIN groups g ON pg.group_name = g.name " +
                "WHERE pg.uuid = ? " +
                "ORDER BY g.weight ASC;";

        ResultSet resultSet = Main.getMain().getMySQL().query(query, uuid);

        while (resultSet != null && resultSet.next()) {
            String prefix = resultSet.getString("prefix");
            if (prefix != null && !prefix.isEmpty()) {
                prefixes.add(prefix);
            }
        }

        return prefixes;
    }





    private String getLowestWeightPrefix(String uuid) throws SQLException {
        String query = "SELECT g.prefix FROM player_groups pg " +
                "JOIN groups g ON pg.group_name = g.name " +
                "WHERE pg.uuid = ? " +
                "ORDER BY g.weight ASC LIMIT 1;";
        ResultSet resultSet = Main.getMain().getMySQL().query(query, uuid);

        if (resultSet != null && resultSet.next()) {
            return resultSet.getString("prefix");
        }
        return "";
    }

    private void removeExpiredPermissions(UUID playerUUID) throws SQLException {
        ResultSet resultSet = Main.getMain().getMySQL().query(
                "SELECT permission, expiry_time FROM permissions WHERE uuid = ?",
                playerUUID.toString()
        );

        while (resultSet.next()) {
            long expiryTime = resultSet.getLong("expiry_time");

            if (expiryTime > 0 && expiryTime <= System.currentTimeMillis()) {
                String permission = resultSet.getString("permission");

                Main.getMain().getMySQL().update(
                        "DELETE FROM permissions WHERE uuid = ? AND permission = ?",
                        playerUUID.toString(),
                        permission
                );
            }
        }

        ResultSet playerGroups = Main.getMain().getMySQL().query(
                "SELECT group_name, expiry_time FROM player_groups WHERE uuid = ?",
                playerUUID.toString()
        );
        while (playerGroups.next()) {
            long expiryTime = playerGroups.getLong("expiry_time");

            if (expiryTime > 0 && expiryTime <= System.currentTimeMillis()) {
                String groupName = playerGroups.getString("group_name");

                Main.getMain().getMySQL().update(
                        "DELETE FROM player_groups WHERE uuid = ? AND group_name = ?",
                        playerUUID.toString(),
                        groupName
                );
            }
        }
    }

    private void checkAndAddMainGroup(Player player) throws SQLException {
        ResultSet resultSet = Main.getMain().getMySQL().query(
                "SELECT * FROM player_groups WHERE uuid = ? AND group_name = 'default'",
                player.getUniqueId().toString()
        );

        if (!resultSet.next()) {
            Main.getMain().getMySQL().update(
                    "INSERT INTO player_groups (uuid, group_name, expiry_time) VALUES (?, 'default', ?)",
                    player.getUniqueId().toString(),
                    new Time().getFutureYearExpiry()
            );
        }
    }

    private void addGroupPermissionsToPlayer(Player player) throws SQLException {
        ResultSet groups = Main.getMain().getMySQL().query(
                "SELECT group_name FROM player_groups WHERE uuid = ?",
                player.getUniqueId().toString()
        );

        while (groups.next()) {
            String groupName = groups.getString("group_name");

            ResultSet permissions = Main.getMain().getMySQL().query(
                    "SELECT permission FROM group_permissions WHERE group_name = ?",
                    groupName
            );

            while (permissions.next()) {
                String permission = permissions.getString("permission");

                if (!player.hasPermission(permission)) {
                    player.addAttachment(Main.getMain(), permission, true);
                }
            }
        }
    }
}