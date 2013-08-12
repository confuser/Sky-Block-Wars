package me.kyle.burnett.SkyBlockWarriors.Listeners;

import java.sql.SQLException;

import me.kyle.burnett.SkyBlockWarriors.GameManager;
import me.kyle.burnett.SkyBlockWarriors.Main;
import me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries.PlayerDeaths;
import me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries.PlayerKills;
import net.minecraft.server.v1_6_R2.Packet205ClientCommand;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeath implements Listener {

    @EventHandler
    public void onDeath(final PlayerDeathEvent e) {

        Entity ent = e.getEntity();

        if (ent instanceof Player) {

            final Player p = (Player) ent;

            if (GameManager.getInstance().isPlayerInGame(p)) {

                if (GameManager.getInstance().hasPlayerGameStarted(p)) {

                    e.setDeathMessage(null);

                    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Main.getInstance(), new Runnable() {

                        @Override
                        public void run() {

                            Packet205ClientCommand packet = new Packet205ClientCommand();
                            packet.a = 1;

                            ((CraftPlayer) p).getHandle().playerConnection.a(packet);

                            p.sendMessage(ChatColor.GOLD + "[" + ChatColor.BLUE + "SBW" + ChatColor.GOLD + "] " + ChatColor.RED + "You were killed by " + ChatColor.GOLD + e.getEntity().getLastDamageCause() + ChatColor.RED + ".");

                            GameManager.getInstance().getPlayerGame(p).removeFromGameDied(p);

                        }


                    }, 1L);

                    try {

                        PlayerDeaths.setPlayerDeaths(p, 1);

                    } catch (ClassNotFoundException | SQLException e1) {

                        e1.printStackTrace();
                    }
                    if (p.getKiller() instanceof Player) {

                        try {

                            PlayerKills.setPlayerKills(p.getKiller(), 1);

                        } catch (ClassNotFoundException | SQLException e1) {

                            e1.printStackTrace();
                        }
                    }

                } else if (!GameManager.getInstance().hasPlayerGameStarted(p)) {

                    Packet205ClientCommand packet = new Packet205ClientCommand();
                    packet.a = 1;

                    ((CraftPlayer) p).getHandle().playerConnection.a(packet);
                }
            }
        }
    }

}
