package me.kyle.burnett.SkyBlockWarriors;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import me.kyle.burnett.SkyBlockWarriors.Configs.ConfigManager;
import me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries.CreatePlayer;
import me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries.PlayerLosses;
import me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries.PlayerPlayed;
import me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries.PlayerWins;
import me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries.Regen.BlockLocation;
import me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries.Regen.RegenArena;
import me.kyle.burnett.SkyBlockWarriors.Events.PlayerJoinArenaEvent;
import me.kyle.burnett.SkyBlockWarriors.Events.PlayerLeaveArenaEvent;
import me.kyle.burnett.SkyBlockWarriors.Utils.WorldEditUtility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Wool;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

public class Game {

    private ScoreboardManager manager = Bukkit.getScoreboardManager();
    private Scoreboard board = manager.getNewScoreboard();
    private Team BLUE = board.registerNewTeam("Blue Team");
    private Team RED = board.registerNewTeam("Red Team");
    private Team YELLOW = board.registerNewTeam("Yellow Team");
    private Team GREEN = board.registerNewTeam("Green Team");
    private Objective objective = board.registerNewObjective("test", "dummy");
    private ArenaState state;
    private List<String> unteamed = new ArrayList<String>();
    private List<String> players = new ArrayList<String>();
    private List<String> voted = new ArrayList<String>();
    private List<String> editors = new ArrayList<String>();
    private List<String> spectators = new ArrayList<String>();
    private HashMap<String, Team> team = new HashMap<String, Team>();
    private HashMap<String, GameMode> saveGM = new HashMap<String, GameMode>();
    private int gameID;
    private int count;
    private int task;
    private int announcer;
    private Score redT = objective.getScore(Bukkit.getOfflinePlayer(ChatColor.RED + "Red: "));
    private Score greenT = objective.getScore(Bukkit.getOfflinePlayer(ChatColor.GREEN + "Green:"));
    private Score blueT = objective.getScore(Bukkit.getOfflinePlayer(ChatColor.BLUE + "Blue: "));
    private Score yellowT = objective.getScore(Bukkit.getOfflinePlayer(ChatColor.YELLOW + "Yellow: "));
    private String prefix = ChatColor.GOLD + "[" + ChatColor.BLUE + "SBW" + ChatColor.GOLD + "] ";
    private boolean starting;
    private boolean deactivate = false;
    private Location min, max;
    private World world;
    GameManager gm = GameManager.getInstance();

    public Game(int gameID, boolean justCreated, boolean justRestarted) {

        this.gameID = gameID;
        this.task = gameID;
        this.world = Bukkit.getServer().getWorld(Main.getInstance().Arena.getString("Arena." + this.gameID + ".World"));

        if (!Main.getInstance().Arena.getBoolean("Arena." + this.gameID + ".Active")) {
            this.deactivate = true;
        }

        this.min = new Location(this.world, Main.getInstance().Arena.getDouble("Arena." + this.gameID + ".MinX"), Main.getInstance().Arena.getDouble("Arena." + this.gameID + ".MinY"), Main.getInstance().Arena.getDouble("Arena." + this.gameID + ".MinZ"));
        this.max = new Location(this.world, Main.getInstance().Arena.getDouble("Arena." + this.gameID + ".MaxX"), Main.getInstance().Arena.getDouble("Arena." + this.gameID + ".MaxY"), Main.getInstance().Arena.getDouble("Arena." + this.gameID + ".MaxZ"));

        this.prepareArena(justCreated, justRestarted);

    }

    public int getGameID() {

        return this.gameID;
    }

    public void prepareArena(boolean justCreated, boolean firstLoad) {

        if (Main.getInstance().debug) {
            Main.getInstance().log.log(Level.INFO, "Preparing arena " + this.gameID);
        }

        this.setState(ArenaState.LOADING);
        this.voted.clear();
        this.players.clear();
        this.team.clear();
        this.unteamed.clear();
        this.editors.clear();
        this.saveGM.clear();
        this.redT.setScore(0);
        this.blueT.setScore(0);
        this.yellowT.setScore(0);
        this.greenT.setScore(0);

        if (Main.getInstance().Arena.getBoolean("Arena." + this.gameID + ".Active")) {

            if (!justCreated) {

                WorldEditUtility.getInstance().loadIslandSchematic(this.gameID);

                try {
                    RegenArena.resetInformation(this.gameID);
                } catch (SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                }

                this.removeEntities();

                this.RED.setDisplayName(ChatColor.RED + "Red");
                this.GREEN.setDisplayName(ChatColor.GREEN + "Green");
                this.BLUE.setDisplayName(ChatColor.RED + "Blue");
                this.YELLOW.setDisplayName(ChatColor.RED + "Yellow");

                this.setState(ArenaState.WAITING);

                if (!firstLoad && !deactivate) {

                    Game.this.broadCastServer(prefix + ChatColor.GREEN + "Arena " + ChatColor.GOLD + Game.this.gameID + ChatColor.GREEN + " is ready to join.");

                    if (Main.getInstance().debug) {
                        Main.getInstance().log.log(Level.INFO, "Arena " + this.gameID + " is ready.");
                    }
                }

                if (deactivate) {

                    this.setState(ArenaState.DEACTIVATED);
                }

                this.updateSignPlayers();
                this.updateSignState();

                return;
            }
        }

        this.setState(ArenaState.IN_SETUP);

        this.updateSignPlayers();
        this.updateSignState();
    }

    public void start() {

        Bukkit.getServer().getScheduler().cancelTask(Game.this.task);

        this.loadChests();

        this.setState(ArenaState.IN_GAME);

        this.broadCastGame(prefix + ChatColor.GREEN + "GO!");
        this.broadCastServer(prefix + ChatColor.GREEN + "Arena " + ChatColor.GOLD + this.gameID + ChatColor.GREEN + " has started.");

        this.addPlayersToTeams();

        this.unteamed.clear();

        this.teleportPlayers();

        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.objective.setDisplayName("Team's");
        this.redT.setScore(this.RED.getPlayers().size());
        this.greenT.setScore(this.GREEN.getPlayers().size());
        this.blueT.setScore(this.BLUE.getPlayers().size());
        this.yellowT.setScore(this.YELLOW.getPlayers().size());

        this.checkEnd();

        this.startGameTimer();

    }

    public void checkStart() {

        if (this.getPlayers().size() >= Main.getInstance().Config.getInt("Auto-Start-Players")) {

            this.countdown();

        } else if (this.voted.size() * 100 / this.players.size() > Main.getInstance().Config.getDouble("Percent-Of-Votes-Needed-To-Start")) {

            this.countdown();
            this.broadCastServer(prefix + ChatColor.GREEN + "Arena " + ChatColor.GOLD + this.gameID + ChatColor.GREEN + " will be starting soon.");

        }

    }

