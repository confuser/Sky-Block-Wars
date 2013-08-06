package me.kyle.burnett.SkyBlockWarriors.Listeners;

import java.sql.SQLException;

import me.kyle.burnett.SkyBlockWarriors.GameManager;
import me.kyle.burnett.SkyBlockWarriors.InvManager;
import me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries.PlayerLosses;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerLeave implements Listener {


    @EventHandler
    public void onLeave(PlayerQuitEvent e) {

        Player p = e.getPlayer();

        if (GameManager.getInstance().isPlayerInGame(p)) {

            GameManager gm = GameManager.getInstance();

            if (gm.hasPlayerGameStarted(p)) {

                gm.getPlayerGame(p).removeFromGameLeft(p);

                InvManager.getInstance().restoreInv(p);

                try {

                    PlayerLosses.setPlayerLosses(p, 1);

                } catch (ClassNotFoundException | SQLException e1) {

                    e1.printStackTrace();
                }


            } else if (!gm.hasPlayerGameStarted(p)) {
                gm.getPlayerGame(p).removeFromGameLeft(p);

            }
        } else {

            GameManager gm = GameManager.getInstance();
            if (gm.isPlayerSpectating(p)) {
                gm.removePlayerSpectating(p);
            }
        }

    }
}
