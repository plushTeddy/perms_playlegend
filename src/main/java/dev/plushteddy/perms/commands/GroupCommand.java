package dev.plushteddy.perms.commands;

import dev.plushteddy.perms.Main;
import dev.plushteddy.perms.Utilities.MySQL;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import dev.plushteddy.perms.Utilities.Time;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GroupCommand implements CommandExecutor, TabCompleter {


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, String label, String[] args) {
        if (!label.equalsIgnoreCase("group") || args.length < 2) {
            sender.sendMessage(Main.getMain().getMessage("group.usage"));
            return false;
        }

        String action = args[0];
        switch (action.toLowerCase()) {
            case "add":
                handleAddGroup(sender, args);
                break;
            case "remove":
                handleRemoveGroup(sender, args);
                break;
            case "create":
                handleCreateGroup(sender, args);
                break;
            case "delete":
                handleDeleteGroup(sender, args);
                break;
            case "edit":
                handleEditGroup(sender, args);
                break;
            default:
                sender.sendMessage(Main.getMain().getMessage("group.unknownAction"));
        }
        return true;
    }

    private void handleEditGroup(CommandSender sender, String[] args) {


        String permission_check = "perms.group_edit";
        if (!sender.hasPermission(permission_check)) {
            sender.sendMessage(Main.getMain().getMessage("noPermission", "permission", permission_check));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(Main.getMain().getMessage("group.edit.usage"));
            return;
        }

        String action = args[1];
        String groupName = args[2];

        CompletableFuture.runAsync(() -> {
            MySQL mySQL = Main.getMain().getMySQL();

            try {
                String groupQuery = "SELECT * FROM groups WHERE name = ?";
                ResultSet rsGroup = mySQL.query(groupQuery, groupName);
                if (rsGroup == null || !rsGroup.next()) {
                    sender.sendMessage(Main.getMain().getMessage("group.notFound"));
                    return;
                }

                boolean isDefaultGroup = groupName.equalsIgnoreCase("default");

                switch (action.toLowerCase()) {
                    case "name":
                        if (isDefaultGroup) {
                            sender.sendMessage(Main.getMain().getMessage("group.edit.name.default"));
                            return;
                        }
                        if (args.length < 4) {
                            sender.sendMessage(Main.getMain().getMessage("group.edit.name.usage"));
                            return;
                        }
                        String newGroupName = args[3];
                        String newNameQuery = "SELECT * FROM groups WHERE name = ?";
                        ResultSet rsNewName = mySQL.query(newNameQuery, newGroupName);
                        if (rsNewName != null && rsNewName.next()) {
                            sender.sendMessage(Main.getMain().getMessage("group.edit.name.exists"));
                            return;
                        }
                        mySQL.update("UPDATE group_permissions SET group_name = ? WHERE group_name = ?", newGroupName, groupName);
                        mySQL.update("UPDATE player_groups SET group_name = ? WHERE group_name = ?", newGroupName, groupName);
                        mySQL.update("UPDATE groups SET name = ? WHERE name = ?", newGroupName, groupName);

                        sender.sendMessage(Main.getMain().getMessage("group.edit.name.success"));
                        break;

                    case "prefix":
                        if (args.length < 4) {
                            sender.sendMessage(Main.getMain().getMessage("group.edit.prefix.usage"));
                            return;
                        }
                        String newPrefix = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                        mySQL.update("UPDATE groups SET prefix = ? WHERE name = ?", newPrefix, groupName);
                        sender.sendMessage(Main.getMain().getMessage("group.edit.prefix.success"));
                        break;

                    case "weight":
                        if (isDefaultGroup) {
                            sender.sendMessage(Main.getMain().getMessage("group.edit.weight.default"));
                            return;
                        }
                        if (args.length < 4) {
                            sender.sendMessage(Main.getMain().getMessage("group.edit.weight.usage"));
                            return;
                        }
                        int newWeight;
                        try {
                            newWeight = Integer.parseInt(args[3]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(Main.getMain().getMessage("group.edit.weight.invalid"));
                            return;
                        }
                        String weightQuery = "SELECT * FROM groups WHERE weight = ?";
                        ResultSet rsWeight = mySQL.query(weightQuery, newWeight);
                        if (rsWeight != null && rsWeight.next()) {
                            sender.sendMessage(Main.getMain().getMessage("group.edit.weight.exists"));
                            return;
                        }
                        mySQL.update("UPDATE groups SET weight = ? WHERE name = ?", newWeight, groupName);
                        sender.sendMessage(Main.getMain().getMessage("group.edit.weight.success"));
                        break;

                    case "addperm":
                        if (args.length < 4) {
                            sender.sendMessage(Main.getMain().getMessage("group.edit.addperm.usage"));
                            return;
                        }
                        String permissionToAdd = args[3];
                        String checkPermAddQuery = "SELECT * FROM group_permissions WHERE group_name = ? AND permission = ?";
                        ResultSet rsAddPerm = mySQL.query(checkPermAddQuery, groupName, permissionToAdd);
                        if (rsAddPerm != null && rsAddPerm.next()) {
                            sender.sendMessage(Main.getMain().getMessage("group.edit.addperm.exists"));
                            return;
                        }
                        mySQL.update("INSERT INTO group_permissions (group_name, permission) VALUES (?, ?)", groupName, permissionToAdd);
                        sender.sendMessage(Main.getMain().getMessage("group.edit.addperm.success"));
                        break;

                    case "removeperm":
                        if (args.length < 4) {
                            sender.sendMessage(Main.getMain().getMessage("group.edit.removeperm.usage"));
                            return;
                        }
                        String permissionToRemove = args[3];
                        String checkPermRemoveQuery = "SELECT * FROM group_permissions WHERE group_name = ? AND permission = ?";
                        ResultSet rsRemovePerm = mySQL.query(checkPermRemoveQuery, groupName, permissionToRemove);
                        if (rsRemovePerm == null || !rsRemovePerm.next()) {
                            sender.sendMessage(Main.getMain().getMessage("group.edit.removeperm.notExists"));
                            return;
                        }
                        mySQL.update("DELETE FROM group_permissions WHERE group_name = ? AND permission = ?", groupName, permissionToRemove);
                        sender.sendMessage(Main.getMain().getMessage("group.edit.removeperm.success"));
                        break;


                    default:
                        sender.sendMessage(Main.getMain().getMessage("group.edit.invalidAction"));
                        break;
                }
            } catch (SQLException e) {
                sender.sendMessage(Main.getMain().getMessage("error"));
                e.printStackTrace();
            }
        });
    }

    private void handleAddGroup(CommandSender sender, String[] args) {
        String permission_check = "perms.group_add";
        if (!sender.hasPermission(permission_check)) {
            sender.sendMessage(Main.getMain().getMessage("noPermission", "permission", permission_check));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(Main.getMain().getMessage("group.add.usage"));
            return;
        }

        String playerName = args[1];
        String groupName = args[2];
        String time = args.length == 4 ? args[3] : "*";

        long expiry;
        if (time.isEmpty() || time.equals("*")) {
            expiry = new Time().getFutureYearExpiry();
        } else {


            if (time.matches("\\d+[dhms]")) {
                long duration = new Time().parseDuration(time);
                if (duration == -1) {
                    sender.sendMessage(Main.getMain().getMessage("group.add.invalidTime"));
                    return;
                }
                expiry = System.currentTimeMillis() + duration;
            } else {
                sender.sendMessage(Main.getMain().getMessage("group.add.invalidTime"));
                return;
            }
        }


        CompletableFuture.runAsync(() -> {
            MySQL mySQL = Main.getMain().getMySQL();
            try {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                if (!offlinePlayer.hasPlayedBefore()) {
                    sender.sendMessage(Main.getMain().getMessage("group.add.playerNotFound"));
                    return;
                }

                String groupQuery = "SELECT * FROM groups WHERE name = ?";
                ResultSet rsGroup = mySQL.query(groupQuery, groupName);
                if (rsGroup == null || !rsGroup.next()) {
                    sender.sendMessage(Main.getMain().getMessage("group.add.groupNotFound"));
                    return;
                }

                String playerGroupQuery = "SELECT * FROM player_groups WHERE uuid = ? AND group_name = ?";
                ResultSet rsPlayerGroup = mySQL.query(playerGroupQuery, offlinePlayer.getUniqueId().toString(), groupName);
                if (rsPlayerGroup != null && rsPlayerGroup.next()) {
                    sender.sendMessage(Main.getMain().getMessage("group.add.alreadyInGroup"));
                    return;
                }

                mySQL.update("INSERT INTO player_groups (uuid, group_name, expiry_time) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE expiry_time = ?",
                        offlinePlayer.getUniqueId().toString(), groupName, expiry, expiry);

                if (offlinePlayer.isOnline()) {
                    Player onlinePlayer = offlinePlayer.getPlayer();
                    String permQuery = "SELECT permission FROM group_permissions WHERE group_name = ?";
                    ResultSet rsPerms = mySQL.query(permQuery, groupName);
                    while (rsPerms != null && rsPerms.next()) {
                        String permission = rsPerms.getString("permission");
                        assert onlinePlayer != null;
                        onlinePlayer.addAttachment(Main.getMain(), permission, true);
                    }

                    assert onlinePlayer != null;
                    setPlayerPrefixAndName(onlinePlayer);
                }

                sender.sendMessage(Main.getMain().getMessage("group.add.success"));

            } catch (Exception e) {
                sender.sendMessage(Main.getMain().getMessage("error"));
                e.printStackTrace();
            }
        });
    }



    private void handleRemoveGroup(CommandSender sender, String[] args) {
        String permission_check = "perms.group_remove";
        if (!sender.hasPermission(permission_check)) {
            sender.sendMessage(Main.getMain().getMessage("noPermission", "permission", permission_check));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(Main.getMain().getMessage("group.remove.usage"));
            return;
        }

        String playerName = args[1];
        String groupName = args[2];


        if ("default".equalsIgnoreCase(groupName)) {
            sender.sendMessage(Main.getMain().getMessage("group.remove.default"));
            return;
        }

        CompletableFuture.runAsync(() -> {
            MySQL mySQL = Main.getMain().getMySQL();
            try {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                if (!offlinePlayer.hasPlayedBefore()) {
                    sender.sendMessage(Main.getMain().getMessage("group.remove.playerNotFound"));
                    return;
                }

                String groupQuery = "SELECT * FROM groups WHERE name = ?";
                ResultSet rsGroup = mySQL.query(groupQuery, groupName);
                if (rsGroup == null || !rsGroup.next()) {
                    sender.sendMessage(Main.getMain().getMessage("group.remove.groupNotFound"));
                    return;
                }

                String playerGroupQuery = "SELECT * FROM player_groups WHERE uuid = ? AND group_name = ?";
                ResultSet rsPlayerGroup = mySQL.query(playerGroupQuery, offlinePlayer.getUniqueId().toString(), groupName);
                if (rsPlayerGroup == null || !rsPlayerGroup.next()) {
                    sender.sendMessage(Main.getMain().getMessage("group.remove.notInGroup"));
                    return;
                }

                mySQL.update("DELETE FROM player_groups WHERE uuid = ? AND group_name = ?",
                        offlinePlayer.getUniqueId().toString(), groupName);

                if (offlinePlayer.isOnline()) {
                    Player onlinePlayer = offlinePlayer.getPlayer();
                    String permQuery = "SELECT permission FROM group_permissions WHERE group_name = ?";
                    ResultSet rsPerms = mySQL.query(permQuery, groupName);
                    while (rsPerms != null && rsPerms.next()) {
                        String permission = rsPerms.getString("permission");
                        assert onlinePlayer != null;
                        onlinePlayer.addAttachment(Main.getMain(), permission, false);
                    }

                    assert onlinePlayer != null;
                    setPlayerPrefixAndName(onlinePlayer);
                }

                sender.sendMessage(Main.getMain().getMessage("group.remove.success"));

            } catch (Exception e) {
                sender.sendMessage(Main.getMain().getMessage("error"));
                e.printStackTrace();
            }
        });
    }




    private void handleDeleteGroup(CommandSender sender, String[] args) {


        String permission_check = "perms.group_delete";
        if (!sender.hasPermission(permission_check)) {
            sender.sendMessage(Main.getMain().getMessage("noPermission", "permission", permission_check));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Main.getMain().getMessage("group.delete.usage"));
            return;
        }
        String groupName = args[1];

        if ("default".equalsIgnoreCase(groupName)) {
            sender.sendMessage(Main.getMain().getMessage("group.delete.cannotDeleteDefault"));
            return;
        }

        CompletableFuture.runAsync(() -> {
            MySQL mySQL = Main.getMain().getMySQL();

            try {
                String groupQuery = "SELECT * FROM groups WHERE name = ?";
                ResultSet rsGroup = mySQL.query(groupQuery, groupName);
                if (rsGroup == null || !rsGroup.next()) {
                    sender.sendMessage(Main.getMain().getMessage("group.delete.groupNotFound"));
                    return;
                }

                String playerGroupQuery = "SELECT uuid FROM player_groups WHERE group_name = ?";
                ResultSet rsPlayers = mySQL.query(playerGroupQuery, groupName);
                while (rsPlayers != null && rsPlayers.next()) {
                    String uuid = rsPlayers.getString("uuid");

                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        String permQuery = "SELECT permission FROM group_permissions WHERE group_name = ?";
                        ResultSet rsPerms = mySQL.query(permQuery, groupName);
                        while (rsPerms != null && rsPerms.next()) {
                            String permission = rsPerms.getString("permission");
                            player.addAttachment(Main.getMain(), permission, false);
                        }
                    }
                    mySQL.update("DELETE FROM player_groups WHERE uuid = ? AND group_name = ?", uuid, groupName);
                }

                mySQL.update("DELETE FROM groups WHERE name = ?", groupName);
                mySQL.update("DELETE FROM group_permissions WHERE group_name = ?", groupName);

                sender.sendMessage(Main.getMain().getMessage("group.delete.success"));

            } catch (SQLException e) {
                sender.sendMessage(Main.getMain().getMessage("error"));
                e.printStackTrace();
            }
        });
    }



    private void handleCreateGroup(CommandSender sender, String[] args) {

        String permission_check = "perms.group_create";
        if (!sender.hasPermission(permission_check)) {
            sender.sendMessage(Main.getMain().getMessage("noPermission", "permission", permission_check));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(Main.getMain().getMessage("group.create.usage"));
            return;
        }
        String groupName = args[1];

        int weight;
        try {
            weight = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Main.getMain().getMessage("group.create.invalidWeight"));
            return;
        }

        StringBuilder prefixBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            if (i > 3) {
                prefixBuilder.append(" ");
            }
            prefixBuilder.append(args[i]);
        }
        String prefix = prefixBuilder.toString();

        CompletableFuture.runAsync(() -> {
            MySQL mySQL = Main.getMain().getMySQL();

            try {
                boolean groupExists = false;
                String groupQuery = "SELECT * FROM groups WHERE name = ?";
                ResultSet rsGroup = mySQL.query(groupQuery, groupName);
                if (rsGroup != null && rsGroup.next()) {
                    groupExists = true;
                }

                boolean weightExists = false;
                String weightQuery = "SELECT * FROM groups WHERE weight = ?";
                ResultSet rsWeight = mySQL.query(weightQuery, weight);
                if (rsWeight != null && rsWeight.next()) {
                    weightExists = true;
                }

                if (groupExists) {
                    sender.sendMessage(Main.getMain().getMessage("group.create.groupExists"));
                    return;
                }

                if (weightExists) {
                    sender.sendMessage(Main.getMain().getMessage("group.create.weightExists"));
                    return;
                }

                mySQL.update(
                        "INSERT INTO groups (name, weight, prefix) VALUES (?, ?, ?)",
                        groupName, weight, prefix
                );
                sender.sendMessage(Main.getMain().getMessage("group.create.success")
                        .replace("{group}", groupName)
                        .replace("{prefix}", prefix));

            } catch (Exception e) {
                sender.sendMessage(Main.getMain().getMessage("error"));
                e.printStackTrace();
            }
        });
    }



    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("add");
            completions.add("remove");
            completions.add("create");
            completions.add("delete");
            completions.add("edit");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("edit")) {
            completions.add("name");
            completions.add("prefix");
            completions.add("weight");
            completions.add("addperm");
            completions.add("removeperm");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("edit") || args.length == 3 && args[0].equalsIgnoreCase("remove") || args.length == 3 && args[0].equalsIgnoreCase("add")) {
            completions.addAll(getGroupNames());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            completions.addAll(getGroupNames());
        }  else if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }

    private List<String> getGroupNames() {
        List<String> groupNames = new ArrayList<>();
        try {
            ResultSet rs = Main.getMain().getMySQL().query("SELECT name FROM groups");
            while (rs != null && rs.next()) {
                groupNames.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return groupNames;
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
}