    public void endGameNormal(final Team team) {

        this.setState(ArenaState.RESETING);

        Bukkit.getServer().getScheduler().cancelTask(this.announcer);

        if (team != null) {

            if (team.equals(Game.this.RED)) {

                Game.this.broadCastGame(prefix + ChatColor.GREEN + "Well done " + ChatColor.RED + "red" + ChatColor.GREEN + " team you won.");

                this.notifySpectators(prefix + ChatColor.GREEN + "The " + ChatColor.RED + "red" + ChatColor.GREEN + " team has won.");

            } else if (team.equals(Game.this.GREEN)) {

                Game.this.broadCastGame(prefix + ChatColor.GREEN + "Well done " + ChatColor.GREEN + "green" + ChatColor.GREEN + " team you won.");

                this.notifySpectators(prefix + ChatColor.GREEN + "The " + ChatColor.GREEN + "green" + ChatColor.GREEN + " team has won.");

            } else if (team.equals(Game.this.BLUE)) {

                Game.this.broadCastGame(prefix + ChatColor.GREEN + "Well done " + ChatColor.BLUE + "blue" + ChatColor.GREEN + " team you won.");

                this.notifySpectators(prefix + ChatColor.GREEN + "The " + ChatColor.BLUE + "blue" + ChatColor.GREEN + " team has won.");

            } else if (team.equals(Game.this.YELLOW)) {

                Game.this.broadCastGame(prefix + ChatColor.GREEN + "Well done " + ChatColor.YELLOW + "yellow" + ChatColor.GREEN + " team you won.");

                this.notifySpectators(prefix + ChatColor.GREEN + "The " + ChatColor.YELLOW + "yellow" + ChatColor.GREEN + " team has won.");
            }

            try {
                PlayerWins.setPlayerWins(players, 1);
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }

        for (int x = 0; x < Game.this.players.size(); x++) {

            Player p = Bukkit.getServer().getPlayer(Game.this.players.get(x));

            if (p != null) {

                Game.this.removeFromGameEnd(p);
            }
        }

        if (!spectators.isEmpty()) {
            this.removeSpectators();
        }

        Game.this.prepareArena(false, false);

    }

    public void endGameTime() {

        this.setState(ArenaState.RESETING);

        Bukkit.getServer().getScheduler().cancelTask(this.announcer);

        for (String s : this.players) {

            Player p = Bukkit.getServer().getPlayer(s);

            this.removeFromGameEnd(p);

        }

        this.updateSignPlayers();

        this.broadCastGame(prefix + ChatColor.RED + "The game has ended because the time limit has been reached.");
        this.prepareArena(false, false);

    }

    public void endGameDisable() {

        this.setState(ArenaState.RESETING);

        Bukkit.getServer().getScheduler().cancelTask(this.announcer);


        for (String s : this.players) {

            Player p = Bukkit.getServer().getPlayer(s);

            this.removeFromGameDisable(p);
        }

        this.updateSignPlayers();

        this.broadCastGame(prefix + ChatColor.RED + "Leaving game because it has been forcefully ended by a player.");
        this.prepareArena(false, false);

    }

    public void endGameShutdown() {

        this.setState(ArenaState.RESETING);

        Bukkit.getServer().getScheduler().cancelTask(this.announcer);

        for (String s : this.players) {

            Player p = Bukkit.getServer().getPlayer(s);

            this.removeFromGameDisable(p);
        }

        this.updateSignPlayers();

        this.broadCastGame(prefix + ChatColor.RED + "Leaving game, server is closing.");

    }

    public void endGameDeactivate(boolean instart) {

        this.setState(ArenaState.RESETING);

        Bukkit.getServer().getScheduler().cancelTask(this.announcer);

        for (String s : this.players) {

            Player p = Bukkit.getServer().getPlayer(s);

            this.removeFromGameCMDEnd(p);

        }

        this.broadCastGame(prefix + ChatColor.RED + "Leaving game because it has been forcefully ended by a player.");
        this.setToDeactivate(true);
        this.prepareArena(false, false);

    }

    public void endGame() {

        this.setState(ArenaState.RESETING);

        Bukkit.getServer().getScheduler().cancelTask(this.announcer);

        for (String s : this.players) {

            Player p = Bukkit.getServer().getPlayer(s);

            this.removeFromGameCMDEnd(p);

        }

        this.updateSignPlayers();

        this.broadCastGame(prefix + ChatColor.RED + "Leaving game because it has been forcefully ended by a player.");
        this.prepareArena(false, false);

    }

    public void checkEnd() {

        int red, green, yellow, blue;
        red = this.RED.getPlayers().size();
        green = this.GREEN.getPlayers().size();
        yellow = this.YELLOW.getPlayers().size();
        blue = this.BLUE.getPlayers().size();

        if (red <= 0 && green <= 0 && yellow <= 0 && blue > 0) {

            this.endGameNormal(this.BLUE);

        } else if (red <= 0 && green <= 0 && blue <= 0 && yellow > 0) {

            this.endGameNormal(this.YELLOW);
        }

        else if (red <= 0 && blue <= 0 && yellow <= 0 && green > 0) {

            this.endGameNormal(this.GREEN);
        }

        else if (green <= 0 && yellow <= 0 && blue <= 0 && red > 0) {

            this.endGameNormal(this.RED);
        }

        else if (green <= 0 && yellow <= 0 && blue <= 0 && yellow <= 0) {

            this.endGameNormal(null);
        }

    }

    public boolean checkEndStart() {

        if (this.players.size() < Main.getInstance().Config.getInt("Minimum-Players-To-Start")) {

            return true;
        }

        return false;
    }

    public void endStart() {

        Bukkit.getScheduler().cancelTask(this.task);

        if (this.players.size() > 0) {

            this.broadCastGame(prefix + ChatColor.RED + "Countdown canceled. Not enough players to start.");
        }

        this.starting = false;
        this.setState(ArenaState.WAITING);
    }

    public void addPlayer(Player p) {

        if (this.getState().equals(ArenaState.WAITING) || this.getState().equals(ArenaState.STARTING)) {

            if (!((Main.getInstance().Config.getInt("Max-People-In-A-Team") * 4) == this.players.size())) {

                this.players.add(p.getName());
                this.unteamed.add(p.getName());
                gm.setPlayerGame(p, this.gameID);
                PlayerJoinArenaEvent event = new PlayerJoinArenaEvent(p, Game.this);
                Bukkit.getServer().getPluginManager().callEvent(event);

                this.broadCastGame(prefix + p.getDisplayName() + ChatColor.GREEN + " has joined the arena.");

                int startPlayers = Main.getInstance().Config.getInt("Auto-Start-Players");
                int max = Main.getInstance().Config.getInt("Max-People-In-A-Team") * 4;

                p.sendMessage(prefix + ChatColor.GREEN + "The game will automatically start when there are " + startPlayers + " players.");
                this.broadCastGame(prefix + ChatColor.GREEN + "There are " + ChatColor.GOLD + this.players.size() + "/" + max + ChatColor.GREEN + " players in the game.");
                Main.getInstance().teleportToWaiting(p);
                this.updateSignPlayers();

                if (!this.getState().equals(ArenaState.STARTING)) {
                    this.checkStart();
                }
            }
        }
        return;
    }

    public void removeFromGameDied(Player p) {

        PlayerLeaveArenaEvent event = new PlayerLeaveArenaEvent(p, Game.this, false);
        Bukkit.getServer().getPluginManager().callEvent(event);

        this.players.remove(p.getName());
        this.unteamed.remove(p.getName());
        this.voted.remove(p.getName());
        gm.removePlayer(p);
        this.updateSignPlayers();

        Scoreboard blankBoard = manager.getNewScoreboard();
        p.setScoreboard(blankBoard);

        if (this.getPlayerTeam(p).equals(this.RED)) {

            this.redT.setScore(this.RED.getPlayers().size());

        } else if (this.getPlayerTeam(p).equals(this.BLUE)) {

            this.blueT.setScore(this.BLUE.getPlayers().size());

        } else if (this.getPlayerTeam(p).equals(this.YELLOW)) {

            this.yellowT.setScore(this.YELLOW.getPlayers().size());

        } else if (this.getPlayerTeam(p).equals(this.GREEN)) {

            this.greenT.setScore(this.GREEN.getPlayers().size());
        }

        this.removeFromTeam(p);

        updateScoreboard();

        InvManager.getInstance().restoreInv(p);
        Main.getInstance().teleportToLobby(p);

        p.setHealth(20.00);
        p.setFoodLevel(20);
        p.setFireTicks(0);
        p.setSaturation(10);

        if (this.saveGM.containsKey(p.getName())) {
            p.setGameMode(this.saveGM.get(p.getName()));
            this.saveGM.keySet().remove(p.getName());
        }

        this.broadCastGame(prefix + ChatColor.GOLD + "Player " + p.getDisplayName() + ChatColor.GOLD + " has died.");

        try {

            PlayerLosses.setPlayerLosses(p, 1);

        } catch (ClassNotFoundException | SQLException e) {

            e.printStackTrace();
        }

        this.checkEnd();

    }

    public void removeFromGameLeft(Player p) {

        PlayerLeaveArenaEvent event = new PlayerLeaveArenaEvent(p, Game.this, false);
        Bukkit.getServer().getPluginManager().callEvent(event);

        this.players.remove(p.getName());
        this.unteamed.remove(p.getName());
        this.voted.remove(p.getName());
        gm.removePlayer(p);

        if (this.getState().equals(ArenaState.IN_GAME)) {

            Scoreboard blankBoard = manager.getNewScoreboard();
            p.setScoreboard(blankBoard);

            if (this.getPlayerTeam(p).equals(this.RED)) {

                this.redT.setScore(this.RED.getPlayers().size() - 1);

            } else if (this.getPlayerTeam(p).equals(this.BLUE)) {

                this.blueT.setScore(this.BLUE.getPlayers().size() - 1);

            } else if (this.getPlayerTeam(p).equals(this.YELLOW)) {

                this.yellowT.setScore(this.YELLOW.getPlayers().size() - 1);

            } else if (this.getPlayerTeam(p).equals(this.GREEN)) {

                this.greenT.setScore(this.GREEN.getPlayers().size() - 1);
            }

            this.removeFromTeam(p);

            InvManager.getInstance().restoreInv(p);

            p.setHealth(20.00);
            p.setFoodLevel(20);
            p.setFireTicks(0);
            p.setSaturation(10);

            try {

                PlayerLosses.setPlayerLosses(p, 1);

            } catch (ClassNotFoundException | SQLException e) {

                e.printStackTrace();
            }

            updateScoreboard();

            this.checkEnd();
        }

        this.updateSignPlayers();

        Main.getInstance().teleportToLobby(p);

        if (this.saveGM.containsKey(p.getName())) {
            p.setGameMode(this.saveGM.get(p.getName()));
            this.saveGM.keySet().remove(p.getName());
        }

        this.broadCastGame(prefix + ChatColor.GOLD + "Player " + p.getDisplayName() + ChatColor.GOLD + " has left.");


        if (starting) {
            if (this.checkEndStart()) {
                this.endStart();
            }
        }

    }

    public void removeFromGameInstart(Player p) {

        PlayerLeaveArenaEvent event = new PlayerLeaveArenaEvent(p, Game.this, false);
        Bukkit.getServer().getPluginManager().callEvent(event);

        this.players.remove(p.getName());
        this.unteamed.remove(p.getName());
        this.voted.remove(p.getName());
        gm.removePlayer(p);
        this.updateSignPlayers();

        this.broadCastGame(prefix + ChatColor.GOLD + p.getName() + ChatColor.GREEN + " has left the arena.");
        Main.getInstance().teleportToLobby(p);

        if (starting) {

            if (checkEndStart()) {

                this.endStart();
                return;
            }
        }
    }

    public void removeFromGameDisable(Player p) {

        PlayerLeaveArenaEvent event = new PlayerLeaveArenaEvent(p, Game.this, false);
        Bukkit.getServer().getPluginManager().callEvent(event);

        this.unteamed.remove(p.getName());
        this.voted.remove(p.getName());
        gm.removePlayer(p);

        if (this.getState().equals(ArenaState.IN_GAME)) {

            Scoreboard blankBoard = manager.getNewScoreboard();
            p.setScoreboard(blankBoard);

            if (this.getPlayerTeam(p).equals(this.RED)) {

                this.redT.setScore(this.RED.getPlayers().size() - 1);

            } else if (this.getPlayerTeam(p).equals(this.BLUE)) {

                this.blueT.setScore(this.BLUE.getPlayers().size() - 1);

            } else if (this.getPlayerTeam(p).equals(this.YELLOW)) {

                this.yellowT.setScore(this.YELLOW.getPlayers().size() - 1);

            } else if (this.getPlayerTeam(p).equals(this.GREEN)) {

                this.greenT.setScore(this.GREEN.getPlayers().size() - 1);
            }

            this.removeFromTeam(p);

            InvManager.getInstance().restoreInv(p);

            p.setHealth(20.00);
            p.setFoodLevel(20);
            p.setFireTicks(0);
            p.setSaturation(10);

            if (this.saveGM.containsKey(p.getName())) {
                p.setGameMode(this.saveGM.get(p.getName()));
                this.saveGM.keySet().remove(p.getName());
            }

        }

        Main.getInstance().teleportToLobby(p);

    }

    public void removeFromGameEnd(Player p) {

        PlayerLeaveArenaEvent event = new PlayerLeaveArenaEvent(p, Game.this, false);
        Bukkit.getServer().getPluginManager().callEvent(event);

        this.unteamed.remove(p.getName());
        this.voted.remove(p.getName());
        gm.removePlayer(p);
        this.updateSignPlayers();

        Scoreboard blankBoard = manager.getNewScoreboard();
        p.setScoreboard(blankBoard);


        if (this.getPlayerTeam(p).equals(this.RED)) {

            this.redT.setScore(this.RED.getPlayers().size() - 1);

        } else if (this.getPlayerTeam(p).equals(this.BLUE)) {

            this.blueT.setScore(this.BLUE.getPlayers().size() - 1);

        } else if (this.getPlayerTeam(p).equals(this.YELLOW)) {

            this.yellowT.setScore(this.YELLOW.getPlayers().size() - 1);

        } else if (this.getPlayerTeam(p).equals(this.GREEN)) {

            this.greenT.setScore(this.GREEN.getPlayers().size() - 1);
        }


        this.removeFromTeam(p);

        InvManager.getInstance().restoreInv(p);
        Main.getInstance().teleportToLobby(p);

        p.setHealth(20.00);
        p.setFoodLevel(20);
        p.setFireTicks(0);
        p.setSaturation(10);

        if (this.saveGM.containsKey(p.getName())) {

            p.setGameMode(this.saveGM.get(p.getName()));
            this.saveGM.keySet().remove(p.getName());
        }

    }

    public void removeFromGameCMDEnd(Player p) {

        PlayerLeaveArenaEvent event = new PlayerLeaveArenaEvent(p, Game.this, false);
        Bukkit.getServer().getPluginManager().callEvent(event);

        this.unteamed.remove(p.getName());
        this.voted.remove(p.getName());
        gm.removePlayer(p);

        if (this.getState().equals(ArenaState.IN_GAME)) {

            Scoreboard blankBoard = manager.getNewScoreboard();
            p.setScoreboard(blankBoard);

            if (this.getPlayerTeam(p).equals(this.RED)) {

                this.redT.setScore(this.RED.getPlayers().size() - 1);

            } else if (this.getPlayerTeam(p).equals(this.BLUE)) {

                this.blueT.setScore(this.BLUE.getPlayers().size() - 1);

            } else if (this.getPlayerTeam(p).equals(this.YELLOW)) {

                this.yellowT.setScore(this.YELLOW.getPlayers().size() - 1);

            } else if (this.getPlayerTeam(p).equals(this.GREEN)) {

                this.greenT.setScore(this.GREEN.getPlayers().size() - 1);
            }

            this.removeFromTeam(p);

            InvManager.getInstance().restoreInv(p);

            p.setHealth(20.00);
            p.setFoodLevel(20);
            p.setFireTicks(0);
            p.setSaturation(10);

        }

        Main.getInstance().teleportToLobby(p);

    }

    public List<String> getPlayers() {

        return players;
    }

    public List<String> getVoted() {

        return voted;
    }

    public void addVoted(Player p) {

        this.voted.add(p.getName());
        checkStart();
    }

    public boolean hasVoted(Player p) {

        if (this.voted.contains(p.getName())) {

            return true;
        }

        return false;
    }

    public Team getPlayerTeam(Player p) {

        Team team = this.team.get(p.getName());

        if (team == null) {

            return null;
        }

        if (team.equals(this.RED)) {

            return this.RED;

        } else if (team.equals(this.GREEN)) {

            return this.GREEN;

        } else if (team.equals(this.BLUE)) {

            return this.BLUE;

        } else if (team.equals(this.YELLOW)) {

            return this.YELLOW;

        } else {

            return null;
        }

    }

    public boolean isPlayerInTeam(Player p) {

        if (this.team.containsKey(p.getName())) {

            if (this.team.get(p.getName()) != null) {

                return true;
            }
        }
        return false;
    }

    public List<String> getEditors() {

        return editors;
    }

    public int getEditorsSize() {

        return getEditors().size();
    }

    public void addEditor(Player p) {

        editors.add(p.getName());
    }

    public void removeEditor(Player p) {

        if (editors.contains(p.getName())) {

            editors.remove(p.getName());
        }
    }

    public Team getYellowTeam() {

        return this.YELLOW;
    }

    public Team getGreenTeam() {

        return this.GREEN;
    }

    public Team getBlueTeam() {

        return this.BLUE;
    }

    public Team getRedTeam() {

        return this.RED;
    }

    public void setTeamRed(Player p) {

        this.removeFromTeam(p);
        this.team.put(p.getName(), this.RED);
        this.RED.addPlayer(p);
    }

    public void setTeamBlue(Player p) {

        this.removeFromTeam(p);
        this.team.put(p.getName(), this.BLUE);
        this.RED.addPlayer(p);
    }

    public void setTeamGreen(Player p) {

        this.removeFromTeam(p);
        this.team.put(p.getName(), this.GREEN);
        this.RED.addPlayer(p);
    }

    public void setTeamYellow(Player p) {

        this.removeFromTeam(p);
        this.team.put(p.getName(), this.YELLOW);
        this.RED.addPlayer(p);
    }

    public void removeFromTeam(Player p) {

        if (this.team.containsKey(p.getName())) {

            this.team.get(p.getName()).removePlayer(p);
            this.team.remove(p.getName());
        }
    }

    public ArenaState getState() {

        return this.state;
    }

    public void setState(ArenaState state) {

        this.state = state;
        this.updateSignState();
    }

    public void broadCastGame(String s) {

        for (int x = 0; x < players.size(); x++) {

            Player p = Bukkit.getServer().getPlayer(players.get(x));

            p.sendMessage(s);

        }
    }

    public void broadCastServer(String s) {

        for (Player p : Bukkit.getServer().getOnlinePlayers()) {

            p.sendMessage(s);

        }
    }

    public String getPlayersAsList() {

        List<String> playersColor = new ArrayList<String>();

        for (int x = 0; x < this.getPlayers().size(); x++) {

            Player p = Bukkit.getServer().getPlayer(this.getPlayers().get(x));
            if (p != null) {


                Team team = this.getPlayerTeam(p);

                if (team == null) {

                    playersColor.add(ChatColor.GRAY + p.getDisplayName());

                } else if (team.equals(this.RED)) {

                    playersColor.add(ChatColor.RED + p.getDisplayName());

                } else if (team.equals(this.GREEN)) {

                    playersColor.add(ChatColor.GREEN + p.getDisplayName());

                } else if (team.equals(this.BLUE)) {

                    playersColor.add(ChatColor.BLUE + p.getDisplayName());

                } else if (team.equals(this.YELLOW)) {

                    playersColor.add(ChatColor.YELLOW + p.getDisplayName());
                }
            }
        }

        return this.getPlayers().toString().replace("[", " ").replace("]", " ");
    }

    public void addChest(ChestType chest, Location loc) {

        if (chest.equals(ChestType.SPAWN)) {

            ArrayList<String> spawnChests = (ArrayList<String>) Main.getInstance().Chest.getStringList("Chest." + this.getGameID() + ".Spawn");

            int x, y, z;

            x = loc.getBlockX();

            y = loc.getBlockY();

            z = loc.getBlockZ();

            spawnChests.add(Integer.toString(x) + "," + Integer.toString(y) + "," + Integer.toString(z));

            Main.getInstance().Chest.set("Chest." + this.getGameID() + ".Spawn", spawnChests);

            ConfigManager.getInstance().saveYamls();

        } else if (chest.equals(ChestType.SIDE)) {

            ArrayList<String> spawnChests = (ArrayList<String>) Main.getInstance().Chest.getStringList("Chest." + this.getGameID() + ".Side");

            int x, y, z;

            x = loc.getBlockX();

            y = loc.getBlockY();

            z = loc.getBlockZ();

            spawnChests.add(Integer.toString(x) + "," + Integer.toString(y) + "," + Integer.toString(z));

            Main.getInstance().Chest.set("Chest." + this.getGameID() + ".Side", spawnChests);

            ConfigManager.getInstance().saveYamls();


        } else if (chest.equals(ChestType.CENTER)) {

            ArrayList<String> spawnChests = (ArrayList<String>) Main.getInstance().Chest.getStringList("Chest." + this.getGameID() + ".Center");

            int x, y, z;

            x = loc.getBlockX();

            y = loc.getBlockY();

            z = loc.getBlockZ();

            spawnChests.add(Integer.toString(x) + "," + Integer.toString(y) + "," + Integer.toString(z));

            Main.getInstance().Chest.set("Chest." + this.getGameID() + ".Center", spawnChests);

            ConfigManager.getInstance().saveYamls();

        }

    }

    public void addRedSpawn(Player p) {

        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Red.X", p.getLocation().getBlockX());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Red.Y", p.getLocation().getBlockY());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Red.Z", p.getLocation().getBlockZ());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Red.YAW", p.getLocation().getYaw());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Red.PITCH", p.getLocation().getPitch());

