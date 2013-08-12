package me.kyle.burnett.SkyBlockWarriors;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

public class Game {

    private ArenaState state;
    private List<String> unteamed = new ArrayList<String>();
    private List<String> players = new ArrayList<String>();
    private List<String> voted = new ArrayList<String>();
    private List<String> editors = new ArrayList<String>();
    private List<String> spectators = new ArrayList<String>();
    private HashMap<String, Team> team = new HashMap<String, Team>();
    private HashMap<String, GameMode> saveGM = new HashMap<String, GameMode>();
    private HashMap<String, Location> spawns = new HashMap<String, Location>();
    private HashMap<String, Integer> spawnsID = new HashMap<String, Integer>();
    private int gameID;
    private int count;
    private int task;
    private int announcer;
    private String prefix = ChatColor.GOLD + "[" + ChatColor.BLUE + "SBW" + ChatColor.GOLD + "] ";
    private boolean starting;
    private boolean deactivate = false;
    private Location min, max;
    private World world;
    private int amountOfPlayersAtStart;

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

        if (Main.getInstance().Arena.getBoolean("Arena." + this.gameID + ".Active")) {

            if (!justCreated) {

                WorldEditUtility.getInstance().loadIslandSchematic(this.gameID);

                try {
                    RegenArena.resetInformation(this.gameID);
                } catch (SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                }

                this.removeEntities();

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

        this.amountOfPlayersAtStart = this.players.size();

        this.setState(ArenaState.IN_GAME);

        this.broadCastGame(prefix + ChatColor.GREEN + "GO!");
        this.broadCastServer(prefix + ChatColor.GREEN + "Arena " + ChatColor.GOLD + this.gameID + ChatColor.GREEN + " has started.");

        this.unteamed.clear();

        this.assignPlayerSpawns();

        this.teleportPlayers();

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


            try {
                PlayerWins.setPlayerWins(players, 1);
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
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

               playersColor.add(p.getDisplayName());
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

    public void addSpawn(Player p) {

        int amount = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Amount") + 1;
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Amount", amount);

        Main.getInstance().Spawns.set("Spawn." + this.gameID + "." + amount + ".X", p.getLocation().getX());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + "." + amount + ".Y", p.getLocation().getY());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + "." + amount + ".Z", p.getLocation().getZ());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + "." + amount + ".PITCH", p.getLocation().getPitch());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + "." + amount + ".YAW", p.getLocation().getYaw());

