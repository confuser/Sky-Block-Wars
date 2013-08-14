package me.kyle.burnett.SkyBlockWarriors.Listeners;

import java.util.List;

import me.kyle.burnett.SkyBlockWarriors.ConfigManager;
import me.kyle.burnett.SkyBlockWarriors.Game;
import me.kyle.burnett.SkyBlockWarriors.GameManager;
import me.kyle.burnett.SkyBlockWarriors.Main;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

public class SignChange implements Listener {

    @EventHandler
    public void onChange(SignChangeEvent e) {


        if (e.getLine(0).contains("[Join]")) {

            String[] split = e.getLine(1).split(" ");

            if (split[0].contains("SBW")) {

                if (Main.getInstance().Arena.contains("Arena." + Integer.parseInt(split[1].trim()))) {

                    if (e.getPlayer().hasPermission("skyblockwars.sign.create")) {

                        int arena = Integer.parseInt(split[1].trim());

                        List<String> signLocations = (List<String>) Main.getInstance().Signs.getStringList("Signs." + arena);

                        signLocations.add(Integer.toString(e.getBlock().getX()) + "," + Integer.toString(e.getBlock().getY()) + "," + Integer.toString(e.getBlock().getZ()));

                        Main.getInstance().Signs.set("Signs." + arena, signLocations);

                        ConfigManager.getInstance().saveYamls();

                        if (GameManager.getInstance().checkGameByID(arena)) {

                            Game g = GameManager.getInstance().getGameByID(arena);

                            e.setLine(0, "   §l§9[Join]");
                            e.setLine(1, "SBW " + arena + " - Waiting");
                            e.setLine(2, "0/" + Main.getInstance().Config.getInt("Max-People-In-A-Team") * 4);
                            e.setLine(3, "");
                            e.getPlayer().sendMessage(ChatColor.GOLD + "[" + ChatColor.BLUE + "SBW" + ChatColor.GOLD + "] " + ChatColor.GREEN + "Sign for arena " + ChatColor.GOLD + arena + ChatColor.GREEN + " has been made.");
                            g.updateSignState();
                            g.updateSignPlayers();

                        }

                    } else if (!e.getPlayer().hasPermission("skyblockwars.sign.join")) {

                        e.getPlayer().sendMessage(ChatColor.GOLD + "[" + ChatColor.BLUE + "SBW" + ChatColor.GOLD + "] " + ChatColor.RED + "You do not have permission to do this.");
                    }

                } else if (!Main.getInstance().Arena.contains("Arena." + Integer.parseInt(split[1].trim()))) {

                    e.getPlayer().sendMessage(ChatColor.GOLD + "[" + ChatColor.BLUE + "SBW" + ChatColor.GOLD + "]" + ChatColor.RED + "That arena does not exist.");
                }
            }
        }

        else if (e.getLine(0).contains("[Leave]")) {

            if (e.getLine(1).contains("SBW")) {

                if (e.getPlayer().hasPermission("skyblockwars.sign.create")) {

                    e.setLine(0, "§l§9[Leave]");
                    e.setLine(1, "");
                    e.setLine(2, "Back to Lobby");
                }
            }
        }
    }
}