        ConfigManager.getInstance().saveYamls();
    }

    public void addBlueSpawn(Player p) {

        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Blue.X", p.getLocation().getBlockX());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Blue.Y", p.getLocation().getBlockY());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Blue.Z", p.getLocation().getBlockZ());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Blue.YAW", p.getLocation().getYaw());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Blue.PITCH", p.getLocation().getPitch());
        ConfigManager.getInstance().saveYamls();
    }

    public void addYellowSpawn(Player p) {

        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Yellow.X", p.getLocation().getBlockX());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Yellow.Y", p.getLocation().getBlockY());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Yellow.Z", p.getLocation().getBlockZ());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Yellow.YAW", p.getLocation().getYaw());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Yellow.PITCH", p.getLocation().getPitch());
        ConfigManager.getInstance().saveYamls();
    }

    public void addGreenSpawn(Player p) {

        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Green.X", p.getLocation().getBlockX());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Green.Y", p.getLocation().getBlockY());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Green.Z", p.getLocation().getBlockZ());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Green.YAW", p.getLocation().getYaw());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Green.PITCH", p.getLocation().getPitch());
        ConfigManager.getInstance().saveYamls();
    }

    public void addSpectatorSpawn(Player p) {

        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Spectator.X", p.getLocation().getBlockX());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Spectator.Y", p.getLocation().getBlockY());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Spectator.Z", p.getLocation().getBlockZ());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Spectator.YAW", p.getLocation().getYaw());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Spectator.PITCH", p.getLocation().getPitch());
        ConfigManager.getInstance().saveYamls();
    }

    public Location getSpawnRed() {

        double x = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Red.X");
        double y = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Red.Y");
        double z = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Red.Z");
        long yaw = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Red.YAW");
        long pitch = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Red.PITCH");

        return new Location(this.world, x, y + 1, z, yaw, pitch);
    }

    public Location getSpawnBlue() {

        double x = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Blue.X");
        double y = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Blue.Y");
        double z = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Blue.Z");
        long yaw = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Blue.YAW");
        long pitch = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Blue.PITCH");

        return new Location(this.world, x, y + 1, z, yaw, pitch);
    }

    public Location getSpawnYellow() {

        double x = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Yellow.X");
        double y = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Yellow.Y");
        double z = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Yellow.Z");
        long yaw = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Yellow.YAW");
        long pitch = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Yellow.PITCH");

        return new Location(this.world, x, y + 1, z, yaw, pitch);
    }

    public Location getSpawnGreen() {

        double x = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Green.X");
        double y = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Green.Y");
        double z = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Green.Z");
        long yaw = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Green.YAW");
        long pitch = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Green.PITCH");

        return new Location(this.world, x, y + 1, z, yaw, pitch);
    }

    public Location getSpawnSpectator() {

        double x = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Spectator.X");
        double y = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Spectator.Y");
        double z = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Spectator.Z");
        long yaw = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Spectator.YAW");
        long pitch = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Spectator.PITCH");

        return new Location(this.world, x, y + 1, z, yaw, pitch);
    }

    public void removeRedSpawn() {

        Main.getInstance().Spawns.set("Spawn.Red", null);

    }

    public void removeBlueSpawn() {

        Main.getInstance().Spawns.set("Spawn.Blue", null);

    }

    public void removeGreenSpawn() {

        Main.getInstance().Spawns.set("Spawn.Green", null);

    }

    public void removeYellowSpawn() {

        Main.getInstance().Spawns.set("Spawn.Yellow", null);

    }

    public void removeSpectatorSpawn() {

        Main.getInstance().Spawns.set("Spawn.Spectator", null);
    }

    public List<String> getUnteamed() {

        return this.unteamed;
    }

    public void checkTeamEliminated() {

        if (this.RED.getPlayers().size() <= 0) {

            this.broadCastGame(prefix + ChatColor.GREEN + "The " + ChatColor.RED + "red " + ChatColor.GREEN + "team has been eliminated.");
        }

        if (this.BLUE.getPlayers().size() <= 0) {

            this.broadCastGame(prefix + ChatColor.GREEN + "The " + ChatColor.BLUE + "blue " + ChatColor.GREEN + "team has been eliminated.");
        }

        if (this.YELLOW.getPlayers().size() <= 0) {

            this.broadCastGame(prefix + ChatColor.GREEN + "The " + ChatColor.YELLOW + "yellow " + ChatColor.GREEN + "team has been eliminated.");
        }

        if (this.GREEN.getPlayers().size() <= 0) {

            this.broadCastGame(prefix + ChatColor.GREEN + "The " + ChatColor.GREEN + "green " + ChatColor.GREEN + "team has been eliminated.");
        }
    }

    public void addPlayersToTeams() {

        for (int y = 0; y < unteamed.size(); y++) {

            Team team = null;

            int red = this.RED.getPlayers().size();
            int blue = this.BLUE.getPlayers().size();
            int yellow = this.YELLOW.getPlayers().size();
            int green = this.GREEN.getPlayers().size();

            if (red < blue && red < yellow && red < green) {
                team = this.RED;
            }

            else if (blue < red && blue < yellow && blue < green) {
                team = this.BLUE;
            }

            else if (yellow < blue && yellow < red && yellow < green) {
                team = this.YELLOW;
            }

            else if (green < red && green < yellow && green < blue) {
                team = this.GREEN;
            }

            else if (red == blue && red == green && red == yellow) {
                team = chooseTeam();
            }

            else if (red == blue && red < green && red < yellow) {

                Random r = new Random();
                int x = r.nextInt(1);

                if (x == 1) {
                    team = this.BLUE;
                }
                team = this.RED;
            }

            else if (red == green && red < blue && red < yellow) {

                Random r = new Random();
                int x = r.nextInt(1);

                if (x == 1) {
                    team = this.GREEN;
                }
                team = this.RED;
            }

            else if (red == yellow && red < blue && red < green) {

                Random r = new Random();
                int x = r.nextInt(1);

                if (x == 1) {
                    team = this.YELLOW;
                }
                team = this.RED;
            }

            else if (blue == green && blue < red && blue < yellow) {

                Random r = new Random();
                int x = r.nextInt(1);

                if (x == 1) {
                    team = this.BLUE;
                }
                team = this.GREEN;
            }

            else if (blue == yellow && blue < red && blue < green) {

                Random r = new Random();
                int x = r.nextInt(1);

                if (x == 1) {
                    team = this.BLUE;
                }
                team = this.YELLOW;
            }

            else if (yellow == green && yellow < red && yellow < blue) {

                Random r = new Random();
                int x = r.nextInt(1);

                if (x == 1) {
                    team = this.YELLOW;
                }
                team = this.GREEN;
            }

            else if (blue == green && green == red && red < yellow) {

                Random r = new Random();
                int x = r.nextInt(2);

                if (x == 0) {
                    team = this.GREEN;
                } else if (x == 1) {
                    team = this.RED;
                } else if (x == 2) {
                    team = this.BLUE;
                }
            }

            else if (blue == green && green == yellow && yellow < red) {

                Random r = new Random();
                int x = r.nextInt(2);

                if (x == 0) {
                    team = this.YELLOW;
                } else if (x == 1) {
                    team = this.GREEN;
                } else if (x == 2) {
                    team = this.BLUE;
                }
            }

            else if (blue == red && red == yellow && yellow < green) {

                Random r = new Random();
                int x = r.nextInt(2);

                if (x == 0) {
                    team = this.YELLOW;
                } else if (x == 1) {
                    team = this.RED;
                } else if (x == 2) {
                    team = this.BLUE;
                }
            }

            else if (green == red && red == yellow && yellow < blue) {

                Random r = new Random();
                int x = r.nextInt(2);

                if (x == 0) {
                    team = this.YELLOW;
                } else if (x == 1) {
                    team = this.RED;
                } else if (x == 2) {
                    team = this.GREEN;
                }
            }

            Player p = Bukkit.getServer().getPlayer(unteamed.get(y));
            team.addPlayer(p);

            this.team.put(p.getName(), team);

            if (team.equals(this.RED)) {

                p.sendMessage(prefix + ChatColor.GREEN + "You have joined the " + ChatColor.RED + "red " + ChatColor.GREEN + " team.");

            } else if (team.equals(this.GREEN)) {

                p.sendMessage(prefix + ChatColor.GREEN + "You have joined the " + ChatColor.DARK_GREEN + "green" + ChatColor.GREEN + " team.");

            } else if (team.equals(this.BLUE)) {

                p.sendMessage(prefix + ChatColor.GREEN + "You have joined the " + ChatColor.BLUE + "blue " + ChatColor.GREEN + " team.");

            } else if (team.equals(this.YELLOW)) {

                p.sendMessage(prefix + ChatColor.GREEN + "You have joined the " + ChatColor.YELLOW + "yellow " + ChatColor.GREEN + " team.");

            }

            InvManager.getInstance().saveInv(p);

            p.setScoreboard(this.board);
        }
    }

    public Team chooseTeam() {

        Random r = new Random();

        int x = r.nextInt(4) + 1;

        Team team = null;

        switch (x) {

        case 1:
            team = this.RED;
            break;
        case 2:
            team = this.GREEN;
            break;
        case 3:
            team = this.YELLOW;
            break;
        case 4:
            team = this.BLUE;
            break;
        }
        return team;
    }

    public boolean isRedAvailable() {

        int red = this.RED.getPlayers().size();
        int blue = this.BLUE.getPlayers().size();
        int yellow = this.YELLOW.getPlayers().size();
        int green = this.GREEN.getPlayers().size();

        if (red >= Main.getInstance().Config.getInt("Max-People-In-A-Team")) {
            return false;
        }

        if (red < blue && red < yellow && red < green) {
            return true;
        }

        else if (red == blue && red == green && red == yellow) {
            return true;
        }

        else if (red == blue && red < green && red < yellow) {
            return true;
        }

        else if (red == green && red < blue && red < yellow) {
            return true;
        }

        else if (red == yellow && red < blue && red < green) {
            return true;
        }

        else if (blue == green && green == red && red < yellow) {
            return true;
        }

        else if (blue == red && red == yellow && yellow < green) {
            return true;
        }

        else if (green == red && red == yellow && yellow < blue) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isGreenAvailable() {

        int red = this.RED.getPlayers().size();
        int blue = this.BLUE.getPlayers().size();
        int yellow = this.YELLOW.getPlayers().size();
        int green = this.GREEN.getPlayers().size();

        if (green >= Main.getInstance().Config.getInt("Max-People-In-A-Team")) {
            return false;
        }

        if (green < blue && green < yellow && green < red) {
            return true;
        }

        else if (green == blue && green == red && green == yellow) {
            return true;
        }

        else if (green == blue && green < red && green < yellow) {
            return true;
        }

        else if (green == red && green < blue && green < yellow) {
            return true;
        }

        else if (green == yellow && green < blue && green < red) {
            return true;
        }

        else if (blue == green && green == red && red < yellow) {
            return true;
        }

        else if (blue == green && green == yellow && yellow < red) {
            return true;
        }

        else if (green == red && red == yellow && yellow < blue) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isBlueAvailable() {

        int red = this.RED.getPlayers().size();
        int blue = this.BLUE.getPlayers().size();
        int yellow = this.YELLOW.getPlayers().size();
        int green = this.GREEN.getPlayers().size();

        if (blue >= Main.getInstance().Config.getInt("Max-People-In-A-Team")) {
            return false;
        }

        if (blue < green && blue < yellow && blue < red) {
            return true;
        }

        else if (blue == green && blue == red && blue == yellow) {
            return true;
        }

        else if (blue == green && blue < red && blue < yellow) {
            return true;
        }

        else if (blue == red && blue < green && blue < yellow) {
            return true;
        }

        else if (blue == yellow && blue < green && blue < red) {
            return true;
        }

        else if (blue == green && green == red && red < yellow) {
            return true;
        }

        else if (blue == green && green == yellow && yellow < red) {
            return true;
        }

        else if (blue == red && red == yellow && yellow < green) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isYellowAvailable() {

        int red = this.RED.getPlayers().size();
        int blue = this.BLUE.getPlayers().size();
        int yellow = this.YELLOW.getPlayers().size();
        int green = this.GREEN.getPlayers().size();

        if (yellow >= Main.getInstance().Config.getInt("Max-People-In-A-Team")) {
            return false;
        }

        if (yellow < green && yellow < blue && yellow < red) {
            return true;
        }

        else if (yellow == green && yellow == red && yellow == blue) {
            return true;
        }

        else if (yellow == green && yellow < red && yellow < blue) {
            return true;
        }

        else if (yellow == red && yellow < green && yellow < blue) {
            return true;
        }

        else if (yellow == blue && yellow < green && yellow < red) {
            return true;
        }

        else if (yellow == green && green == red && red < yellow) {
            return true;
        }

        else if (yellow == red && red == blue && yellow < green) {
            return true;
        }

        else if (yellow == blue && blue == green && yellow < red) {
            return true;
        } else {
            return false;
        }
    }

    public void teleportPlayers() {

        Location red = this.getSpawnRed();
        Location green = this.getSpawnGreen();
        Location blue = this.getSpawnBlue();
        Location yellow = this.getSpawnYellow();

        for (OfflinePlayer p : this.RED.getPlayers()) {

            p.getPlayer().teleport(red);

            Wool wool = new Wool(DyeColor.RED);

            p.getPlayer().getInventory().setHelmet(wool.toItemStack(1));

            if (!p.getPlayer().getGameMode().equals(GameMode.SURVIVAL)) {

                this.saveGM.put(p.getName(), p.getPlayer().getGameMode());
            }

            p.getPlayer().setGameMode(GameMode.SURVIVAL);
            p.getPlayer().setHealth(20.00);
            p.getPlayer().setFoodLevel(20);
            p.getPlayer().setFireTicks(0);
            p.getPlayer().setSaturation(10);
            p.getPlayer().getActivePotionEffects().clear();

            try {
                CreatePlayer.enterNewUser(p.getPlayer());
                PlayerPlayed.setPlayerPlayed(p.getPlayer(), 1);
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }

        for (OfflinePlayer p : this.GREEN.getPlayers()) {

            p.getPlayer().teleport(green);

            Wool wool = new Wool(DyeColor.GREEN);

            p.getPlayer().getInventory().setHelmet(wool.toItemStack(1));

            if (!p.getPlayer().getGameMode().equals(GameMode.SURVIVAL)) {
                this.saveGM.put(p.getName(), p.getPlayer().getGameMode());
            }

            p.getPlayer().setGameMode(GameMode.SURVIVAL);
            p.getPlayer().setHealth(20.00);
            p.getPlayer().setFoodLevel(20);
            p.getPlayer().setFireTicks(0);
            p.getPlayer().setSaturation(10);
            p.getPlayer().getActivePotionEffects().clear();

            try {
                CreatePlayer.enterNewUser(p.getPlayer());
                PlayerPlayed.setPlayerPlayed(p.getPlayer(), 1);
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }

        for (OfflinePlayer p : this.BLUE.getPlayers()) {

            p.getPlayer().teleport(blue);

            Wool wool = new Wool(DyeColor.BLUE);

            p.getPlayer().getInventory().setHelmet(wool.toItemStack(1));

            if (!p.getPlayer().getGameMode().equals(GameMode.SURVIVAL)) {
                this.saveGM.put(p.getName(), p.getPlayer().getGameMode());
            }

            p.getPlayer().setGameMode(GameMode.SURVIVAL);
            p.getPlayer().setHealth(20.00);
            p.getPlayer().setFoodLevel(20);
            p.getPlayer().setFireTicks(0);
            p.getPlayer().setSaturation(10);
            p.getPlayer().getActivePotionEffects().clear();

            try {
                CreatePlayer.enterNewUser(p.getPlayer());
                PlayerPlayed.setPlayerPlayed(p.getPlayer(), 1);
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }

        for (OfflinePlayer p : this.YELLOW.getPlayers()) {

            p.getPlayer().teleport(yellow);

            Wool wool = new Wool(DyeColor.YELLOW);

            p.getPlayer().getInventory().setHelmet(wool.toItemStack(1));

            if (!p.getPlayer().getGameMode().equals(GameMode.SURVIVAL)) {
                this.saveGM.put(p.getName(), p.getPlayer().getGameMode());
            }

            p.getPlayer().setGameMode(GameMode.SURVIVAL);
            p.getPlayer().setHealth(20.00);
            p.getPlayer().setFoodLevel(20);
            p.getPlayer().setFireTicks(0);
            p.getPlayer().setSaturation(10);
            p.getPlayer().getActivePotionEffects().clear();

            try {
                CreatePlayer.enterNewUser(p.getPlayer());
                PlayerPlayed.setPlayerPlayed(p.getPlayer(), 1);
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void teleportSpectator(Player p) {

        if (Main.getInstance().Spawns.contains("Spawn." + gameID + ".Spectator")) {

            p.teleport(this.getSpawnSpectator());
            spectators.add(p.getName());
            gm.setPlayerSpectating(p, this.gameID);
            p.sendMessage(prefix + "You are now spectating arena " + this.gameID + ".");

        } else {
            p.sendMessage(prefix + "This arena does not have a spectator spawn set.");
        }
    }

    public void removeSpectators(Player p) {

        if (spectators.contains(p.getName())) {

            Main.getInstance().teleportToLobby(p);
            spectators.remove(p.getName());
            gm.removePlayerSpectating(p);
            p.sendMessage(prefix + "You are no longer spectating.");
        } else {

            p.sendMessage(prefix + "You are not currently spectating a game.");
        }
    }

    public void removeSpectators() {

        for (int i = 0; i < this.spectators.size(); i++) {

            Player p = Bukkit.getPlayer(spectators.get(i));
            if (spectators.contains(p.getName())) {

                Main.getInstance().teleportToLobby(p);
                spectators.remove(p.getName());
                gm.removePlayerSpectating(p);
                p.sendMessage(prefix + ChatColor.GREEN + "You are no longer spectating.");

            } else {

                p.sendMessage(prefix + ChatColor.GREEN + "You are not currently spectating a game.");
            }
        }
    }

    public void notifySpectators(String message) {

        if (!spectators.isEmpty()) {

            for (int i = 0; i < spectators.size(); i++) {

                Player p = Bukkit.getPlayer(spectators.get(i));
                p.sendMessage(message);
            }
        }
    }

    public void removeChest(Location l) {

        List<String> spawnChests = (ArrayList<String>) Main.getInstance().Chest.getStringList("Chest." + this.gameID + ".Spawn");
        List<String> sideChests = (ArrayList<String>) Main.getInstance().Chest.getStringList("Chest." + this.gameID + ".Side");
        List<String> centerChests = (ArrayList<String>) Main.getInstance().Chest.getStringList("Chest." + this.gameID + ".Center");

        int x = l.getBlockX();
        int y = l.getBlockY();
        int z = l.getBlockZ();

        String s = Integer.toString(x) + "," + Integer.toString(y) + "," + Integer.toString(z);

        if (spawnChests.contains(s)) {

            spawnChests.remove(s);

            Main.getInstance().Chest.set("Chest." + this.gameID + ".Spawn", spawnChests);

        } else if (sideChests.contains(s)) {

            sideChests.remove(s);

            Main.getInstance().Chest.set("Chest." + this.gameID + ".Side", sideChests);

        } else if (centerChests.contains(s)) {

            centerChests.remove(s);

            Main.getInstance().Chest.set("Chest." + this.gameID + ".Center", centerChests);

        }
    }

    public void loadChests() {

        this.fillChests(this.world, Main.getInstance().Chest.getStringList("Chest." + this.gameID + ".Spawn"), Main.getInstance().Config.getStringList("Chests.Spawn-Chests.ItemID/Amount"));
        this.fillChests(this.world, Main.getInstance().Chest.getStringList("Chest." + this.gameID + ".Side"), Main.getInstance().Config.getStringList("Chests.Side-Chests.ItemID/Amount"));
        this.fillChests(this.world, Main.getInstance().Chest.getStringList("Chest." + this.gameID + ".Center"), Main.getInstance().Config.getStringList("Chests.Middle-Chest.ItemID/Amount"));
    }

    public void fillChests(World world, List<String> chestLocations, List<String> chestContents) {
        ItemStack[] items = new ItemStack[27];

        int i = 0;

        for (String item : chestContents) {

            items[i++] = this.itemFromString(item);
        }

        for (String locString : chestLocations) {

            Block b = world.getBlockAt(this.vecFromString(locString).toLocation(world));


            if (b.getType().equals(Material.CHEST)) {

                Chest c = (Chest) b.getState();
                c.getInventory().setContents(items);

            } else {

                Main.getInstance().getLogger().warning("Failed to find chest at " + locString + ", skipping...");
            }
        }
    }

    private Vector vecFromString(String string) {

        String[] split = string.split(",");
        return new Vector(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
    }

    private ItemStack itemFromString(String string) {

        String[] split = string.split(",");

        for (int x = 0; x < split.length; x++) {
            split[x] = split[x].toLowerCase().trim();
        }
        if (split.length < 1)
            return null;
        if (split.length == 1)
            return new ItemStack(Integer.parseInt(split[0]));
        if (split.length == 2)
            return new ItemStack(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
        if (split.length == 3) {
            return new ItemStack(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Short.parseShort(split[2]));
        }

        return null;
    }

    public boolean isBlockInArenaMove(Location l) {

        if (l.getWorld() != this.min.getWorld())
            return false;

        double x = l.getX();
        double z = l.getZ();

        return (x >= this.min.getBlockX()) && (x < this.max.getBlockX() + 1) && (z >= this.min.getBlockZ()) && (z < this.max.getBlockZ() + 1);
    }

    public boolean isBlockInArenaPlace(Location l) {

        if (l.getWorld() != this.min.getWorld())
            return false;

        double x = l.getX();
        double z = l.getZ();
        double y = l.getY();

        return (x >= this.min.getBlockX()) && (x < this.max.getBlockX() + 1) && (y >= this.min.getBlockY()) && (y < this.max.getBlockY() + 1) && (z >= this.min.getBlockZ()) && (z < this.max.getBlockZ() + 1);
    }

    public void updateSignPlayers() {

        List<String> signs = Main.getInstance().Signs.getStringList("Signs." + this.gameID);

        for (int x = 0; x < signs.size(); x++) {

            Vector v = this.vecFromString(signs.get(x));

            Block b = null;

            if(this.world.getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ()).equals(Material.SIGN) || this.world.getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ()).equals(Material.SIGN_POST)){
                b = this.world.getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ());
            }

            if (b != null) {

                if (b.getState() instanceof Sign) {

                    Sign s = (Sign) b.getState();

                    if (this.players.size() >= Main.getInstance().Config.getInt("Max-People-In-A-Team") * 4) {

                        s.setLine(0, "§4§l[Full]");
                    }

                    s.setLine(2, this.players.size() + "/" + Main.getInstance().Config.getInt("Max-People-In-A-Team") * 4);

                    s.update(true);
                }
            }
        }
    }

    public void loadChunk(){
        List<String> signs = Main.getInstance().Signs.getStringList("Signs." + this.gameID);

        for (int x = 0; x < signs.size(); x++) {

            Vector v = this.vecFromString(signs.get(x));

            Block b = null;

            if(this.world.getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ()).equals(Material.SIGN) || this.world.getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ()).equals(Material.SIGN_POST)){
                b = this.world.getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ());
            }

            this.world.getChunkAt(b).load();
            this.updateSignState();
            this.updateSignPlayers();
        }

    }

    public void updateSignState() {

        List<String> signs = Main.getInstance().Signs.getStringList("Signs." + this.gameID);

        for (int x = 0; x < signs.size(); x++) {

            Vector v = this.vecFromString(signs.get(x));

            Block b = null;

            if(this.world.getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ()).equals(Material.SIGN) || this.world.getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ()).equals(Material.SIGN_POST)){
                b = this.world.getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ());
            }

            if (b != null) {

                if (b.getState() instanceof Sign) {

                    Sign s = (Sign) b.getState();

                    if (this.getState().equals(ArenaState.WAITING)) {

                        ((Sign) s).setLine(0, "§l§9[Join]");

                        ((Sign) s).setLine(1, "SBW " + this.gameID + " - Waiting");

                    } else if (this.getState().equals(ArenaState.STARTING)) {

                        if (this.players.size() >= Main.getInstance().Config.getInt("Max-People-In-A-Team") * 4) {

                            ((Sign) s).setLine(0, "§4§l[Full]");

                            ((Sign) s).setLine(1, "SBW " + this.gameID + " -Starting");

                            s.update();

                            return;
                        }

                        ((Sign) s).setLine(0, "§l§9[Join]");

                        ((Sign) s).setLine(1, "SBW " + this.gameID + " -Starting");

                    } else if (this.getState().equals(ArenaState.IN_GAME)) {

                        if (Main.getInstance().Spawns.contains("Spawn." + gameID + ".Spectator")) {

                            ((Sign) s).setLine(0, "§l§9[Spectate]");

                            ((Sign) s).setLine(1, "SBW " + this.gameID + " - InGame");

                        } else {

                            ((Sign) s).setLine(0, "§l§9[InGame]");

                            ((Sign) s).setLine(1, "SBW " + this.gameID + " - InGame");

                        }

                    } else {

                        ((Sign) s).setLine(0, "§l§4UnJoinable");

                        ((Sign) s).setLine(1, "SBW " + this.gameID + " - Other");
                    }

                    s.update(true);

                }
            }
        }
    }

    public void setToDeactivate(boolean bool) {
        this.deactivate = bool;
    }

    public void updateScoreboard() {

        for (int x = 0; x < players.size(); x++) {

            Bukkit.getServer().getPlayer(players.get(x));

        }
    }

    public void countdown() {

        this.count = Main.getInstance().Config.getInt("Auto-Start-Time");

        if (this.state == ArenaState.WAITING) {

            this.setState(ArenaState.STARTING);

            this.starting = true;

            this.task = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getInstance(), new Runnable() {

                public void run() {

                    if (Game.this.players.size() < Main.getInstance().Config.getInt("Minimum-Players-To-Start")) {

                        Game.this.endStart();

                    } else if (Game.this.count > 0) {

                        if (Game.this.count % 10 == 0) {
                            Game.this.broadCastGame(Game.this.prefix + ChatColor.GREEN + "Starting in " + ChatColor.GOLD + count + ChatColor.GREEN + ".");
                        }
                        if (Game.this.count < 6) {
                            Game.this.broadCastGame(Game.this.prefix + ChatColor.GREEN + "Starting in " + ChatColor.GOLD + count + ChatColor.GREEN + ".");
                        }

                        Game.this.count -= 1;

                    } else {

                        Game.this.start();
                    }
                }
            }, 0L, 20L);
        }
    }

    public void startGameTimer() {

        this.announcer = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getInstance(), new Runnable() {

            int x = Main.getInstance().Config.getInt("Time-Limit-Seconds");


            @Override
            public void run() {

                if (x % 60 == 0) {


                    if (x != 0) {

                        if (x != 60) {

                            Game.this.broadCastGame(Game.this.prefix + ChatColor.GOLD + x / 60 + ChatColor.GREEN + " minutes remaining until the game ends.");
                        }

                        if (x == 60) {
                            Game.this.broadCastGame(Game.this.prefix + ChatColor.GOLD + x / 60 + ChatColor.GREEN + " minute remaining until the game ends.");
                        }
                    }

                }

                if (x / 60 < 1) {

                    if (x == 30) {

                        Game.this.broadCastGame(Game.this.prefix + ChatColor.GOLD + x + ChatColor.GREEN + " seconds remaining until the game ends.");

                    }

                    if (x <= 10) {
                        Game.this.broadCastGame(Game.this.prefix + ChatColor.GOLD + x + ChatColor.GREEN + " seconds remaining until the game ends.");

                    }

                    if (x <= 0) {
                        Game.this.endGameTime();
                    }


                }

                x -= 1;


            }

        }, 0L, 20L);

    }

    public void removeEntities() {

        Location l1 = new Location(Bukkit.getWorld(Main.getInstance().Arena.getString("Arena." + gameID + ".World")), Main.getInstance().Arena.getInt("Arena." + gameID + ".MinX"), 0, Main.getInstance().Arena.getInt("Arena." + gameID + ".MinZ"));
        Location l2 = new Location(Bukkit.getWorld(Main.getInstance().Arena.getString("Arena." + gameID + ".World")), Main.getInstance().Arena.getInt("Arena." + gameID + ".MaxX"), 0, Main.getInstance().Arena.getInt("Arena." + gameID + ".MaxZ"));

        double x = 0;
        double z = 0;

        for (Entity e : world.getEntities()) {

            x = e.getLocation().getX();
            z = e.getLocation().getZ();

            if (x > l1.getX() && z > l1.getZ() && x < l2.getX() && z < l2.getZ() && !(e instanceof Player)) {

                e.remove();
            }
        }
    }

    public void build(){

        try {

            if(!RegenArena.getBlocksPlaced(this.gameID).isEmpty()){

                for(int x = 0; x < RegenArena.getBlocksPlaced(this.gameID).size(); x++){

                    List<BlockLocation> blocks = RegenArena.getBlocksPlaced(this.gameID);

                    Block b = Bukkit.getServer().getWorld(blocks.get(x).getWorld()).getBlockAt(blocks.get(x).getX(), blocks.get(x).getY(), blocks.get(x).getZ());

                    b.setTypeId(0);
                }
            }

            if(!RegenArena.getBlocksBroken(this.gameID).isEmpty()){

                for(int x = 0; x < RegenArena.getBlocksBroken(this.gameID).size(); x++){

                    List<BlockLocation> blocks = RegenArena.getBlocksBroken(this.gameID);

                    Block b = Bukkit.getServer().getWorld(blocks.get(x).getWorld()).getBlockAt(blocks.get(x).getX(), blocks.get(x).getY(), blocks.get(x).getZ());

                    b.setData(blocks.get(x).getData());
                    b.setTypeId(blocks.get(x).getBlock());

                }
            }

        } catch (SQLException | ClassNotFoundException e) {

            e.printStackTrace();
        }

    }
}
