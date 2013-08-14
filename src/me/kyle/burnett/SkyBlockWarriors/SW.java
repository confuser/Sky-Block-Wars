package me.kyle.burnett.SkyBlockWarriors;

import java.sql.SQLException;

import me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries.Leaderboard;
import me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries.PlayerSearch;
import me.kyle.burnett.SkyBlockWarriors.Utils.WorldEditUtility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;

public class SW implements CommandExecutor {


    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

        if (cmd.getName().equalsIgnoreCase("sw") || cmd.getName().equalsIgnoreCase("skyblockw")) {

            Player p = (Player) sender;

            GameManager gm = GameManager.getInstance();
            ChestHandler chestH = ChestHandler.getInstance();


            String prefix = ChatColor.GOLD + "[" + ChatColor.BLUE + "SBW" + ChatColor.GOLD + "] ";
            String noperms = ChatColor.RED + "You do not have permission to do this.";
            String perm = prefix + noperms;

            if (p.hasPermission("skyblockwars.user")) {

                if (args.length == 0) {

                    PluginDescriptionFile pdf = Main.getInstance().getDescription();

                    p.sendMessage(ChatColor.GOLD + "----------" + ChatColor.GREEN + "Sky Block War's" + ChatColor.GOLD + "----------");
                    p.sendMessage(ChatColor.BLUE + "Author: " + ChatColor.GOLD + "Burnett");
                    p.sendMessage(ChatColor.BLUE + "Contributor: " + ChatColor.GOLD + "kbunkrams");
                    p.sendMessage(ChatColor.BLUE + "Version: " + ChatColor.GOLD + pdf.getVersion());
                    p.sendMessage(ChatColor.BLUE + "/sw help - " + ChatColor.GOLD + "Show's the user command help.");
                    p.sendMessage(ChatColor.BLUE + "/sw help builder - " + ChatColor.GOLD + "Show's the builder command help.");
                    p.sendMessage(ChatColor.BLUE + "/sw help admin -  " + ChatColor.GOLD + "Show's the admin command help.");

                    return true;
                }

                if (args.length == 1) {

                    if (args[0].equalsIgnoreCase("help")) {

                        p.sendMessage(ChatColor.GOLD + "----------" + ChatColor.GREEN + "Sky Block War's Help" + ChatColor.GOLD + "----------");
                        p.sendMessage(ChatColor.GOLD + "/sw join [arena] - " + ChatColor.BLUE + "Join a specific arena or get teleported to the lobby.");
                        p.sendMessage(ChatColor.GOLD + "/sw spectate [arena] - " + ChatColor.BLUE + "Specate a specific arena.");
                        p.sendMessage(ChatColor.GOLD + "/sw list - " + ChatColor.BLUE + "List players in your current game.");
                        p.sendMessage(ChatColor.GOLD + "/sw listgames -  " + ChatColor.BLUE + "List all available games.");
                        p.sendMessage(ChatColor.GOLD + "/sw vote -  " + ChatColor.BLUE + "Vote to start the game.");
                        p.sendMessage(ChatColor.GOLD + "/sw leader {Kills, Deaths, Wins, Losses, Played} - " + ChatColor.BLUE + "Shows leaderboards for selected category.");
                        p.sendMessage(ChatColor.GOLD + "/sw stats {player} - " + ChatColor.BLUE + "Gives more indepth stats for specified player.");

                        return true;
                    }

                    if (args[0].equalsIgnoreCase("join")) {

                        if (p.hasPermission("skyblockwars.join")) {

                            if (!gm.isPlayerInGame(p)) {

                                if (Main.getInstance().teleportToLobby(p)) {

                                    p.sendMessage(prefix + ChatColor.GREEN + "Arena not specifed teleporting to lobby.");

                                } else if (!Main.getInstance().teleportToLobby(p)) {

                                    p.sendMessage(prefix + ChatColor.RED + "Tryed to teleport to lobby but it was not found. Please tell server staff.");
                                }

                            } else if (gm.isPlayerInGame(p)) {

                                p.sendMessage(prefix + ChatColor.RED + "You have already joined.");
                            }

                        } else if (!p.hasPermission("skyblockwars.join")) {

                            p.sendMessage(perm);
                        }

                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("spectate")) {

                        if (p.hasPermission("skyblockswars.spectate")) {

                            p.sendMessage(prefix + ChatColor.RED + "You must specify the arena number to spectate.");

                        } else {

                            p.sendMessage(perm);

                        }

                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("leave")) {

                        if (p.hasPermission("skyblockwars.user")) {

                            if (gm.isPlayerInGame(p)) {

                                Game game = gm.getPlayerGame(p);

                                p.sendMessage(prefix + ChatColor.GREEN + "You have left the arena.");

                                game.removeFromGameLeft(p);

                            } else if (!gm.isPlayerInGame(p)) {

                                if (gm.isPlayerSpectating(p)) {

                                    gm.getPlayerSpectating(p).removeSpectators(p);

                                    p.sendMessage(prefix + "You have left the arena.");

                                } else {

                                    p.sendMessage(prefix + ChatColor.RED + "You are not in a game.");
                                }
                            }

                        } else if (!p.hasPermission("skyblockwars.join")) {

                            p.sendMessage(perm);
                        }
                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("list")) {

                        if (p.hasPermission("skyblockwars.list")) {

                            if (gm.isPlayerInGame(p)) {

                                p.sendMessage(prefix + ChatColor.GOLD + "Player's:");
                                p.sendMessage(gm.getPlayerGame(p).getPlayersAsList());

                            } else if (!gm.isPlayerInGame(p)) {

                                p.sendMessage(prefix + ChatColor.RED + "You are not in a game.");
                            }

                        } else if (!p.hasPermission("skyblockwars.list")) {

                            p.sendMessage(perm);
                        }
                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("listgames")) {

                        if (p.hasPermission("skyblockwars.listgames")) {

                            String arenas = gm.listGames();

                            p.sendMessage(prefix + ChatColor.GOLD + "Arena List:");
                            p.sendMessage(ChatColor.GRAY + arenas);

                        } else if (!p.hasPermission("skyblockwars.listgames")) {

                            p.sendMessage(perm);
                        }
                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("create")) {

                        if (p.hasPermission("skyblockwars.create")) {

                            if (Main.getInstance().doesLobbyExist()) {

                                if (WorldEditUtility.getInstance().doesSelectionExist(p)) {

                                    int arena = gm.createGame(p);
                                    p.sendMessage(prefix + ChatColor.GREEN + "Arena " + ChatColor.GOLD + arena + ChatColor.GREEN + " created. Once you have edited it use '" + ChatColor.GOLD + "/sw activate arena" + ChatColor.GREEN + "' to allow people to join.");

                                } else if (!WorldEditUtility.getInstance().doesSelectionExist(p)) {

                                    p.sendMessage(prefix + ChatColor.RED + "Please make a selection of the arena first.");
                                }

                            } else if (!Main.getInstance().doesLobbyExist()) {

                                p.sendMessage(prefix + ChatColor.RED + "You need to set a lobby first.");
                            }

                        } else if (!p.hasPermission("skyblockwars.create")) {

                            p.sendMessage(perm);
                        }
                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("confirm")) {

                        if (p.hasPermission("skyblockwars.confirm")) {

                            if (gm.getConfirming().containsKey(p.getName())) {

                                p.sendMessage(prefix + ChatColor.GREEN + "Arena " + ChatColor.GOLD + gm.getConfirming().get(p.getName()) + ChatColor.GREEN + " has been overwritten.");
                                gm.overrideArena(p, gm.getConfirming().get(p.getName()));

                            } else if (!gm.getConfirming().containsKey(p.getName())) {

                                p.sendMessage(prefix + ChatColor.RED + "You are not waiting to confirm anything.");
                            }

                        } else if (!p.hasPermission("skyblockwars.confirm")) {

                            p.sendMessage(perm);
                        }
                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("setlobby")) {

                        if (p.hasPermission("skyblockwars.setlobby")) {

                            Main.getInstance().setLobby(p);
                            p.sendMessage(prefix + ChatColor.GREEN + "Lobby set succesfully.");

                        } else if (!p.hasPermission("skyblockwars.setlobby")) {

                            p.sendMessage(perm);
                        }
                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("finish")) {

                        if (p.hasPermission("skyblockwars.edit")) {

                            if (gm.isEditing(p)) {

                                p.sendMessage(prefix + ChatColor.GREEN + "Finished editing arena " + ChatColor.GOLD + gm.getEditing().get(p.getName()) + ChatColor.GREEN + ".");
                                gm.removeEditor(p);

                            } else if (!gm.isEditing(p)) {

                                p.sendMessage(prefix + ChatColor.RED + "You are not editing an arena.");
                            }

                        } else if (!p.hasPermission("skyblockwars.edit")) {

                            p.sendMessage(perm);
                        }
                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("vote")) {

                        if (p.hasPermission("skyblockwars.vote")) {

                            if (gm.isPlayerInGame(p)) {

                                if (!gm.hasPlayerGameStarted(p)) {

                                    if (!gm.getPlayerGame(p).getState().equals(ArenaState.STARTING)) {

                                        if (gm.getPlayerGame(p).getPlayers().size() >= Main.getInstance().Config.getInt("Minimum-Players-To-Start")) {

                                            if (!gm.getPlayerGame(p).hasVoted(p)) {

                                                p.sendMessage(prefix + ChatColor.GREEN + "Voted!");
                                                gm.getPlayerGame(p).broadCastGame(prefix + p.getDisplayName() + ChatColor.GREEN + " has voted to start.");
                                                gm.getPlayerGame(p).addVoted(p);

                                            } else if (gm.getPlayerGame(p).hasVoted(p)) {

                                                p.sendMessage(prefix + ChatColor.RED + "You have already voted.");
                                            }

                                        } else if (gm.getPlayerGame(p).getPlayers().size() < Main.getInstance().Config.getInt("Minimum-Players-To-Start")) {

                                            p.sendMessage(prefix + ChatColor.RED + "You cant vote. There are not enough players to start.");
                                        }


                                    } else if (gm.getPlayerGame(p).getState().equals(ArenaState.STARTING)) {

                                        p.sendMessage(prefix + ChatColor.RED + "Game is already starting.");
                                    }

                                } else if (gm.hasPlayerGameStarted(p)) {

                                    p.sendMessage(prefix + ChatColor.RED + "You can not vote when the game has started.");
                                }

                            } else if (!gm.isPlayerInGame(p)) {

                                p.sendMessage(prefix + ChatColor.RED + "You are not in an arena.");
                            }

                        } else if (!p.hasPermission("skyblockwars.vote")) {

                            p.sendMessage(perm);
                        }
                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("updatesigns")) {

                        if (p.hasPermission("skyblockwars.updatesigns")) {

                            for (Game g : gm.getGames()) {
                                g.updateSignPlayers();
                                g.updateSignState();
                            }

                            p.sendMessage(prefix + ChatColor.GREEN + "All signs have been reloaded.");


                        } else {

                            p.sendMessage(perm);
                        }

                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("reload")) {

                        if (p.hasPermission("skyblockwars.reload")) {

                            ConfigManager.getInstance().loadYamls();
                            ConfigManager.getInstance().saveYamls();

                            try {

                                ConfigManager.getInstance().firstRun();

                            } catch (Exception e) {

                                e.printStackTrace();
                            }

                            p.sendMessage(prefix + ChatColor.GREEN + "Config's reloaded.");

                        } else if (!p.hasPermission("skyblockwars.reload")) {

                            p.sendMessage(perm);
                        }

                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("leaderboard") || args[0].equalsIgnoreCase("leader")) {

                        if (p.hasPermission("skyblockwars.leader")) {

                            try {
                                Leaderboard.getTopPlayers(p, "kills");
                            } catch (ClassNotFoundException | SQLException e) {
                                e.printStackTrace();
                            }

                        } else if (!p.hasPermission("skyblockwars.leader")) {

                            p.sendMessage(perm);
                        }
                        return true;

                    } else if (args[0].equalsIgnoreCase("stats")) {

                        if (p.hasPermission("skyblockwars.stats")) {

                            try {
                                PlayerSearch.getPlayerData(p, p.getName());
                            } catch (ClassNotFoundException | SQLException e) {
                                e.printStackTrace();
                            }

                        } else if (!p.hasPermission("skyblockwars.stats")) {

                            p.sendMessage(perm);
                        }
                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("removechest")) {

                        if (p.hasPermission("skyblockwars.addchest")) {

                            if (gm.isEditing(p)) {

                                if (WorldEditUtility.getInstance().doesSelectionExist(p)) {

                                    if (WorldEditUtility.getInstance().isChest(p)) {

                                        Location loc = WorldEditUtility.getInstance().getChestLocation(p);

                                        if (loc.equals(null)) {

                                            p.sendMessage(prefix + ChatColor.RED + "An error occured. Please try again.");

                                            return true;
                                        }

                                        if (WorldEditUtility.getInstance().isChestAlreadyAdded(p)) {

                                            gm.getGameEditing(p).removeChest(WorldEditUtility.getInstance().getChestLocation(p));

                                            p.sendMessage(prefix + ChatColor.GREEN + "Chest removed.");

                                        } else if (!WorldEditUtility.getInstance().isChestAlreadyAdded(p)) {

                                            p.sendMessage(prefix + ChatColor.RED + "That chest is not added,");
                                        }

                                    } else if (!WorldEditUtility.getInstance().isChest(p)) {

                                        p.sendMessage(prefix + ChatColor.RED + "Your selection is either more than one block or is not a chest.");
                                    }

                                } else if (!WorldEditUtility.getInstance().doesSelectionExist(p)) {

                                    p.sendMessage(prefix + ChatColor.RED + "You do not have a selection of a chest.");
                                }

                            } else if (!gm.isEditing(p)) {

                                p.sendMessage(prefix + ChatColor.RED + "You are not editing an arena.");
                            }

                        } else if (p.hasPermission("skyblockwars.addchest")) {

                            p.sendMessage(perm);
                        }

                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("reloadarenas")) {

                        if (p.hasPermission("skyblockwars.reloadarenas")) {

                            for (Game g : gm.getGames()) {

                                if (g.getState().equals(ArenaState.IN_GAME) || g.getState().equals(ArenaState.STARTING)) {

                                    g.endGame();
                                }
                            }

                            p.sendMessage(prefix + ChatColor.GREEN + "All arenas have been reloaded.");

                        } else {

                            p.sendMessage(perm);
                        }

                        return true;
                    } else if (args[0].equalsIgnoreCase("addspawn")) {

                        if (p.hasPermission("skyblockwars.addspawn")) {

                            if (gm.isEditing(p)) {

                                Game g = gm.getGameEditing(p);

                                g.addSpawn(p);

                                p.sendMessage(prefix + ChatColor.GREEN + "Spawn " + ChatColor.GOLD + g.getSpawnAmount() + ChatColor.GREEN +  " has been added.");

                            } else if (!gm.isEditing(p)) {

                                p.sendMessage(prefix + ChatColor.RED + "You are not editing an arena.");
                            }

                        } else if (!p.hasPermission("skyblockwars.addspawn")) {

                            p.sendMessage(perm);
                        }

                        return true;
                    }

                    p.sendMessage(prefix + ChatColor.RED + "Unknown command. Use '/sw help' for a list of commands.");

                    return true;
                }

                if (args.length == 2) {


                    if (args[0].equalsIgnoreCase("removespawn")) {

                        if (p.hasPermission("skyblockwars.setspawn")) {

                            if (gm.isEditing(p)) {

                                if (gm.getGameEditing(p).isSpawn(Integer.parseInt(args[1]))) {

                                    Game g = gm.getGameEditing(p);

                                    g.removeSpawn(Integer.parseInt(args[1]));

                                    p.sendMessage(prefix + ChatColor.GREEN + "Spawn " + ChatColor.GOLD + args[1] + " has been removed.");

                                } else if (args[1].equalsIgnoreCase("spectator")) {

                                    Game g = gm.getGameEditing(p);

                                    g.removeSpectatorSpawn();

                                    p.sendMessage(prefix + ChatColor.GRAY + "Spectator " + ChatColor.GREEN + "spawn has been removed.");

                                } else if (!gm.getGameEditing(p).isSpawn(Integer.parseInt(args[1]))) {

                                    p.sendMessage(prefix + ChatColor.RED + "That spawn does not exist.");
                                }

                            } else if (!gm.isEditing(p)) {

                                p.sendMessage(prefix + ChatColor.RED + "You are not editing an arena.");
                            }

                        } else if (!p.hasPermission("skyblockwars.setspawn")) {

                            p.sendMessage(perm);
                        }

                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("setwaiting")) {

                        if (p.hasPermission("skyblockwars.setwaiting")) {

                            if (gm.checkGameByConfig(Integer.parseInt(args[1]))) {

                                Main.getInstance().setWaiting(p, Integer.parseInt(args[1]));

                                p.sendMessage(prefix + ChatColor.GREEN + "Waiting room set succesfully.");

                            } else if (gm.checkGameByConfig(Integer.parseInt(args[1]))) {

                                p.sendMessage(prefix + ChatColor.RED + "That arena does not exist.");
                            }

                        } else if (!p.hasPermission("skyblockwars.setwaiting")) {

                            p.sendMessage(perm);
                        }
                        return true;

                    }

                    else if (args[0].equalsIgnoreCase("addchesttype")) {

                        if (p.hasPermission("skyblockwars.addchesttype")) {

                            if (!chestH.doesCChestExist(args[1])) {

                                chestH.addCustomChest(args[1]);

                                p.sendMessage(prefix + ChatColor.GREEN + "You have added '" + ChatColor.GOLD + args[1] + ChatColor.GREEN + "' as a new chest type. Add items via the custom chest type file.");

                            } else if (!chestH.doesCChestExist(args[1])) {
                                p.sendMessage(prefix + ChatColor.RED + "That chest type already exists.");
                            }

                        } else if (p.hasPermission("skyblockwars.addchesttpye")) {

                            p.sendMessage(perm);
                        }

                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("removechesttype")) {

                        if (p.hasPermission("skyblockwars.addchesttype")) {

                            if (chestH.doesCChestExist(args[1])) {

                                chestH.removeCustomChest(args[1]);

                                p.sendMessage(prefix + ChatColor.GREEN + "You have removed '" + ChatColor.GOLD + args[1] + ChatColor.GREEN + "' as a new chest type.");

                            } else if (chestH.doesCChestExist(args[1])) {
                                p.sendMessage(prefix + ChatColor.RED + "That chest type does not exist.");
                            }

                        } else if (p.hasPermission("skyblockwars.addchesttype")) {

                            p.sendMessage(perm);
                        }

                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("create")) {

                        if (p.hasPermission("skyblockwars.create.override")) {

                            if (Main.getInstance().doesLobbyExist()) {

                                if (gm.isInteger(args[1])) {

                                    int id = Integer.parseInt(args[1]);

                                    if (!(id > gm.getArenaAmount())) {

                                        p.sendMessage(prefix + ChatColor.RED + "You are away to override a previous arena with a new selection. This will overwrite all the arena's previous data but keep the same id. Do '/sw confirm' to confirm this action.");

                                        gm.getConfirming().put(p.getName(), id);

                                    } else if (id > gm.getArenaAmount()) {

                                        p.sendMessage(prefix + ChatColor.RED + "That number is bigger than your amount of arenas. Use '/sw create' to add arenas.");
                                    }
                                }

                            } else if (!Main.getInstance().doesLobbyExist()) {

                                p.sendMessage(prefix + ChatColor.RED + "You need to set a lobby first.");
                            }

                        } else if (!p.hasPermission("skyblockwars.create.override")) {

                            p.sendMessage(perm);
                        }

                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("join")) {

                        if (p.hasPermission("skyblockwars.join." + args[1]) || p.hasPermission("skyblockwars.join")) {

                            if (gm.checkGameByID(Integer.parseInt(args[1]))) {

                                if (!gm.isPlayerInGame(p)) {

                                    if (gm.isEnabled(Integer.parseInt(args[1]))) {

                                        if (gm.isActive(Integer.parseInt(args[1]))) {

                                            Game game = gm.getGameByID(Integer.parseInt(args[1]));

                                            if (game.getState().equals(ArenaState.WAITING) || game.getState().equals(ArenaState.STARTING)) {

                                                if (game.getPlayers().size() < game.getSpawnAmount()) {

                                                    game.addPlayer(p);

                                                } else if (game.getPlayers().size() >= game.getSpawnAmount()) {

                                                    p.sendMessage(prefix + ChatColor.RED + "That arena is full.");
                                                }

                                            } else if (!game.getState().equals(ArenaState.WAITING) || !game.getState().equals(ArenaState.STARTING)) {

                                                p.sendMessage(prefix + ChatColor.RED + "Can not join the because the arena is " + game.getState().toString().toLowerCase().replaceAll("_", " ") + ".");
                                            }


                                        } else if (!gm.isActive(Integer.parseInt(args[1]))) {

                                            p.sendMessage(prefix + ChatColor.RED + "That arena is disabled.");
                                        }

                                    } else if (!gm.isEnabled(Integer.parseInt(args[1]))) {
                                        p.sendMessage(prefix + ChatColor.RED + "That arena is disabled.");
                                    }

                                } else if (gm.isPlayerInGame(p)) {

                                    p.sendMessage(prefix + ChatColor.RED + "You are already in a game.");
                                }

                            } else if (!gm.checkGameByID(Integer.parseInt(args[1]))) {

                                p.sendMessage(prefix + ChatColor.RED + "That game does not exist.");
                            }

                        } else if (p.hasPermission("skyblockwars.join." + args[1]) || p.hasPermission("skyblockwars.join")) {

                            p.sendMessage(perm);
                        }

                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("spectate")) {

                        if (p.hasPermission("skyblockwars.spectate")) {

                            if (gm.checkGameByID(Integer.parseInt(args[1]))) {

                                if (!gm.isPlayerInGame(p)) {

                                    Game game = gm.getGameByID(Integer.parseInt(args[1]));
                                    game.teleportSpectator(p);
                                } else {

                                    p.sendMessage(prefix + ChatColor.RED + "You cannot spectate while in a game.");
                                }
                            } else {
                                p.sendMessage(prefix + ChatColor.RED + "That arena does not exists.");
                            }
                        } else {

                            p.sendMessage(perm);
                        }
                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("edit")) {

                        if (p.hasPermission("skyblockwars.edit")) {

                            if (!gm.isEditing(p)) {

                                if (gm.checkGameByID(Integer.parseInt(args[1]))) {

                                    gm.addEditor(p, Integer.parseInt(args[1]));

                                    gm.getGameByID(Integer.parseInt(args[1])).setState(ArenaState.GETTING_EDITED);

                                    p.sendMessage(prefix + ChatColor.GREEN + "Now editing arena " + ChatColor.GOLD + args[1] + ChatColor.GREEN + ".");

                                } else if (gm.checkGameByID(Integer.parseInt(args[1]))) {

                                    p.sendMessage(prefix + ChatColor.RED + "That arena does not exist.");
                                }

                            } else if (gm.isEditing(p)) {

                                p.sendMessage(prefix + ChatColor.RED + "You are already editing an arena.");

                            }

                        } else if (p.hasPermission("skyblockwars.edit")) {

                            p.sendMessage(perm);
                        }

                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("addchest")) {

                        if (p.hasPermission("skyblockwars.addchest")) {

                            if (gm.isEditing(p)) {

                                if (WorldEditUtility.getInstance().doesSelectionExist(p)) {

                                    if (WorldEditUtility.getInstance().isChest(p)) {

                                        if (!WorldEditUtility.getInstance().isChestAlreadyAdded(p)) {

                                            Location loc = WorldEditUtility.getInstance().getChestLocation(p);

                                            if (loc.equals(null)) {

                                                p.sendMessage(prefix + ChatColor.RED + "An error occured. Please try again.");

                                                return true;
                                            }

                                            if (chestH.doesCChestExist(args[1])) {

                                                gm.getGameEditing(p).addChest(args[1], loc);

                                                p.sendMessage(prefix + ChatColor.GREEN + "You have added a " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " chest to arena " + ChatColor.GOLD + gm.getPlayerEditing(p) + ChatColor.GREEN + ".");

                                            } else if (!chestH.doesCChestExist(args[1])) {

                                                p.sendMessage(prefix + ChatColor.RED + "That chest type does not exist.");
                                            }

                                        } else if (WorldEditUtility.getInstance().isChestAlreadyAdded(p)) {

                                            p.sendMessage(prefix + ChatColor.RED + "That chest is already added. You can remove it with /sw removechest");
                                        }

                                    } else if (!WorldEditUtility.getInstance().isChest(p)) {

                                        p.sendMessage(prefix + ChatColor.RED + "Your selection is either more than one block or is not a chest.");
                                    }

                                } else if (!WorldEditUtility.getInstance().doesSelectionExist(p)) {

                                    p.sendMessage(prefix + ChatColor.RED + "You do not have a selection of a chest.");
                                }

                            } else if (!gm.isEditing(p)) {

                                p.sendMessage(prefix + ChatColor.RED + "You are not editing an arena.");
                            }

                        } else if (p.hasPermission("skyblockwars.addchest")) {

                            p.sendMessage(perm);
                        }

                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("enable")) {

                        if (p.hasPermission("skyblockwars.enable")) {

                            if (gm.checkGameByConfig(Integer.parseInt(args[1]))) {

                                if (!gm.isEnabled(Integer.parseInt(args[1]))) {

                                    gm.enableGame(Integer.parseInt(args[1]));

                                    p.sendMessage(prefix + ChatColor.GREEN + "You enabled arena " + ChatColor.GOLD + args[1] + ChatColor.GREEN + ".");

                                } else if (gm.isEnabled(Integer.parseInt(args[1]))) {

                                    p.sendMessage(prefix + ChatColor.RED + "Arena is already enabled.");
                                }

                            } else if (gm.checkGameByConfig(Integer.parseInt(args[1]))) {

                                p.sendMessage(prefix + ChatColor.RED + "That is not an arena.");
                            }

                        } else if (p.hasPermission("skyblockwars.enable")) {

                            p.sendMessage(perm);
                        }

                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("disable")) {

                        if (p.hasPermission("skyblockwars.disable")) {

                            if (gm.checkGameByID(Integer.parseInt(args[1]))) {

                                Game g = gm.getGameByID(Integer.parseInt(args[1]));

                                if (gm.isEnabled(Integer.parseInt(args[1]))) {

                                    if (gm.isActive(Integer.parseInt(args[1]))) {

                                        if (g.getState().equals(ArenaState.GETTING_EDITED)) {

                                            p.sendMessage(prefix + ChatColor.RED + "Could not disable arena " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " because it is being edited.");

                                            return true;
                                        }

                                        gm.getGameByID(Integer.parseInt(args[1])).endGameDisable();

                                        gm.setDisabled(Integer.parseInt(args[1]));

                                        p.sendMessage(prefix + ChatColor.GREEN + "Arena " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " has been disabled.");

                                    } else if (!gm.isActive(Integer.parseInt(args[1]))) {

                                        gm.setDisabled(Integer.parseInt(args[1]));

                                        p.sendMessage(prefix + ChatColor.GREEN + "You disabled arena " + ChatColor.GOLD + args[1] + ChatColor.GREEN + ".");
                                    }

                                } else if (!gm.isEnabled(Integer.parseInt(args[1]))) {

                                    p.sendMessage(prefix + ChatColor.RED + "Arena is already disabled.");
                                }

                            } else if (gm.checkGameByID(Integer.parseInt(args[1]))) {

                                p.sendMessage(prefix + ChatColor.RED + "That is not an arena.");
                            }

                        } else if (p.hasPermission("skyblockwars.disable")) {

                            p.sendMessage(perm);
                        }
                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("activate")) {

                        if (p.hasPermission("skyblockwars.activate")) {

                            if (gm.checkGameByID(Integer.parseInt(args[1]))) {

                                p.sendMessage(prefix + ChatColor.GREEN + "Arena " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " has been activated.");

                                gm.activate(Integer.parseInt(args[1]));

                            } else if (gm.checkGameByID(Integer.parseInt(args[1]))) {

                                p.sendMessage(prefix + ChatColor.RED + "That arena does not exist.");
                            }

                        } else if (p.hasPermission("skyblockwars.activate")) {

                            p.sendMessage(perm);
                        }

                        return true;
                    }

                    if (args[0].equalsIgnoreCase("deactivate")) {

                        if (p.hasPermission("skyblockwars.deactivate")) {

                            if (gm.checkGameByID(Integer.parseInt(args[1]))) {

                                if (gm.isActive(Integer.parseInt(args[1]))) {

                                    if (gm.getGameByID(Integer.parseInt(args[1])).getState().equals(ArenaState.WAITING) || gm.getGameByID(Integer.parseInt(args[1])).getState().equals(ArenaState.STARTING)) {

                                        gm.setDeactivated(Integer.parseInt(args[1]));
                                        gm.getGameByID(Integer.parseInt(args[1])).endGameDeactivate(true);

                                        p.sendMessage(prefix + ChatColor.GREEN + "Arena " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " has been deactivated.");

                                    } else if (gm.getGameByID(Integer.parseInt(args[1])).getState().equals(ArenaState.IN_GAME)) {

                                        gm.setDeactivated(Integer.parseInt(args[1]));
                                        gm.getGameByID(Integer.parseInt(args[1])).setToDeactivate(true);
                                        p.sendMessage(prefix + ChatColor.GREEN + "Arena " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " will be deactivated after the game is finished.");
                                    }

                                } else if (!gm.isActive(Integer.parseInt(args[1]))) {

                                    p.sendMessage(prefix + ChatColor.RED + "Arena " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " is already deactivated.");
                                }

                            } else if (gm.checkGameByID(Integer.parseInt(args[1]))) {

                                p.sendMessage(prefix + ChatColor.RED + "That arena does not exist.");
                            }

                        } else if (p.hasPermission("skyblockwars.deactivate")) {

                            p.sendMessage(perm);
                        }

                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("endgame")) {

                        if (p.hasPermission("skyblockwars.endgame")) {

                            if (gm.checkGameByID(Integer.parseInt(args[1]))) {

                                Game g = gm.getGameByID(Integer.parseInt(args[1]));

                                if (g.getState().equals(ArenaState.WAITING) || g.getState().equals(ArenaState.STARTING)) {

                                    p.sendMessage(prefix + ChatColor.GREEN + "Arena " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " has been forcefully ended.");

                                    gm.getGameByID(Integer.parseInt(args[1])).endGame();

                                }

                            } else if (gm.checkGameByID(Integer.parseInt(args[1]))) {

                                p.sendMessage(prefix + ChatColor.RED + "That arena does not exist.");
                            }

                        } else if (!p.hasPermission("skyblockwars.endgame")) {

                            p.sendMessage(perm);
                        }

                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("save")) {

                        if (p.hasPermission("skyblockwars.save")) {

                            if (gm.checkGameByID(Integer.parseInt(args[1]))) {

                                WorldEditUtility.getInstance().resaveArena(Integer.parseInt(args[1]));

                                p.sendMessage(prefix + ChatColor.GREEN + "Arena " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " has been saved.");


                            } else if (gm.checkGameByID(Integer.parseInt(args[1]))) {

                                p.sendMessage(prefix + ChatColor.RED + "That arena does not exist.");
                            }

                        } else if (p.hasPermission("skyblockwars.save")) {

                            p.sendMessage(perm);
                        }

                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("load")) {

                        if (p.hasPermission("skyblockwars.load")) {

                            if (gm.checkGameByID(Integer.parseInt(args[1]))) {

                                if (WorldEditUtility.getInstance().loadIslandSchematic(Integer.parseInt(args[1]))) {

                                    p.sendMessage(prefix + ChatColor.GREEN + "Arena " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " has been loaded.");

                                } else if (!WorldEditUtility.getInstance().loadIslandSchematic(Integer.parseInt(args[1]))) {

                                    p.sendMessage(prefix + ChatColor.RED + "An error occured while trying to load the schematic for arena " + ChatColor.GOLD + args[1] + ChatColor.GREEN + ".");
                                }

                            } else if (gm.checkGameByID(Integer.parseInt(args[1]))) {

                                p.sendMessage(prefix + ChatColor.RED + "That arena does not exist.");
                            }

                        } else if (p.hasPermission("skyblockwars.load")) {

                            p.sendMessage(perm);
                        }

                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("prepare")) {

                        if (p.hasPermission("skyblockwars.prepare")) {

                            if (gm.checkGameByID(Integer.parseInt(args[1]))) {

                                if (gm.getGameByID(Integer.parseInt(args[1])).getState().equals(ArenaState.IN_GAME)) {

                                    p.sendMessage(prefix + ChatColor.GREEN + "Arena " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " has been prepared.");

                                    gm.getGameByID(Integer.parseInt(args[1])).endGame();

                                } else {

                                    gm.getGameByID(Integer.parseInt(args[1])).prepareArena(false, false);

                                    p.sendMessage(prefix + ChatColor.GREEN + "Arena " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " has been prepared.");
                                }

                            } else if (!gm.checkGameByID(Integer.parseInt(args[1]))) {

                                p.sendMessage(prefix + ChatColor.RED + "That arena does not exist.");
                            }

                        } else if (!p.hasPermission("skyblockwars.prepare")) {

                            p.sendMessage(perm);
                        }
                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("leaderboard") || args[0].equalsIgnoreCase("leader")) {

                        if (p.hasPermission("skyblockwars.leader")) {

                            if (args[1].equalsIgnoreCase("kills") || args[1].equalsIgnoreCase("deaths") || args[1].equalsIgnoreCase("wins") || args[1].equalsIgnoreCase("losses") || args[1].equalsIgnoreCase("played")) {

                                try {
                                    Leaderboard.getTopPlayers(p, args[1]);
                                } catch (ClassNotFoundException | SQLException e) {
                                    e.printStackTrace();
                                }

                            } else {
                                p.sendMessage(prefix + ChatColor.RED + "Incorrect variable. Please use Kills, Deaths, Wins, Losses, or Played.");
                            }

                        } else {
                            p.sendMessage(perm);
                        }

                        return true;

                    }

                    else if (args[0].equalsIgnoreCase("stats")) {

                        if (p.hasPermission("skyblockwars.stats")) {

                            if (Bukkit.getPlayer(args[1]) != null) {

                                try {
                                    PlayerSearch.getPlayerData(p, Bukkit.getPlayer(args[1]).getName());
                                } catch (ClassNotFoundException | SQLException e) {
                                    e.printStackTrace();
                                }

                            } else {

                                try {
                                    PlayerSearch.getPlayerData(p, args[1]);
                                } catch (ClassNotFoundException | SQLException e) {
                                    e.printStackTrace();
                                }
                            }

                        } else if (!p.hasPermission("skyblockwars.stats")) {

                            p.sendMessage(perm);
                        }
                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("info")) {

                        if (p.hasPermission("skyblockwars.info")) {

                            if (gm.checkGameByID(Integer.parseInt(args[1]))) {

                                /*
                                 *
                                 *
                                 *
                                 * LIST INFO ON ARENA. e.g. State, players,
                                 * time left,
                                 */

                            } else if (!gm.checkGameByID(Integer.parseInt(args[1]))) {

                                p.sendMessage(prefix + ChatColor.RED + "That arena does not exist.");
                            }

                        } else if (!p.hasPermission("skyblockwars.info")) {

                            p.sendMessage(perm);
                        }

                        return true;
                    }

                    else if (args[0].equalsIgnoreCase("help")) {

                        if (args[1].equalsIgnoreCase("builder")) {

                            if (p.hasPermission("skyblockwars.help.builder")) {

                                p.sendMessage(ChatColor.GOLD + "----------" + ChatColor.GREEN + "Sky Block War's Builder Help" + ChatColor.GOLD + "----------");
                                p.sendMessage(ChatColor.GOLD + "/sw create [arena] - " + ChatColor.BLUE + "Create's a new arena or overrides a previous arena if specified.");
                                p.sendMessage(ChatColor.GOLD + "/sw confirm - " + ChatColor.BLUE + "Confirm an override action.");
                                p.sendMessage(ChatColor.GOLD + "/sw edit <arena> - " + ChatColor.BLUE + "Enter edit mode of an arena");
                                p.sendMessage(ChatColor.GOLD + "/sw addspawn - " + ChatColor.BLUE + "Set the spawns for each team.");
                                p.sendMessage(ChatColor.GOLD + "/sw removespawn <spawn>" + " - " + ChatColor.BLUE + "Remove the spawn for a team.");
                                p.sendMessage(ChatColor.GOLD + "/sw addchest <type> - " + ChatColor.BLUE + "Add the location and type of the arena chests.");
                                p.sendMessage(ChatColor.GOLD + "/sw removechest - " + ChatColor.BLUE + "Removes the current chest of your selection.");

                            } else if (!p.hasPermission("skyblockwars.help.builder")) {

                                p.sendMessage(perm);
                            }

                        } else if (args[1].equalsIgnoreCase("admin")) {

                            if (p.hasPermission("skyblockwars.help.admin")) {


                                p.sendMessage(ChatColor.GOLD + "----------" + ChatColor.GREEN + "Sky Block War's Admin Help" + ChatColor.GOLD + "----------");
                                p.sendMessage(ChatColor.GOLD + "/sw setlobby - " + ChatColor.BLUE + "Set the lobby where player's get teleported to.");
                                p.sendMessage(ChatColor.GOLD + "/sw setwaiting <arena> - " + ChatColor.BLUE + "Set the waiting lobby where player's get teleported to after they join.");
                                p.sendMessage(ChatColor.GOLD + "/sw addchesttype <name> - " + ChatColor.BLUE + "Add a custom chest type. Edit items in custom-chests.yml");
                                p.sendMessage(ChatColor.GOLD + "/sw removechesttype <name> - " + ChatColor.BLUE + "Remove a custom chest type.");
                                p.sendMessage(ChatColor.GOLD + "/sw prepare <arena> - " + ChatColor.BLUE + "Completely restart the arena disregarding what state its in.");
                                p.sendMessage(ChatColor.GOLD + "/sw enable <arena> - " + ChatColor.BLUE + "Enable an arena for editing.");
                                p.sendMessage(ChatColor.GOLD + "/sw disable <arena> - " + ChatColor.BLUE + "Disable an arena. Will not show up in '/sw listgames'.");
                                p.sendMessage(ChatColor.GOLD + "/sw activate <arena> - " + ChatColor.BLUE + "Activate an arena for players to join.");
                                p.sendMessage(ChatColor.GOLD + "/sw deactivate <arena> - " + ChatColor.BLUE + "Deactivate an arena to stop players joining.");
                                p.sendMessage(ChatColor.GOLD + "/sw endgame <arena> - " + ChatColor.BLUE + "Forcefully end a game.");
                                p.sendMessage(ChatColor.GOLD + "/sw save <arena> - " + ChatColor.BLUE + "Save block changes made to an arena.");
                                p.sendMessage(ChatColor.GOLD + "/sw load <arena> - " + ChatColor.BLUE + "Load a schematic of an arena.");
                                p.sendMessage(ChatColor.GOLD + "/sw reload - " + ChatColor.BLUE + "Reload the configs.");
                                p.sendMessage(ChatColor.GOLD + "/sw reloadarenas - " + ChatColor.BLUE + "Reload's all the arenas.");

                            } else if (!p.hasPermission("skyblockwars.help.admin")) {

                                p.sendMessage(perm);
                            }

                        }
                        return true;
                    }

                    p.sendMessage(prefix + ChatColor.RED + "Unknown command. Use '/sw help' for a list of commands.");
                    return true;
                }

                if (args.length >= 3) {
                    p.sendMessage(prefix + ChatColor.RED + "Unknown command. Use '/sw help' for a list of commands.");
                    return true;
                }
            } else if (!p.hasPermission("skyblockwars.user")) {
                p.sendMessage(perm);
            }
            return true;
        }
        return false;
    }

}
