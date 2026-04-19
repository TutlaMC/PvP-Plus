package net.tutla.pvpPlus.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuiManager implements Listener {

    private static final Map<UUID, GuiHandler> handlers = new HashMap<>();

    public static void register(Player player, GuiHandler handler) {
        handlers.put(player.getUniqueId(), handler);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        GuiHandler handler = handlers.get(player.getUniqueId());
        if (handler != null) handler.onClick(event);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        handlers.remove(event.getPlayer().getUniqueId());
    }
}
