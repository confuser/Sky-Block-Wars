package me.kyle.burnett.SkyBlockWarriors.Listeners;

import me.kyle.burnett.SkyBlockWarriors.GameManager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

public class InventoryEvent implements Listener {

    @EventHandler
    public void inventoryEvent(InventoryClickEvent e) {
        if (e.getSlotType() == InventoryType.SlotType.ARMOR && GameManager.getInstance().isPlayerInGame((Player) e.getWhoClicked()) == true) {
            if (e.getRawSlot() == 5) {
                e.setCancelled(true);
            }
        }
    }
}
