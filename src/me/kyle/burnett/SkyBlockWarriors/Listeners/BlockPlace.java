package me.kyle.burnett.SkyBlockWarriors.Listeners;

import java.sql.SQLException;

import me.kyle.burnett.SkyBlockWarriors.ArenaState;
import me.kyle.burnett.SkyBlockWarriors.GameManager;
import me.kyle.burnett.SkyBlockWarriors.Main;
import me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries.Regen.BlockLocation;
import me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries.Regen.RegenArena;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlace implements Listener {

    @EventHandler
    public void blockBreak(BlockPlaceEvent e) {

        GameManager gm = GameManager.getInstance();

        if (!gm.isPlayerInGame(e.getPlayer())) {

            if (!gm.isEditing(e.getPlayer())) {

                if (gm.isBlockInArena(e.getBlock())) {

                    e.setCancelled(true);
                }
            }
        }

        else if (gm.isPlayerInGame(e.getPlayer())) {

            if(gm.getPlayerGame(e.getPlayer()).getState().equals(ArenaState.IN_GAME)) {

                Block b = e.getBlock();

                BlockLocation bl = Main.getInstance().blockToBlockLocation(b);

                try {

                    if(!RegenArena.getBlocksBroken(gm.getPlayerGame(e.getPlayer()).getGameID()).contains(bl)){

                        RegenArena.addBlockPlaced(b.getWorld().getName(), b.getX(), b.getY(), b.getZ(), b.getTypeId(), b.getData(), gm.getPlayerGame(e.getPlayer()).getGameID());
                    }

                } catch (SQLException | ClassNotFoundException e1) {
                    e1.printStackTrace();
                }

            }

        }
    }
}
