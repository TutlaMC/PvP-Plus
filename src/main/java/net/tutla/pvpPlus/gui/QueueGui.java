package net.tutla.pvpPlus.gui;

import net.tutla.pvpPlus.kit.Kit;
import net.tutla.pvpPlus.kit.KitManager;
import net.tutla.pvpPlus.queue.QueueManager;
import net.tutla.pvpPlus.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class QueueGui {

    public static void open(Player player, QueueManager queueManager, KitManager kitManager) {
        queueManager.checkAndLeave(player);

        List<Kit> kits = new ArrayList<>(kitManager.getServerKits());
        int size = Math.max(9, (int)(Math.ceil(kits.size() / 9.0)) * 9);
        Inventory inv = Bukkit.createInventory(null, Math.min(54, size),
                TextUtil.parse("<gold>⚔ Queue — Select Kit"));

        for (int i = 0; i < kits.size() && i < 45; i++) {
            Kit kit = kits.get(i);
            int queueSize = queueManager.getQueueSize(kit.getName());
            boolean waiting = queueSize > 0;

            ItemStack icon = kit.hasIcon()
                    ? kit.getIcon().clone() : new ItemStack(Material.IRON_SWORD);
            icon.setAmount(Math.max(1, queueSize));

            ItemMeta meta = icon.getItemMeta();
            meta.displayName(TextUtil.parse("<yellow>" + kit.getName()));
            meta.lore(List.of(
                    TextUtil.parse("<gray>Players in queue: <white>" + queueSize),
                    TextUtil.parse("<gray>Rounds: <white>" + kit.getDefaultRounds()),
                    TextUtil.parse(waiting
                            ? "<green>Someone is waiting! Click to join."
                            : "<gray>Click to join queue")
            ));

            // Enchant glow if someone is waiting
            if (waiting) {
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            icon.setItemMeta(meta);
            inv.setItem(i, icon);
        }

        player.openInventory(inv);
        GuiManager.register(player, new QueueGuiHandler(kits, queueManager));
    }

    public record QueueGuiHandler(
            List<Kit> kits, QueueManager queueManager
    ) implements GuiHandler {
        @Override
        public void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();
            if (slot >= kits.size()) return;
            Kit kit = kits.get(slot);
            player.closeInventory();
            var result = queueManager.joinQueue(player, kit);
            player.sendMessage(switch (result) {
                case JOINED -> TextUtil.parse(
                        "<green>Joined queue for <yellow>" + kit.getName() + "</yellow>!");
                case ALREADY_IN_QUEUE -> TextUtil.parse("<red>You're already in a queue.");
                case IN_FIGHT -> TextUtil.parse("<red>You're in a fight.");
                case NO_KIT -> TextUtil.parse("<red>Invalid kit.");
            });
        }
    }
}
