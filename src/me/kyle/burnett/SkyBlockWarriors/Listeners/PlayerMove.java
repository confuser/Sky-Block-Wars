package me.kyle.burnett.SkyBlockWarriors.Listeners;

import java.util.ArrayList;
import java.util.List;

import me.kyle.burnett.SkyBlockWarriors.Game;
import me.kyle.burnett.SkyBlockWarriors.GameManager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMove implements Listener {


    private List<String> players = new ArrayList<String>();

    @EventHandler
    public void onMove(PlayerMoveEvent e) {

        if (GameManager.getInstance().isPlayerInGame(e.getPlayer())) {

            if (GameManager.getInstance().hasPlayerGameStarted(e.getPlayer())) {

                Player p = e.getPlayer();

                Game g = GameManager.getInstance().getPlayerGame(p);

                if ((e.getFrom().getBlockX() == e.getTo().getBlockX()) && (e.getFrom().getBlockZ() == e.getTo().getBlockZ()) && (e.getFrom().getBlockY() == e.getTo().getBlockY())) {
                    return;
                }

                if (!g.isBlockInArenaMove(p.getLocation())) {

                    e.setCancelled(true);

                    e.setTo(e.getFrom());
                    players.add(e.getPlayer().getName());
                }
            }
        }
    }


}