        ConfigManager.getInstance().saveYamls();
    }

    public void removeSpawn(int id) {
        Main.getInstance().Spawns.set("Spawn." + this.gameID + "." + id + ".YAW", null);

        ConfigManager.getInstance().saveYamls();
    }

    public boolean isSpawn(int id){
        if(Main.getInstance().Spawns.contains("Spawn." + this.gameID + "." + id)){
            return true;
        }
        return false;
    }

    public void addSpectatorSpawn(Player p) {

        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Spectator.X", p.getLocation().getBlockX());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Spectator.Y", p.getLocation().getBlockY());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Spectator.Z", p.getLocation().getBlockZ());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Spectator.YAW", p.getLocation().getYaw());
        Main.getInstance().Spawns.set("Spawn." + this.gameID + ".Spectator.PITCH", p.getLocation().getPitch());
        ConfigManager.getInstance().saveYamls();
    }

    public Location getSpawnSpectator() {

        double x = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Spectator.X");
        double y = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Spectator.Y");
        double z = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Spectator.Z");
        long yaw = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Spectator.YAW");
        long pitch = Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Spectator.PITCH");

        return new Location(this.world, x, y + 1, z, yaw, pitch);
    }

    public void removeSpectatorSpawn() {

        Main.getInstance().Spawns.set("Spawn.Spectator", null);
    }

    public List<String> getUnteamed() {

        return this.unteamed;
    }

    public int getSpawnAmount() {
        return Main.getInstance().Spawns.getInt("Spawn." + this.gameID + ".Amount");
    }

    public void assignPlayerSpawns(){

        int count = 1;

        for(String s : this.players) {

            if(count < this.getSpawnAmount()){

                Location loc = new Location(this.world, Main.getInstance().Spawns.getDouble("Spawn." + this.gameID + "." + count + ".X"), Main.getInstance().Spawns.getDouble("Spawn." + this.gameID + "." + count + ".Y"), Main.getInstance().Spawns.getDouble("Spawn." + this.gameID + "." + count + ".Z"), Main.getInstance().Spawns.getLong("Spawn." + this.gameID + "." + count + ".YAW"), Main.getInstance().Spawns.getLong("Spawn." + this.gameID + "." + count + ".PITCH"));

                spawnsID.put(s, count);
                spawns.put(s, loc);

                count++;
            }
        }
    }

    public void teleportPlayers() {

        for (String ps : this.players) {

            Player p = Bukkit.getServer().getPlayer(ps);

            InvManager.getInstance().saveInv(p);

            p.teleport(spawns.get(p.getName()));

            if (!p.getGameMode().equals(GameMode.SURVIVAL)) {

                this.saveGM.put(p.getName(), p.getPlayer().getGameMode());
            }

            p.setGameMode(GameMode.SURVIVAL);
            p.setHealth(20.00);
            p.setFoodLevel(20);
            p.setFireTicks(0);
            p.setSaturation(10);
            p.getActivePotionEffects().clear();

            try {
                CreatePlayer.enterNewUser(p);
                PlayerPlayed.setPlayerPlayed(p, 1);
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

            Block b = this.world.getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ());

            if (b.getState() instanceof Sign) {

                Sign s = (Sign) b.getState();

                if (this.players.size() >= this.getSpawnAmount()) {

                    s.setLine(0, "§4§l[Full]");
                }

                s.setLine(2, this.players.size() + "/" + this.amountOfPlayersAtStart);

                s.update(true);

            }

        }
    }

    public void loadChunk() {
        List<String> signs = Main.getInstance().Signs.getStringList("Signs." + this.gameID);

        for (int x = 0; x < signs.size(); x++) {

            Vector v = this.vecFromString(signs.get(x));

            Block b = this.world.getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ());

            if (b.getState() instanceof Sign) {

                this.world.getChunkAt(b).load();
                this.updateSignState();
                this.updateSignPlayers();
            }
        }

    }

    public void updateSignState() {

        List<String> signs = Main.getInstance().Signs.getStringList("Signs." + this.gameID);

        for (int x = 0; x < signs.size(); x++) {

            Vector v = this.vecFromString(signs.get(x));

            Block b = this.world.getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ());

            if (b.getState() instanceof Sign) {

                Sign s = (Sign) b.getState();

                if (this.getState().equals(ArenaState.WAITING)) {

                    ((Sign) s).setLine(0, "§l§9[Join]");

                    ((Sign) s).setLine(1, "SBW " + this.gameID + " - Waiting");

                } else if (this.getState().equals(ArenaState.STARTING)) {

                    if (this.players.size() >= this.getSpawnAmount()) {

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

    public void build() {

        try {

            if (!RegenArena.getBlocksPlaced(this.gameID).isEmpty()) {

                for (int x = 0; x < RegenArena.getBlocksPlaced(this.gameID).size(); x++) {

                    List<BlockLocation> blocks = RegenArena.getBlocksPlaced(this.gameID);

                    Block b = Bukkit.getServer().getWorld(blocks.get(x).getWorld()).getBlockAt(blocks.get(x).getX(), blocks.get(x).getY(), blocks.get(x).getZ());

                    b.setTypeId(0);
                }
            }

            if (!RegenArena.getBlocksBroken(this.gameID).isEmpty()) {

                for (int x = 0; x < RegenArena.getBlocksBroken(this.gameID).size(); x++) {

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
