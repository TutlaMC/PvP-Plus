package net.tutla.pvpPlus.gui;

import net.tutla.pvpPlus.kit.Kit;
import net.tutla.pvpPlus.kit.KitManager;
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

public class KitSelectGui {

    private static final int PAGE_SIZE = 45; // 5 rows of 9, bottom row for navigation

    public static void open(Player player, KitManager kitManager, int page, Consumer<Kit> onSelect) {
        List<Kit> kits = new ArrayList<>(kitManager.getServerKits());
        int totalPages = Math.max(1, (int) Math.ceil(kits.size() / (double) PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54,
                TextUtil.parse("<dark_aqua>Select Kit <gray>(Page " + (page + 1) + "/" + totalPages + ")"));

        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && (start + i) < kits.size(); i++) {
            Kit kit = kits.get(start + i);
            ItemStack icon = kit.hasIcon()
                    ? kit.getIcon().clone()
                    : new ItemStack(Material.IRON_SWORD);
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(TextUtil.parse("<yellow>" + kit.getName()));
            meta.lore(List.of(
                    TextUtil.parse("<gray>Rounds: <white>" + kit.getDefaultRounds()),
                    TextUtil.parse("<gray>Click to select")
            ));
            icon.setItemMeta(meta);
            inv.setItem(i, icon);
        }

        // Navigation row (row 6)
        if (page > 0) {
            inv.setItem(45, makeNavItem(Material.ARROW, "<green>← Previous Page"));
        }
        if (page < totalPages - 1) {
            inv.setItem(53, makeNavItem(Material.ARROW, "<green>Next Page →"));
        }
        inv.setItem(49, makeNavItem(Material.BARRIER, "<red>Close"));

        player.openInventory(inv);
        GuiManager.register(player, new KitSelectGuiHandler(kits, page, totalPages, kitManager, onSelect));
    }

    private static ItemStack makeNavItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.parse(name));
        item.setItemMeta(meta);
        return item;
    }

    // Handler stored by GuiManager, called on click
    public record KitSelectGuiHandler(
            List<Kit> kits, int page, int totalPages,
            KitManager kitManager, Consumer<Kit> onSelect
    ) implements GuiHandler {
        @Override
        public void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            if (slot == 49) { player.closeInventory(); return; }
            if (slot == 45 && page > 0) {
                KitSelectGui.open(player, kitManager, page - 1, onSelect); return;
            }
            if (slot == 53 && page < totalPages - 1) {
                KitSelectGui.open(player, kitManager, page + 1, onSelect); return;
            }

            int kitIndex = page * PAGE_SIZE + slot;
            if (kitIndex < kits.size()) {
                onSelect.accept(kits.get(kitIndex));
            }
        }
    }
}