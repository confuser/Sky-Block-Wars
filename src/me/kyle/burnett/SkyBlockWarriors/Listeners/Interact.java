package me.kyle.burnett.SkyBlockWarriors.Listeners;

import me.kyle.burnett.SkyBlockWarriors.ArenaState;
import me.kyle.burnett.SkyBlockWarriors.GameManager;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class Interact implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {

        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
            return;

        Block b = e.getClickedBlock();

        GameManager gm = GameManager.getInstance();

        if (gm.isPlayerInGame(e.getPlayer())) {

            if (!gm.getPlayerGame(e.getPlayer()).getState().equals(ArenaState.IN_GAME)) {

                e.setCancelled(true);

            } else {

                if (!gm.getPlayerGame(e.getPlayer()).isBlockInArenaPlace(b.getLocation())) {

                    e.setCancelled(true);
                }
            }
        }

        else if (!gm.isPlayerInGame(e.getPlayer())) {

            if (!gm.isEditing(e.getPlayer())) {

                if (gm.isBlockInArenaPlace(b)) {

                    e.setCancelled(true);
                }
            }
        }

        if (b.getState() instanceof Sign) {

            Sign s = (Sign) b.getState();

            if (s.getLine(0).contains("§l§9[Join]")) {

                e.setCancelled(true);

                String[] split = s.getLine(1).split(" ");

                Bukkit.getServer().dispatchCommand(e.getPlayer(), "sw join " + split[1]);

            } else if (s.getLine(0).contains("§l§4[NotJoinable]")) {

                e.setCancelled(true);

                String[] split = s.getLine(1).split(" ");

                Bukkit.getServer().dispatchCommand(e.getPlayer(), "sw join " + split[1]);

            } else if (s.getLine(0).contains("§l§9[Leave]")) {

                e.setCancelled(true);

                Bukkit.getServer().dispatchCommand(e.getPlayer(), "sw leave");
            } else if (s.getLine(0).contains("§l§9[Spectate]")) {

                e.setCancelled(true);

                String[] split = s.getLine(1).split(" ");

                Bukkit.getServer().dispatchCommand(e.getPlayer(), "sw spectate " + split[1]);

            }

        }
    }

}
