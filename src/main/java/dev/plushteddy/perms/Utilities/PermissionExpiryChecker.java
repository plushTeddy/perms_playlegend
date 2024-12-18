package dev.plushteddy.perms.Utilities;

import dev.plushteddy.perms.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PermissionExpiryChecker {

    public static void startExpiryChecker() {
        Bukkit.getScheduler().runTaskTimer(Main.getMain(), () -> {
            checkExpiredPermissions();
            checkExpiredGroups();
        }, 20L, 20L);
    }

    private static final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    private static void checkExpiredPermissions() {
        CompletableFuture.runAsync(() -> {
            MySQL mySQL = Main.getMain().getMySQL();
            try {
                String query = "SELECT * FROM permissions WHERE expiry_time < ?";
                ResultSet rs = mySQL.query(query, System.currentTimeMillis());
                while (rs != null && rs.next()) {
                    String uuid = rs.getString("uuid");
                    String permission = rs.getString("permission");

                    Player player = Bukkit.getPlayer(UUID.fromString(uuid));
                    if (player != null) {
                        player.getEffectivePermissions().forEach(permissionAttachmentInfo -> {
                            if (permissionAttachmentInfo.getAttachment() != null) {
                                permissionAttachmentInfo.getAttachment().unsetPermission(permission);
                            }
                        });
                        player.recalculatePermissions();
                    }

                    String deleteQuery = "DELETE FROM permissions WHERE uuid = ? AND permission = ?";
                    mySQL.update(deleteQuery, uuid, permission);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }



    private static void checkExpiredGroups() {
        CompletableFuture.runAsync(() -> {
            MySQL mySQL = Main.getMain().getMySQL();
            try {
                String query = "SELECT * FROM player_groups WHERE expiry_time < ?";
                ResultSet rs = mySQL.query(query, System.currentTimeMillis());
                while (rs != null && rs.next()) {
                    String uuid = rs.getString("uuid");
                    String groupName = rs.getString("group_name");

                    String groupPermQuery = "SELECT permission FROM group_permissions WHERE group_name = ?";
                    ResultSet groupPerms = mySQL.query(groupPermQuery, groupName);

                    Player player = Bukkit.getPlayer(UUID.fromString(uuid));
                    if (player != null && groupPerms != null) {
                        PermissionAttachment attachment = attachments.get(player.getUniqueId());

                        while (groupPerms.next()) {
                            String permission = groupPerms.getString("permission");
                            if (attachment != null) {
                                attachment.unsetPermission(permission);
                            }
                        }
                        player.recalculatePermissions();
                    }

                    String deleteGroupQuery = "DELETE FROM player_groups WHERE uuid = ? AND group_name = ?";
                    mySQL.update(deleteGroupQuery, uuid, groupName);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }



}