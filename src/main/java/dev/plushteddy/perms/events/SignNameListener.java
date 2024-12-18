package dev.plushteddy.perms.events;


import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class SignNameListener implements Listener {

    @EventHandler
    public void onPlayerClickSign(PlayerInteractEvent event) {
        if (event.getAction().toString().contains("LEFT_CLICK") && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();

            if (block.getState() instanceof Sign) {
                Sign sign = (Sign) block.getState();
                Player player = event.getPlayer();

                String newLine = player.getName();

                sign.setLine(0, "ddd");
                sign.setLine(1, "Spieler: " + newLine);
                sign.update();
            }
        }
    }
}
