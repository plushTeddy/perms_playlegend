package dev.plushteddy.perms;

import dev.plushteddy.perms.Utilities.MySQL;
import dev.plushteddy.perms.Utilities.PermissionExpiryChecker;
import dev.plushteddy.perms.commands.GroupCommand;
import dev.plushteddy.perms.commands.MeCommand;
import dev.plushteddy.perms.commands.PermissionsCommand;
import dev.plushteddy.perms.events.ConnectEvents;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.io.File;

public class Main extends JavaPlugin {

    private static Main main;
    private MySQL mySQL;
    private FileConfiguration messagesConfig;
    private String language;

    @Override
    public void onEnable() {

        try {
            main = this;
            saveDefaultConfig();
            saveDefaultConfig();
            loadMessagesConfig();

            language = getConfig().getString("language", "en_GB");

            PermissionExpiryChecker.startExpiryChecker();


            mySQL = MySQL.newBuilder()
                    .withUrl(getConfig().getString("database.url"))
                    .withDatabase(getConfig().getString("database.database"))
                    .withUser(getConfig().getString("database.user"))
                    .withPassword(getConfig().getString("database.password"))
                    .withPort(getConfig().getInt("database.port"))
                    .create();

            mySQL.update("""
                CREATE TABLE IF NOT EXISTS permissions (
                    uuid VARCHAR(36),
                    permission VARCHAR(255),
                    expiry_time BIGINT,
                    PRIMARY KEY (uuid, permission)
                );
                """);

            mySQL.update("""
                CREATE TABLE IF NOT EXISTS groups (
                    name VARCHAR(255) NOT NULL,
                    weight INT NOT NULL,
                    prefix VARCHAR(255),
                    PRIMARY KEY (name)
                );""");

            mySQL.update("""
                INSERT INTO groups (name, weight, prefix)
                SELECT 'default', 9999, '§cDefault'
                WHERE NOT EXISTS (
                    SELECT 1 FROM groups WHERE name = 'default'
                );""");



            mySQL.update("""
                CREATE TABLE IF NOT EXISTS player_groups (
                    uuid VARCHAR(36) NOT NULL,
                    group_name VARCHAR(255) NOT NULL,
                    expiry_time BIGINT DEFAULT NULL,
                    PRIMARY KEY (uuid, group_name)
                );""");

            mySQL.update("""
                CREATE TABLE IF NOT EXISTS `group_permissions` (
                    `group_name` VARCHAR(255) NOT NULL,
                    `permission` VARCHAR(255) NOT NULL,
                    PRIMARY KEY (`group_name`, `permission`)
                );
                """);


            getServer().getPluginManager().registerEvents(new ConnectEvents(), this);
            Objects.requireNonNull(getCommand("permissions")).setExecutor(new PermissionsCommand());
            Objects.requireNonNull(getCommand("group")).setExecutor(new GroupCommand());
            Objects.requireNonNull(getCommand("me")).setExecutor(new MeCommand());


            getLogger().info("The plugin was successfully enabled!");
            getLogger().info("Language:" + language);

        } catch (Exception e) {
            e.printStackTrace();
            getLogger().info("Error! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("The plugin was successfully disabled!");
    }

    public static Main getMain() {
        return main;
    }

    public MySQL getMySQL() {
        return mySQL;
    }

    public List<String> getPermissions(Player player) {
        List<String> permissions = new ArrayList<>();

        try {
            ResultSet resultSet = mySQL.query("SELECT permission FROM permissions WHERE uuid = ?", player.getUniqueId().toString());

            while (resultSet.next()) {
                permissions.add(resultSet.getString("permission"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return permissions;
    }

    private void loadMessagesConfig() {
        File messagesFile = new File(getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getMessage(String key, Object... placeholders) {
        String[] pathParts = key.split("\\.");
        StringBuilder path = new StringBuilder(language);

        for (String part : pathParts) {
            path.append(".").append(part);
        }

        String message = messagesConfig.getString(path.toString());

        if (message == null) {
            getLogger().warning("Message not found: " + path);
            return "Message not found: " + key;
        }

        if (getConfig().getBoolean("prefix", false) && !key.contains("-")) {
            String prefix = messagesConfig.getString(language + ".prefix");
            if (prefix != null) {
                message = prefix + " " + message;
            }
        }

        return replacePlaceholders(message, placeholders);
    }



    private String replacePlaceholders(String message, Object... placeholders) {
        if (placeholders.length % 2 != 0) {
            getLogger().warning("Invalid placeholder format.");
            return message;
        }

        Map<String, String> placeholderMap = new HashMap<>();
        for (int i = 0; i < placeholders.length; i += 2) {
            placeholderMap.put(placeholders[i].toString(), placeholders[i + 1].toString());
        }

        for (Map.Entry<String, String> entry : placeholderMap.entrySet()) {
            message = message.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }

        return message;
    }
}