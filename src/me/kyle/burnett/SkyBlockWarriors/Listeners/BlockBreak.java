package me.kyle.burnett.SkyBlockWarriors.Listeners;

import java.sql.SQLException;
import java.util.List;

import me.kyle.burnett.SkyBlockWarriors.ArenaState;
import me.kyle.burnett.SkyBlockWarriors.GameManager;
import me.kyle.burnett.SkyBlockWarriors.Main;
import me.kyle.burnett.SkyBlockWarriors.Configs.ConfigManager;
import me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries.Regen.BlockLocation;
import me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries.Regen.RegenArena;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreak implements Listener {

    @EventHandler
    public void blockBreak(BlockBreakEvent e) {

        GameManager gm = GameManager.getInstance();

        if (!gm.isPlayerInGame(e.getPlayer())) {

            if (!gm.isEditing(e.getPlayer())) {

                if (gm.isBlockInArena(e.getBlock())) {

                    e.setCancelled(true);
                }
            }
        }

        if (gm.isPlayerInGame(e.getPlayer())) {

            if(gm.getPlayerGame(e.getPlayer()).getState().equals(ArenaState.IN_GAME)) {

                Block b = e.getBlock();

                BlockLocation bl = Main.getInstance().blockToBlockLocation(b);

                try {

                    if(!RegenArena.getBlocksPlaced(gm.getPlayerGame(e.getPlayer()).getGameID()).contains(bl)){

                        RegenArena.addBlockBroken(b.getWorld().getName(), b.getX(), b.getY(), b.getZ(), new byte[b.getData()], gm.getPlayerGame(e.getPlayer()).getGameID());

                    }

                    else if(RegenArena.getBlocksPlaced(gm.getPlayerGame(e.getPlayer()).getGameID()).contains(bl)){

                        //remove from placed.
                    }

                } catch (SQLException | ClassNotFoundException e1) {
                    e1.printStackTrace();
                }

            }

        }

        if (e.getBlock().getState() instanceof Sign) {

            Sign s = (Sign) e.getBlock().getState();

            String[] split = s.getLine(1).split(" ");

            if (split[0].equals("SBW") && gm.checkGameByConfig(Integer.parseInt(split[1]))) {

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
