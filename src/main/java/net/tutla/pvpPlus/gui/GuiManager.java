package net.tutla.pvpPlus.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuiManager implements Listener {
    public record GuiSession(Inventory inventory, GuiHandler handler) {}
    private static final Map<UUID, GuiSession> handlers = new HashMap<>();

    public static void register(Player player, GuiHandler handler) {
        handlers.put(player.getUniqueId(), new GuiSession(player.getInventory(), handler));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        System.out.println(handlers);
        System.out.println(handlers.get(player.getUniqueId()));
        GuiSession session = handlers.get(player.getUniqueId());
        if (session == null) return;
        session.handler().onClick(event);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        handlers.remove(event.getPlayer().getUniqueId());
    }
}
