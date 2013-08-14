package me.kyle.burnett.SkyBlockWarriors;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InvManager {

    static InvManager instance = new InvManager();

    public static InvManager getInstance() {
        return instance;
    }

    public void saveInv(Player p) {
        List<ItemStack> main = Arrays.asList(p.getInventory().getContents());
        List<ItemStack> armor = Arrays.asList(p.getInventory().getArmorContents());

        Main.getInstance().Inv.set(p.getName() + ".Main", main);
        Main.getInstance().Inv.set(p.getName() + ".Armor", armor);

        p.getInventory().clear();
        p.getInventory().setArmorContents(null);

        ConfigManager.getInstance().saveYamls();
    }

    @SuppressWarnings("unchecked")
    public void restoreInv(Player p) {

        List<ItemStack> main = (List<ItemStack>) Main.getInstance().Inv.getList(p.getName() + ".Main");
        List<ItemStack> armor = (List<ItemStack>) Main.getInstance().Inv.getList(p.getName() + ".Armor");

        // Check the cast
        try {
            for (@SuppressWarnings("unused")
            ItemStack i : main)
                ;

            for (@SuppressWarnings("unused")
            ItemStack i : armor)
                ;
        } catch (ClassCastException e) {

            p.sendMessage(ChatColor.RED + "There was an error restoring your inventory! :(");
            p.sendMessage(ChatColor.RED + "The admin has been notified.");
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Error: Unable to restore inventory of " + ChatColor.YELLOW + p.getName());
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Please inspect the inventorys.yml file.");
            e.printStackTrace();
        }

        p.getInventory().setContents(main.toArray(new ItemStack[36]));
        p.getInventory().setArmorContents(armor.toArray(new ItemStack[4]));

        Main.getInstance().Inv.set(p.getName(), null);
        ConfigManager.getInstance().saveYamls();
    }
}
