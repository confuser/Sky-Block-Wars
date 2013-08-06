package me.kyle.burnett.SkyBlockWarriors.Listeners;

import me.kyle.burnett.SkyBlockWarriors.GameManager;
import me.kyle.burnett.SkyBlockWarriors.Main;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class Command implements Listener {


    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {

        if (GameManager.getInstance().isPlayerInGame(e.getPlayer())) {

            if (Main.getInstance().Config.getBoolean("Block-Commands")) {


                if (e.getMessage().startsWith("/sw")) {
                    return;
                }

                else if (!e.getMessage().startsWith("/sw")) {

                    if (!e.getPlayer().hasPermission("skyblockwars.usecommands")) {

                        e.setCancelled(true);
                        e.getPlayer().sendMessage("[" + ChatColor.BLUE + "SBW" + ChatColor.GOLD + "]" + ChatColor.RED + "You are not allowed to use commands in game.");
                    }
                }

            }
        }

    }


}
