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
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
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

                        String prefix = ChatColor.GOLD + "[" + ChatColor.BLUE + "SBW" + ChatColor.GOLD + "] ";
                        GameManager gm = GameManager.getInstance();

                        @Override
                        public void run() {

                            Packet205ClientCommand packet = new Packet205ClientCommand();
                            packet.a = 1;

                            ((CraftPlayer) p).getHandle().playerConnection.a(packet);

                            if (p.getLastDamageCause().equals(DamageCause.VOID)) {

                                p.sendMessage(prefix + ChatColor.RED + "You fell into the void!.");

                                gm.getPlayerGame(p).broadCastGame(prefix + ChatColor.RED + p.getDisplayName() + " fell into the void!");

                            } else if (p.getLastDamageCause().equals(DamageCause.CONTACT)) {

                                if (p.getLastDamageCause().getEntity() instanceof Player) {

                                    Player pk = (Player) p.getLastDamageCause().getEntity();

                                    gm.getPlayerGame(p).broadCastGame(prefix + ChatColor.RED + p.getDisplayName() + " was slain by " + pk.getDisplayName());

                                }

                            } else if (p.getLastDamageCause().equals(DamageCause.LAVA) || p.getLastDamageCause().equals(DamageCause.FIRE) || p.getLastDamageCause().equals(DamageCause.FIRE_TICK)) {

                                gm.getPlayerGame(p).broadCastGame(prefix + ChatColor.RED + p.getDisplayName() + " burned to death.");

                            } else if (p.getLastDamageCause().equals(DamageCause.DROWNING)) {

                                gm.getPlayerGame(p).broadCastGame(prefix + ChatColor.RED + p.getDisplayName() + " drowned.");

                            } else if (p.getLastDamageCause().equals(DamageCause.STARVATION)) {

                                gm.getPlayerGame(p).broadCastGame(prefix + ChatColor.RED + p.getDisplayName() + " starved to death.");

                            } else if (p.getLastDamageCause().equals(DamageCause.LIGHTNING)) {

                                gm.getPlayerGame(p).broadCastGame(prefix + ChatColor.RED + p.getDisplayName() + " somehow got killed by lightning.");

                            } else if (p.getLastDamageCause().equals(DamageCause.THORNS)) {

                                gm.getPlayerGame(p).broadCastGame(prefix + ChatColor.RED + p.getDisplayName() + " got pricked to death.");

                            } else if (p.getLastDamageCause().equals(DamageCause.FALL)) {

                                gm.getPlayerGame(p).broadCastGame(prefix + ChatColor.RED + p.getDisplayName() + " fell to there death.");

                            } else if (p.getLastDamageCause().equals(DamageCause.CUSTOM)) {

                                gm.getPlayerGame(p).broadCastGame(prefix + ChatColor.RED + p.getDisplayName() + " mysteriously died.");

                            }

                            gm.getPlayerGame(p).removeFromGameDied(p);
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
