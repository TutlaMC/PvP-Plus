package net.tutla.pvpPlus.gui;

import net.tutla.pvpPlus.arena.Arena;
import net.tutla.pvpPlus.arena.ArenaManager;
import net.tutla.pvpPlus.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ArenaSelectGui {

    private static final int PAGE_SIZE = 45;

    public static void open(Player player, ArenaManager arenaManager, int page, Consumer<Arena> onSelect) {
        List<Arena> arenas = new ArrayList<>(arenaManager.getAllArenas());
        int totalPages = Math.max(1, (int) Math.ceil(arenas.size() / (double) PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54,
                TextUtil.parse("<dark_aqua>Select Arena <gray>(Page " + (page + 1) + "/" + totalPages + ")"));

        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && (start + i) < arenas.size(); i++) {
            Arena arena = arenas.get(start + i);
            boolean available = !arena.isInUse();
            Material mat = available ? Material.GRASS_BLOCK : Material.BEDROCK;
            ItemStack icon = new ItemStack(mat);
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(TextUtil.parse(
                    (available ? "<green>" : "<red>") + arena.getName()));
            meta.lore(List.of(
                    TextUtil.parse(available ? "<gray>Available" : "<red>Currently in use"),
                    TextUtil.parse("<gray>Spawns: T1=" + arena.getTeam1Spawns().size() +
                            " T2=" + arena.getTeam2Spawns().size()),
                    available
                            ? TextUtil.parse("<yellow>Click to select")
                            : TextUtil.parse("<dark_gray>Unavailable")
            ));
            icon.setItemMeta(meta);
            inv.setItem(i, icon);
        }

        if (page > 0) inv.setItem(45, makeItem(Material.ARROW, "<green>← Previous"));
        if (page < totalPages - 1) inv.setItem(53, makeItem(Material.ARROW, "<green>Next →"));
        inv.setItem(49, makeItem(Material.BARRIER, "<red>Close"));

        player.openInventory(inv);
        GuiManager.register(player, new ArenaSelectGuiHandler(arenas, page, totalPages, arenaManager, onSelect));
    }

    private static ItemStack makeItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.parse(name));
        item.setItemMeta(meta);
        return item;
    }

    public record ArenaSelectGuiHandler(
            List<Arena> arenas, int page, int totalPages,
            ArenaManager arenaManager, Consumer<Arena> onSelect
    ) implements GuiHandler {
        @Override
        public void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            if (slot == 49) { player.closeInventory(); return; }
            if (slot == 45 && page > 0) {
                ArenaSelectGui.open(player, arenaManager, page - 1, onSelect); return;
            }
            if (slot == 53 && page < totalPages - 1) {
                ArenaSelectGui.open(player, arenaManager, page + 1, onSelect); return;
            }

            int index = page * PAGE_SIZE + slot;
            if (index < arenas.size() && !arenas.get(index).isInUse()) {
                onSelect.accept(arenas.get(index));
            }
        }
    }
}