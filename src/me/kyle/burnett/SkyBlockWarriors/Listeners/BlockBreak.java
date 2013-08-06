package me.kyle.burnett.SkyBlockWarriors.Listeners;

import java.util.List;

import me.kyle.burnett.SkyBlockWarriors.GameManager;
import me.kyle.burnett.SkyBlockWarriors.Main;
import me.kyle.burnett.SkyBlockWarriors.Configs.ConfigManager;

import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreak implements Listener {

    @EventHandler
    public void blockBreak(BlockBreakEvent e) {

        if (!GameManager.getInstance().isPlayerInGame(e.getPlayer())) {

            if (!GameManager.getInstance().isEditing(e.getPlayer())) {

                if (GameManager.getInstance().isBlockInArena(e.getBlock())) {

                    e.setCancelled(true);
                }
            }
        }

        if (e.getBlock().getState() instanceof Sign) {

            Sign s = (Sign) e.getBlock().getState();

            String[] split = s.getLine(1).split(" ");

            if (split[0].equals("SBW") && GameManager.getInstance().checkGameByConfig(Integer.parseInt(split[1]))) {

                int arena = Integer.parseInt(split[1]);

                List<String> signLocations = (List<String>) Main.getInstance().Signs.getStringList("Signs." + arena);

                String loc = Integer.toString(e.getBlock().getX()) + "," + Integer.toString(e.getBlock().getY()) + "," + Integer.toString(e.getBlock().getZ());

                if (signLocations.contains(loc)) {

                    signLocations.remove(loc);

                    Main.getInstance().Signs.set("Signs." + arena, signLocations);

                    ConfigManager.getInstance().saveYamls();
                }

            }

        }
    }

}
