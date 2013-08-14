package me.kyle.burnett.SkyBlockWarriors;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class ChestHandler {

    static Main main = Main.getInstance();

    private static ChestHandler instance;

    public void loadChests(int gameID, World world) {

        List<String> chests = main.CChests.getStringList("Custom-Chests");

        for (int x = 0; x < main.CChests.getInt("Chest-Amounts"); x++) {

            fillChests(world, main.Chest.getStringList("Chest." + gameID + "." + chests.get(x)), main.CChests.getStringList("ChestItems." + chests.get(x)));

        }
        //this.fillChests(this.world, main.Chest.getStringList("Chest." + this.gameID + ".Spawn"), main.Config.getStringList("Chests.Spawn-Chests.ItemID/Amount"));
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

                main.getLogger().warning("Failed to find chest at " + locString + ", skipping...");
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

    public boolean doesCChestExist(String name) {

        if (Main.getInstance().CChests.getStringList("Custom-Chests").contains(name)) {
            return true;
        }

        return false;
    }

    public void addCustomChest(String name) {

        List<String> chests = Main.getInstance().CChests.getStringList("Custom-Chests");

        chests.add(name);

        Main.getInstance().CChests.set("Custom-Chests", chests);

        Main.getInstance().CChests.set("ChestItems." + name, new ArrayList<String>());

        ConfigManager.getInstance().saveYamls();
    }

    public void removeCustomChest(String name) {

        List<String> chests = Main.getInstance().CChests.getStringList("Custom-Chests");

        chests.remove(name);

        Main.getInstance().CChests.set("Custom-Chests", chests);

        Main.getInstance().CChests.set("ChestItems." + name, null);

        ConfigManager.getInstance().saveYamls();
    }

    public static ChestHandler getInstance() {

        if (instance == null) {
            instance = new ChestHandler();
        }

        return instance;
    }
}
