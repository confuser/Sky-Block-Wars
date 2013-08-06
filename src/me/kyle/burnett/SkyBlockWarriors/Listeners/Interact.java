package me.kyle.burnett.SkyBlockWarriors.Listeners;

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

        Block b = e.getClickedBlock();

        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
            return;

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
